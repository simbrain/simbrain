package org.simbrain.network.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.op.GradientCheck
import org.simbrain.network.tensor.op.Tape
import org.simbrain.network.tensor.op.Gradients

class TeachingTransformerTest {

    @Test
    fun `whole plan is trainable and analytic gradients match finite differences`() {
        val model = TeachingTransformerModel(TeachingTransformerConfig(
            contextSize = 4, embedDim = 8, numHeads = 2, hiddenDim = 10, vocabSize = 9, numLayers = 2
        ))
        assertTrue(model.plan.trainable, "every op on the loss path must have a VJP")

        model.setSample(intArrayOf(1, 7, 3, 2), intArrayOf(7, 3, 2, 8))
        val tape = Tape()
        val grads = Gradients()
        model.plan.forward(tape)
        tape.backward(model.loss, grads)

        val lossValue = { model.plan.forward(); model.loss.tensor.data.get(0) }
        val checked = listOf(
            "embed.table", "embed.pos",
            "layers.0.attn.wq", "layers.0.attn.wo", "layers.0.attn.norm.gamma",
            "layers.1.attn.wk", "layers.1.mlp.w1", "layers.1.mlp.b2", "layers.1.mlp.norm.beta",
            "final_norm.gamma", "unembed.weight",
        )
        for (name in checked) {
            val port = model.params.getValue(name)
            val worst = GradientCheck.maxRelativeError(port.tensor, grads.of(port.tensor), samples = 16, loss = lossValue)
            assertTrue(worst < 2e-2f, "gradient of $name off by $worst relative")
        }
    }

    @Test
    fun `model overfits a tiny cyclic corpus and predicts its continuation`() {
        val config = TeachingTransformerConfig(
            contextSize = 6, embedDim = 12, numHeads = 3, hiddenDim = 16, vocabSize = 5, numLayers = 1
        )
        val model = TeachingTransformerModel(config, seed = 7L)

        val corpus = IntArray(40) { it % 5 }
        val windows = (0 until corpus.size - config.contextSize).map { start ->
            val tokens = corpus.copyOfRange(start, start + config.contextSize)
            val targets = corpus.copyOfRange(start + 1, start + config.contextSize + 1)
            tokens to targets
        }

        var firstEpochLoss = 0f
        var lastEpochLoss = 0f
        repeat(60) { epoch ->
            var epochLoss = 0f
            for ((tokens, targets) in windows) {
                epochLoss += model.trainStep(tokens, targets)
            }
            if (epoch == 0) firstEpochLoss = epochLoss
            lastEpochLoss = epochLoss
        }
        assertTrue(lastEpochLoss < 0.1f * firstEpochLoss,
            "loss did not fall: $firstEpochLoss -> $lastEpochLoss")

        model.setSample(intArrayOf(2, 3, 4, 0, 1, 2))
        model.forward()
        for (position in 0 until config.contextSize) {
            val distribution = model.distributionAt(position)
            val predicted = distribution.indices.maxBy { distribution[it] }
            assertEquals((position + 3) % 5, predicted, "wrong next token at position $position")
        }
    }

    @Test
    fun `short contexts pad with unsupervised positions and still produce a finite loss`() {
        val model = TeachingTransformerModel(TeachingTransformerConfig(
            contextSize = 8, embedDim = 8, numHeads = 2, hiddenDim = 8, vocabSize = 6, numLayers = 1
        ))
        model.setSample(intArrayOf(1, 2), intArrayOf(2, 3))
        val loss = model.forward()
        assertTrue(loss.isFinite() && loss > 0f, "loss $loss")
        val tail = model.distributionAt(7)
        assertTrue(tail.all { it.isFinite() }, "padded position distribution must stay finite")
    }
}
