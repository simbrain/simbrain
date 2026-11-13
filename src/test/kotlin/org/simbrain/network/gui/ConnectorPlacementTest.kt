/**
 * What connectors draw on their arrows is kept clear of layers and of each other on a panel: drawings that would
 * land on the same spot are moved apart without one giving way to the other, an arrow whose way a layer blocks goes
 * around the layer, and a drawing gets out of the way of a layer dragged across it.
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
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.addNeurons
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.onEdt
import org.simbrain.util.widgets.BezierArrow
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
        // Neither gives way to the other: the same size, at the same height, either side of the middle
        val firstImage = panel.imageOf(rising)
        val secondImage = panel.imageOf(falling)
        assertEquals(firstImage.width, secondImage.width, 0.5)
        assertEquals(firstImage.centerY, secondImage.centerY, 0.5)
        assertEquals(115.0 - firstImage.centerX, secondImage.centerX - 115.0, 1.0)
    }

    @Test
    fun `the arrows of a reciprocal pair run parallel between layers of different widths`() {
        val network = Network()
        val wide = NeuronCollection(runBlocking { network.addNeurons(9) })
        onEdt { wide.neuronList.forEachIndexed { index, neuron -> neuron.location = Point2D.Double((index - 4) * 45.0, 0.0) } }
        val narrow = arrayAt(0.0, -460.0)
        val up = WeightMatrix(wide, narrow)
        val down = WeightMatrix(narrow, wide)
        val panel = panelFor(network, wide, narrow, up, down)

        val arrows = onEdt { panel.canvas.layer.allNodes.filterIsInstance<BezierArrow>().map { it.globalFullBounds } }
        assertEquals(2, arrows.size)
        // An arrow straight up or down is as wide as its head and no wider
        arrows.forEach { assertEquals(40.0, it.width, 1.0, "arrow $it") }
        assertEquals(panel.imageOf(up).width, panel.imageOf(down).width, 0.5)
    }

    @Test
    fun `the arrows of a reciprocal pair pass either side of a layer too narrow for both`() {
        val network = Network()
        val wide = NeuronCollection(runBlocking { network.addNeurons(9) })
        onEdt { wide.neuronList.forEachIndexed { index, neuron -> neuron.location = Point2D.Double((index - 4) * 45.0, 0.0) } }
        // Drawn as a column, whose bottom edge has no room for two arrows side by side
        val tall = NeuronArray(8).apply {
            verticalLayout = true
            location = Point2D.Double(0.0, -460.0)
        }
        val up = WeightMatrix(wide, tall)
        val down = WeightMatrix(tall, wide)
        val panel = panelFor(network, wide, tall, up, down)
        fun arrowOf(matrix: WeightMatrix) = onEdt {
            (panel.getNode(matrix) as WeightMatrixNode).connectionDrawing.allNodes.filterIsInstance<BezierArrow>().single()
        }

        val narrow = panel.footprintOf(tall)
        val upArrow = onEdt { arrowOf(up).globalFullBounds }
        val downArrow = onEdt { arrowOf(down).globalFullBounds }
        assertTrue(onEdt { arrowOf(up).hasOwnLane && arrowOf(down).hasOwnLane })
        // One reaches out past the layer on its left and the other on its right
        fun reachesLeftOnly(arrow: PBounds) = arrow.x < narrow.x - 20.0 && arrow.maxX < narrow.maxX + 20.0
        fun reachesRightOnly(arrow: PBounds) = arrow.maxX > narrow.maxX + 20.0 && arrow.x > narrow.x - 20.0
        val arrows = listOf(upArrow, downArrow)
        assertTrue(arrows.any { reachesLeftOnly(it) } && arrows.any { reachesRightOnly(it) }) {
            "up $upArrow down $downArrow layer $narrow"
        }
        // Each has the whole of its arrow, so the two images are the same size and clear of each other
        assertEquals(panel.imageOf(up).width, panel.imageOf(down).width, 1.0)
        assertFalse(panel.imageOf(up).intersects(panel.imageOf(down)))
        assertTrue(panel.imageOf(up).width > 60.0, "image ${panel.imageOf(up)}")
    }

    @Test
    fun `an arrow whose way a layer blocks goes around the layer, and straight again when the layer has gone`() {
        val bottom = arrayAt(0.0, 0.0)
        val middle = arrayAt(0.0, -270.0)
        val top = arrayAt(0.0, -540.0)
        val lower = WeightMatrix(bottom, middle)
        val upper = WeightMatrix(middle, top)
        val network = Network()
        val panel = panelFor(network, bottom, middle, top, lower, upper)
        val lowerAlone = panel.imageOf(lower)
        val skip = WeightMatrix(bottom, top)
        runBlocking { network.addNetworkModel(skip, usePlacementManager = false) }
        panel.validateCanvas()
        fun skipArrow() = onEdt {
            (panel.getNode(skip) as WeightMatrixNode).connectionDrawing.allNodes.filterIsInstance<BezierArrow>()
                .single().globalFullBounds
        }

        val inTheWay = panel.footprintOf(middle)
        val around = skipArrow()
        assertTrue(around.maxX > inTheWay.maxX + 40.0 || around.x < inTheWay.x - 40.0, "arrow $around layer $inTheWay")
        assertFalse(panel.footprintOf(skip).intersects(inTheWay))
        assertTrue(panel.imageOf(skip).width > 60.0, "image ${panel.imageOf(skip)}")
        // The connections it used to share a line with are the size they were without it, and about where they were
        assertEquals(lowerAlone.width, panel.imageOf(lower).width, 0.5)
        assertEquals(lowerAlone.centerY, panel.imageOf(lower).centerY, 5.0)

        onEdt { middle.location = Point2D.Double(500.0, -270.0) }
        panel.awaitCanvasIdle()

        assertEquals(40.0, skipArrow().width, 1.0, "The arrow should run straight up again")
        assertTrue(panel.canvasBoundsReport().settled)
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
