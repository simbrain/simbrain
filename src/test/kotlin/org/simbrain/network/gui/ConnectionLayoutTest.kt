/**
 * The pass that lays out a panel's connectors: each connector that asked is laid out once, arrows before what is
 * drawn on them, and the pass is part of the canvas's own update, ahead of its bounds validation.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.piccolo2d.PCanvas
import org.piccolo2d.PNode
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.onEdt
import org.simbrain.util.widgets.BezierArrow
import java.awt.Dimension
import java.awt.geom.Point2D
import javax.swing.JDialog
import javax.swing.SwingUtilities

class ConnectionLayoutTest {

    /** A connector that records what the pass asks of it. */
    private class RecordingConnector(private val label: String, private val log: MutableList<String>) : PNode(), LaidOutConnector {
        override fun layoutArrow() {
            log += "arrow $label"
        }

        override fun placeOnArrow() {
            log += "place $label"
        }
    }

    /** Lets the update a request schedules run. */
    private fun awaitEdt() = repeat(3) { SwingUtilities.invokeAndWait { } }

    @Test
    fun `a connector that asks several times is laid out once, and every arrow is drawn before anything is placed`() {
        val canvas = onEdt { PCanvas() }
        val layout = onEdt { ConnectionLayout(canvas) }
        val log = mutableListOf<String>()
        val first = RecordingConnector("first", log)
        val second = RecordingConnector("second", log)
        onEdt {
            canvas.layer.addChild(first)
            canvas.layer.addChild(second)
            layout.request(first)
            layout.request(second)
            layout.request(first)
        }
        awaitEdt()

        assertEquals(listOf("arrow first", "arrow second", "place first", "place second"), onEdt { log.toList() })
    }

    @Test
    fun `a connector that is not on the canvas is not laid out`() {
        val canvas = onEdt { PCanvas() }
        val layout = onEdt { ConnectionLayout(canvas) }
        val log = mutableListOf<String>()
        val removed = RecordingConnector("removed", log)
        onEdt {
            canvas.layer.addChild(removed)
            layout.request(removed)
            removed.removeFromParent()
        }
        awaitEdt()

        assertTrue(onEdt { log.isEmpty() }, "Laid out: $log")
    }

    @Test
    fun `a request can be made off the event thread`() {
        val canvas = onEdt { PCanvas() }
        val layout = onEdt { ConnectionLayout(canvas) }
        val log = mutableListOf<String>()
        val connector = RecordingConnector("connector", log)
        onEdt { canvas.layer.addChild(connector) }

        layout.request(connector)
        awaitEdt()

        assertEquals(listOf("arrow connector", "place connector"), onEdt { log.toList() })
    }

    @Test
    fun `connectors waiting to be laid out are laid out by the canvas's next update, which leaves bounds settled`() {
        val source = NeuronArray(4).apply { location = Point2D.Double(0.0, 0.0) }
        val target = NeuronArray(4).apply { location = Point2D.Double(0.0, -420.0) }
        val matrix = WeightMatrix(source, target)
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        runBlocking {
            listOf<NetworkModel>(source, target, matrix).forEach { network.addNetworkModel(it, usePlacementManager = false) }
        }
        onEdt {
            panel.preferredSize = Dimension(800, 800)
            JDialog().apply { contentPane = panel; pack() }
            panel.autoZoom = false
        }
        panel.validateCanvas()
        fun arrowBounds() = panel.canvas.layer.allNodes.filterIsInstance<BezierArrow>().single().globalFullBounds

        val report = onEdt {
            val before = arrowBounds()
            val matrixNode = panel.getNode(matrix) as WeightMatrixNode
            // Moves the layer's node at once. The arrow is only asked to follow
            panel.getNode(target)!!.offset(0.0, -200.0)
            panel.connectionLayout.request(matrixNode)
            assertEquals(before, arrowBounds(), "The arrow should not move until the canvas updates")

            panel.canvas.root.processInputs()

            assertEquals(before.height + 200.0, arrowBounds().height, 1e-6)
            panel.canvasBoundsReport()
        }
        assertTrue(report.settled) { "After one update of the canvas: $report" }
    }
}
