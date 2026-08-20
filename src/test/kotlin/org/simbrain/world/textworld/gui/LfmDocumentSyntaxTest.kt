package org.simbrain.world.textworld.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LfmDocumentSyntaxTest {

    private fun regionAt(runs: List<StructureRun>, index: Int) =
        runs.first { index >= it.start && index < it.end }.region

    @Test
    fun `runs are contiguous and cover the whole text`() {
        val text = "<|im_start|>system\nList of tools: [current_time]<|im_end|>\n" +
            "<|tool_call_start|>[current_time()]<|tool_call_end|>"
        val runs = documentStructureRuns(text)
        assertEquals(0, runs.first().start)
        assertEquals(text.length, runs.last().end)
        runs.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
    }

    @Test
    fun `system turn styling carries onto later lines`() {
        val text = "<|im_start|>system\nYou are helpful.\nBe brief."
        val runs = documentStructureRuns(text)
        assertEquals(DocumentRegion.SYSTEM_CONTENT, regionAt(runs, text.indexOf("Be brief")))
    }

    @Test
    fun `user and assistant turns retain distinct regions`() {
        val text = "<|im_start|>user\nhi<|im_end|><|im_start|>assistant\nhello"
        val runs = documentStructureRuns(text)
        assertEquals(DocumentRegion.USER_LABEL, regionAt(runs, text.indexOf("user")))
        assertEquals(DocumentRegion.USER_CONTENT, regionAt(runs, text.indexOf("hi")))
        assertEquals(DocumentRegion.ASSISTANT_LABEL, regionAt(runs, text.indexOf("assistant")))
        assertEquals(DocumentRegion.ASSISTANT_CONTENT, regionAt(runs, text.indexOf("hello")))
    }

    @Test
    fun `markers and tool content are recognized`() {
        val text = "<|tool_call_start|>[current_time()]<|tool_call_end|>after"
        val runs = documentStructureRuns(text)
        assertEquals(DocumentRegion.MARKER, regionAt(runs, 0))
        assertEquals(DocumentRegion.TOOL_CONTENT, regionAt(runs, text.indexOf("[current_time")))
        assertEquals(DocumentRegion.ORDINARY, regionAt(runs, text.indexOf("after")))
    }

    @Test
    fun `unterminated marker is styled as current content`() {
        val text = "<|im_start|>user\nhello <|im_en"
        val runs = documentStructureRuns(text)
        assertEquals(DocumentRegion.USER_CONTENT, regionAt(runs, text.indexOf("<|im_en")))
        assertEquals(text.length, runs.last().end)
    }

    @Test
    fun `tool list line is split out of system content but prose lines are not`() {
        val text = "<|im_start|>system\nYou are helpful.\nList of tools: [{\"name\": \"current_time\"}]\nBe brief."
        val runs = documentStructureRuns(text)
        assertEquals(DocumentRegion.SYSTEM_CONTENT, regionAt(runs, text.indexOf("You are helpful")))
        assertEquals(DocumentRegion.TOOL_LIST, regionAt(runs, text.indexOf("List of tools")))
        assertEquals(DocumentRegion.SYSTEM_CONTENT, regionAt(runs, text.indexOf("Be brief")))
        assertEquals(0, runs.first().start)
        assertEquals(text.length, runs.last().end)
        runs.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
    }

    @Test
    fun `unknown role labels are structural and reset content to ordinary`() {
        val text = "<|im_start|>tool\nresult"
        val runs = documentStructureRuns(text)
        assertEquals(DocumentRegion.OTHER_LABEL, regionAt(runs, text.indexOf("tool")))
        assertEquals(DocumentRegion.ORDINARY, regionAt(runs, text.indexOf("result")))
    }
}
