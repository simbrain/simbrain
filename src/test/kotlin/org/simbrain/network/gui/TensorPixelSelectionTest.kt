/**
 * Mouse and keyboard editing of individual tensor layer activations and convolution kernel weights, and the sizing
 * of the kernel grid against the space between its layers.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.piccolo2d.PNode
import org.piccolo2d.util.PNodeFilter
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.Network
import org.simbrain.network.core.Padding
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.network.gui.nodes.TensorConnectorNode
import org.simbrain.network.gui.nodes.TensorNode
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import javax.swing.JDialog
import javax.swing.SwingUtilities

class TensorPixelSelectionTest {

    private class Fixture(channels: Int = 2, filters: Int = 2) {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val sourceShape = TensorShape(4, 4, channels)
        val source = TensorLayer(sourceShape).apply { location = Point2D.Double(0.0, 0.0) }
        val target = TensorLayer(sourceShape.convOutputShape(3, 1, Padding.SAME, filters)).apply {
            location = Point2D.Double(0.0, -400.0)
        }
        val conv = ConvolutionConnector(source, target, 3, filters, 1, Padding.SAME)

        init {
            runBlocking {
                network.addNetworkModel(source, usePlacementManager = false)
                network.addNetworkModel(target, usePlacementManager = false)
                network.addNetworkModel(conv, usePlacementManager = false)
            }
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

        val sourceNode get() = panel.getNode(source) as TensorNode
        val targetNode get() = panel.getNode(target) as TensorNode
        val convNode get() = panel.getNode(conv) as TensorConnectorNode

        /** Node placement, relayout and zoom run on the Swing dispatcher; let them land. */
        fun settle() {
            Thread.sleep(100)
            repeat(3) { SwingUtilities.invokeAndWait { panel.canvas.layer.fullBounds } }
        }

        /** The first visible pixel-bearing child of [node], such as a tensor's main image or a kernel image. */
        fun pixelTargetOf(node: ScreenElement): PNode = node.getAllNodes(object : PNodeFilter {
            override fun accept(n: PNode) = n.isPixelTarget && n !== node && n.visible
            override fun acceptChildrenOf(n: PNode) = n.visible
        }, null).first() as PNode

        /** Canvas point at the given fractions across [pnode]'s global bounds. */
        fun canvasPoint(pnode: PNode, fx: Double, fy: Double): Point2D {
            val b = pnode.globalBounds
            return panel.canvas.camera.viewToLocal(Point2D.Double(b.x + b.width * fx, b.y + b.height * fy))
        }

        fun click(p: Point2D, shift: Boolean = false) {
            val mods = InputEvent.BUTTON1_DOWN_MASK or (if (shift) InputEvent.SHIFT_DOWN_MASK else 0)
            SwingUtilities.invokeAndWait {
                listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED).forEach { id ->
                    panel.canvas.dispatchEvent(
                        MouseEvent(panel.canvas, id, System.currentTimeMillis(), mods, p.x.toInt(), p.y.toInt(), 1, false, MouseEvent.BUTTON1)
                    )
                }
            }
        }

        fun onEdt(block: () -> Unit) = SwingUtilities.invokeAndWait(block)
    }

    @Test
    fun `plain click on a tensor cell selects that activation and the node`() {
        val f = Fixture()
        val node = f.sourceNode
        f.click(f.canvasPoint(f.pixelTargetOf(node), 0.125, 0.125))
        assertEquals(setOf(f.sourceShape.index(0, 0, 0)), node.pixelSelection)
        assertTrue(node in f.panel.selectionManager)
    }

    @Test
    fun `shift click adds a second tensor cell and keeps the node selected`() {
        val f = Fixture()
        val node = f.sourceNode
        val image = f.pixelTargetOf(node)
        f.click(f.canvasPoint(image, 0.125, 0.125))
        f.click(f.canvasPoint(image, 0.875, 0.875), shift = true)
        assertEquals(setOf(f.sourceShape.index(0, 0, 0), f.sourceShape.index(3, 3, 0)), node.pixelSelection)
        assertTrue(node in f.panel.selectionManager)
    }

    @Test
    fun `a selected cell is on the channel the tensor shows`() {
        val f = Fixture()
        f.onEdt { f.source.currentChannel = 1 }
        f.settle()
        val node = f.sourceNode
        f.click(f.canvasPoint(f.pixelTargetOf(node), 0.375, 0.625))
        assertEquals(setOf(f.sourceShape.index(2, 1, 1)), node.pixelSelection)
    }

    @Test
    fun `a cell of the rgb composite selects all three channels`() {
        val f = Fixture(channels = 3)
        f.onEdt { f.source.rgbComposite = true }
        f.settle()
        val node = f.sourceNode
        f.click(f.canvasPoint(f.pixelTargetOf(node), 0.125, 0.125))
        assertEquals((0 until 3).map { f.sourceShape.index(0, 0, it) }.toSet(), node.pixelSelection)
    }

    @Test
    fun `increment and clear act on the selected activation only`() {
        val f = Fixture()
        val node = f.sourceNode
        val selected = f.sourceShape.index(0, 0, 0)
        f.click(f.canvasPoint(f.pixelTargetOf(node), 0.125, 0.125))

        f.onEdt { f.panel.incrementSelectedObjects() }
        assertEquals(f.source.increment, f.source.activations[selected])
        assertEquals(f.source.increment, f.source.activations.sum())

        f.onEdt { f.panel.hardClearSelectedObjects() }
        assertEquals(0.0, f.source.activations.sum())
    }

    @Test
    fun `pixel edit dialog replaces the tensor editor while cells are selected`() {
        val f = Fixture()
        val node = f.sourceNode
        f.onEdt {
            assertFalse(node.propertyDialog.title.contains("pixel"))
            node.pixelSelection = setOf(0, 1)
            assertEquals("Edit 2 pixels", node.propertyDialog.title)
        }
    }

    @Test
    fun `tensor node has no tooltip while cells are selected`() {
        val f = Fixture()
        val node = f.sourceNode
        assertNotNull(node.toolTipText)
        f.onEdt { node.pixelSelection = setOf(0) }
        assertNull(node.toolTipText)
    }

    @Test
    fun `stepping channels drops the selection but a channel change from elsewhere keeps it`() {
        val f = Fixture()
        val node = f.sourceNode
        f.onEdt {
            node.pixelSelection = setOf(0)
            // As a receptive field trace does while the mouse hovers another layer
            f.source.currentChannel = 1
        }
        assertEquals(setOf(0), node.pixelSelection)
        f.onEdt { node.nextChannel() }
        assertTrue(node.pixelSelection.isEmpty())
    }

    @Test
    fun `leaving the rgb composite drops a selection made on it`() {
        val f = Fixture(channels = 3)
        f.onEdt { f.source.rgbComposite = true }
        f.settle()
        val node = f.sourceNode
        f.click(f.canvasPoint(f.pixelTargetOf(node), 0.125, 0.125))
        assertEquals(3, node.pixelSelection.size)

        // As the property dialog does when its thumbnail strip box is ticked
        f.onEdt { f.source.thumbnailStripMode = true }
        f.settle()
        assertTrue(node.pixelSelection.isEmpty())
    }

    @Test
    fun `clicking a kernel weight selects it and decrement changes only that weight`() {
        val f = Fixture()
        val node = f.convNode
        val center = f.conv.kernelIndex(0, 0, 1, 1)
        val before = f.conv.kernels.copyOf()
        f.click(f.canvasPoint(f.pixelTargetOf(node), 0.5, 0.5))
        assertEquals(setOf(center), node.pixelSelection)
        assertTrue(node in f.panel.selectionManager)

        f.onEdt { f.panel.decrementSelectedObjects() }
        val expected = before.copyOf().also { it[center] -= f.conv.increment }
        assertEquals(expected.toList(), f.conv.kernels.toList())
    }

    @Test
    fun `clicking a tile of the kernel grid selects a weight of that filter and channel`() {
        val f = Fixture()
        f.onEdt { f.conv.kernelGridMode = true }
        f.settle()
        val node = f.convNode
        // The grid is two filters by two channels of 3x3 kernels; aim at the last weight of the last tile
        val grid = node.kernelGridGroup.globalFullBounds
        val p = f.panel.canvas.camera.viewToLocal(Point2D.Double(grid.maxX - 3.0, grid.maxY - 3.0))
        f.click(p)
        assertEquals(setOf(f.conv.kernelIndex(1, 1, 2, 2)), node.pixelSelection)
    }

    @Test
    fun `kernel tooltip names the filter channel and position`() {
        val f = Fixture()
        val index = f.conv.kernelIndex(1, 0, 2, 1)
        f.conv.kernels[index] = -0.25
        assertEquals("F2 C1 (row 2, col 1): -0.25", f.convNode.kernelTooltipText(f.conv, index))
    }

    @Test
    fun `wand brush over a tensor reaches the cells of the shown channel`() {
        val f = Fixture()
        f.onEdt { f.source.currentChannel = 1 }
        f.settle()
        val image = f.pixelTargetOf(f.sourceNode).globalBounds
        val brush = Ellipse2D.Double(image.x + 1, image.y + 1, 2.0, 2.0)
        val pixels = f.panel.pixelsInGlobalEllipse(brush).toList()
        assertEquals(listOf(PixelTarget.TensorPixel(f.source, f.sourceShape.index(0, 0, 1))), pixels)
    }

    @Test
    fun `kernel grid tiles grow with the space between the layers without reaching them`() {
        val f = Fixture()
        f.onEdt { f.conv.kernelGridMode = true }
        f.settle()
        val node = f.convNode
        val near = node.kernelGridGroup.globalFullBounds
        assertFalse(near.intersects(f.sourceNode.globalFullBounds))
        assertFalse(near.intersects(f.targetNode.globalFullBounds))
        // Larger than the fixed 20 pixel tiles of a grid that ignores its surroundings
        assertTrue(near.height > 2 * 40.0, "grid height ${near.height}")

        f.onEdt { f.target.location = Point2D.Double(0.0, -800.0) }
        f.settle()
        val far = node.kernelGridGroup.globalFullBounds
        assertTrue(far.height > near.height, "far ${far.height} near ${near.height}")
        assertFalse(far.intersects(f.targetNode.globalFullBounds))
    }
}
