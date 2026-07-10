package org.simbrain.network.llm

import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.openblas.global.openblas_nolapack.CblasNoTrans
import org.bytedeco.openblas.global.openblas_nolapack.CblasRowMajor
import org.bytedeco.openblas.global.openblas_nolapack.CblasTrans
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sgemv
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import org.simbrain.network.tensor.op.TensorOp
import org.simbrain.network.tensor.op.TensorPort

/**
 * Decode-loop state shared by the position-dependent ops: which token is being fed and where it
 * lands in the caches. The model advances [position] after each full pass.
 */
class Lfm2DecodeState {
    var tokenId = 0
    var position = 0
}

/** Copies the embedding row for the current token into [out]. */
class EmbedLookupOp(
    name: String,
    val embed: TensorPort,
    val out: TensorPort,
    private val state: Lfm2DecodeState,
) : TensorOp(name) {

    override val inputs = listOf(embed)
    override val outputs = listOf(out)

    override fun forward() {
        val hidden = out.tensor.size
        out.tensor.data.duplicate().put(embed.tensor.data.slice(state.tokenId * hidden, hidden))
    }
}

/** Fills the per-position rotary cos/sin tables read by every attention layer this token. */
class RopeAnglesOp(
    name: String,
    val cosOut: TensorPort,
    val sinOut: TensorPort,
    private val invFreq: DoubleArray,
    private val state: Lfm2DecodeState,
) : TensorOp(name) {

    override val inputs = emptyList<TensorPort>()
    override val outputs = listOf(cosOut, sinOut)

    override fun forward() {
        for (i in invFreq.indices) {
            val angle = state.position * invFreq[i]
            cosOut.tensor.data.put(i, cos(angle).toFloat())
            sinOut.tensor.data.put(i, sin(angle).toFloat())
        }
    }
}

/**
 * Per-head QK-RMSNorm (one [weight] of headDim, shared across heads) followed by rotate-half
 * RoPE at the current position's angles — the order LFM2 applies before caching/attending.
 */
class HeadwiseNormRopeOp(
    name: String,
    val src: TensorPort,
    val weight: TensorPort,
    val cosIn: TensorPort,
    val sinIn: TensorPort,
    val out: TensorPort,
    val numHeads: Int,
    private val headDim: Int,
    private val eps: Float,
) : TensorOp(name) {

    override val inputs = listOf(src, weight, cosIn, sinIn)
    override val outputs = listOf(out)

    override fun forward() {
        val s = src.tensor.data
        val w = weight.tensor.data
        val o = out.tensor.data
        val cosBuf = cosIn.tensor.data
        val sinBuf = sinIn.tensor.data
        val half = headDim / 2
        for (head in 0 until numHeads) {
            val off = head * headDim
            var sumSq = 0f
            for (i in 0 until headDim) {
                val a = s.get(off + i)
                sumSq += a * a
            }
            val inv = 1f / sqrt(sumSq / headDim + eps)
            for (i in 0 until headDim) {
                o.put(off + i, s.get(off + i) * inv * w.get(i))
            }
            for (i in 0 until half) {
                val a = o.get(off + i)
                val b = o.get(off + half + i)
                o.put(off + i, a * cosBuf.get(i) - b * sinBuf.get(i))
                o.put(off + half + i, b * cosBuf.get(i) + a * sinBuf.get(i))
            }
        }
    }
}

/** Appends [src]'s vector as row `position` of the KV [cache]. */
class CacheWriteOp(
    name: String,
    val src: TensorPort,
    val cache: TensorPort,
    private val state: Lfm2DecodeState,
) : TensorOp(name) {

    override val inputs = listOf(src)
    override val outputs = listOf(cache)

    override fun forward() {
        val width = cache.tensor.cols
        cache.tensor.data.duplicate()
            .also { it.position(state.position * width) }
            .put(src.tensor.data.slice(0, width))
    }
}

/**
 * Scaled dot-product scores + softmax for every query head against the KV cache: row h of
 * [weights] holds head h's attention distribution over positions 0..position (GQA: query head h
 * reads KV head h / groupSize). Columns past the current position are stale/zero.
 */
class AttendScoresOp(
    name: String,
    val q: TensorPort,
    val kCache: TensorPort,
    val weights: TensorPort,
    private val state: Lfm2DecodeState,
    val numHeads: Int,
    private val numKvHeads: Int,
    private val headDim: Int,
) : TensorOp(name) {

    override val inputs = listOf(q, kCache)
    override val outputs = listOf(weights)

    override fun forward() {
        val seen = state.position + 1
        val scale = 1f / sqrt(headDim.toFloat())
        val groupSize = numHeads / numKvHeads
        val kvDim = kCache.tensor.cols
        val maxSeqLen = weights.tensor.cols
        for (h in 0 until numHeads) {
            val kvHead = h / groupSize
            cblas_sgemv(
                CblasRowMajor, CblasNoTrans, seen, headDim, scale,
                FloatPointer(kCache.tensor.pointer).position((kvHead * headDim).toLong()), kvDim,
                FloatPointer(q.tensor.pointer).position((h * headDim).toLong()), 1,
                0f, FloatPointer(weights.tensor.pointer).position((h * maxSeqLen).toLong()), 1
            )
            softmaxRow(weights.tensor.data, h * maxSeqLen, seen)
        }
    }

    private fun softmaxRow(buf: FloatBuffer, offset: Int, n: Int) {
        var max = Float.NEGATIVE_INFINITY
        for (i in 0 until n) max = maxOf(max, buf.get(offset + i))
        var sum = 0f
        for (i in 0 until n) {
            val e = exp(buf.get(offset + i) - max)
            buf.put(offset + i, e)
            sum += e
        }
        val inv = 1f / sum
        for (i in 0 until n) buf.put(offset + i, buf.get(offset + i) * inv)
    }
}

/** Attention-weighted sum over the V cache: head h of [out] = weights[h] . vCache rows. */
class AttendMixOp(
    name: String,
    val weights: TensorPort,
    val vCache: TensorPort,
    val out: TensorPort,
    private val state: Lfm2DecodeState,
    val numHeads: Int,
    private val numKvHeads: Int,
    private val headDim: Int,
) : TensorOp(name) {

    override val inputs = listOf(weights, vCache)
    override val outputs = listOf(out)

    override fun forward() {
        val seen = state.position + 1
        val groupSize = numHeads / numKvHeads
        val kvDim = vCache.tensor.cols
        val maxSeqLen = weights.tensor.cols
        for (h in 0 until numHeads) {
            val kvHead = h / groupSize
            cblas_sgemv(
                CblasRowMajor, CblasTrans, seen, headDim, 1f,
                FloatPointer(vCache.tensor.pointer).position((kvHead * headDim).toLong()), kvDim,
                FloatPointer(weights.tensor.pointer).position((h * maxSeqLen).toLong()), 1,
                0f, FloatPointer(out.tensor.pointer).position((h * headDim).toLong()), 1
            )
        }
    }
}

/** out[i] = a[aOffset + i] * b[bOffset + i] — the conv block's B and C gates over in_proj chunks. */
class OffsetGateOp(
    name: String,
    val a: TensorPort,
    private val aOffset: Int,
    val b: TensorPort,
    private val bOffset: Int,
    val out: TensorPort,
) : TensorOp(name) {

    override val inputs = listOf(a, b)
    override val outputs = listOf(out)

    override fun forward() {
        val ad = a.tensor.data
        val bd = b.tensor.data
        val od = out.tensor.data
        for (i in 0 until out.tensor.size) {
            od.put(i, ad.get(aOffset + i) * bd.get(bOffset + i))
        }
    }
}

/**
 * Depthwise causal conv1d (kernel k, no bias) over the rolling per-channel [cache]: shifts each
 * channel's window left, appends [src], and writes the dot with [weight] to [out]. The newest
 * element sits at cache/weight index k-1.
 */
class CausalConvOp(
    name: String,
    val src: TensorPort,
    val cache: TensorPort,
    val weight: TensorPort,
    val out: TensorPort,
) : TensorOp(name) {

    override val inputs = listOf(src, weight)
    override val outputs = listOf(cache, out)

    override fun forward() {
        val c = cache.tensor.data
        val w = weight.tensor.data
        val s = src.tensor.data
        val o = out.tensor.data
        val k = cache.tensor.cols
        for (channel in 0 until out.tensor.size) {
            val base = channel * k
            var acc = 0f
            for (j in 0 until k - 1) {
                val shifted = c.get(base + j + 1)
                c.put(base + j, shifted)
                acc += shifted * w.get(base + j)
            }
            val newest = s.get(channel)
            c.put(base + k - 1, newest)
            acc += newest * w.get(base + k - 1)
            o.put(channel, acc)
        }
    }
}
