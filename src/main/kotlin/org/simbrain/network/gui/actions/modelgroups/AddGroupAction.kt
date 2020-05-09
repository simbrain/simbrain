package org.simbrain.network.gui.actions.modelgroups

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.StandardDialog
import java.awt.event.ActionEvent
import javax.swing.AbstractAction

class AddGroupAction(
        val networkPanel: NetworkPanel,
        name: String,
        val createDialog: AddGroupAction.(NetworkPanel) -> StandardDialog
) : AbstractAction(name) {

    init {
        putValue(SHORT_DESCRIPTION, "Add $name group to network")
    }

    override fun actionPerformed(e: ActionEvent) {
        networkPanel.repaint()
        with(createDialog(networkPanel)) {
            pack()
            setLocationRelativeTo(networkPanel)
            isVisible = true

            // Not sure why call below needed, but for some reason the ok button
            // sometimes goes out of focus when creating a new dialog.
            // TODO: copied from old code. check if note still valid
            rootPane.defaultButton = okButton
        }
    }

}
