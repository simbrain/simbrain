package org.simbrain.network.gui.nodes

import net.miginfocom.swing.MigLayout
import org.simbrain.network.core.InfoText
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.display
import javax.swing.*

class TextInfoNode(netPanel: NetworkPanel, text: InfoText) : TextNode(netPanel, text) {

    override val isDraggable = false

    override val propertyDialog: StandardDialog? = createInfoTextEditor(text)

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()
            
            // Add the info text position/spacing editor
            val infoTextAction = networkPanel.createAction(
                name = "Info text settings...",
                description = "Set position and spacing for info text",
                iconPath = "menu_icons/Prefs.png"
            ) {
                createInfoTextEditor(textObject as InfoText).display()
            }
            contextMenu.add(infoTextAction)
            
            // Add the general text properties editor
            contextMenu.add(networkPanel.networkActions.setTextPropertiesAction(listOf(this)))
            
            return contextMenu
        }

}

/**
 * Creates a simple editor dialog for InfoText position and spacing settings.
 */
private fun createInfoTextEditor(infoText: InfoText): StandardDialog {
    val panel = JPanel(MigLayout("gap 10px 10px, ins 10 10 0 10"))

    val positionLabel = JLabel("Position:").apply {
        toolTipText = "Where to position the text relative to the subnetwork"
    }
    panel.add(positionLabel)
    
    val positionCombo = JComboBox(InfoText.Position.values()).apply {
        selectedItem = infoText.position
        toolTipText = "Select text position"
    }
    panel.add(positionCombo, "growx, wrap")

    val spacingLabel = JLabel("Spacing:").apply {
        toolTipText = "Distance in pixels from the subnetwork"
    }
    panel.add(spacingLabel)
    
    val spacingField = JTextField(infoText.spacing.toString(), 10).apply {
        toolTipText = "Enter spacing value"
    }
    panel.add(spacingField, "growx")
    
    // Create dialog
    val dialog = StandardDialog(panel).apply {
        title = "Info Text Settings"
    }
    
    // Add commit task to save changes
    dialog.addCommitTask {
        try {
            infoText.position = positionCombo.selectedItem as InfoText.Position
            infoText.spacing = spacingField.text.toDouble()
        } catch (e: NumberFormatException) {
            // Show error message if spacing is not a valid number
            JOptionPane.showMessageDialog(
                dialog,
                "Please enter a valid number for spacing.",
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    
    return dialog
}
