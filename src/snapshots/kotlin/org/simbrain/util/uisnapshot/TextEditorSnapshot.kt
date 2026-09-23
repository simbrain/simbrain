/** Shows single and mixed-selection text/font editing, including the explicit bulk replacement control. */
package org.simbrain.util.uisnapshot

import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.gui.dialogs.text.TextEditorPanel
import java.awt.Component
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.SwingUtilities

class TextEditorSnapshot : UiSnapshotDef {
    override val name = "text_editor"

    override fun build(): Component {
        lateinit var result: JPanel
        SwingUtilities.invokeAndWait {
            result = JPanel(GridLayout(1, 2, 16, 0)).apply {
                border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
                add(TextEditorPanel(listOf(NetworkTextObject("Input layer\nSensory features"))).apply {
                    border = BorderFactory.createTitledBorder("One text object")
                })
                add(TextEditorPanel(listOf(
                    NetworkTextObject("Input").apply { fontName = Font.SERIF; fontSize = 14; isBold = true },
                    NetworkTextObject("Output").apply { fontName = Font.MONOSPACED; fontSize = 20; isItalic = true }
                )).apply {
                    border = BorderFactory.createTitledBorder("Two text objects with different values")
                })
            }
        }
        return result
    }
}
