package org.simbrain.util.uisnapshot

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import java.awt.Component
import javax.swing.SwingUtilities

class NetworkToolbarSnapshot : UiSnapshotDef {
    override val name = "network_toolbar"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        lateinit var toolbar: Component
        SwingUtilities.invokeAndWait {
            toolbar = NetworkPanel(component).mainToolBar
        }
        return toolbar
    }
}
