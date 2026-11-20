package org.simbrain.network.gui.dialogs

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.MessageKind
import org.simbrain.util.showMessage

/**
 * Dialog for creating a Backprop network.
 */
class BackpropCreationDialog(networkPanel: NetworkPanel) : FeedForwardCreationDialog(networkPanel) {

    init {
        title = "New backprop network"
    }

    override fun closeDialogOk() {
        try {
            networkCreationPanel.commit(networkPanel, "Backprop")
            dispose()
        } catch (nfe: NumberFormatException) {
            showMessage("Inappropriate field values (numbers only in all fields)", MessageKind.ERROR)
            nfe.printStackTrace()
        }
    }
}