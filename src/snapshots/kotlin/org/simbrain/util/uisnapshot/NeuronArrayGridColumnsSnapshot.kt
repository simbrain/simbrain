/**
 * Neuron arrays drawn as grids with an explicit number of columns, in both pixel and circle mode.
 *
 * Top row: a 20-neuron pixel grid that is automatic (square), ten wide, and five wide but transposed by a
 * vertical layout. Bottom row: the same three as circles, labelled by index so the fill order is visible,
 * with the last one using wider horizontal spacing. Pixels should stay square, borders should hug the
 * grid, and the transposed grids should count down the columns rather than across the rows.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.piccolo2d.util.PBounds
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class NeuronArrayGridColumnsSnapshot : UiSnapshotDef {
    override val name = "neuron_array_grid_columns"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1400, 800)
        }

        fun array(x: Int, y: Int, circles: Boolean, columns: Int, vertical: Boolean) = NeuronArray(20).apply {
            location = point(x, y)
            activationArray = DoubleArray(20) { it / 20.0 }
            labelArray = Array(20) { "$it" }
            gridMode = true
            circleMode = circles
            gridColumns = columns
            verticalLayout = vertical
        }

        val arrays = listOf(
            array(0, 0, circles = false, columns = 0, vertical = false),
            array(300, 0, circles = false, columns = 10, vertical = false),
            array(700, 0, circles = false, columns = 5, vertical = true),
            array(0, 400, circles = true, columns = 0, vertical = false),
            array(300, 400, circles = true, columns = 10, vertical = false),
            array(700, 400, circles = true, columns = 5, vertical = true).apply { circleSpacingX = 90.0 },
        )
        runBlocking {
            arrays.forEach { network.addNetworkModel(it, usePlacementManager = false) }
        }
        SwingUtilities.invokeAndWait { JDialog().apply { contentPane = panel; pack() } }
        Thread.sleep(300)
        repeat(8) { SwingUtilities.invokeAndWait { } }

        SwingUtilities.invokeAndWait {
            val content = panel.canvas.layer.fullBounds
            panel.canvas.camera.setViewBounds(
                PBounds(content.x - PAD, content.y - PAD, content.width + 2 * PAD, content.height + 2 * PAD)
            )
        }
        repeat(3) { SwingUtilities.invokeAndWait { } }
        return panel
    }

    companion object {
        private const val PAD = 20.0
    }
}
