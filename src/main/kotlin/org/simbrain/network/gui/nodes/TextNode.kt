package org.simbrain.network.gui.nodes

import org.piccolo2d.extras.nodes.PStyledText
import org.piccolo2d.util.PBounds
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.*
import java.util.stream.Collectors
import javax.swing.JPopupMenu
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

/**
 * An editable text element, which wraps a PStyledText object.
 */
open class TextNode(
    netPanel: NetworkPanel,
    val textObject: NetworkTextObject
) : ScreenElement(netPanel) {

    val pStyledText: PStyledText = PStyledText()

    init {
        pStyledText.document = DefaultStyledDocument()
        this.addChild(pStyledText)

        val events = textObject.events
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
            contextMenu.add(actions.duplicateAction)
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()

            val textNodes = networkPanel.selectionManager.selection.stream()
                .filter { obj: ScreenElement? -> TextNode::class.java.isInstance(obj) }
                .map { obj: ScreenElement? -> TextNode::class.java.cast(obj) }
                .collect(Collectors.toSet())
            textNodes.add(this)

            if (textNodes.size == 1) {
                contextMenu.add(networkPanel.createAction(name = "Edit ${textObject.displayName}...") {
                    textEntryDialog(textObject.text, "Edit Text", 20, 5) {
                        textObject.text = it
                        update()
                    }.display()
                })
            }

            contextMenu.add(networkPanel.networkActions.setTextPropertiesAction(textNodes))
            contextMenu.addSeparator()

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

    override val toolTipText: String?
        get() = "Location: (" + textObject.locationX.toInt() + "," + textObject.locationY.toInt() + ")"

    /**
     * Updates the position of the view text based on the position of the model
     * text object.
     */
    private fun recenterTextObject() {
        globalTranslation = textObject.location
        pStyledText.offset = -pStyledText.bounds.center2D
    }

    override val propertyDialog: StandardDialog?
        get() = textEntryDialog(textObject.text, "Edit Text", 20, 5) { text ->
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
