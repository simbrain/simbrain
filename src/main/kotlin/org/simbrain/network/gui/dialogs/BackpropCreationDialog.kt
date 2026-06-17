package org.simbrain.network.gui.dialogs

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.showErrorDialog

/**
 * Dialog for creating a Backprop network.
 */
class BackpropCreationDialog(networkPanel: NetworkPanel) : FeedForwardCreationDialog(networkPanel) {

    init {
        title = "New Backprop Network"
    }

    override fun closeDialogOk() {
        try {
            networkCreationPanel.commit(networkPanel, "Backprop")
            dispose()
        } catch (nfe: NumberFormatException) {
            showErrorDialog("Inappropriate Field Values (Numbers only in all fields)")
            nfe.printStackTrace()
        }
    }
}