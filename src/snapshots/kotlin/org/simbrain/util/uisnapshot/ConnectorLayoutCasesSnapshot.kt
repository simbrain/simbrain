/**
 * The network of the connector layout cases simulation: a grid of common arrangements of layers and connectors, each
 * under its title. See [org.simbrain.custom_sims.simulations.demos.connectorLayoutCases].
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.piccolo2d.util.PBounds
import org.simbrain.custom_sims.simulations.demos.addConnectorLayoutCases
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class ConnectorLayoutCasesSnapshot : UiSnapshotDef {
    override val name = "connector_layout_cases"

    override fun build(): Component {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("snapshot", network)).apply {
            preferredSize = Dimension(2200, 2800)
        }
        runBlocking { network.addConnectorLayoutCases() }
        SwingUtilities.invokeAndWait { JDialog().apply { contentPane = panel; pack() } }
        Thread.sleep(500)
        repeat(8) { SwingUtilities.invokeAndWait { } }

        SwingUtilities.invokeAndWait {
            val content = panel.canvas.layer.fullBounds
            panel.canvas.camera.setViewBounds(
                PBounds(content.x - PAD, content.y - PAD, content.width + 2 * PAD, content.height + 2 * PAD)
            )
        }
        Thread.sleep(200)
        repeat(4) { SwingUtilities.invokeAndWait { } }
        return panel
    }

    companion object {
        private const val PAD = 30.0
    }
}
