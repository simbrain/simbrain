package org.simbrain.network.llm

import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.TensorRole
import org.simbrain.network.tensor.op.AddOp
import org.simbrain.network.tensor.op.BiasOp
import org.simbrain.network.tensor.op.CausalMaskedRowSoftmaxOp
import org.simbrain.network.tensor.op.Gradients
import org.simbrain.network.tensor.op.HeadMixOp
import org.simbrain.network.tensor.op.HeadScoresOp
import org.simbrain.network.tensor.op.LayerNormOp
import org.simbrain.network.tensor.op.MatMulLinearOp
import org.simbrain.network.tensor.op.MergeHeadsOp
import org.simbrain.network.tensor.op.OpPlan
import org.simbrain.network.tensor.op.ReLUOp
import org.simbrain.network.tensor.op.SeqEmbedOp
import org.simbrain.network.tensor.op.SeqSoftmaxCrossEntropyOp
import org.simbrain.network.tensor.op.SplitHeadsOp
import org.simbrain.network.tensor.op.Tape
import org.simbrain.network.tensor.op.TensorAdam
import org.simbrain.network.tensor.op.TensorOp
import org.simbrain.network.tensor.op.TensorPort
import kotlin.math.sqrt
import kotlin.random.Random

data class TeachingTransformerConfig(
    val contextSize: Int = 24,
    val embedDim: Int = 20,
    val numHeads: Int = 4,
    val hiddenDim: Int = 30,
    val vocabSize: Int = 100,
    val numLayers: Int = 1,
    val normEps: Float = 1e-5f,
) {
    val headDim get() = embedDim / numHeads

    init {
        require(embedDim % numHeads == 0) { "embedDim $embedDim not divisible by $numHeads heads" }
    }
}

/**
 * Headless GPT-style teaching transformer, expressed as one explicit [OpPlan] spanning
 * embed -> (attention -> MLP) x numLayers -> unembed -> sequence cross-entropy. There is no
 * block class: a "layer" is a `layers.<l>.*` range of port names, so [org.simbrain.network.tensor.op.OpPlan.cursor]
 * micro-steps the whole model and one [Tape] records the full loss-to-param path.
 *
 * Unlike [Lfm2Model] this processes the FULL context each forward (no caches) and owns its
 * randomly initialized parameters, trained in place via [Tape] + [TensorAdam]. Pre-norm
 * residuals: the residual trunk is a pure identity path (the visual spine's skip connection);
 * layer norm sits at the entry of each attention/MLP limb.
 */
class TeachingTransformerModel(val config: TeachingTransformerConfig, seed: Long = 42L) {

    private val rng = Random(seed)

    /** Parameters by stable name — the Adam moment keys and the serialization keys. */
    val params = LinkedHashMap<String, TensorPort>()

    private fun param(name: String, rows: Int, cols: Int, init: (Int) -> Float): TensorPort {
        val tensor = FloatTensor(rows, cols, TensorRole.PARAMETER)
        for (i in 0 until tensor.size) tensor.data.put(i, init(i))
        tensor.markMutated()
        return TensorPort(name, tensor).also { params[name] = it }
    }

    private fun weightParam(name: String, rows: Int, cols: Int): TensorPort {
        val bound = 1f / sqrt(cols.toFloat())
        return param(name, rows, cols) { (rng.nextFloat() * 2f - 1f) * bound }
    }

    private fun onesParam(name: String, size: Int) = param(name, 1, size) { 1f }

    private fun zerosParam(name: String, size: Int) = param(name, 1, size) { 0f }

    private fun workspace(name: String, rows: Int, cols: Int) =
        TensorPort(name, FloatTensor(rows, cols).apply { fill(0f) })

    private lateinit var embedOp: SeqEmbedOp
    private lateinit var ceOp: SeqSoftmaxCrossEntropyOp

    val plan: OpPlan = buildPlan()

    val loss: TensorPort get() = plan.port("loss")

    /** Per-position next-token distributions, seq x vocab. */
    val probs: TensorPort get() = plan.port("probs")

    val tape = Tape()
    val grads = Gradients()
    val adam = TensorAdam()

    private fun buildPlan(): OpPlan {
        val c = config
        val seq = c.contextSize
        val dim = c.embedDim
        val ops = ArrayList<TensorOp>()

        val embedTable = weightParam("embed.table", c.vocabSize, dim)
        val posTable = weightParam("embed.pos", seq, dim)
        val embedded = workspace("embed", seq, dim)
        embedOp = SeqEmbedOp("embed", embedTable, embedded)
        ops += embedOp
        var resid = workspace("resid0", seq, dim)
        ops += AddOp("add_pos", embedded, posTable, resid)

        for (l in 0 until c.numLayers) {
            val prefix = "layers.$l"

            val attnNormed = workspace("$prefix.attn.normed", seq, dim)
            ops += LayerNormOp("$prefix.attn.norm", resid,
                onesParam("$prefix.attn.norm.gamma", dim), zerosParam("$prefix.attn.norm.beta", dim),
                attnNormed, c.normEps)
            val q = workspace("$prefix.attn.q", seq, dim)
            val k = workspace("$prefix.attn.k", seq, dim)
            val v = workspace("$prefix.attn.v", seq, dim)
            ops += MatMulLinearOp("$prefix.attn.q_proj", weightParam("$prefix.attn.wq", dim, dim), attnNormed, q)
            ops += MatMulLinearOp("$prefix.attn.k_proj", weightParam("$prefix.attn.wk", dim, dim), attnNormed, k)
            ops += MatMulLinearOp("$prefix.attn.v_proj", weightParam("$prefix.attn.wv", dim, dim), attnNormed, v)
            val qHeads = workspace("$prefix.attn.q_heads", c.numHeads * seq, c.headDim)
            val kHeads = workspace("$prefix.attn.k_heads", c.numHeads * seq, c.headDim)
            val vHeads = workspace("$prefix.attn.v_heads", c.numHeads * seq, c.headDim)
            ops += SplitHeadsOp("$prefix.attn.q_split", q, qHeads, c.numHeads)
            ops += SplitHeadsOp("$prefix.attn.k_split", k, kHeads, c.numHeads)
            ops += SplitHeadsOp("$prefix.attn.v_split", v, vHeads, c.numHeads)
            val scores = workspace("$prefix.attn.scores", c.numHeads * seq, seq)
            ops += HeadScoresOp("$prefix.attn.score", qHeads, kHeads, scores, c.numHeads)
            val attnWeights = workspace("$prefix.attn.weights", c.numHeads * seq, seq)
            ops += CausalMaskedRowSoftmaxOp("$prefix.attn.softmax", scores, attnWeights, c.numHeads)
            val mixed = workspace("$prefix.attn.mixed", c.numHeads * seq, c.headDim)
            ops += HeadMixOp("$prefix.attn.mix", attnWeights, vHeads, mixed, c.numHeads)
            val merged = workspace("$prefix.attn.merged", seq, dim)
            ops += MergeHeadsOp("$prefix.attn.merge", mixed, merged, c.numHeads)
            val attnOut = workspace("$prefix.attn.out", seq, dim)
            ops += MatMulLinearOp("$prefix.attn.out_proj", weightParam("$prefix.attn.wo", dim, dim), merged, attnOut)
            val attnResid = workspace("$prefix.attn_resid", seq, dim)
            ops += AddOp("$prefix.attn_residual", resid, attnOut, attnResid)

            val mlpNormed = workspace("$prefix.mlp.normed", seq, dim)
            ops += LayerNormOp("$prefix.mlp.norm", attnResid,
                onesParam("$prefix.mlp.norm.gamma", dim), zerosParam("$prefix.mlp.norm.beta", dim),
                mlpNormed, c.normEps)
            val hiddenRaw = workspace("$prefix.mlp.hidden_raw", seq, c.hiddenDim)
            val hidden = workspace("$prefix.mlp.hidden", seq, c.hiddenDim)
            val act = workspace("$prefix.mlp.act", seq, c.hiddenDim)
            val outRaw = workspace("$prefix.mlp.out_raw", seq, dim)
            val mlpOut = workspace("$prefix.mlp.out", seq, dim)
            ops += MatMulLinearOp("$prefix.mlp.up_proj", weightParam("$prefix.mlp.w1", c.hiddenDim, dim), mlpNormed, hiddenRaw)
            ops += BiasOp("$prefix.mlp.up_bias", hiddenRaw, zerosParam("$prefix.mlp.b1", c.hiddenDim), hidden)
            ops += ReLUOp("$prefix.mlp.relu", hidden, act)
            ops += MatMulLinearOp("$prefix.mlp.down_proj", weightParam("$prefix.mlp.w2", dim, c.hiddenDim), act, outRaw)
            ops += BiasOp("$prefix.mlp.down_bias", outRaw, zerosParam("$prefix.mlp.b2", dim), mlpOut)
            val layerResid = workspace("$prefix.resid", seq, dim)
            ops += AddOp("$prefix.residual", attnResid, mlpOut, layerResid)
            resid = layerResid
        }

        val finalNormed = workspace("final_normed", seq, dim)
        ops += LayerNormOp("final_norm", resid,
            onesParam("final_norm.gamma", dim), zerosParam("final_norm.beta", dim),
            finalNormed, c.normEps)
        val logits = workspace("logits", seq, c.vocabSize)
        ops += MatMulLinearOp("unembed", weightParam("unembed.weight", c.vocabSize, dim), finalNormed, logits)
        ceOp = SeqSoftmaxCrossEntropyOp("cross_entropy", logits,
            workspace("probs", seq, c.vocabSize), workspace("loss", 1, 1))
        ops += ceOp

        return OpPlan(ops)
    }

    /**
     * Binds one sample: [tokens] fill the context from position 0 (shorter contexts pad with -1,
     * which embeds to zero rows), [targets] supervise matching positions (-1 = unsupervised).
     */
    fun setSample(tokens: IntArray, targets: IntArray = IntArray(0)) {
        require(tokens.size <= config.contextSize) { "${tokens.size} tokens > context ${config.contextSize}" }
        require(targets.isEmpty() || targets.size == tokens.size) {
            "targets size ${targets.size} != tokens size ${tokens.size}"
        }
        embedOp.tokenIds = IntArray(config.contextSize) { tokens.getOrElse(it) { -1 } }
        ceOp.targetIds = IntArray(config.contextSize) { targets.getOrElse(it) { -1 } }
    }

    /** Full forward pass on the bound sample; returns the loss (0 when nothing is supervised). */
    fun forward(): Float {
        plan.forward()
        return loss.tensor.data.get(0)
    }

    /** One tape-recorded forward + backward + Adam update on [tokens]/[targets]; returns the loss. */
    fun trainStep(tokens: IntArray, targets: IntArray): Float {
        setSample(tokens, targets)
        tape.clear()
        grads.zeroAll()
        plan.forward(tape)
        val lossValue = loss.tensor.data.get(0)
        tape.backward(loss, grads)
        applyOptimizer()
        return lossValue
    }

    private fun applyOptimizer() {
        adam.step()
        params.forEach { (name, port) -> adam.update(name, port.tensor, grads.of(port.tensor)) }
    }

    enum class StepPhase { IDLE, FORWARD, BACKWARD }

    /** Where a micro-stepped training step currently is; [stepOp] advances through the phases. */
    var stepPhase = StepPhase.IDLE
        private set

    /**
     * Arms a micro-stepped training step on [tokens]/[targets]: subsequent [stepOp] calls run one
     * op at a time — the whole forward pass, then every VJP in reverse, then the Adam update —
     * with [stepPhase] tracking where the walk is.
     */
    fun beginSteppedTrainStep(tokens: IntArray, targets: IntArray) {
        check(stepPhase == StepPhase.IDLE) { "A stepped train step is already in progress" }
        check(plan.cursor == 0) { "Plan is mid-pass at op ${plan.cursor}" }
        setSample(tokens, targets)
        tape.clear()
        grads.zeroAll()
        stepPhase = StepPhase.FORWARD
    }

    /** The op the next [stepOp] will run, or null when idle. */
    fun nextOp(): TensorOp? = when (stepPhase) {
        StepPhase.IDLE -> null
        StepPhase.FORWARD -> plan.ops[plan.cursor]
        StepPhase.BACKWARD -> tape.peekBackward()
    }

    /**
     * Advances a stepped training step by one op. Forward completion arms the backward pass;
     * backward completion applies the optimizer and returns to idle.
     */
    fun stepOp(): TensorOp {
        return when (stepPhase) {
            StepPhase.IDLE -> error("No stepped train step in progress; call beginSteppedTrainStep")
            StepPhase.FORWARD -> plan.stepOp(tape).also {
                if (plan.cursor == 0) {
                    tape.beginBackward(loss, grads)
                    stepPhase = StepPhase.BACKWARD
                }
            }
            StepPhase.BACKWARD -> tape.stepBackward(grads).also {
                if (!tape.isBackwardInProgress) {
                    applyOptimizer()
                    stepPhase = StepPhase.IDLE
                }
            }
        }
    }

    /**
     * Advances a plain inference pass by one op (no tape, no gradients) — token/layer/op-level
     * stepping outside training. Only valid when no stepped training step is active.
     */
    fun stepForwardOnly(): TensorOp {
        check(stepPhase == StepPhase.IDLE) { "A stepped train step is in progress" }
        return plan.stepOp()
    }

    /** Next-token distribution at [position] (the row of [probs] the next token is sampled from). */
    fun distributionAt(position: Int): FloatArray {
        val p = probs.tensor
        return FloatArray(p.cols) { p[position, it] }
    }
}
