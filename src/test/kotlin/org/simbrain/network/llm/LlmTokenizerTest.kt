package org.simbrain.network.llm

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class LlmTokenizerTest {

    private fun tokenizerPath(): Path? = Lfm2Weights.findWeightsDirectory()?.resolve("tokenizer.json")

    @Test
    fun `encoding matches python tokenizers ids and decode round trips`() {
        val path = tokenizerPath()
        val manifestPath = Path.of(System.getProperty("user.home"), ".cache", "simbrain", "lfm2-parity", "manifest.json")
        assumeOrRequireLfm2(path != null && manifestPath.exists(),
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

    @Test
    fun `specials-off encoding maps marker text to single ids and adds no bos`() {
        val path = tokenizerPath()
        assumeOrRequireLfm2(path != null, "LFM2 tokenizer not found in the Simbrain or HF cache")

        LlmTokenizer(path!!).use { tokenizer ->
            assertArrayEquals(intArrayOf(6), tokenizer.encode("<|im_start|>", addSpecials = false),
                "literal marker text must encode to its single added-token id")
            assertEquals(1, tokenizer.encode("Hello").first(), "the post-processor prepends BOS by default")
            assertNotEquals(1, tokenizer.encode("Hello", addSpecials = false).first())
        }
    }

    @Test
    fun `encoding is not truncated at the tokenizer file's model max length`() {
        val path = tokenizerPath()
        assumeOrRequireLfm2(path != null, "LFM2 tokenizer not found in the Simbrain or HF cache")

        LlmTokenizer(path!!).use { tokenizer ->
            val ids = tokenizer.encode("word ".repeat(1200), addSpecials = false)
            assertTrue(ids.size > 512, "expected more than 512 ids, got ${ids.size}")
        }
    }

    @Test
    fun `skip-specials decode drops scaffolding but keeps tool call markers`() {
        val path = tokenizerPath()
        assumeOrRequireLfm2(path != null, "LFM2 tokenizer not found in the Simbrain or HF cache")

        LlmTokenizer(path!!).use { tokenizer ->
            val ids = tokenizer.encode("Hello")
            assertTrue(tokenizer.decode(ids).contains("<|startoftext|>"))
            assertFalse(tokenizer.decode(ids, skipSpecials = true).contains("<|"))
            val toolIds = tokenizer.encode("<|tool_call_start|>[now()]<|tool_call_end|>", addSpecials = false)
            assertTrue(tokenizer.decode(toolIds, skipSpecials = true).contains("<|tool_call_start|>"),
                "tool-call markers are deliberately non-special and must survive a skip-specials decode")
        }
    }
}
