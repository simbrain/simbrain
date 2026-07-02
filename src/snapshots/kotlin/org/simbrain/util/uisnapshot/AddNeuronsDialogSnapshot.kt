package org.simbrain.util.uisnapshot

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.neuron.AddNeuronsDialog
import java.awt.Component
import javax.swing.SwingUtilities

class AddNeuronsDialogSnapshot : UiSnapshotDef {
    override val name = "add_neurons_dialog"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        lateinit var dialog: Component
        SwingUtilities.invokeAndWait {
            val panel = NetworkPanel(component)
            dialog = AddNeuronsDialog(panel)
        }
        return dialog
    }
}
