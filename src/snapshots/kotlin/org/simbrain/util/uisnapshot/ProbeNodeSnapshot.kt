package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.trainers.createProbe
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.NetworkTheme
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

/**
 * A probe on a small host network, with a prediction computed and the dataset marked stale, so the
 * probe's custom info text (probed layer, predicted label, staleness) renders below its outline.
 */
class ProbeNodeSnapshot : UiSnapshotDef {
    override val name = "probe_node"

    override fun build(): Component {
        // Building the panel runs app code that can reinstall its own LookAndFeel; remember the
        // harness-requested mode so the canvas can be recolored under it before the snapshot.
        val requestedDark = NetworkTheme.isDark
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply { preferredSize = Dimension(900, 500) }
        runBlocking {
            val hostInput = NeuronArray(4).apply { isClamped = true; location = point(0.0, 250.0) }
            val hostHidden = NeuronArray(3).apply { location = point(0.0, 0.0) }
            network.addNetworkModel(hostInput, usePlacementManager = false)
            network.addNetworkModel(hostHidden, usePlacementManager = false)
            network.addNetworkModel(WeightMatrix(hostInput, hostHidden), usePlacementManager = false)
            val probe = with(network) {
                createProbe(hostHidden, readoutSize = 2, readoutLabels = arrayOf("No loop", "Loop"), label = "Loop probe")
            }
            with(network) {
                hostInput.setActivations(doubleArrayOf(1.0, 0.0, 1.0, 0.0))
                hostHidden.accumulateInputs()
                hostHidden.update()
                probe.refreshOutput()
            }
            probe.datasetRebuilder = { }
            probe.stale = true
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            setupTheme(if (requestedDark) "dark" else "light")
            panel.preferenceLoader()
            network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
