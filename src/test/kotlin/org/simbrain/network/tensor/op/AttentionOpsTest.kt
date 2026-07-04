package org.simbrain.network.tensor.op

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.FloatTensor
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class AttentionOpsTest {

    private fun randomTensor(rows: Int, cols: Int, rng: Random) =
        FloatTensor.of(rows, cols, FloatArray(rows * cols) { (rng.nextFloat() - 0.5f) })

    @Test
    fun `split then merge is the identity and copies rather than aliasing`() {
        val rng = Random(5)
        val x = TensorPort("x", randomTensor(4, 6, rng))
        val split = TensorPort("split", FloatTensor(2 * 4, 3))
        val merged = TensorPort("merged", FloatTensor(4, 6))
        SplitHeadsOp("split", x, split, numHeads = 2).forward()
        MergeHeadsOp("merge", split, merged, numHeads = 2).forward()

        assertNotSame(x.tensor.pointer, split.tensor.pointer)
        for (r in 0 until 4) for (c in 0 until 6) {
            assertEquals(x.tensor[r, c], merged.tensor[r, c], 0f, "($r,$c)")
        }
        assertEquals(x.tensor[1, 4], split.tensor[4 + 1, 1], 0f, "head 1 row 1 col 1")
    }

    @Test
    fun `head scores match a manual dot product with scaling`() {
        val q = TensorPort("q", FloatTensor.of(2, 2, floatArrayOf(1f, 2f, 3f, 4f)))
        val k = TensorPort("k", FloatTensor.of(2, 2, floatArrayOf(5f, 6f, 7f, 8f)))
        val scores = TensorPort("scores", FloatTensor(2, 2))
        HeadScoresOp("scores", q, k, scores, numHeads = 1).forward()
        val scale = 1f / sqrt(2f)
        assertEquals((1f * 5f + 2f * 6f) * scale, scores.tensor[0, 0], 1e-6f)
        assertEquals((3f * 7f + 4f * 8f) * scale, scores.tensor[1, 1], 1e-6f)
    }

    @Test
    fun `causal softmax zeroes the future and normalizes the visible prefix per head`() {
        val rng = Random(9)
        val seq = 5
        val heads = 3
        val scores = TensorPort("scores", randomTensor(heads * seq, seq, rng))
        val weights = TensorPort("weights", FloatTensor(heads * seq, seq))
        CausalMaskedRowSoftmaxOp("softmax", scores, weights, heads).forward()
        for (h in 0 until heads) {
            for (i in 0 until seq) {
                var sum = 0f
                for (j in 0 until seq) {
                    val w = weights.tensor[h * seq + i, j]
                    if (j > i) {
                        assertEquals(0f, w, 0f, "head $h row $i col $j must be masked")
                    } else {
                        assertTrue(w > 0f, "head $h row $i col $j must be positive")
                        sum += w
                    }
                }
                assertTrue(abs(sum - 1f) < 1e-6f, "head $h row $i sums to $sum")
            }
        }
    }

    @Test
    fun `analytic gradients match finite differences through a full attention block`() {
        val rng = Random(23)
        val seq = 4
        val dim = 6
        val heads = 2
        val headDim = dim / heads
        val vocab = 5

        val x = TensorPort("x", randomTensor(seq, dim, rng))
        val wq = TensorPort("wq", randomTensor(dim, dim, rng))
        val wk = TensorPort("wk", randomTensor(dim, dim, rng))
        val wv = TensorPort("wv", randomTensor(dim, dim, rng))
        val wo = TensorPort("wo", randomTensor(dim, dim, rng))
        val w2 = TensorPort("w2", randomTensor(vocab, dim, rng))

        val q = TensorPort("q", FloatTensor(seq, dim))
        val k = TensorPort("k", FloatTensor(seq, dim))
        val v = TensorPort("v", FloatTensor(seq, dim))
        val qh = TensorPort("qh", FloatTensor(heads * seq, headDim))
        val kh = TensorPort("kh", FloatTensor(heads * seq, headDim))
        val vh = TensorPort("vh", FloatTensor(heads * seq, headDim))
        val scores = TensorPort("scores", FloatTensor(heads * seq, seq))
        val attnWeights = TensorPort("attnWeights", FloatTensor(heads * seq, seq))
        val mixed = TensorPort("mixed", FloatTensor(heads * seq, headDim))
        val merged = TensorPort("merged", FloatTensor(seq, dim))
        val projected = TensorPort("projected", FloatTensor(seq, dim))
        val resid = TensorPort("resid", FloatTensor(seq, dim))
        val logits = TensorPort("logits", FloatTensor(seq, vocab))
        val probs = TensorPort("probs", FloatTensor(seq, vocab))
        val loss = TensorPort("loss", FloatTensor(1, 1))

        val ce = SeqSoftmaxCrossEntropyOp("ce", logits, probs, loss).apply {
            targetIds = intArrayOf(3, 0, 4, 1)
        }
        val plan = OpPlan(listOf(
            MatMulLinearOp("q_proj", wq, x, q),
            MatMulLinearOp("k_proj", wk, x, k),
            MatMulLinearOp("v_proj", wv, x, v),
            SplitHeadsOp("q_split", q, qh, heads),
            SplitHeadsOp("k_split", k, kh, heads),
            SplitHeadsOp("v_split", v, vh, heads),
            HeadScoresOp("scores", qh, kh, scores, heads),
            CausalMaskedRowSoftmaxOp("softmax", scores, attnWeights, heads),
            HeadMixOp("mix", attnWeights, vh, mixed, heads),
            MergeHeadsOp("merge", mixed, merged, heads),
            MatMulLinearOp("out_proj", wo, merged, projected),
            AddOp("residual", x, projected, resid),
            MatMulLinearOp("unembed", w2, resid, logits),
            ce,
        ))
        assertTrue(plan.trainable)

        val tape = Tape()
        val grads = Gradients()
        plan.forward(tape)
        tape.backward(loss, grads)

        val lossValue = { plan.forward(); loss.tensor.data.get(0) }
        for (port in listOf(x, wq, wk, wv, wo, w2)) {
            val worst = GradientCheck.maxRelativeError(port.tensor, grads.of(port.tensor), samples = 24, loss = lossValue)
            assertTrue(worst < 2e-2f, "gradient of ${port.name} off by $worst relative")
        }
    }
}
