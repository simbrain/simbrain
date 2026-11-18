/**
 * Sizing of weight matrix images and single kernel images against the space between the layers they connect.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.Padding
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.network.gui.nodes.TensorConnectorNode
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.widgets.BezierArrow
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.JDialog
import javax.swing.SwingUtilities

class ConnectorImageFitTest {

    private class Fixture(vararg models: NetworkModel) {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))

        init {
            runBlocking { models.forEach { network.addNetworkModel(it, usePlacementManager = false) } }
            SwingUtilities.invokeAndWait {
                panel.preferredSize = Dimension(800, 800)
                JDialog().apply { contentPane = panel; pack() }
                panel.autoZoom = false
            }
            settle()
            SwingUtilities.invokeAndWait {
                panel.canvas.camera.animateViewToCenterBounds(panel.canvas.layer.fullBounds, true, 0)
            }
        }

        fun node(model: NetworkModel): ScreenElement = panel.getNode(model)!!

        /** Node placement and relayout run on the Swing dispatcher; let them land. */
        fun settle() {
            Thread.sleep(100)
            repeat(3) { SwingUtilities.invokeAndWait { panel.canvas.layer.fullBounds } }
        }

        fun onEdt(block: () -> Unit) = SwingUtilities.invokeAndWait(block)

        fun click(globalPoint: Point2D) {
            val p = panel.canvas.camera.viewToLocal(globalPoint)
            SwingUtilities.invokeAndWait {
                listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED).forEach { id ->
                    panel.canvas.dispatchEvent(
                        MouseEvent(
                            panel.canvas, id, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
                            p.x.toInt(), p.y.toInt(), 1, false, MouseEvent.BUTTON1
                        )
                    )
                }
            }
        }
    }

    private fun arrayAt(x: Double, y: Double, size: Int = 4) =
        NeuronArray(size).apply { location = Point2D.Double(x, y) }

    @Test
    fun `weight matrix image grows with the space between stacked layers without reaching them`() {
        val source = arrayAt(0.0, 0.0)
        val target = arrayAt(0.0, -220.0)
        val wm = WeightMatrix(source, target)
        val f = Fixture(source, target, wm)
        val node = f.node(wm) as WeightMatrixNode

        val near = node.imageBox.globalBounds
        assertEquals(near.width, near.height, 1e-6)
        assertFalse(node.globalFullBounds.intersects(f.node(source).globalFullBounds))
        assertFalse(node.interactionBox.globalFullBounds.intersects(f.node(target).globalFullBounds))

        f.onEdt { target.location = Point2D.Double(0.0, -420.0) }
        f.settle()
        val far = node.imageBox.globalBounds
        assertTrue(far.height > near.height + 20.0, "far ${far.height} near ${near.height}")
        assertFalse(node.imageBox.globalFullBounds.intersects(f.node(source).globalFullBounds))
        assertFalse(node.interactionBox.globalFullBounds.intersects(f.node(target).globalFullBounds))
    }

    @Test
    fun `weight matrix image leaves half of its arrow showing whatever the space between the layers`() {
        for (distance in listOf(240.0, 270.0, 300.0)) {
            val source = arrayAt(0.0, 0.0)
            val target = arrayAt(0.0, -distance)
            val wm = WeightMatrix(source, target)
            val f = Fixture(source, target, wm)
            val node = f.node(wm) as WeightMatrixNode

            val shaft = node.connectionDrawing.allNodes.filterIsInstance<BezierArrow>().single().shaft!!
            // The image with its label box and caption, against the arrow from the layer to the base of its head
            assertEquals(0.5 * (shaft.y1 - shaft.y2), node.footprint.height, 2.0, "at a distance of $distance")
        }
    }

    @Test
    fun `weight matrix image fits the gap between layers placed side by side`() {
        val source = arrayAt(0.0, 0.0)
        val target = arrayAt(300.0, 0.0)
        val wm = WeightMatrix(source, target)
        val f = Fixture(source, target, wm)
        val node = f.node(wm) as WeightMatrixNode

        val image = node.imageBox.globalFullBounds
        val sourceBounds = f.node(source).globalFullBounds
        val targetBounds = f.node(target).globalFullBounds
        assertTrue(image.x > sourceBounds.maxX && image.maxX < targetBounds.x, "image $image")
        // More than a sliver of the gap, which the arrowhead also takes part of, and no more than half of it
        val gap = targetBounds.x - sourceBounds.maxX
        assertTrue(image.width > gap / 3 && image.width < gap / 2, "image $image gap $gap")
    }

    @Test
    fun `reciprocal weight matrices keep clear of each other and of both layers`() {
        val bottom = arrayAt(0.0, 0.0)
        val top = arrayAt(0.0, -400.0)
        val up = WeightMatrix(bottom, top)
        val down = WeightMatrix(top, bottom)
        val f = Fixture(bottom, top, up, down)
        val upNode = f.node(up) as WeightMatrixNode
        val downNode = f.node(down) as WeightMatrixNode

        fun WeightMatrixNode.drawn() = imageBox.globalFullBounds.createUnion(interactionBox.globalFullBounds)
        assertTrue(upNode.drawn().y > downNode.drawn().maxY, "up ${upNode.drawn()} down ${downNode.drawn()}")
        assertFalse(upNode.drawn().intersects(f.node(bottom).globalFullBounds))
        assertFalse(downNode.drawn().intersects(f.node(top).globalFullBounds))
    }

    @Test
    fun `clicking a cell of a fitted weight matrix selects that cell`() {
        val source = arrayAt(0.0, 0.0)
        val target = arrayAt(0.0, -400.0, size = 2)
        val wm = WeightMatrix(source, target)
        val f = Fixture(source, target, wm)
        val node = f.node(wm) as WeightMatrixNode

        // Bottom right cell of the image, which is the last source column of the last target row
        val image = node.imageBox.globalBounds
        f.click(Point2D.Double(image.maxX - image.width / 8, image.maxY - image.height / 4))
        assertEquals(setOf(1 to 3), node.pixelSelection)
    }

    @Test
    fun `single kernel image grows with the space between the layers without reaching them`() {
        val sourceShape = TensorShape(4, 4, 2)
        val source = TensorLayer(sourceShape).apply { location = Point2D.Double(0.0, 0.0) }
        val target = TensorLayer(sourceShape.convOutputShape(3, 1, Padding.SAME, 2)).apply {
            location = Point2D.Double(0.0, -300.0)
        }
        val conv = ConvolutionConnector(source, target, 3, 2, 1, Padding.SAME)
        val f = Fixture(source, target, conv)
        val node = f.node(conv) as TensorConnectorNode

        val near = node.imageBox.globalBounds
        assertEquals(near.width, near.height, 1e-6)
        assertFalse(node.imageBox.globalFullBounds.intersects(f.node(source).globalFullBounds))
        assertFalse(node.interactionBox.globalFullBounds.intersects(f.node(target).globalFullBounds))

        f.onEdt { target.location = Point2D.Double(0.0, -600.0) }
        f.settle()
        val far = node.imageBox.globalBounds
        assertTrue(far.height > near.height + 20.0, "far ${far.height} near ${near.height}")
        assertFalse(node.interactionBox.globalFullBounds.intersects(f.node(target).globalFullBounds))
    }
}
