/**
 * The connector layout cases simulation builds, and across all of its cases no weight matrix image is drawn over a
 * layer or over another image.
 */
package org.simbrain.custom_sims.simulations.demos

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.canvasBoundsReport
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.network.gui.validateCanvas
import org.simbrain.util.onEdt
import java.awt.Dimension
import javax.swing.JDialog

class ConnectorLayoutCasesTest {

    @Test
    fun `the simulation builds without a desktop`() {
        runBlocking { connectorLayoutCases.run() }
    }

    @Test
    fun `no weight matrix image in any case is drawn over a layer or over another image`() {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        runBlocking { network.addConnectorLayoutCases() }
        onEdt {
            panel.preferredSize = Dimension(1200, 820)
            JDialog().apply { contentPane = panel; pack() }
            panel.autoZoom = false
        }
        panel.validateCanvas()
        panel.validateCanvas()

        onEdt {
            val layers = panel.screenElements.filter { it.isConnectorObstacle }.map { it.model.id to it.footprint }
            val images = network.getModels<WeightMatrix>().map {
                it.id to (panel.getNode(it) as WeightMatrixNode).imageBox.globalBounds
            }
            assertTrue(images.size > 20, "Only ${images.size} weight matrices")
            val problems = mutableListOf<String>()
            for ((matrix, image) in images) {
                layers.filter { (_, layer) -> image.intersects(layer) }.forEach { (layer, _) -> problems += "$matrix over $layer" }
            }
            for (i in images.indices) {
                for (j in i + 1 until images.size) {
                    if (images[i].second.intersects(images[j].second)) problems += "${images[i].first} over ${images[j].first}"
                }
            }
            assertTrue(problems.isEmpty()) { problems.joinToString() }
        }
        assertTrue(panel.canvasBoundsReport().settled)
    }
}
