package org.simbrain.network.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class Lfm2ToolLoopTest {

    private fun weightsDirectory(): Path? = Lfm2Weights.findWeightsDirectory()

    @Test
    fun `parser reads single calls, quoted commas, multiple calls, and rejects garbage`() {
        assertEquals(
            listOf(Lfm2ChatFormat.ToolCall("current_time", emptyMap())),
            Lfm2ChatFormat.parseToolCalls("[current_time()]"),
        )
        assertEquals(
            listOf(Lfm2ChatFormat.ToolCall(
                "get_weather",
                mapOf("location" to "Boston, MA", "unit" to "celsius"),
            )),
            Lfm2ChatFormat.parseToolCalls("[get_weather(location='Boston, MA', unit='celsius')]"),
        )
        assertEquals(
            listOf(
                Lfm2ChatFormat.ToolCall("current_time", emptyMap()),
                Lfm2ChatFormat.ToolCall("get_weather", mapOf("location" to "Paris")),
            ),
            Lfm2ChatFormat.parseToolCalls("[current_time(), get_weather(location='Paris')]"),
        )
        assertEquals(
            listOf(Lfm2ChatFormat.ToolCall("f", mapOf("n" to "2"))),
            Lfm2ChatFormat.parseToolCalls("[f(n=2)]"),
            "unquoted values are kept verbatim",
        )
        assertEquals(
            listOf(Lfm2ChatFormat.ToolCall("get_weather", mapOf("location" to "Boston, MA"))),
            Lfm2ChatFormat.parseToolCalls("""[get_weather(location="Boston, MA")]"""),
            "the model emits double quotes in practice, though the template renders single",
        )
        assertTrue(Lfm2ChatFormat.parseToolCalls("no call syntax here").isEmpty())
    }

    @Test
    fun `the real model calls the demo weather tool and answers from its result`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 256)
        languageModel.promptMode = PromptMode.CHAT
        languageModel.enableDemoTools = true
        languageModel.loadWeights()
        languageModel.sendUserMessage("What is the weather in Boston?")

        var steps = 0
        while (languageModel.canAdvance && steps < 240) {
            languageModel.step()
            steps++
        }

        assertTrue(languageModel.isSealed, "the run seals at the answer's im_end")
        assertTrue(languageModel.text.contains("get_weather"),
            "the model called the tool, got: ${languageModel.text}")
        assertTrue(languageModel.text.contains("Sunny, 22 degrees C in Boston (demo data)"),
            "the parsed location reaches the tool cleanly, got: ${languageModel.text}")
        assertTrue(languageModel.text.substringAfterLast("assistant").contains("22"),
            "the model's own answer uses the tool result, got: ${languageModel.text}")
    }

    @Test
    fun `a scripted tool call executes at the turn boundary and decoding continues`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 256)
        languageModel.promptMode = PromptMode.CHAT
        languageModel.enableDemoTools = true
        languageModel.loadWeights()
        val tokenizer = languageModel.loaded!!.tokenizer

        val promptIds = tokenizer.encode(
            Lfm2ChatFormat.chatPrompt(
                "What is the weather in Boston?",
                Lfm2ChatFormat.toolListLine(LanguageModel.demoTools),
            ),
            addSpecials = false,
        )
        val callIds = tokenizer.encode("[get_weather(location='Boston')]", addSpecials = false)
        val eos = languageModel.loaded!!.model.config.eosTokenId

        val script = ArrayDeque<Int>()
        repeat(promptIds.size - 1) { script.addLast(0) }
        script.addLast(Lfm2ChatFormat.TOOL_CALL_START_ID)
        callIds.forEach { script.addLast(it) }
        script.addLast(Lfm2ChatFormat.TOOL_CALL_END_ID)
        script.addLast(eos)
        languageModel.sampleOverride = { if (script.isEmpty()) 0 else script.removeFirst() }

        languageModel.sendUserMessage("What is the weather in Boston?")
        val stepsToEos = promptIds.size + 2 + callIds.size + 1
        repeat(stepsToEos) { languageModel.step() }

        assertTrue(languageModel.canAdvance,
            "im_end with a pending tool call continues instead of sealing")
        assertTrue(languageModel.text.contains("<|tool_call_start|>[get_weather(location='Boston')]"),
            "the call is visible in the text, got: ${languageModel.text}")
        assertTrue(languageModel.text.contains("Sunny, 22 degrees C in Boston (demo data)"),
            "the executed result is in the text, got: ${languageModel.text}")

        val turnIds = tokenizer.encode(
            Lfm2ChatFormat.toolResultTurn("Sunny, 22 degrees C in Boston (demo data)"),
            addSpecials = false,
        )
        val positionAtInjection = languageModel.loaded!!.model.position
        repeat(1 + turnIds.size) { languageModel.step() }
        assertEquals(positionAtInjection + 1 + turnIds.size, languageModel.loaded!!.model.position,
            "the closing im_end and the tool turn prefill through the model")
        assertTrue(languageModel.canAdvance)
        assertTrue(languageModel.contextWindow.contains(
            "<|tool_response_start|>Sunny, 22 degrees C in Boston (demo data)<|tool_response_end|>"),
            "the window shows the tool turn")
    }

    @Test
    fun `an unknown tool call answers with an error and decoding continues`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 256)
        languageModel.promptMode = PromptMode.CHAT
        languageModel.enableDemoTools = true
        languageModel.loadWeights()
        val tokenizer = languageModel.loaded!!.tokenizer

        val promptIds = tokenizer.encode(
            Lfm2ChatFormat.chatPrompt(
                "Hi",
                Lfm2ChatFormat.toolListLine(LanguageModel.demoTools),
            ),
            addSpecials = false,
        )
        val callIds = tokenizer.encode("[launch_rocket()]", addSpecials = false)
        val eos = languageModel.loaded!!.model.config.eosTokenId

        val script = ArrayDeque<Int>()
        repeat(promptIds.size - 1) { script.addLast(0) }
        script.addLast(Lfm2ChatFormat.TOOL_CALL_START_ID)
        callIds.forEach { script.addLast(it) }
        script.addLast(Lfm2ChatFormat.TOOL_CALL_END_ID)
        script.addLast(eos)
        languageModel.sampleOverride = { if (script.isEmpty()) 0 else script.removeFirst() }

        languageModel.sendUserMessage("Hi")
        repeat(promptIds.size + 2 + callIds.size + 1) { languageModel.step() }

        assertTrue(languageModel.canAdvance)
        assertTrue(languageModel.text.contains("error: unknown tool launch_rocket"),
            "got: ${languageModel.text}")
    }

    @Test
    fun `a plain im_end with no pending call seals the run`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

        // The tool-advertising prompt alone is ~133 tokens; the window must hold it plus the EOS
        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 256)
        languageModel.promptMode = PromptMode.CHAT
        languageModel.enableDemoTools = true
        languageModel.loadWeights()
        val tokenizer = languageModel.loaded!!.tokenizer

        val promptIds = tokenizer.encode(
            Lfm2ChatFormat.chatPrompt(
                "Hi",
                Lfm2ChatFormat.toolListLine(LanguageModel.demoTools),
            ),
            addSpecials = false,
        )
        val script = ArrayDeque<Int>()
        repeat(promptIds.size - 1) { script.addLast(0) }
        script.addLast(languageModel.loaded!!.model.config.eosTokenId)
        languageModel.sampleOverride = { if (script.isEmpty()) 0 else script.removeFirst() }

        languageModel.sendUserMessage("Hi")
        repeat(promptIds.size) { languageModel.step() }
        assertTrue(languageModel.isSealed)
    }
}
