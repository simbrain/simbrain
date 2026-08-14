package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.simbrain.network.llm.assumeOrRequireLfm2
import org.junit.jupiter.api.Test
import org.simbrain.network.llm.Lfm2Weights
import java.nio.file.Path

class HuggingFaceFileTokenizerTest {

    private fun tokenizerPath(): Path? = Lfm2Weights.findWeightsDirectory()?.resolve("tokenizer.json")

    @Test
    fun `spans are contiguous, inclusive-end, and free of byte-level artifacts`() {
        val path = tokenizerPath()
        assumeOrRequireLfm2(path != null, "LFM2 tokenizer not found in the Simbrain or HF cache")

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
        assumeOrRequireLfm2(path != null, "LFM2 tokenizer not found in the Simbrain or HF cache")

        val tokenizer = HuggingFaceFileTokenizer(path.toString())
        val tokens = tokenizer.tokenize("<|startoftext|><|im_start|>user\nHi<|im_end|>")
        assertEquals("<|startoftext|>", tokens[0].token)
        assertEquals("<|im_start|>", tokens[1].token)
        assertTrue(tokens.any { it.token == "<|im_end|>" })
    }

    @Test
    fun `tokenization is not truncated at the tokenizer file's model max length`() {
        val path = tokenizerPath()
        assumeOrRequireLfm2(path != null, "LFM2 tokenizer not found in the Simbrain or HF cache")

        val tokenizer = HuggingFaceFileTokenizer(path.toString())
        val text = "word ".repeat(1200).trim()
        val tokens = tokenizer.tokenize(text)
        assertTrue(tokens.size > 512, "expected more than 512 tokens, got ${tokens.size}")
        assertEquals(text.length - 1, tokens.last().end, "spans must cover the entire text")
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
