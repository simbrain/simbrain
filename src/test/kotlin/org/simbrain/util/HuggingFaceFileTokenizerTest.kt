package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

class HuggingFaceFileTokenizerTest {

    private fun tokenizerPath(): Path? {
        val hub = Path.of(System.getProperty("user.home"), ".cache", "huggingface", "hub",
            "models--LiquidAI--LFM2.5-230M", "snapshots")
        if (!hub.exists()) return null
        return hub.listDirectoryEntries().asSequence()
            .map { it.resolve("tokenizer.json") }
            .firstOrNull { it.exists() }
    }

    @Test
    fun `spans are contiguous, inclusive-end, and free of byte-level artifacts`() {
        val path = tokenizerPath()
        assumeTrue(path != null, "LFM2 tokenizer not present in the HF cache")

        val tokenizer = HuggingFaceFileTokenizer(path.toString())
        val text = "The capital of France is Paris."
        val tokens = tokenizer.tokenize(text)

        assertEquals(0, tokens.first().start)
        assertEquals(text.length - 1, tokens.last().end, "end offsets are inclusive")
        tokens.zipWithNext().forEach { (a, b) ->
            assertEquals(a.end + 1, b.start, "token spans tile the text with no gaps")
        }
        tokens.forEach {
            assertEquals(text.substring(it.start, it.end + 1), it.token,
                "token text is the source substring, not the raw BPE piece")
            assertFalse(it.token.contains("Ġ"))
        }
        assertEquals(text, tokenizer.joinTokens(tokens.map { it.token }))
    }

    @Test
    fun `literal chat scaffolding tokenizes to single whole-marker tokens`() {
        val path = tokenizerPath()
        assumeTrue(path != null, "LFM2 tokenizer not present in the HF cache")

        val tokenizer = HuggingFaceFileTokenizer(path.toString())
        val tokens = tokenizer.tokenize("<|startoftext|><|im_start|>user\nHi<|im_end|>")
        assertEquals("<|startoftext|>", tokens[0].token)
        assertEquals("<|im_start|>", tokens[1].token)
        assertTrue(tokens.any { it.token == "<|im_end|>" })
    }

    @Test
    fun `a missing tokenizer file tokenizes to nothing instead of failing`() {
        val tokenizer = HuggingFaceFileTokenizer("/no/such/tokenizer.json")
        assertTrue(tokenizer.tokenize("some text").isEmpty())
    }

    @Test
    fun `copies are independent but share the path`() {
        val tokenizer = HuggingFaceFileTokenizer("/some/path.json")
        val copy = tokenizer.copy()
        assertEquals("/some/path.json", copy.path)
        assertFalse(copy === tokenizer)
    }
}
