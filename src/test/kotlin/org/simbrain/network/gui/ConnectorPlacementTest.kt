/**
 * What connectors draw on their arrows is kept clear of layers and of each other on a panel: drawings that would
 * land on the same spot are moved apart, a drawing whose arrow passes a layer is moved off that layer, and a drawing
 * gets out of the way of a layer dragged across it.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.piccolo2d.util.PBounds
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.onEdt
import java.awt.Dimension
import java.awt.geom.Point2D
import javax.swing.JDialog

class ConnectorPlacementTest {

    /** A panel laid out at a fixed size and validated, showing [network] with [models] added to it. */
    private fun panelFor(network: Network, vararg models: NetworkModel): NetworkPanel {
        val panel = NetworkPanel(NetworkComponent("test", network))
        runBlocking { models.forEach { network.addNetworkModel(it, usePlacementManager = false) } }
        onEdt {
            panel.preferredSize = Dimension(800, 800)
            JDialog().apply { contentPane = panel; pack() }
            panel.autoZoom = false
        }
        panel.validateCanvas()
        return panel
    }

    private fun arrayAt(x: Double, y: Double) = NeuronArray(4).apply { location = Point2D.Double(x, y) }

    private fun NetworkPanel.footprintOf(model: NetworkModel): PBounds = onEdt { getNode(model)!!.footprint }

    private fun NetworkPanel.imageOf(matrix: WeightMatrix): PBounds =
        onEdt { (getNode(matrix) as WeightMatrixNode).imageBox.globalBounds }

    @Test
    fun `two matrices whose arrows cross are drawn apart`() {
        val lowerLeft = arrayAt(0.0, 0.0)
        val lowerRight = arrayAt(230.0, 0.0)
        val upperLeft = arrayAt(0.0, -520.0)
        val upperRight = arrayAt(230.0, -520.0)
        val rising = WeightMatrix(lowerLeft, upperRight)
        val falling = WeightMatrix(lowerRight, upperLeft)
        val panel = panelFor(Network(), lowerLeft, lowerRight, upperLeft, upperRight, rising, falling)

        val first = panel.footprintOf(rising)
        val second = panel.footprintOf(falling)
        assertFalse(first.intersects(second), "first $first second $second")
        listOf(lowerLeft, lowerRight, upperLeft, upperRight).forEach { layer ->
            assertFalse(second.intersects(panel.footprintOf(layer)), "second $second overlaps a layer")
        }
    }

    @Test
    fun `a matrix whose arrow passes a layer it does not join is drawn clear of that layer`() {
        val bottom = arrayAt(0.0, 0.0)
        val middle = arrayAt(160.0, -400.0)
        val top = arrayAt(320.0, -800.0)
        val skip = WeightMatrix(bottom, top)
        val panel = panelFor(Network(), bottom, middle, top, skip)

        // The middle of the arrow is behind the middle layer
        val inTheWay = panel.footprintOf(middle)
        val image = panel.imageOf(skip)
        assertFalse(panel.footprintOf(skip).intersects(inTheWay), "footprint ${panel.footprintOf(skip)} layer $inTheWay")
        assertTrue(image.width > 60.0, "The image should slide clear rather than shrink: $image")
    }

    @Test
    fun `a matrix gets out of the way of a layer dragged across it and goes back when the layer has gone`() {
        val source = arrayAt(0.0, 0.0)
        val target = arrayAt(0.0, -900.0)
        val matrix = WeightMatrix(source, target)
        val passerBy = arrayAt(500.0, -450.0)
        val panel = panelFor(Network(), source, target, matrix, passerBy)
        val atRest = panel.imageOf(matrix)

        // Dragged over the middle of the arrow, where the image is
        onEdt { passerBy.location = Point2D.Double(0.0, -450.0) }
        panel.awaitCanvasIdle()

        val pushed = panel.imageOf(matrix)
        assertFalse(panel.footprintOf(matrix).intersects(panel.footprintOf(passerBy)), "image $pushed")
        assertEquals(atRest.width, pushed.width, 0.5, "The image should keep its size")
        assertTrue(panel.canvasBoundsReport().settled)

        onEdt { passerBy.location = Point2D.Double(500.0, -450.0) }
        panel.awaitCanvasIdle()

        val back = panel.imageOf(matrix)
        assertEquals(atRest.centerY, back.centerY, 0.5)
        assertEquals(atRest.width, back.width, 0.5)
    }

    @Test
    fun `a matrix is not moved by a layer that is nowhere near it`() {
        val source = arrayAt(0.0, 0.0)
        val target = arrayAt(0.0, -420.0)
        val matrix = WeightMatrix(source, target)
        val elsewhere = arrayAt(600.0, -200.0)
        val panel = panelFor(Network(), source, target, matrix, elsewhere)
        val before = panel.imageOf(matrix)

        onEdt { elsewhere.location = Point2D.Double(450.0, -260.0) }
        panel.awaitCanvasIdle()

        assertEquals(before, panel.imageOf(matrix))
    }

    @Test
    fun `deleting a layer lets a matrix it was in the way of go back to the middle of its arrow`() {
        val source = arrayAt(0.0, 0.0)
        val target = arrayAt(0.0, -900.0)
        val matrix = WeightMatrix(source, target)
        val inTheWay = arrayAt(0.0, -450.0)
        val network = Network()
        val panel = panelFor(network, source, target, matrix, inTheWay)
        val pushed = panel.imageOf(matrix)

        runBlocking { network.deleteModels(listOf(inTheWay)) }
        panel.awaitCanvasIdle()

        val free = panel.imageOf(matrix)
        assertTrue(kotlin.math.abs(free.centerY - (-450.0)) < kotlin.math.abs(pushed.centerY - (-450.0)) - 20.0) {
            "pushed $pushed free $free"
        }
    }
}
