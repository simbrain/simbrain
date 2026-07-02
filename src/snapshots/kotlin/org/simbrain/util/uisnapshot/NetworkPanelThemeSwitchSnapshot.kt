package org.simbrain.util.uisnapshot

import com.formdev.flatlaf.FlatDarkLaf
import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

/**
 * Builds the panel under the LIGHT theme (so its nodes are constructed with light colors), then
 * installs the DARK look-and-feel and runs the live recolor hook ([NetworkPanel.preferenceLoader]).
 * A correct dark result proves the live recolor path works (not just construction-time theming).
 */
class NetworkPanelThemeSwitchSnapshot : UiSnapshotDef {
    override val name = "network_panel_theme_switch"

    override fun build(): Component {
        setupTheme("light")
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply { preferredSize = Dimension(600, 400) }
        runBlocking {
            val a = Neuron().apply { location = point(-80.0, 0.0) }
            val b = Neuron().apply { location = point(80.0, 0.0) }
            network.addNetworkModel(a, usePlacementManager = false)
            network.addNetworkModel(b, usePlacementManager = false)
            network.addNetworkModel(Synapse(a, b, 0.7), usePlacementManager = false)
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            network.events.zoomToFitPage.fire()
            // Live theme switch: install the dark look-and-feel, then run the recolor hook.
            FlatDarkLaf.setup()
            panel.preferenceLoader()
        }
        return panel
    }
}
