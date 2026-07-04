package org.simbrain.network.llm

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

class LlmTokenizerTest {

    private fun tokenizerPath(): Path? {
        val hub = Path.of(System.getProperty("user.home"), ".cache", "huggingface", "hub",
            "models--LiquidAI--LFM2.5-230M", "snapshots")
        if (!hub.exists()) return null
        return hub.listDirectoryEntries().asSequence()
            .map { it.resolve("tokenizer.json") }
            .firstOrNull { it.exists() }
    }

    @Test
    fun `encoding matches python tokenizers ids and decode round trips`() {
        val path = tokenizerPath()
        val manifestPath = Path.of(System.getProperty("user.home"), ".cache", "simbrain", "lfm2-parity", "manifest.json")
        assumeTrue(path != null && manifestPath.exists(),
            "LFM2 tokenizer or parity manifest not present; run lfm2_export_reference.py first")

        LlmTokenizer(path!!).use { tokenizer ->
            val prompts = JSONObject(manifestPath.readText()).getJSONArray("prompts")
            for (pi in 0 until prompts.length()) {
                val entry = prompts.getJSONObject(pi)
                val prompt = entry.getString("prompt")
                val expected = entry.getJSONArray("token_ids").let { arr -> IntArray(arr.length()) { arr.getInt(it) } }
                assertArrayEquals(expected, tokenizer.encode(prompt), "ids differ for \"$prompt\"")
                assertTrue(tokenizer.decode(expected).contains(prompt), "decode lost \"$prompt\"")
            }
        }
    }
}
