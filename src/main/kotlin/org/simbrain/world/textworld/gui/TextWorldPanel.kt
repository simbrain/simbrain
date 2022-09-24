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

import org.simbrain.world.textworld.TextWorld
import org.simbrain.world.textworld.TextWorldActions.getExtractDictionaryAction
import org.simbrain.world.textworld.TextWorldActions.showDictionaryEditor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent

/**
 * Display panel for reading data from user and showing text world's state.
 *
 * @author jyoshimi
 */
class TextWorldPanel private constructor(
    val world: TextWorld, toolbar: JToolBar?
) : JPanel() {

    /**
     * Text area for inputting text into networks.
     */
    private val textArea = JTextArea()

    /**
     * The main scroll panel.
     */
    val inputScrollPane: JScrollPane

    /**
     * Displays the current parse style and allows it to be set.
     */
    private val parseStyle = ButtonGroup()

    /**
     * Parse style is word-based.
     */
    private val wordButton = JRadioButton("Word")

    /**
     * Parse style is character based.
     */
    private val charButton = JRadioButton("Character")

    /**
     * Toolbar for opening and closing the world. Must be defined at component
     * level.
     */
    private var openCloseToolBar: JToolBar? = null

    /**
     * Initialize the panel with an open / close toolbar.
     *
     * @param theWorld the reader world to display
     * @param toolbar  the openClose toolbar.
     */
    init {
        openCloseToolBar = toolbar
        this.layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
        textArea.lineWrap = true
        textArea.text = world.text
        inputScrollPane =
            JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        add(inputScrollPane)

        // Add toolbars
        val topToolbarPanel = JPanel()
        topToolbarPanel.layout = FlowLayout(FlowLayout.LEFT)
        // if (openCloseToolBar != null) {
        //     topToolbarPanel.add(openCloseToolBar);
        // }
        val dictionaryToolBar = JToolBar()
        dictionaryToolBar.add(showDictionaryEditor(world))
        dictionaryToolBar.add(getExtractDictionaryAction(world))
        topToolbarPanel.add(dictionaryToolBar)
        add(topToolbarPanel, BorderLayout.NORTH)
        val bottomToolbarPanel = JPanel()
        bottomToolbarPanel.layout = FlowLayout(FlowLayout.LEFT)
        bottomToolbarPanel.add(toolbarModeSelect)
        add(bottomToolbarPanel, BorderLayout.SOUTH)

        // Syncs the parse style buttons to the underlying model state.
        if (world.parseStyle === TextWorld.ParseStyle.CHARACTER) {
            charButton.isSelected = true
        } else if (world.parseStyle === TextWorld.ParseStyle.WORD) {
            wordButton.isSelected = true
        }

    }

    /**
     * Init the listeners, called by factor method, outside the constructor.
     */
    private fun initListeners() {

        // Reset text position when user clicks in text area
        textArea.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                world.setPosition(textArea.caretPosition, false)
                world.updateMatcher()
            }
        })

        // Listener for changes in the textarea (i.e. adding or removing text
        // directly in the area).
        textArea.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(arg0: DocumentEvent) {
                //System.out.println("readerworld: changedUpdate");
                world.setText(textArea.text, false)
            }

            override fun insertUpdate(arg0: DocumentEvent) {
                //System.out.println("readerworld: insertUpdate");
                world.setText(textArea.text, false)
            }

            override fun removeUpdate(arg0: DocumentEvent) {
                //System.out.println("readerworld: removeUpdate");
                world.setText(textArea.text, false)
            }
        })

        // Force component to fill up parent panel
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                // textArea.setPreferredSize(ReaderPanel.this.getPreferredSize());
                inputScrollPane.preferredSize =
                    Dimension(
                        this@TextWorldPanel.preferredSize.width - 25,
                        this@TextWorldPanel.preferredSize.height - 25
                    )
                // inputScrollPane.revalidate();
            }
        })
        world.events.onTextChanged {
            textArea.text = world.text
            if (world.position < textArea.document.length) {
                textArea.caretPosition = world.position
            }
        }

        world.events.onCursorPositionChanged {
            textArea.caretPosition = world.position
        }

        world.events.onCurrentTokenChanged {
            if (it!!.text.equals("", ignoreCase = true)) {
                removeHighlights(textArea)
            } else {
                highlight(it!!.beginPosition, it!!.endPosition)
            }
        }

        //     override fun preferencesChanged() {
        //         if (world.parseStyle === TextWorld.ParseStyle.CHARACTER) {
        //             charButton.isSelected = true
        //         } else if (world.parseStyle === TextWorld.ParseStyle.WORD) {
        //             wordButton.isSelected = true
        //         }
        //     }
        // })
    }

    /**
     * Highlight word beginning at `begin` nd ending at
     * `end`.
     *
     * @param begin offset of beginning of highlight
     * @param end   offset of end of highlight
     */
    fun highlight(begin: Int, end: Int) {
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

    /**
     * Removes highlights from specified component.
     *
     * @param textComp text component to remove highlights from.
     */
    fun removeHighlights(textComp: JTextComponent) {
        val hilite = textComp.highlighter
        val hilites = hilite.highlights
        for (i in hilites.indices) {
            if (hilites[i].painter is MyHighlightPainter) {
                hilite.removeHighlight(hilites[i])
            }
        }
    }

    /**
     * A private subclass of the default highlight painter.
     */
    internal inner class MyHighlightPainter
    /**
     * Sets the color of highlighter.
     *
     * @param color Color of highlight
     */
        (color: Color?) :
        DefaultHighlightPainter(color)// add action listener for switching between char and word buttons:
    // wordButton.addActionListener(a);
    /**
     * Return a toolbar with buttons for switching between word and character
     * mode.
     *
     * @return the mode selection toolbar
     */
    val toolbarModeSelect: JToolBar
        get() {
            val toolbar = JToolBar()
            parseStyle.add(wordButton)
            parseStyle.add(charButton)
            wordButton.isSelected = true
            toolbar.add(wordButton)
            toolbar.add(charButton)
            wordButton.addActionListener { world.parseStyle = TextWorld.ParseStyle.WORD }
            charButton.addActionListener { world.parseStyle = TextWorld.ParseStyle.CHARACTER }

            // add action listener for switching between char and word buttons:
            // wordButton.addActionListener(a);
            return toolbar
        }

    companion object {
        /**
         * Factory method for panel (so that listeners are not created in
         * constructor).
         *
         * @param theWorld the world
         * @param toolbar  pass in open / close toolbar
         * @return the constructed panel
         */
        fun createReaderPanel(theWorld: TextWorld, toolbar: JToolBar?): TextWorldPanel {
            val panel = TextWorldPanel(theWorld, toolbar)
            panel.initListeners()
            return panel
        }

        /**
         * Factory method for panel (so that listeners are not created in
         * constructor).
         *
         * @param theWorld the world
         * @return the constructed panel
         */
        fun createReaderPanel(theWorld: TextWorld): TextWorldPanel {
            val panel = TextWorldPanel(theWorld, null)
            panel.initListeners()
            return panel
        }
    }
}