package org.simbrain.util.uisnapshot

import org.simbrain.network.connections.DistanceBased
import org.simbrain.network.gui.ConnectionStrategyPanel
import org.simbrain.util.StandardDialog
import java.awt.Component
import javax.swing.SwingUtilities

class DistanceBasedConnectionDialogSnapshot : UiSnapshotDef {
    override val name = "distance_based_connection_dialog"

    override fun build(): Component {
        lateinit var dialog: Component
        SwingUtilities.invokeAndWait {
            val panel = ConnectionStrategyPanel(DistanceBased())
            dialog = StandardDialog(null, "Dialog").apply { contentPane = panel }
        }
        return dialog
    }
}
