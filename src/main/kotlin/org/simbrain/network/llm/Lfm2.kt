package org.simbrain.network.llm

import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.openblas.global.openblas_nolapack.CblasNoTrans
import org.bytedeco.openblas.global.openblas_nolapack.CblasRowMajor
import org.bytedeco.openblas.global.openblas_nolapack.CblasTrans
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sgemv
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.matvec
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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
 * Headless LFM2 forward pass on the FloatTensor substrate: token-by-token decode through the
 * 14-layer stack (gated short-conv and GQA attention mixers, SwiGLU MLPs, pre-norm residuals),
 * with rolling conv caches and KV caches. Mirrors the reference `modeling_lfm2.py` math in f32.
 *
 * Weights come from [Safetensors.load] keyed by the file's names; the unembedding is tied to
 * `model.embed_tokens.weight`. All workspaces are preallocated — steady-state decode allocates
 * nothing per token.
 */
class Lfm2Model(val config: Lfm2Config, private val params: Map<String, FloatTensor>) {

    private fun param(name: String) = params[name] ?: error("Missing parameter $name")

    private val embedTokens = param("model.embed_tokens.weight")
    private val embeddingNorm = param("model.embedding_norm.weight")

    private inner class Layer(idx: Int) {
        val isAttention = idx in config.attentionLayers
        val operatorNorm = param("model.layers.$idx.operator_norm.weight")
        val ffnNorm = param("model.layers.$idx.ffn_norm.weight")
        val w1 = param("model.layers.$idx.feed_forward.w1.weight")
        val w2 = param("model.layers.$idx.feed_forward.w2.weight")
        val w3 = param("model.layers.$idx.feed_forward.w3.weight")
        val qProj = if (isAttention) param("model.layers.$idx.self_attn.q_proj.weight") else null
        val kProj = if (isAttention) param("model.layers.$idx.self_attn.k_proj.weight") else null
        val vProj = if (isAttention) param("model.layers.$idx.self_attn.v_proj.weight") else null
        val outProj = if (isAttention) param("model.layers.$idx.self_attn.out_proj.weight") else null
        val qNorm = if (isAttention) param("model.layers.$idx.self_attn.q_layernorm.weight") else null
        val kNorm = if (isAttention) param("model.layers.$idx.self_attn.k_layernorm.weight") else null
        val convWeight = if (!isAttention) param("model.layers.$idx.conv.conv.weight") else null
        val convInProj = if (!isAttention) param("model.layers.$idx.conv.in_proj.weight") else null
        val convOutProj = if (!isAttention) param("model.layers.$idx.conv.out_proj.weight") else null

        val kCache = if (isAttention) FloatTensor(config.maxSeqLen, config.kvDim) else null
        val vCache = if (isAttention) FloatTensor(config.maxSeqLen, config.kvDim) else null
        val convCache = if (!isAttention) FloatTensor(config.hiddenSize, config.convKernel).apply { fill(0f) } else null
    }

    private val layers = List(config.numLayers) { Layer(it) }

    private val x = FloatTensor(1, config.hiddenSize)
    private val normed = FloatTensor(1, config.hiddenSize)
    private val bcx = FloatTensor(1, 3 * config.hiddenSize)
    private val gated = FloatTensor(1, config.hiddenSize)
    private val q = FloatTensor(1, config.numHeads * config.headDim)
    private val k = FloatTensor(1, config.kvDim)
    private val v = FloatTensor(1, config.kvDim)
    private val scores = FloatTensor(1, config.maxSeqLen)
    private val attnOut = FloatTensor(1, config.numHeads * config.headDim)
    private val mlpGate = FloatTensor(1, config.intermediateSize)
    private val mlpUp = FloatTensor(1, config.intermediateSize)
    private val logits = FloatTensor(1, config.vocabSize)

    private val invFreq = DoubleArray(config.headDim / 2) { i ->
        1.0 / config.ropeTheta.pow(2.0 * i / config.headDim)
    }
    private val ropeCos = FloatArray(config.headDim / 2)
    private val ropeSin = FloatArray(config.headDim / 2)

    var position = 0
        private set

    fun reset() {
        position = 0
        layers.forEach { it.convCache?.fill(0f) }
    }

    /**
     * Runs one token through the stack at the current position and returns the logits workspace
     * (valid until the next call). [captureHidden] receives the residual stream as heap copies:
     * index 0 = embedding, index i = raw output of layer i, index numLayers + 1 = the final
     * embedding_norm output. (Transformers' `output_hidden_states` list differs in one spot:
     * its last entry is the post-norm state, not the raw final-layer residual.)
     */
    fun forwardToken(tokenId: Int, captureHidden: ((Int, FloatArray) -> Unit)? = null): FloatTensor {
        require(tokenId in 0 until config.vocabSize) { "Token id $tokenId out of vocab" }
        check(position < config.maxSeqLen) { "KV cache full at $position (maxSeqLen ${config.maxSeqLen})" }

        x.data.duplicate().put(embedTokens.data.slice(tokenId * config.hiddenSize, config.hiddenSize))
        x.markMutated()
        captureHidden?.invoke(0, x.toFloatArray())

        val half = config.headDim / 2
        for (i in 0 until half) {
            val angle = position * invFreq[i]
            ropeCos[i] = cos(angle).toFloat()
            ropeSin[i] = sin(angle).toFloat()
        }

        layers.forEachIndexed { layerIdx, layer ->
            rmsNormInto(x.data, 0, config.hiddenSize, layer.operatorNorm.data, normed.data, 0)
            normed.markMutated()
            if (layer.isAttention) attentionMixer(layer) else convMixer(layer)

            rmsNormInto(x.data, 0, config.hiddenSize, layer.ffnNorm.data, normed.data, 0)
            normed.markMutated()
            matvec(layer.w1, normed, mlpGate)
            matvec(layer.w3, normed, mlpUp)
            val g = mlpGate.data
            for (i in 0 until config.intermediateSize) {
                val a = g.get(i)
                g.put(i, a / (1f + exp(-a)) * mlpUp.data.get(i))
            }
            mlpGate.markMutated()
            matvec(layer.w2, mlpGate, x, beta = 1f)

            captureHidden?.invoke(layerIdx + 1, x.toFloatArray())
        }

        rmsNormInto(x.data, 0, config.hiddenSize, embeddingNorm.data, normed.data, 0)
        normed.markMutated()
        captureHidden?.invoke(config.numLayers + 1, normed.toFloatArray())
        matvec(embedTokens, normed, logits)

        position++
        return logits
    }

    /** x += out_proj(attention(operator_norm(x))) for one decode step. */
    private fun attentionMixer(layer: Layer) {
        matvec(layer.qProj!!, normed, q)
        matvec(layer.kProj!!, normed, k)
        matvec(layer.vProj!!, normed, v)

        for (h in 0 until config.numHeads) {
            rmsNormInto(q.data, h * config.headDim, config.headDim, layer.qNorm!!.data, q.data, h * config.headDim)
        }
        for (h in 0 until config.numKvHeads) {
            rmsNormInto(k.data, h * config.headDim, config.headDim, layer.kNorm!!.data, k.data, h * config.headDim)
        }
        for (h in 0 until config.numHeads) applyRope(q.data, h * config.headDim)
        for (h in 0 until config.numKvHeads) applyRope(k.data, h * config.headDim)
        q.markMutated()
        k.markMutated()

        val kCache = layer.kCache!!
        val vCache = layer.vCache!!
        kCache.data.duplicate().also { it.position(position * config.kvDim) }.put(k.data.duplicate())
        vCache.data.duplicate().also { it.position(position * config.kvDim) }.put(v.data.duplicate())
        kCache.markMutated()
        vCache.markMutated()

        val seen = position + 1
        val scale = 1f / sqrt(config.headDim.toFloat())
        val groupSize = config.numHeads / config.numKvHeads
        for (h in 0 until config.numHeads) {
            val kvHead = h / groupSize
            val headOffset = (kvHead * config.headDim).toLong()
            cblas_sgemv(
                CblasRowMajor, CblasNoTrans, seen, config.headDim, scale,
                FloatPointer(kCache.pointer).position(headOffset), config.kvDim,
                FloatPointer(q.pointer).position((h * config.headDim).toLong()), 1,
                0f, scores.pointer, 1
            )
            softmaxInPlace(scores.data, seen)
            cblas_sgemv(
                CblasRowMajor, CblasTrans, seen, config.headDim, 1f,
                FloatPointer(vCache.pointer).position(headOffset), config.kvDim,
                scores.pointer, 1,
                0f, FloatPointer(attnOut.pointer).position((h * config.headDim).toLong()), 1
            )
        }
        scores.markMutated()
        attnOut.markMutated()

        matvec(layer.outProj!!, attnOut, x, beta = 1f)
    }

    /** x += out_proj(C * conv(B * xProj)) for one decode step, rolling the k-wide conv cache. */
    private fun convMixer(layer: Layer) {
        matvec(layer.convInProj!!, normed, bcx)
        val h = config.hiddenSize
        val b = bcx.data
        for (i in 0 until h) {
            gated.data.put(i, b.get(i) * b.get(2 * h + i))
        }
        gated.markMutated()

        val cache = layer.convCache!!.data
        val w = layer.convWeight!!.data
        val kk = config.convKernel
        for (c in 0 until h) {
            val base = c * kk
            var acc = 0f
            for (j in 0 until kk - 1) {
                val shifted = cache.get(base + j + 1)
                cache.put(base + j, shifted)
                acc += shifted * w.get(base + j)
            }
            val newest = gated.data.get(c)
            cache.put(base + kk - 1, newest)
            acc += newest * w.get(base + kk - 1)
            gated.data.put(c, acc * b.get(h + c))
        }
        layer.convCache!!.markMutated()
        gated.markMutated()

        matvec(layer.convOutProj!!, gated, x, beta = 1f)
    }

    private fun rmsNormInto(src: FloatBuffer, srcOffset: Int, n: Int, weight: FloatBuffer, dst: FloatBuffer, dstOffset: Int) {
        var sumSq = 0f
        for (i in 0 until n) {
            val a = src.get(srcOffset + i)
            sumSq += a * a
        }
        val inv = 1f / sqrt(sumSq / n + config.normEps)
        for (i in 0 until n) {
            dst.put(dstOffset + i, src.get(srcOffset + i) * inv * weight.get(i))
        }
    }

    /** Rotate-half RoPE over one head at the precomputed position angles. */
    private fun applyRope(buf: FloatBuffer, offset: Int) {
        val half = config.headDim / 2
        for (i in 0 until half) {
            val a = buf.get(offset + i)
            val bVal = buf.get(offset + half + i)
            buf.put(offset + i, a * ropeCos[i] - bVal * ropeSin[i])
            buf.put(offset + half + i, bVal * ropeCos[i] + a * ropeSin[i])
        }
    }

    private fun softmaxInPlace(buf: FloatBuffer, n: Int) {
        var max = Float.NEGATIVE_INFINITY
        for (i in 0 until n) max = maxOf(max, buf.get(i))
        var sum = 0f
        for (i in 0 until n) {
            val e = exp(buf.get(i) - max)
            buf.put(i, e)
            sum += e
        }
        val inv = 1f / sum
        for (i in 0 until n) buf.put(i, buf.get(i) * inv)
    }
}
