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
package org.simbrain.network.gui.nodes

import org.piccolo2d.extras.nodes.PStyledText
import org.piccolo2d.util.PBounds
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.*
import java.awt.event.ActionEvent
import java.util.stream.Collectors
import javax.swing.AbstractAction
import javax.swing.JDialog
import javax.swing.JPopupMenu
import javax.swing.text.*

/**
 * An editable text element, which wraps a PStyledText object.
 */
open class TextNode(
    netPanel: NetworkPanel,
    /**
     * Underlying model text object.
     */
    val textObject: NetworkTextObject
) : ScreenElement(netPanel) {
    /**
     * The text object.
     */
    val pStyledText: PStyledText = PStyledText()

    /**
     * Construct text object at specified location.
     *
     * @param netPanel reference to networkPanel
     * @param text     the network text object
     */
    init {
        pStyledText.document = DefaultStyledDocument()
        this.addChild(pStyledText)

        val events = textObject.events
        events.deleted.on(swingDispatcher) { removeFromParent() }
        events.locationChanged.on { this.recenterTextObject() }
        events.textUpdated.on(swingDispatcher) { this.update() }

        update()
    }

    override val isDraggable: Boolean
        get() = true

    override val contextMenu: JPopupMenu?
        get() {
            val contextMenu = JPopupMenu()

            val actions = networkPanel.networkActions

            contextMenu.add(actions.cutAction)
            contextMenu.add(actions.copyAction)
            contextMenu.add(actions.pasteAction)
            contextMenu.addSeparator()

            val textNodes = networkPanel.selectionManager.selection.stream()
                .filter { obj: ScreenElement? -> TextNode::class.java.isInstance(obj) }
                .map { obj: ScreenElement? -> TextNode::class.java.cast(obj) }
                .collect(Collectors.toSet())
            textNodes.add(this)

            if (textNodes.size == 1) {
                contextMenu.add(object : AbstractAction() {
                    init {
                        putValue(NAME, "Edit Text...")
                    }

                    override fun actionPerformed(e: ActionEvent) {
                        textEntryDialog(textObject.text, "Edit Text", 20, 5) {
                            textObject.setText(it)
                            update()
                        }.display()
                    }
                })
            }

            contextMenu.add(networkPanel.networkActions.setTextPropertiesAction(textNodes))

            contextMenu.addSeparator()
            contextMenu.add(networkPanel.networkActions.deleteAction)

            return contextMenu
        }

    override val model: NetworkTextObject
        get() = textObject

    override fun getBounds(): PBounds {
        return pStyledText.bounds
    }

    /**
     * Update the styled text object based on the model object.
     */
    fun update() {
        try {
            val simpleAttributeSet = createAttributeSet(textObject.fontName, textObject.fontSize, textObject.isItalic, textObject.isBold)
            pStyledText.document.remove(0, pStyledText.document.length)
            pStyledText.document.insertString(0, textObject.text, simpleAttributeSet)
            pStyledText.syncWithDocument()
            recenterTextObject()
        } catch (e: BadLocationException) {
            e.printStackTrace()
        }
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        textObject.location += point(dx, dy)
        recenterTextObject()
    }

    /**
     * Updates the position of the view text based on the position of the model
     * text object.
     */
    private fun recenterTextObject() {
        globalTranslation = textObject.location
        pStyledText.offset = -pStyledText.bounds.center2D
    }

    override val propertyDialog: JDialog?
        get() = textEntryDialog(textObject.text, "Edit Text", 20, 5) { text: String? ->
            textObject.text = text
            update()
        }
}

/**
 * Creates an attribute set of the specified kind.
 *
 * @param fontName name of font in attribute set
 * @param fontSize size of font in attribute set
 * @param italic   italic or not
 * @param bold     bold or not
 * @return the resulting attribute set
 * @author Aaron Dixon
 */
fun createAttributeSet(fontName: String?, fontSize: Int, italic: Boolean, bold: Boolean) = SimpleAttributeSet().apply {
    addAttribute(StyleConstants.CharacterConstants.FontFamily, fontName)
    addAttribute(StyleConstants.CharacterConstants.FontSize, fontSize)
    addAttribute(StyleConstants.CharacterConstants.Italic, italic)
    addAttribute(StyleConstants.CharacterConstants.Bold, bold)
    addAttribute(StyleConstants.ALIGN_RIGHT, true)
}
