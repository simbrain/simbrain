package org.simbrain.network.llm

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.Blas
import java.nio.file.Path

class Lfm2GenerationTest {

    private fun snapshotDir(): Path? = Lfm2Weights.findWeightsDirectory()

    @Test
    fun `greedy decoding produces text end to end`() {
        val snapshot = snapshotDir()
        assumeOrRequireLfm2(snapshot != null, "LFM2 weights not found in the Simbrain or HF cache")

        Blas.withThreads(4) {
            val params = Safetensors.load(snapshot!!.resolve("model.safetensors"))
            val model = Lfm2Model(Lfm2Config(), params)
            LlmTokenizer(snapshot.resolve("tokenizer.json")).use { tokenizer ->
                val promptIds = tokenizer.encode("The capital of France is")
                var last = 0
                for (id in promptIds) {
                    last = argmax(model.forwardToken(id))
                }
                val generated = ArrayList<Int>()
                val t0 = System.nanoTime()
                repeat(30) {
                    generated.add(last)
                    last = argmax(model.forwardToken(last))
                }
                val tokensPerSec = 30 / ((System.nanoTime() - t0) / 1e9)
                val text = tokenizer.decode(generated.toIntArray())
                println("generated: $text")
                println("decode speed: ${"%.1f".format(tokensPerSec)} tokens/sec")
                assertTrue(text.isNotBlank(), "Generated no text")
                assertTrue(text.contains("Paris"), "Expected greedy completion to mention Paris, got: $text")
            }
        }
    }

    private fun argmax(logits: org.simbrain.network.tensor.FloatTensor): Int {
        val buf = logits.data
        var best = 0
        var bestVal = Float.NEGATIVE_INFINITY
        for (i in 0 until logits.size) {
            val v = buf.get(i)
            if (v > bestVal) {
                bestVal = v
                best = i
            }
        }
        return best
    }
}
