package org.simbrain.network.llm

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class Lfm2ChatFormatTest {

    private fun tokenizerPath(): Path? = Lfm2Weights.findWeightsDirectory()?.resolve("tokenizer.json")

    @Test
    fun `chat prompt renders the exact single-turn template`() {
        assertEquals(
            "<|startoftext|><|im_start|>user\nHi<|im_end|>\n<|im_start|>assistant\n",
            Lfm2ChatFormat.chatPrompt("Hi"),
        )
        assertEquals(
            "<|startoftext|><|im_start|>system\nBe brief.<|im_end|>\n" +
                "<|im_start|>user\nHi<|im_end|>\n<|im_start|>assistant\n",
            Lfm2ChatFormat.chatPrompt("Hi", "Be brief."),
        )
    }

    @Test
    fun `tool result turn wraps the response markers and reopens the assistant turn`() {
        assertEquals(
            "<|im_start|>tool\n<|tool_response_start|>22C<|tool_response_end|><|im_end|>\n" +
                "<|im_start|>assistant\n",
            Lfm2ChatFormat.toolResultTurn("22C"),
        )
    }

    @Test
    fun `templated encoding matches python apply_chat_template ids`() {
        val path = tokenizerPath()
        assumeTrue(path != null, "LFM2 tokenizer not found in the Simbrain or HF cache")

        LlmTokenizer(path!!).use { tokenizer ->
            // Reference ids from transformers 5.13.0 apply_chat_template(add_generation_prompt=True)
            assertArrayEquals(
                intArrayOf(1, 6, 6423, 708, 3493, 856, 779, 5706, 803, 4481, 540, 7, 708, 6, 64015, 708),
                tokenizer.encode(
                    Lfm2ChatFormat.chatPrompt("What is the capital of France?"),
                    addSpecials = false,
                ),
            )
            assertArrayEquals(
                intArrayOf(1, 6, 24131, 708, 4083, 938, 768, 56412, 16701, 523, 7, 708, 6,
                    6423, 708, 3493, 856, 779, 5706, 803, 4481, 540, 7, 708, 6, 64015, 708),
                tokenizer.encode(
                    Lfm2ChatFormat.chatPrompt(
                        "What is the capital of France?",
                        "You are a concise assistant.",
                    ),
                    addSpecials = false,
                ),
            )
        }
    }
}
