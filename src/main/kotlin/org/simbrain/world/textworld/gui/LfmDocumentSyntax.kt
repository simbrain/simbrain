/**
 * Role and structure coloring for LFM2 chat documents shown in the text world: `<|...|>`
 * markers and role labels render bold and monospaced, and per-role content gets foreground
 * colors, so the chat scaffolding reads at a glance against the proportional prose. [documentStructureRuns] is the pure structural scan; the
 * [applyDocumentStructureDisplay] extension paints the runs onto a [JTextPane]'s styled
 * document. Attribute application fires document CHANGE events, so callers that listen to the
 * document (e.g. [TextWorldPanel]) must guard against re-entrant handling, and should wrap
 * calls in [org.simbrain.util.widgets.SimbrainTextPane.withUndoSuspended] to keep styling out
 * of the undo history.
 */
package org.simbrain.world.textworld.gui

import org.simbrain.world.textworld.DocumentStructureDisplay
import java.awt.Color
import java.awt.Font
import javax.swing.JTextPane
import javax.swing.UIManager
import javax.swing.text.AttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

private const val APPLIED_STRUCTURE_KEY = "simbrain.appliedDocumentStructure"

/** A foreground color with an explicit variant for each look-and-feel. */
internal data class ThemedColor(val light: Color, val dark: Color) {
    fun on(dark: Boolean) = if (dark) this.dark else light
}

private val ink = ThemedColor(Color(17, 24, 39), Color(248, 250, 252))
private val scaffold = ThemedColor(Color(107, 114, 128), Color(148, 163, 184))
private val dimmedScaffold = ThemedColor(Color(180, 185, 195), Color(91, 100, 115))
private val systemAccent = ThemedColor(Color(109, 40, 217), Color(216, 180, 254))
private val toolAccent = ThemedColor(Color(13, 148, 136), Color(94, 234, 212))
private val userAccent = ThemedColor(Color(37, 99, 235), Color(147, 197, 253))
private val assistantAccent = ThemedColor(Color(5, 120, 87), Color(110, 231, 183))
private val userLabelAccent = ThemedColor(Color(29, 78, 216), Color(96, 165, 250))
private val assistantLabelAccent = ThemedColor(Color(4, 120, 87), Color(52, 211, 153))

internal data class RegionStyle(
    val color: ThemedColor,
    val bold: Boolean = false,
    val monospaced: Boolean = false,
)

private fun scaffoldStyle(color: ThemedColor) = RegionStyle(color, bold = true, monospaced = true)

/**
 * Each region carries its style for the two display modes that style at all (OFF clears
 * styling before the palette is consulted). Scaffolding — markers and role labels — renders
 * monospaced, so the machine tokens read as machinery against the proportional prose.
 */
internal enum class DocumentRegion(
    private val roleColorsStyle: RegionStyle,
    private val conversationFocusStyle: RegionStyle,
) {
    ORDINARY(RegionStyle(ink), RegionStyle(ink)),
    MARKER(scaffoldStyle(scaffold), scaffoldStyle(dimmedScaffold)),
    SYSTEM_CONTENT(RegionStyle(systemAccent, bold = true), RegionStyle(dimmedScaffold)),
    TOOL_LIST(
        RegionStyle(systemAccent, bold = true, monospaced = true),
        RegionStyle(dimmedScaffold, monospaced = true),
    ),
    TOOL_CONTENT(
        RegionStyle(toolAccent, bold = true, monospaced = true),
        RegionStyle(dimmedScaffold, monospaced = true),
    ),
    USER_CONTENT(RegionStyle(userAccent), RegionStyle(ink)),
    ASSISTANT_CONTENT(RegionStyle(assistantAccent), RegionStyle(ink)),
    USER_LABEL(scaffoldStyle(userLabelAccent), scaffoldStyle(dimmedScaffold)),
    ASSISTANT_LABEL(scaffoldStyle(assistantLabelAccent), scaffoldStyle(dimmedScaffold)),
    OTHER_LABEL(scaffoldStyle(scaffold), scaffoldStyle(dimmedScaffold));

    fun style(display: DocumentStructureDisplay): RegionStyle? = when (display) {
        DocumentStructureDisplay.OFF -> null
        DocumentStructureDisplay.ROLE_COLORS -> roleColorsStyle
        DocumentStructureDisplay.CONVERSATION_FOCUS -> conversationFocusStyle
    }
}

internal data class StructureRun(val start: Int, val end: Int, val region: DocumentRegion)

/** The fixed prefix [org.simbrain.network.llm.Lfm2ChatFormat.toolListLine] emits. */
private const val TOOL_LIST_PREFIX = "List of tools:"

/**
 * Splits [text] into contiguous structural runs: `<|...|>` markers, the role word after
 * `<|im_start|>`, and content regions carried until the next marker changes them. An
 * unterminated `<|` is treated as content of the current region. Inside system content, the
 * chat template's tool advertisement line is split out as [DocumentRegion.TOOL_LIST], so the
 * machine-formatted line can style differently from prose system prompts.
 */
internal fun documentStructureRuns(text: String): List<StructureRun> {
    val runs = ArrayList<StructureRun>()
    fun add(start: Int, end: Int, region: DocumentRegion) {
        if (end <= start) return
        if (region != DocumentRegion.SYSTEM_CONTENT) {
            runs.add(StructureRun(start, end, region))
            return
        }
        var lineStart = start
        while (lineStart < end) {
            val newline = text.indexOf('\n', lineStart)
            val lineEnd = if (newline in lineStart until end) newline + 1 else end
            val lineRegion = if (text.startsWith(TOOL_LIST_PREFIX, lineStart)) {
                DocumentRegion.TOOL_LIST
            } else {
                region
            }
            runs.add(StructureRun(lineStart, lineEnd, lineRegion))
            lineStart = lineEnd
        }
    }

    var index = 0
    var content = DocumentRegion.ORDINARY
    while (index < text.length) {
        val markerStart = text.indexOf("<|", index)
        if (markerStart < 0) {
            add(index, text.length, content)
            break
        }
        add(index, markerStart, content)
        val markerEnd = text.indexOf("|>", markerStart)
        if (markerEnd < 0) {
            add(markerStart, text.length, content)
            break
        }
        val marker = text.substring(markerStart, markerEnd + 2)
        add(markerStart, markerEnd + 2, DocumentRegion.MARKER)
        index = markerEnd + 2
        when (marker) {
            "<|im_start|>" -> {
                var roleEnd = index
                while (roleEnd < text.length && text[roleEnd].isLetter()) roleEnd++
                if (roleEnd > index) {
                    val role = text.substring(index, roleEnd)
                    content = when (role) {
                        "system" -> DocumentRegion.SYSTEM_CONTENT
                        "user" -> DocumentRegion.USER_CONTENT
                        "assistant" -> DocumentRegion.ASSISTANT_CONTENT
                        else -> DocumentRegion.ORDINARY
                    }
                    val label = when (role) {
                        "user" -> DocumentRegion.USER_LABEL
                        "assistant" -> DocumentRegion.ASSISTANT_LABEL
                        else -> DocumentRegion.OTHER_LABEL
                    }
                    add(index, roleEnd, label)
                    index = roleEnd
                }
            }
            "<|im_end|>" -> content = DocumentRegion.ORDINARY
            "<|tool_call_start|>", "<|tool_response_start|>" -> content = DocumentRegion.TOOL_CONTENT
            "<|tool_call_end|>", "<|tool_response_end|>" -> content = DocumentRegion.ORDINARY
        }
    }
    return runs
}

fun JTextPane.applyDocumentStructureDisplay(display: DocumentStructureDisplay) {
    val doc = styledDocument
    val text = doc.getText(0, doc.length)
    val dark = UIManager.getBoolean("laf.dark")
    // Attribute sweeps run on every generated token; skip when nothing changed.
    val applied = Triple(display, dark, text)
    if (getClientProperty(APPLIED_STRUCTURE_KEY) == applied) return
    putClientProperty(APPLIED_STRUCTURE_KEY, applied)
    doc.setCharacterAttributes(0, doc.length, SimpleAttributeSet(), true)
    if (display == DocumentStructureDisplay.OFF) return
    val attributes = DocumentRegion.entries.associateWith { it.style(display)?.toAttributes(dark) }
    documentStructureRuns(text).forEach { run ->
        attributes[run.region]?.let {
            doc.setCharacterAttributes(run.start, run.end - run.start, it, true)
        }
    }
}

private fun RegionStyle.toAttributes(dark: Boolean): AttributeSet = SimpleAttributeSet().apply {
    StyleConstants.setForeground(this, color.on(dark))
    if (bold) StyleConstants.setBold(this, true)
    if (monospaced) StyleConstants.setFontFamily(this, Font.MONOSPACED)
}
