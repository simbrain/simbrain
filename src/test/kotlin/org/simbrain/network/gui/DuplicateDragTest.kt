/**
 * Alt-dragging on the network canvas drags out a duplicate of the selection and leaves the original where it was.
 * Something that cannot be copied is moved instead, like any other drag. The drags are made with mouse events sent
 * to the canvas.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.util.awaitUntil
import org.simbrain.util.onEdt
import org.simbrain.util.point
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.JDialog

class DuplicateDragTest {

    private val tolerance = 3.0

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
        onEdt { panel.canvas.camera.animateViewToCenterBounds(panel.canvas.layer.fullBounds, true, 0) }
        panel.validateCanvas()
        return panel
    }

    private fun neuronAt(x: Double, y: Double) = Neuron().apply { location = point(x, y) }

    /**
     * Presses on the middle of the node of [model] with alt held, drags by ([dx], [dy]) in network coordinates in
     * several steps, and releases.
     */
    private fun NetworkPanel.altDrag(model: LocatableModel, dx: Double, dy: Double) {
        val start = onEdt { getNode(model)!!.globalFullBounds.let { point(it.centerX, it.centerY) } }
        fun send(id: Int, fraction: Double, button: Int) = onEdt {
            val p = canvas.camera.viewToLocal(Point2D.Double(start.x + dx * fraction, start.y + dy * fraction))
            val modifiers = InputEvent.BUTTON1_DOWN_MASK or InputEvent.ALT_DOWN_MASK
            canvas.dispatchEvent(
                MouseEvent(canvas, id, System.currentTimeMillis(), modifiers, p.x.toInt(), p.y.toInt(), 1, false, button)
            )
        }
        send(MouseEvent.MOUSE_PRESSED, 0.0, MouseEvent.BUTTON1)
        listOf(0.25, 0.5, 0.75, 1.0).forEach { send(MouseEvent.MOUSE_DRAGGED, it, MouseEvent.NOBUTTON) }
        send(MouseEvent.MOUSE_RELEASED, 1.0, MouseEvent.BUTTON1)
    }

    private fun assertNear(expected: Point2D, actual: Point2D) {
        assertEquals(expected.x, actual.x, tolerance)
        assertEquals(expected.y, actual.y, tolerance)
    }

    @Test
    fun `alt-dragging a neuron drags out a copy and one undo removes it`() {
        val network = Network()
        val source = neuronAt(0.0, 0.0)
        val panel = panelFor(network, source, neuronAt(600.0, 600.0))

        panel.altDrag(source, 120.0, 60.0)

        awaitUntil(message = "No copy was made") { network.getModels<Neuron>().size == 3 }
        val copy = network.getModels<Neuron>().last()
        awaitUntil(message = "The copy did not follow the drag: ${copy.location}") {
            copy.location.distance(point(120.0, 60.0)) < tolerance
        }
        assertEquals(point(0.0, 0.0), source.location)
        assertEquals(listOf<NetworkModel>(copy), onEdt { panel.selectionManager.selectedModels.toList() })

        runBlocking { panel.undoManager.undo() }
        awaitUntil(message = "Undo did not remove the copy") { network.getModels<Neuron>().size == 2 }
        assertEquals(point(0.0, 0.0), source.location)
    }

    @Test
    fun `alt-dragging something that cannot be copied moves it and the move can be undone`() {
        val network = Network()
        val layer = TensorLayer(TensorShape(4, 4, 2)).apply { location = point(0.0, 0.0) }
        val onClipboard = neuronAt(600.0, 600.0)
        val panel = panelFor(network, layer, onClipboard)
        Clipboard.add(listOf(onClipboard))
        val clipboardBefore = Clipboard.contentVersion

        panel.altDrag(layer, 100.0, 50.0)

        awaitUntil(message = "The layer did not follow the drag: ${layer.location}") {
            layer.location.distance(point(100.0, 50.0)) < tolerance
        }
        assertEquals(1, network.getModels<TensorLayer>().size)
        assertEquals(clipboardBefore, Clipboard.contentVersion)

        runBlocking { panel.undoManager.undo() }
        awaitUntil(message = "Undo did not move the layer back: ${layer.location}") {
            layer.location.distance(point(0.0, 0.0)) < tolerance
        }
        assertNear(point(0.0, 0.0), layer.location)
    }

}
