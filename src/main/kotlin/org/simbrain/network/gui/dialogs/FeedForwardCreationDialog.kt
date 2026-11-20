package org.simbrain.network.gui.dialogs

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.MessageKind
import org.simbrain.util.StandardDialog
import org.simbrain.util.showMessage

/**
 * Dialog for creating a generic layered network.
 */
open class FeedForwardCreationDialog(protected val networkPanel: NetworkPanel) : StandardDialog() {

    protected val networkCreationPanel: LayeredNetworkCreationPanel

    init {
        title = "New feed-forward network"
        networkCreationPanel = LayeredNetworkCreationPanel(3, this)
        contentPane = networkCreationPanel
    }

    override fun closeDialogOk() {
        try {
            networkCreationPanel.commit(networkPanel, "FeedForward")
            super.closeDialogOk()
        } catch (nfe: NumberFormatException) {
            showMessage("Inappropriate field values (numbers only in all fields)", MessageKind.ERROR)
            nfe.printStackTrace()
        }
    }
}