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
        title = "New Feed-forward Network"
        networkCreationPanel = LayeredNetworkCreationPanel(3, this)
        contentPane = networkCreationPanel
    }

    override fun closeDialogOk() {
        try {
            networkCreationPanel.commit(networkPanel, "FeedForward")
            super.closeDialogOk()
        } catch (nfe: NumberFormatException) {
            showMessage("Inappropriate Field Values (Numbers only in all fields)", MessageKind.ERROR)
            nfe.printStackTrace()
        }
    }
}