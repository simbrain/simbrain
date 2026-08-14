package org.simbrain.network.llm

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.Blas
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Compares the Kotlin LFM2 forward pass layer-by-layer against reference activations exported
 * from Python transformers by `src/test/python/lfm2_export_reference.py`. Skips unless the
 * model weights and the exported reference directory are both present locally.
 */
class Lfm2ParityTest {

    private fun modelPath(): Path? = Lfm2Weights.findWeightsDirectory()?.resolve("model.safetensors")

    private fun parityDir(): Path =
        Path.of(System.getProperty("user.home"), ".cache", "simbrain", "lfm2-parity")

    private fun readFloats(path: Path): FloatArray {
        val bytes = Files.readAllBytes(path)
        val floats = FloatArray(bytes.size / 4)
        java.nio.ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(floats)
        return floats
    }

    @Test
    fun `forward pass matches transformers reference layer by layer`() {
        val weights = modelPath()
        val manifestPath = parityDir().resolve("manifest.json")
        assumeOrRequireLfm2(weights != null && manifestPath.exists(),
            "LFM2 weights or parity reference not present; run lfm2_export_reference.py first")

        Blas.numThreads = 1
        val params = Safetensors.load(weights!!)
        val model = Lfm2Model(Lfm2Config(), params)
        val manifest = JSONObject(manifestPath.readText())

        val prompts = manifest.getJSONArray("prompts")
        var worstRel = 0.0
        for (pi in 0 until prompts.length()) {
            val entry = prompts.getJSONObject(pi)
            val ids = entry.getJSONArray("token_ids").let { arr -> IntArray(arr.length()) { arr.getInt(it) } }
            val numHidden = entry.getInt("num_hidden_states")
            val hiddenSize = entry.getInt("hidden_size")
            val vocabSize = entry.getInt("vocab_size")
            val files = entry.getJSONObject("files")

            model.reset()
            val captured = Array(numHidden + 1) { ArrayList<FloatArray>(ids.size) }
            val hooks = buildList {
                add(model.onPort("embed") { captured[0].add(it.tensor.toFloatArray()) })
                for (layer in 0 until model.config.numLayers) {
                    add(model.onPort("layers.$layer.resid") { captured[layer + 1].add(it.tensor.toFloatArray()) })
                }
                add(model.onPort("final_norm") { captured[numHidden].add(it.tensor.toFloatArray()) })
            }
            val capturedLogits = ArrayList<FloatArray>(ids.size)
            for (id in ids) {
                capturedLogits.add(model.forwardToken(id).toFloatArray())
            }
            hooks.forEach { it.remove() }
            // Transformers' last hidden_states entry is the post-embedding_norm state, which the
            // model reports at capture index numHidden (= numLayers + 1); raw layer outputs are 0..numHidden-1.
            captured[numHidden - 1] = captured[numHidden]

            println("prompt $pi (${ids.size} tokens): \"${entry.getString("prompt")}\"")
            println("| tensor | ref scale | max abs diff | rel |")
            println("|---|---|---|---|")
            for (layer in 0 until numHidden) {
                val ref = readFloats(parityDir().resolve(files.getString("hidden$layer")))
                val rel = compare("hidden$layer", ref, captured[layer], ids.size, hiddenSize)
                worstRel = maxOf(worstRel, rel)
            }
            val refLogits = readFloats(parityDir().resolve(files.getString("logits")))
            val rel = compare("logits", refLogits, capturedLogits, ids.size, vocabSize)
            worstRel = maxOf(worstRel, rel)
        }
        assertTrue(worstRel < 1e-3, "Worst scale-relative diff $worstRel exceeds 1e-3")
    }

    private fun compare(label: String, ref: FloatArray, actual: List<FloatArray>, seq: Int, width: Int): Double {
        assertTrue(ref.size == seq * width, "$label: reference size ${ref.size} != ${seq * width}")
        assertTrue(actual.size == seq, "$label: captured ${actual.size} positions, expected $seq")
        var maxDiff = 0.0
        var scale = 0.0
        for (t in 0 until seq) {
            val row = actual[t]
            for (i in 0 until width) {
                val r = ref[t * width + i]
                maxDiff = maxOf(maxDiff, Math.abs(row[i] - r).toDouble())
                scale = maxOf(scale, Math.abs(r).toDouble())
            }
        }
        val rel = if (scale > 0) maxDiff / scale else maxDiff
        println("| $label | ${"%.3f".format(scale)} | ${"%.3e".format(maxDiff)} | ${"%.3e".format(rel)} |")
        return rel
    }
}
