package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.piccolo2d.PNode
import org.piccolo2d.util.PNodeFilter
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.nodes.NeuronArrayNode
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.network.gui.nodes.WeightMatrixNode
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.JDialog
import javax.swing.SwingUtilities

class PixelSelectionGestureTest {

    private class Fixture {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val source = NeuronArray(4).apply { location = Point2D.Double(0.0, 0.0) }
        val target = NeuronArray(2).apply { location = Point2D.Double(0.0, 300.0) }
        val wm = WeightMatrix(source, target)

        init {
            runBlocking {
                network.addNetworkModel(source, usePlacementManager = false)
                network.addNetworkModel(target, usePlacementManager = false)
                network.addNetworkModel(wm, usePlacementManager = false)
            }
            SwingUtilities.invokeAndWait {
                panel.preferredSize = Dimension(800, 600)
                JDialog().apply { contentPane = panel; pack() }
                panel.autoZoom = false
            }
            // Node placement and auto-zoom run on the Swing dispatcher; let them settle, then fix the view
            Thread.sleep(100)
            SwingUtilities.invokeAndWait {
                panel.canvas.camera.animateViewToCenterBounds(panel.canvas.layer.fullBounds, true, 0)
            }
        }

        val sourceNode get() = panel.getNode(source) as NeuronArrayNode
        val wmNode get() = panel.getNode(wm) as WeightMatrixNode

        /** The first pixel-bearing child of [node], such as its activation image or matrix image box. */
        fun pixelTargetOf(node: ScreenElement): PNode = node.getAllNodes(object : PNodeFilter {
            override fun accept(n: PNode) = n.isPixelTarget && n !== node
            override fun acceptChildrenOf(n: PNode) = true
        }, null).first() as PNode

        /** Canvas point at the given fractions across [pnode]'s global bounds. */
        fun canvasPoint(pnode: PNode, fx: Double, fy: Double): Point2D {
            val b = pnode.globalFullBounds
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
    }

    @Test
    fun `plain click on a pixel selects that pixel and the node`() {
        val f = Fixture()
        val node = f.sourceNode
        f.click(f.canvasPoint(f.pixelTargetOf(node), 0.125, 0.5))
        assertEquals(setOf(0), node.pixelSelection)
        assertTrue(node in f.panel.selectionManager)
    }

    @Test
    fun `shift click adds a second pixel and keeps the node selected`() {
        val f = Fixture()
        val node = f.sourceNode
        val image = f.pixelTargetOf(node)
        f.click(f.canvasPoint(image, 0.125, 0.5))
        f.click(f.canvasPoint(image, 0.875, 0.5), shift = true)
        assertEquals(setOf(0, 3), node.pixelSelection)
        assertTrue(node in f.panel.selectionManager)
    }

    @Test
    fun `plain click on the title box keeps the node selected but drops its pixels`() {
        val f = Fixture()
        val node = f.sourceNode
        f.click(f.canvasPoint(f.pixelTargetOf(node), 0.125, 0.5))
        assertEquals(setOf(0), node.pixelSelection)
        val b = node.globalFullBounds
        f.click(f.panel.canvas.camera.viewToLocal(Point2D.Double(b.centerX, b.y + 4.0)))
        assertTrue(node.pixelSelection.isEmpty())
        // The title box is its own screen element, so the array stays selected through it
        assertTrue(f.source in f.panel.selectionManager.selectedModels)
    }

    @Test
    fun `clicking a weight matrix cell moves the selection from the array to the matrix`() {
        val f = Fixture()
        val arrayNode = f.sourceNode
        val wmNode = f.wmNode
        f.click(f.canvasPoint(f.pixelTargetOf(arrayNode), 0.125, 0.5))
        f.click(f.canvasPoint(f.pixelTargetOf(wmNode), 0.5, 0.5))
        assertEquals(1, wmNode.pixelSelection.size)
        assertTrue(wmNode in f.panel.selectionManager)
        assertFalse(arrayNode in f.panel.selectionManager)
        assertTrue(arrayNode.pixelSelection.isEmpty())
    }
}
