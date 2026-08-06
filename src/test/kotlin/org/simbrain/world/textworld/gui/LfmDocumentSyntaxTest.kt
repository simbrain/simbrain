package org.simbrain.world.textworld.gui

import org.fife.ui.rsyntaxtextarea.TokenTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.swing.text.Segment

class LfmDocumentSyntaxTest {

    @Test
    fun `token maker emits only supported syntax style indices`() {
        val maker = LfmDocumentTokenMaker()
        val text = "<|im_start|>system\nList of tools: [current_time]<|im_end|>\n" +
            "<|tool_call_start|>[current_time()]<|tool_call_end|>"
        var token = maker.getTokenList(Segment(text.toCharArray(), 0, text.length), 42, 0)
        while (token != null) {
            assertTrue(token.type in 0 until TokenTypes.DEFAULT_NUM_TOKEN_TYPES, "${token.type}: ${token.lexeme}")
            token = token.nextToken
        }
    }

    @Test
    fun `system turn styling carries onto later lines`() {
        val maker = LfmDocumentTokenMaker()
        val opening = "<|im_start|>system"
        assertEquals(TokenTypes.RESERVED_WORD, maker.getLastTokenTypeOnLine(
            Segment(opening.toCharArray(), 0, opening.length),
            TokenTypes.NULL,
        ))
    }

    @Test
    fun `user and assistant turns retain distinct token types`() {
        val maker = LfmDocumentTokenMaker()
        val user = "<|im_start|>user"
        val assistant = "<|im_start|>assistant"

        assertEquals(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, maker.getLastTokenTypeOnLine(
            Segment(user.toCharArray(), 0, user.length),
            TokenTypes.NULL,
        ))
        assertEquals(TokenTypes.LITERAL_CHAR, maker.getLastTokenTypeOnLine(
            Segment(assistant.toCharArray(), 0, assistant.length),
            TokenTypes.NULL,
        ))
    }
}
