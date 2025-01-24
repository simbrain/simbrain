/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.textworld.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.util.TokenizerResult
import org.simbrain.util.widgets.SimbrainTextArea
import org.simbrain.world.textworld.TextWorld
import org.simbrain.world.textworld.textWorldPrefs
import org.simbrain.world.textworld.viewTokenEmbedding
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JToolBar
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent
import javax.swing.text.View


/**
 * Display panel for reading data from user and showing text world's state.
 *
 * @author jyoshimi
 */
class TextWorldPanel constructor(
    val world: TextWorld,
) : JPanel() {

    /**
     * Text area for inputting text into networks.
     */
    val textArea = SimbrainTextArea()

    /**
     * The main scroll panel.
     */
    val inputScrollPane: JScrollPane

    /**
     * Initialize the panel with an open / close toolbar.
     *
     * @param theWorld the reader world to display
     */
    init {

        this.layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
        textArea.lineWrap = true
        textArea.text = world.text
        inputScrollPane =
            JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        add(inputScrollPane)

        // Top toolbar
        val topToolBar = JToolBar()
        topToolBar.add(world.viewTokenEmbedding)
        topToolBar.addSeparator()
        topToolBar.add(world.textWorldPrefs)
        add(topToolBar,  BorderLayout.NORTH)

        // Bottom toolbar
        val bottomToolbarPanel = JPanel()
        bottomToolbarPanel.layout = FlowLayout(FlowLayout.LEFT)
        val toolbarModeSelect = JToolBar()
        bottomToolbarPanel.add(toolbarModeSelect)
        add(bottomToolbarPanel, BorderLayout.SOUTH)

        // Reset text position when user clicks in text area
        textArea.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                world.setPosition(textArea.caretPosition, false)
            }
        })

        // Listener for changes in the textarea (i.e. adding or removing text
        // directly in the area).
        textArea.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(arg0: DocumentEvent) {
                // System.out.println("readerworld: changedUpdate");
                world.setTextNoEvent(textArea.text)
                updateHighlights()
            }

            override fun insertUpdate(arg0: DocumentEvent) {
                // System.out.println("readerworld: insertUpdate");
                world.setTextNoEvent(textArea.text)
                updateHighlights()
            }

            override fun removeUpdate(arg0: DocumentEvent) {
                // System.out.println("readerworld: removeUpdate");
                world.setTextNoEvent(textArea.text)
                updateHighlights()
            }
        })

        // Force component to fill up parent panel
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                inputScrollPane.preferredSize =
                    Dimension(
                        this@TextWorldPanel.preferredSize.width - 25,
                        this@TextWorldPanel.preferredSize.height - 25
                    )
            }
        })
        world.events.textChanged.on(Dispatchers.Swing) {
            textArea.text = world.text
            if (world.position <= textArea.document.length) {
                textArea.caretPosition = world.position
            }
        }

        world.events.cursorPositionChanged.on(Dispatchers.Swing, wait = true) {
            textArea.caretPosition = world.position
        }

        world.events.currentTokenChanged.on(Dispatchers.Swing, wait = true) {
            updateHighlights()
        }

        world.events.preferencesChanged.on(Dispatchers.Swing, wait = true) {

            updateHighlights()
        }

    }

    fun updateHighlights() {
        textArea.highlighter.removeAllHighlights()
        if (world.showTokenBoundaries) {
            world.tokens.forEach(::highlightToken)
        }
        world.tokens.getOrNull(world.currentTokenIndex)?.let { highlight(it.start, it.end + 1) }
    }

    /**
     * Draw boxes around tokens.
     */
    class TokenHighlighter(): DefaultHighlightPainter(Color(0, 0, 0, 0)) {
        override fun paintLayer(
            g: Graphics, offs0: Int, offs1: Int,
            bounds: Shape?, c: JTextComponent?, view: View?
        ): Shape {
            val s = super.paintLayer(g, offs0, offs1, bounds, c, view)
            if (s is Rectangle) {
                val g2 = g.create() as Graphics2D
                g2.paint = Color.GRAY
                val r = s.bounds
                g2.drawRect(r.x, r.y, r.width, r.height - 1)
                g2.dispose()
            }
            return s
        }
    }

    internal inner class MyHighlightPainter(color: Color?) : DefaultHighlightPainter(color)

    fun highlight(begin: Int, end: Int) {
        if (!world.highlightCurrentToken) return
        // An instance of the private subclass of the default highlight painter
        val myHighlightPainter: Highlighter.HighlightPainter = MyHighlightPainter(world.highlightColor)
        removeHighlights(textArea)
        try {
            val hilite = textArea.highlighter
            hilite.addHighlight(begin, end, myHighlightPainter)
        } catch (e: BadLocationException) {
            System.err.checkError()
        }
    }

    fun highlightToken(token: TokenizerResult) {
        val tokenHighlighter = TokenHighlighter()
        try {
            textArea.highlighter.addHighlight(token.start, token.end + 1, tokenHighlighter)
        } catch (e: BadLocationException) {
            System.err.checkError()
        }
    }


    fun removeHighlights(textComp: JTextComponent) {
        val hilite = textComp.highlighter
        val hilites = hilite.highlights
        for (i in hilites.indices) {
            if (hilites[i].painter is MyHighlightPainter) {
                hilite.removeHighlight(hilites[i])
            }
        }
    }


}