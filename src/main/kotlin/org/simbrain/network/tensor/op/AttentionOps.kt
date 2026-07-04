package org.simbrain.network.tensor.op

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Multi-head causal self-attention ops over full sequences, all VJP-complete. Per-head tensors
 * use a stacked-rows layout: heads x seq x width stored 2-D as (heads*seq) x width, head h
 * occupying the contiguous row block [h*seq, (h+1)*seq). Split and merge COPY between layouts —
 * never alias via [org.simbrain.network.tensor.FloatTensor.reshaped], which would silently share
 * a buffer between two ports and corrupt tape saves. Per-head products are plain loops: at
 * teaching dimensions they are far below BLAS call overhead.
 */

/** Reshapes seq x (heads*headDim) into stacked per-head blocks (heads*seq) x headDim, by copy. */
class SplitHeadsOp(name: String, val x: TensorPort, val out: TensorPort, val numHeads: Int) : TensorOp(name) {

    private val seq = x.tensor.rows
    private val headDim = x.tensor.cols / numHeads

    init {
        require(x.tensor.cols % numHeads == 0) { "${x.tensor.cols} columns not divisible by $numHeads heads" }
        require(out.tensor.rows == numHeads * seq && out.tensor.cols == headDim) {
            "split out ${out.tensor.rows}x${out.tensor.cols} != ${numHeads * seq}x$headDim"
        }
    }

    override val inputs = listOf(x)
    override val outputs = listOf(out)

    override fun forward() {
        val src = x.tensor.data
        val dst = out.tensor.data
        for (h in 0 until numHeads) {
            for (r in 0 until seq) {
                for (c in 0 until headDim) {
                    dst.put((h * seq + r) * headDim + c, src.get(r * x.tensor.cols + h * headDim + c))
                }
            }
        }
    }

    override val hasBackward get() = true

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor).data
        val gx = grads.of(x.tensor).data
        for (h in 0 until numHeads) {
            for (r in 0 until seq) {
                for (c in 0 until headDim) {
                    val i = r * x.tensor.cols + h * headDim + c
                    gx.put(i, gx.get(i) + g.get((h * seq + r) * headDim + c))
                }
            }
        }
    }
}

/** Inverse of [SplitHeadsOp]: concatenates (heads*seq) x headDim blocks back to seq x (heads*headDim). */
class MergeHeadsOp(name: String, val x: TensorPort, val out: TensorPort, val numHeads: Int) : TensorOp(name) {

    private val seq = out.tensor.rows
    private val headDim = x.tensor.cols

    init {
        require(x.tensor.rows == numHeads * seq && out.tensor.cols == numHeads * headDim) {
            "merge ${x.tensor.rows}x${x.tensor.cols} into ${out.tensor.rows}x${out.tensor.cols} with $numHeads heads"
        }
    }

    override val inputs = listOf(x)
    override val outputs = listOf(out)

    override fun forward() {
        val src = x.tensor.data
        val dst = out.tensor.data
        for (h in 0 until numHeads) {
            for (r in 0 until seq) {
                for (c in 0 until headDim) {
                    dst.put(r * out.tensor.cols + h * headDim + c, src.get((h * seq + r) * headDim + c))
                }
            }
        }
    }

    override val hasBackward get() = true

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor).data
        val gx = grads.of(x.tensor).data
        for (h in 0 until numHeads) {
            for (r in 0 until seq) {
                for (c in 0 until headDim) {
                    val i = (h * seq + r) * headDim + c
                    gx.put(i, gx.get(i) + g.get(r * out.tensor.cols + h * headDim + c))
                }
            }
        }
    }
}

/**
 * Per-head scaled attention scores: for each head, scores = q . k^T / sqrt(headDim), on stacked
 * per-head layouts (q, k: (heads*seq) x headDim; out: (heads*seq) x seq).
 */
class HeadScoresOp(name: String, val q: TensorPort, val k: TensorPort, val out: TensorPort, val numHeads: Int) : TensorOp(name) {

    private val headDim = q.tensor.cols
    private val seq = q.tensor.rows / numHeads
    private val scale = 1f / sqrt(headDim.toFloat())

    init {
        require(q.tensor.rows == numHeads * seq && k.tensor.rows == q.tensor.rows && k.tensor.cols == headDim) {
            "scores q ${q.tensor.rows}x${q.tensor.cols} vs k ${k.tensor.rows}x${k.tensor.cols}"
        }
        require(out.tensor.rows == numHeads * seq && out.tensor.cols == seq) {
            "scores out ${out.tensor.rows}x${out.tensor.cols} != ${numHeads * seq}x$seq"
        }
    }

    override val inputs = listOf(q, k)
    override val outputs = listOf(out)

    override fun forward() {
        val qd = q.tensor.data
        val kd = k.tensor.data
        val dst = out.tensor.data
        for (h in 0 until numHeads) {
            for (i in 0 until seq) {
                for (j in 0 until seq) {
                    var sum = 0f
                    for (c in 0 until headDim) {
                        sum += qd.get((h * seq + i) * headDim + c) * kd.get((h * seq + j) * headDim + c)
                    }
                    dst.put((h * seq + i) * seq + j, sum * scale)
                }
            }
        }
    }

    override val hasBackward get() = true
    override val savedForBackward get() = listOf(q.tensor, k.tensor)

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor).data
        val gq = grads.of(q.tensor).data
        val gk = grads.of(k.tensor).data
        val qd = q.tensor.data
        val kd = k.tensor.data
        for (h in 0 until numHeads) {
            for (i in 0 until seq) {
                for (j in 0 until seq) {
                    val gs = g.get((h * seq + i) * seq + j) * scale
                    if (gs == 0f) continue
                    for (c in 0 until headDim) {
                        val qi = (h * seq + i) * headDim + c
                        val kj = (h * seq + j) * headDim + c
                        gq.put(qi, gq.get(qi) + gs * kd.get(kj))
                        gk.put(kj, gk.get(kj) + gs * qd.get(qi))
                    }
                }
            }
        }
    }
}

/**
 * Causal mask fused with per-row softmax: within each head, row i is a distribution over
 * positions j <= i; masked positions come out exactly zero. The VJP needs no mask branches —
 * masked probabilities are zero, so their gradient terms vanish on their own.
 */
class CausalMaskedRowSoftmaxOp(name: String, val scores: TensorPort, val out: TensorPort, val numHeads: Int) : TensorOp(name) {

    private val seq = scores.tensor.cols

    init {
        require(scores.tensor.rows == numHeads * seq) {
            "softmax scores ${scores.tensor.rows}x${scores.tensor.cols} not $numHeads heads of $seq rows"
        }
        require(out.tensor.rows == scores.tensor.rows && out.tensor.cols == seq) {
            "softmax out ${out.tensor.rows}x${out.tensor.cols} != ${scores.tensor.rows}x$seq"
        }
    }

    override val inputs = listOf(scores)
    override val outputs = listOf(out)

    override fun forward() {
        val src = scores.tensor.data
        val dst = out.tensor.data
        for (h in 0 until numHeads) {
            for (i in 0 until seq) {
                val row = (h * seq + i) * seq
                var max = Float.NEGATIVE_INFINITY
                for (j in 0..i) max = maxOf(max, src.get(row + j))
                var sum = 0f
                for (j in 0..i) {
                    val e = exp(src.get(row + j) - max)
                    dst.put(row + j, e)
                    sum += e
                }
                val invSum = 1f / sum
                for (j in 0..i) dst.put(row + j, dst.get(row + j) * invSum)
                for (j in i + 1 until seq) dst.put(row + j, 0f)
            }
        }
    }

    override val hasBackward get() = true
    override val savedForBackward get() = listOf(out.tensor)

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor).data
        val gScores = grads.of(scores.tensor).data
        val p = out.tensor.data
        for (h in 0 until numHeads) {
            for (i in 0 until seq) {
                val row = (h * seq + i) * seq
                var dot = 0f
                for (j in 0..i) dot += g.get(row + j) * p.get(row + j)
                for (j in 0..i) {
                    gScores.put(row + j, gScores.get(row + j) + p.get(row + j) * (g.get(row + j) - dot))
                }
            }
        }
    }
}

/**
 * Per-head weighted mix of values: for each head, out = weights . v, on stacked per-head layouts
 * (weights: (heads*seq) x seq; v, out: (heads*seq) x headDim).
 */
class HeadMixOp(name: String, val weights: TensorPort, val v: TensorPort, val out: TensorPort, val numHeads: Int) : TensorOp(name) {

    private val headDim = v.tensor.cols
    private val seq = weights.tensor.cols

    init {
        require(weights.tensor.rows == numHeads * seq && v.tensor.rows == numHeads * seq) {
            "mix weights ${weights.tensor.rows}x${weights.tensor.cols} vs v ${v.tensor.rows}x${v.tensor.cols}"
        }
        require(out.tensor.rows == numHeads * seq && out.tensor.cols == headDim) {
            "mix out ${out.tensor.rows}x${out.tensor.cols} != ${numHeads * seq}x$headDim"
        }
    }

    override val inputs = listOf(weights, v)
    override val outputs = listOf(out)

    override fun forward() {
        val w = weights.tensor.data
        val vd = v.tensor.data
        val dst = out.tensor.data
        for (h in 0 until numHeads) {
            for (i in 0 until seq) {
                for (c in 0 until headDim) {
                    var sum = 0f
                    for (j in 0 until seq) {
                        sum += w.get((h * seq + i) * seq + j) * vd.get((h * seq + j) * headDim + c)
                    }
                    dst.put((h * seq + i) * headDim + c, sum)
                }
            }
        }
    }

    override val hasBackward get() = true
    override val savedForBackward get() = listOf(weights.tensor, v.tensor)

    override fun backward(grads: Gradients) {
        val g = grads.of(out.tensor).data
        val gw = grads.of(weights.tensor).data
        val gv = grads.of(v.tensor).data
        val w = weights.tensor.data
        val vd = v.tensor.data
        for (h in 0 until numHeads) {
            for (i in 0 until seq) {
                for (j in 0 until seq) {
                    var forW = 0f
                    for (c in 0 until headDim) {
                        val go = g.get((h * seq + i) * headDim + c)
                        forW += go * vd.get((h * seq + j) * headDim + c)
                        val vj = (h * seq + j) * headDim + c
                        gv.put(vj, gv.get(vj) + go * w.get((h * seq + i) * seq + j))
                    }
                    val wi = (h * seq + i) * seq + j
                    gw.put(wi, gw.get(wi) + forW)
                }
            }
        }
    }
}
