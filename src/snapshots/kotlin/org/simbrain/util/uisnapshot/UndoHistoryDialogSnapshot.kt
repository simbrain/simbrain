package org.simbrain.util.uisnapshot

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.buildUndoHistoryDialog
import java.awt.Component
import javax.swing.SwingUtilities

class UndoHistoryDialogSnapshot : UiSnapshotDef {
    override val name = "undo_history_dialog"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component)
        repeat(4) { i ->
            panel.undoManager.addUndoableAction("Add neuron ${i + 1}", undo = {}, redo = {})
        }
        lateinit var dialog: Component
        SwingUtilities.invokeAndWait {
            dialog = panel.buildUndoHistoryDialog()
        }
        return dialog
    }
}
