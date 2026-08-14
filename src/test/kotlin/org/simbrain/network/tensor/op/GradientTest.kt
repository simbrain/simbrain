package org.simbrain.network.tensor.op

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.FloatTensor
import kotlin.random.Random

class GradientTest {

    private fun randomTensor(rows: Int, cols: Int, rng: Random) =
        FloatTensor.of(rows, cols, FloatArray(rows * cols) { (rng.nextFloat() - 0.5f) })

    @Test
    fun `analytic gradients match finite differences through every core op`() {
        val rng = Random(7)
        val x = TensorPort("x", randomTensor(1, 8, rng))
        val normWeight = TensorPort("norm.weight", randomTensor(1, 8, rng))
        val w1g = TensorPort("w1g", randomTensor(8, 8, rng))
        val w1u = TensorPort("w1u", randomTensor(8, 8, rng))
        val w2 = TensorPort("w2", randomTensor(4, 8, rng))

        val normed = TensorPort("normed", FloatTensor(1, 8))
        val gate = TensorPort("gate", FloatTensor(1, 8))
        val up = TensorPort("up", FloatTensor(1, 8))
        val act = TensorPort("act", FloatTensor(1, 8))
        val resid = TensorPort("resid", FloatTensor(1, 8))
        val logits = TensorPort("logits", FloatTensor(1, 4))
        val probs = TensorPort("probs", FloatTensor(1, 4))
        val loss = TensorPort("loss", FloatTensor(1, 1))

        val ce = SoftmaxCrossEntropyOp("ce", logits, probs, loss).apply { targetIndex = 2 }
        val plan = OpPlan(listOf(
            RmsNormOp("norm", x, normWeight, normed, eps = 1e-5f),
            LinearOp("gate_proj", w1g, normed, gate),
            LinearOp("up_proj", w1u, normed, up),
            SiluGateOp("silu_gate", gate, up, act),
            AddOp("residual", act, normed, resid),
            LinearOp("out_proj", w2, resid, logits),
            ce,
        ))
        assertTrue(plan.trainable)

        val tape = Tape()
        val grads = Gradients()
        plan.forward(tape)
        tape.backward(loss, grads)

        val lossValue = { plan.forward(); loss.tensor.data.get(0) }
        for (port in listOf(x, normWeight, w1g, w1u, w2)) {
            val worst = GradientCheck.maxRelativeError(port.tensor, grads.of(port.tensor), samples = 24, loss = lossValue)
            assertTrue(worst < 2e-2f, "gradient of ${port.name} off by $worst relative")
        }
    }

    @Test
    fun `toy silu-gated block learns XOR with tape backward and adam`() {
        val rng = Random(3)
        val x = TensorPort("x", FloatTensor(1, 3))
        val w1g = TensorPort("w1g", randomTensor(8, 3, rng))
        val w1u = TensorPort("w1u", randomTensor(8, 3, rng))
        val w2 = TensorPort("w2", randomTensor(2, 8, rng))
        val gate = TensorPort("gate", FloatTensor(1, 8))
        val up = TensorPort("up", FloatTensor(1, 8))
        val act = TensorPort("act", FloatTensor(1, 8))
        val logits = TensorPort("logits", FloatTensor(1, 2))
        val probs = TensorPort("probs", FloatTensor(1, 2))
        val loss = TensorPort("loss", FloatTensor(1, 1))

        val ce = SoftmaxCrossEntropyOp("ce", logits, probs, loss)
        val plan = OpPlan(listOf(
            LinearOp("gate_proj", w1g, x, gate),
            LinearOp("up_proj", w1u, x, up),
            SiluGateOp("silu_gate", gate, up, act),
            LinearOp("out_proj", w2, act, logits),
            ce,
        ))

        val samples = listOf(
            floatArrayOf(0f, 0f, 1f) to 0,
            floatArrayOf(0f, 1f, 1f) to 1,
            floatArrayOf(1f, 0f, 1f) to 1,
            floatArrayOf(1f, 1f, 1f) to 0,
        )
        val params = mapOf("w1g" to w1g, "w1u" to w1u, "w2" to w2)
        val tape = Tape()
        val grads = Gradients()
        val adam = TensorAdam(learningRate = 0.01f)

        var firstEpochLoss = 0f
        var lastEpochLoss = 0f
        repeat(400) { epoch ->
            var epochLoss = 0f
            for ((input, target) in samples) {
                x.tensor.copyFrom(input)
                ce.targetIndex = target
                tape.clear()
                grads.zeroAll()
                plan.forward(tape)
                epochLoss += loss.tensor.data.get(0)
                tape.backward(loss, grads)
                adam.step()
                params.forEach { (key, port) -> adam.update(key, port.tensor, grads.of(port.tensor)) }
            }
            if (epoch == 0) firstEpochLoss = epochLoss
            lastEpochLoss = epochLoss
        }

        assertTrue(lastEpochLoss < 0.05f, "XOR loss did not converge: $lastEpochLoss (started $firstEpochLoss)")

        for ((input, target) in samples) {
            x.tensor.copyFrom(input)
            plan.forward()
            val predicted = if (probs.tensor[0, 1] > probs.tensor[0, 0]) 1 else 0
            org.junit.jupiter.api.Assertions.assertEquals(target, predicted,
                "wrong prediction for ${input.toList()}")
        }
    }
}
