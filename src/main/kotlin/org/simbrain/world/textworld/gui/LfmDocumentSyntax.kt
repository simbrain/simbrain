package org.simbrain.world.textworld.gui

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.Style
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.SyntaxScheme
import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory
import org.fife.ui.rsyntaxtextarea.TokenMap
import org.fife.ui.rsyntaxtextarea.TokenTypes
import org.simbrain.world.textworld.DocumentStructureDisplay
import java.awt.Color
import java.awt.Font
import javax.swing.UIManager
import javax.swing.text.Segment

private const val LFM_DOCUMENT_STYLE = "text/lfm-document"

fun RSyntaxTextArea.applyDocumentStructureDisplay(display: DocumentStructureDisplay) {
    if (display == DocumentStructureDisplay.OFF) {
        syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_NONE
        return
    }
    val factory = TokenMakerFactory.getDefaultInstance()
    if (factory is AbstractTokenMakerFactory) {
        factory.putMapping(LFM_DOCUMENT_STYLE, LfmDocumentTokenMaker::class.java.name)
    }
    syntaxEditingStyle = LFM_DOCUMENT_STYLE
    val roleColors = display == DocumentStructureDisplay.ROLE_COLORS
    val conversationFocus = display == DocumentStructureDisplay.CONVERSATION_FOCUS
    val dark = UIManager.getBoolean("laf.dark")
    syntaxScheme = (syntaxScheme.clone() as SyntaxScheme).apply {
        val ordinaryText = if (dark) Color(248, 250, 252) else Color(17, 24, 39)
        val structuralText = when {
            conversationFocus && dark -> Color(91, 100, 115)
            conversationFocus -> Color(180, 185, 195)
            dark -> Color(148, 163, 184)
            else -> Color(107, 114, 128)
        }
        val systemText = when {
            conversationFocus -> structuralText
            dark && roleColors -> Color(216, 180, 254)
            dark -> Color(196, 181, 253)
            roleColors -> Color(109, 40, 217)
            else -> Color(124, 58, 169)
        }
        val toolText = when {
            conversationFocus -> structuralText
            dark && roleColors -> Color(94, 234, 212)
            dark -> Color(94, 220, 200)
            roleColors -> Color(13, 148, 136)
            else -> Color(15, 118, 110)
        }
        val userText = if (dark) Color(147, 197, 253) else Color(37, 99, 235)
        val assistantText = if (dark) Color(110, 231, 183) else Color(5, 120, 87)
        val userRoleText = when {
            conversationFocus -> structuralText
            dark -> Color(96, 165, 250)
            else -> Color(29, 78, 216)
        }
        val assistantRoleText = when {
            conversationFocus -> structuralText
            dark -> Color(52, 211, 153)
            else -> Color(4, 120, 87)
        }
        val userOrOrdinaryText = if (roleColors) userText else ordinaryText
        val assistantOrOrdinaryText = if (roleColors) assistantText else ordinaryText
        setStyle(TokenTypes.IDENTIFIER, Style(ordinaryText))
        setStyle(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, Style(userOrOrdinaryText))
        setStyle(TokenTypes.LITERAL_CHAR, Style(assistantOrOrdinaryText))
        setStyle(TokenTypes.RESERVED_WORD_2, Style(userRoleText, null, font.deriveFont(Font.BOLD)))
        setStyle(TokenTypes.FUNCTION, Style(assistantRoleText, null, font.deriveFont(Font.BOLD)))
        setStyle(TokenTypes.MARKUP_TAG_DELIMITER, Style(structuralText, null, font.deriveFont(Font.BOLD)))
        setStyle(TokenTypes.MARKUP_TAG_NAME, Style(structuralText, null, font.deriveFont(Font.BOLD)))
        setStyle(TokenTypes.RESERVED_WORD, Style(systemText, null, if (roleColors) font.deriveFont(Font.BOLD) else null))
        setStyle(TokenTypes.DATA_TYPE, Style(toolText, null, if (roleColors) font.deriveFont(Font.BOLD) else null))
    }
}

class LfmDocumentTokenMaker : AbstractTokenMaker() {

    override fun getWordsToHighlight() = TokenMap()

    override fun getLastTokenTypeOnLine(segment: Segment, initialTokenType: Int): Int {
        val text = segment.toString()
        var index = 0
        var type = initialTokenType.carriedType()
        while (index < text.length) {
            val markerStart = text.indexOf("<|", index)
            if (markerStart < 0) return type
            val markerEnd = text.indexOf("|>", markerStart)
            if (markerEnd < 0) return type
            val marker = text.substring(markerStart, markerEnd + 2)
            index = markerEnd + 2
            when (marker) {
                "<|im_start|>" -> {
                    var roleEnd = index
                    while (roleEnd < text.length && text[roleEnd].isLetter()) roleEnd++
                    type = text.substring(index, roleEnd).contentTokenType()
                    index = roleEnd
                }
                "<|im_end|>" -> type = TokenTypes.NULL
                "<|tool_call_start|>", "<|tool_response_start|>" -> type = TokenTypes.DATA_TYPE
                "<|tool_call_end|>", "<|tool_response_end|>" -> type = TokenTypes.NULL
            }
        }
        return type
    }

    override fun getMarkOccurrencesOfTokenType(type: Int) = false

    override fun getTokenList(segment: Segment, initialTokenType: Int, startOffset: Int): Token {
        resetTokenList()
        val text = segment.toString()
        var index = 0
        var type = initialTokenType.carriedType()
        while (index < text.length) {
            val markerStart = text.indexOf("<|", index)
            if (markerStart < 0) {
                add(segment, index, text.length - 1, startOffset, type.contentType())
                break
            }
            if (markerStart > index) add(segment, index, markerStart - 1, startOffset, type.contentType())
            val markerEnd = text.indexOf("|>", markerStart)
            if (markerEnd < 0) {
                add(segment, markerStart, text.length - 1, startOffset, type.contentType())
                break
            }
            val marker = text.substring(markerStart, markerEnd + 2)
            add(segment, markerStart, markerEnd + 1, startOffset, TokenTypes.MARKUP_TAG_DELIMITER)
            index = markerEnd + 2
            when (marker) {
                "<|im_start|>" -> {
                    var roleEnd = index
                    while (roleEnd < text.length && text[roleEnd].isLetter()) roleEnd++
                    if (roleEnd > index) {
                        val role = text.substring(index, roleEnd)
                        type = role.contentTokenType()
                        add(segment, index, roleEnd - 1, startOffset, role.labelTokenType())
                        index = roleEnd
                    }
                }
                "<|im_end|>" -> type = TokenTypes.NULL
                "<|tool_call_start|>", "<|tool_response_start|>" -> type = TokenTypes.DATA_TYPE
                "<|tool_call_end|>", "<|tool_response_end|>" -> type = TokenTypes.NULL
            }
        }
        addNullToken()
        return firstToken
    }

    private fun add(segment: Segment, start: Int, end: Int, offset: Int, type: Int) {
        if (end >= start) addToken(segment.array, segment.offset + start, segment.offset + end, type, offset + start)
    }

    private fun Int.contentType() = when (this) {
        TokenTypes.RESERVED_WORD, TokenTypes.DATA_TYPE, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, TokenTypes.LITERAL_CHAR -> this
        else -> TokenTypes.IDENTIFIER
    }

    private fun Int.carriedType() = when (this) {
        TokenTypes.RESERVED_WORD, TokenTypes.DATA_TYPE, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, TokenTypes.LITERAL_CHAR -> this
        else -> TokenTypes.NULL
    }

    private fun String.contentTokenType() = when (this) {
        "system" -> TokenTypes.RESERVED_WORD
        "user" -> TokenTypes.LITERAL_STRING_DOUBLE_QUOTE
        "assistant" -> TokenTypes.LITERAL_CHAR
        else -> TokenTypes.NULL
    }

    private fun String.labelTokenType() = when (this) {
        "user" -> TokenTypes.RESERVED_WORD_2
        "assistant" -> TokenTypes.FUNCTION
        else -> TokenTypes.MARKUP_TAG_NAME
    }
}
