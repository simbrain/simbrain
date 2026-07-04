package org.simbrain.network.llm

import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.op.HookHandle
import org.simbrain.network.tensor.op.LinearOp
import org.simbrain.network.tensor.op.AddOp
import org.simbrain.network.tensor.op.OpPlan
import org.simbrain.network.tensor.op.RmsNormOp
import org.simbrain.network.tensor.op.SiluGateOp
import org.simbrain.network.tensor.op.TensorOp
import org.simbrain.network.tensor.op.TensorPort
import kotlin.math.pow

/**
 * LFM2 model family hyperparameters. Defaults are LFM2.5-230M. [maxSeqLen] sizes the KV caches
 * (an allocation bound, not the model's 128k position limit).
 */
data class Lfm2Config(
    val hiddenSize: Int = 1024,
    val numLayers: Int = 14,
    val attentionLayers: Set<Int> = setOf(2, 4, 6, 8, 10, 12),
    val numHeads: Int = 16,
    val numKvHeads: Int = 8,
    val headDim: Int = 64,
    val intermediateSize: Int = 2560,
    val vocabSize: Int = 65536,
    val ropeTheta: Double = 1_000_000.0,
    val normEps: Float = 1e-5f,
    val convKernel: Int = 3,
    val maxSeqLen: Int = 2048,
) {
    val kvDim get() = numKvHeads * headDim
}

/**
 * Headless LFM2 forward pass, expressed as an explicit [OpPlan]: token-by-token decode through
 * the 14-layer stack (gated short-conv and GQA attention mixers, SwiGLU MLPs, pre-norm
 * residuals) with rolling conv caches and KV caches. Mirrors the reference `modeling_lfm2.py`
 * math in f32.
 *
 * Every intermediate is a named [TensorPort] with per-layer workspaces (`layers.3.attn.weights`,
 * `layers.5.conv.gated`, ...), so probes hook values via [onPort], the renderer sees per-tensor
 * dirty versions, and [plan] can be micro-stepped op by op. Weights come from [Safetensors.load]
 * keyed by the file's names; the unembedding is tied to `model.embed_tokens.weight`. All
 * workspaces are preallocated — steady-state decode allocates nothing per token.
 */
class Lfm2Model(val config: Lfm2Config, private val params: Map<String, FloatTensor>) {

    private fun paramPort(name: String) =
        TensorPort(name, params[name] ?: error("Missing parameter $name"))

    private fun workspace(name: String, cols: Int, rows: Int = 1) =
        TensorPort(name, FloatTensor(rows, cols).apply { fill(0f) })

    private val state = Lfm2DecodeState()

    private val embedPort = paramPort("model.embed_tokens.weight")
    private val convCaches = ArrayList<FloatTensor>()

    val plan: OpPlan = buildPlan()

    val logits: TensorPort get() = plan.port("logits")

    val position get() = state.position

    private fun buildPlan(): OpPlan {
        val c = config
        val ops = ArrayList<TensorOp>()
        val invFreq = DoubleArray(c.headDim / 2) { i -> 1.0 / c.ropeTheta.pow(2.0 * i / c.headDim) }

        var resid = workspace("embed", c.hiddenSize)
        ops += EmbedLookupOp("embed_lookup", embedPort, resid, state)

        val ropeCos = workspace("rope.cos", c.headDim / 2)
        val ropeSin = workspace("rope.sin", c.headDim / 2)
        ops += RopeAnglesOp("rope_angles", ropeCos, ropeSin, invFreq, state)

        for (i in 0 until c.numLayers) {
            val prefix = "layers.$i"
            val weightPrefix = "model.layers.$i"

            val normed = workspace("$prefix.operator_normed", c.hiddenSize)
            ops += RmsNormOp("$prefix.operator_norm", resid,
                paramPort("$weightPrefix.operator_norm.weight"), normed, c.normEps)

            val mixerOut = if (i in c.attentionLayers) {
                attentionOps(ops, prefix, weightPrefix, normed, ropeCos, ropeSin)
            } else {
                convOps(ops, prefix, weightPrefix, normed)
            }

            val mixerResid = workspace("$prefix.mixer_resid", c.hiddenSize)
            ops += AddOp("$prefix.mixer_residual", resid, mixerOut, mixerResid)

            val ffnNormed = workspace("$prefix.ffn_normed", c.hiddenSize)
            ops += RmsNormOp("$prefix.ffn_norm", mixerResid,
                paramPort("$weightPrefix.ffn_norm.weight"), ffnNormed, c.normEps)
            val mlpGate = workspace("$prefix.mlp.gate", c.intermediateSize)
            val mlpUp = workspace("$prefix.mlp.up", c.intermediateSize)
            val mlpAct = workspace("$prefix.mlp.act", c.intermediateSize)
            val mlpOut = workspace("$prefix.mlp.out", c.hiddenSize)
            ops += LinearOp("$prefix.mlp.w1", paramPort("$weightPrefix.feed_forward.w1.weight"), ffnNormed, mlpGate)
            ops += LinearOp("$prefix.mlp.w3", paramPort("$weightPrefix.feed_forward.w3.weight"), ffnNormed, mlpUp)
            ops += SiluGateOp("$prefix.mlp.silu_gate", mlpGate, mlpUp, mlpAct)
            ops += LinearOp("$prefix.mlp.w2", paramPort("$weightPrefix.feed_forward.w2.weight"), mlpAct, mlpOut)

            val layerResid = workspace("$prefix.resid", c.hiddenSize)
            ops += AddOp("$prefix.residual", mixerResid, mlpOut, layerResid)
            resid = layerResid
        }

        val finalNormed = workspace("final_norm", c.hiddenSize)
        ops += RmsNormOp("embedding_norm", resid,
            paramPort("model.embedding_norm.weight"), finalNormed, c.normEps)
        ops += LinearOp("unembed", embedPort, finalNormed, workspace("logits", c.vocabSize))

        return OpPlan(ops)
    }

    private fun attentionOps(
        ops: MutableList<TensorOp>,
        prefix: String,
        weightPrefix: String,
        normed: TensorPort,
        ropeCos: TensorPort,
        ropeSin: TensorPort,
    ): TensorPort {
        val c = config
        val qRaw = workspace("$prefix.attn.q_raw", c.numHeads * c.headDim)
        val kRaw = workspace("$prefix.attn.k_raw", c.kvDim)
        val v = workspace("$prefix.attn.v", c.kvDim)
        ops += LinearOp("$prefix.attn.q_proj", paramPort("$weightPrefix.self_attn.q_proj.weight"), normed, qRaw)
        ops += LinearOp("$prefix.attn.k_proj", paramPort("$weightPrefix.self_attn.k_proj.weight"), normed, kRaw)
        ops += LinearOp("$prefix.attn.v_proj", paramPort("$weightPrefix.self_attn.v_proj.weight"), normed, v)

        val q = workspace("$prefix.attn.q", c.numHeads * c.headDim)
        val k = workspace("$prefix.attn.k", c.kvDim)
        ops += HeadwiseNormRopeOp("$prefix.attn.q_norm_rope", qRaw,
            paramPort("$weightPrefix.self_attn.q_layernorm.weight"), ropeCos, ropeSin, q,
            c.numHeads, c.headDim, c.normEps)
        ops += HeadwiseNormRopeOp("$prefix.attn.k_norm_rope", kRaw,
            paramPort("$weightPrefix.self_attn.k_layernorm.weight"), ropeCos, ropeSin, k,
            c.numKvHeads, c.headDim, c.normEps)

        val kCache = workspace("$prefix.attn.k_cache", c.kvDim, rows = c.maxSeqLen)
        val vCache = workspace("$prefix.attn.v_cache", c.kvDim, rows = c.maxSeqLen)
        ops += CacheWriteOp("$prefix.attn.k_cache_write", k, kCache, state)
        ops += CacheWriteOp("$prefix.attn.v_cache_write", v, vCache, state)

        val weights = workspace("$prefix.attn.weights", c.maxSeqLen, rows = c.numHeads)
        ops += AttendScoresOp("$prefix.attn.scores", q, kCache, weights, state,
            c.numHeads, c.numKvHeads, c.headDim)
        val context = workspace("$prefix.attn.context", c.numHeads * c.headDim)
        ops += AttendMixOp("$prefix.attn.mix", weights, vCache, context, state,
            c.numHeads, c.numKvHeads, c.headDim)

        val attnOut = workspace("$prefix.attn.out", c.hiddenSize)
        ops += LinearOp("$prefix.attn.out_proj", paramPort("$weightPrefix.self_attn.out_proj.weight"), context, attnOut)
        return attnOut
    }

    private fun convOps(
        ops: MutableList<TensorOp>,
        prefix: String,
        weightPrefix: String,
        normed: TensorPort,
    ): TensorPort {
        val c = config
        val bcx = workspace("$prefix.conv.bcx", 3 * c.hiddenSize)
        ops += LinearOp("$prefix.conv.in_proj", paramPort("$weightPrefix.conv.in_proj.weight"), normed, bcx)

        val bx = workspace("$prefix.conv.bx", c.hiddenSize)
        ops += OffsetGateOp("$prefix.conv.b_gate", bcx, 0, bcx, 2 * c.hiddenSize, bx)

        val cache = workspace("$prefix.conv.cache", c.convKernel, rows = c.hiddenSize)
        convCaches.add(cache.tensor)
        val convRaw = workspace("$prefix.conv.raw", c.hiddenSize)
        ops += CausalConvOp("$prefix.conv.causal_conv", bx, cache,
            paramPort("$weightPrefix.conv.conv.weight"), convRaw)

        val gated = workspace("$prefix.conv.gated", c.hiddenSize)
        ops += OffsetGateOp("$prefix.conv.c_gate", convRaw, 0, bcx, c.hiddenSize, gated)

        val convOut = workspace("$prefix.conv.out", c.hiddenSize)
        ops += LinearOp("$prefix.conv.out_proj", paramPort("$weightPrefix.conv.out_proj.weight"), gated, convOut)
        return convOut
    }

    /**
     * Runs one token through the plan at the current position and returns the logits workspace
     * (valid until the next call). Intermediate values are exposed on named ports — see [onPort].
     */
    fun forwardToken(tokenId: Int): FloatTensor {
        require(tokenId in 0 until config.vocabSize) { "Token id $tokenId out of vocab" }
        check(state.position < config.maxSeqLen) {
            "KV cache full at ${state.position} (maxSeqLen ${config.maxSeqLen})"
        }
        state.tokenId = tokenId
        plan.forward()
        state.position++
        return logits.tensor
    }

    /**
     * Registers a probe-style hook that fires with the port's fresh value each time its op runs.
     * Residual-stream ports: `embed`, `layers.<i>.resid`, `final_norm`; see the plan for all.
     */
    fun onPort(name: String, hook: (TensorPort) -> Unit): HookHandle = plan.onPort(name, hook)

    fun reset() {
        state.position = 0
        convCaches.forEach { it.fill(0f) }
    }
}
