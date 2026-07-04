package org.simbrain.network.tensor.op

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.FloatTensor
import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random

class SequenceOpsTest {

    private fun randomTensor(rows: Int, cols: Int, rng: Random) =
        FloatTensor.of(rows, cols, FloatArray(rows * cols) { (rng.nextFloat() - 0.5f) })

    @Test
    fun `seq embed gathers table rows and zeroes negative ids`() {
        val table = TensorPort("table", FloatTensor.of(3, 2, floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)))
        val out = TensorPort("embedded", FloatTensor(4, 2))
        val embed = SeqEmbedOp("embed", table, out)
        embed.tokenIds = intArrayOf(2, 0, -1, 1)
        embed.forward()
        assertEquals(5f, out.tensor[0, 0])
        assertEquals(6f, out.tensor[0, 1])
        assertEquals(1f, out.tensor[1, 0])
        assertEquals(0f, out.tensor[2, 0])
        assertEquals(0f, out.tensor[2, 1])
        assertEquals(3f, out.tensor[3, 0])
    }

    @Test
    fun `bias broadcasts one value per column across all rows`() {
        val x = TensorPort("x", FloatTensor.of(2, 3, floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)))
        val bias = TensorPort("bias", FloatTensor.of(1, 3, floatArrayOf(10f, 20f, 30f)))
        val out = TensorPort("out", FloatTensor(2, 3))
        BiasOp("bias", x, bias, out).forward()
        assertEquals(11f, out.tensor[0, 0])
        assertEquals(22f, out.tensor[0, 1])
        assertEquals(36f, out.tensor[1, 2])
    }

    @Test
    fun `layer norm rows are standardized before scale and shift`() {
        val rng = Random(11)
        val x = TensorPort("x", randomTensor(3, 8, rng))
        val gamma = TensorPort("gamma", FloatTensor.of(1, 8, FloatArray(8) { 1f }))
        val beta = TensorPort("beta", FloatTensor(1, 8).apply { fill(0f) })
        val out = TensorPort("out", FloatTensor(3, 8))
        LayerNormOp("norm", x, gamma, beta, out).forward()
        for (r in 0 until 3) {
            var mean = 0f
            var meanSq = 0f
            for (c in 0 until 8) {
                mean += out.tensor[r, c]
                meanSq += out.tensor[r, c] * out.tensor[r, c]
            }
            mean /= 8
            meanSq /= 8
            assertTrue(abs(mean) < 1e-5f, "row $r mean $mean")
            assertTrue(abs(meanSq - 1f) < 1e-3f, "row $r variance $meanSq")
        }
    }

    @Test
    fun `sequence cross entropy averages supervised rows and skips negative targets`() {
        val logits = TensorPort("logits", FloatTensor.of(3, 2, floatArrayOf(0f, 0f, 1f, 0f, 5f, -5f)))
        val probs = TensorPort("probs", FloatTensor(3, 2))
        val loss = TensorPort("loss", FloatTensor(1, 1))
        val ce = SeqSoftmaxCrossEntropyOp("ce", logits, probs, loss)
        ce.targetIds = intArrayOf(0, -1, 0)
        ce.forward()
        for (r in 0 until 3) {
            assertTrue(abs(probs.tensor[r, 0] + probs.tensor[r, 1] - 1f) < 1e-6f, "row $r not normalized")
        }
        val expected = (-ln(probs.tensor[0, 0]) + -ln(probs.tensor[2, 0])) / 2f
        assertEquals(expected, loss.tensor[0, 0], 1e-6f)
    }

    @Test
    fun `analytic gradients match finite differences through every sequence op`() {
        val rng = Random(19)
        val seq = 3
        val dim = 4
        val hidden = 6
        val vocab = 7

        val table = TensorPort("embed.table", randomTensor(vocab, dim, rng))
        val posTable = TensorPort("embed.pos", randomTensor(seq, dim, rng))
        val w1 = TensorPort("w1", randomTensor(hidden, dim, rng))
        val b1 = TensorPort("b1", randomTensor(1, hidden, rng))
        val gamma = TensorPort("gamma", FloatTensor.of(1, hidden, FloatArray(hidden) { 1f + 0.1f * it }))
        val beta = TensorPort("beta", randomTensor(1, hidden, rng))
        val w2 = TensorPort("w2", randomTensor(vocab, hidden, rng))

        val embedded = TensorPort("embedded", FloatTensor(seq, dim))
        val positioned = TensorPort("positioned", FloatTensor(seq, dim))
        val projected = TensorPort("projected", FloatTensor(seq, hidden))
        val biased = TensorPort("biased", FloatTensor(seq, hidden))
        val activated = TensorPort("activated", FloatTensor(seq, hidden))
        val normed = TensorPort("normed", FloatTensor(seq, hidden))
        val logits = TensorPort("logits", FloatTensor(seq, vocab))
        val probs = TensorPort("probs", FloatTensor(seq, vocab))
        val loss = TensorPort("loss", FloatTensor(1, 1))

        val embed = SeqEmbedOp("embed", table, embedded).apply { tokenIds = intArrayOf(2, 5, 0) }
        val ce = SeqSoftmaxCrossEntropyOp("ce", logits, probs, loss).apply { targetIds = intArrayOf(5, 0, 3) }
        val plan = OpPlan(listOf(
            embed,
            AddOp("add_pos", embedded, posTable, positioned),
            MatMulLinearOp("project", w1, positioned, projected),
            BiasOp("bias", projected, b1, biased),
            ReLUOp("relu", biased, activated),
            LayerNormOp("norm", activated, gamma, beta, normed),
            MatMulLinearOp("unembed", w2, normed, logits),
            ce,
        ))
        assertTrue(plan.trainable)

        val tape = Tape()
        val grads = Gradients()
        plan.forward(tape)
        tape.backward(loss, grads)

        val lossValue = { plan.forward(); loss.tensor.data.get(0) }
        for (port in listOf(table, posTable, w1, b1, gamma, beta, w2)) {
            val worst = GradientCheck.maxRelativeError(port.tensor, grads.of(port.tensor), samples = 24, loss = lossValue)
            assertTrue(worst < 2e-2f, "gradient of ${port.name} off by $worst relative")
        }
    }
}
