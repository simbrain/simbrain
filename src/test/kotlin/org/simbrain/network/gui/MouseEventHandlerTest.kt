/**
 * Mouse gestures on the network canvas, made with mouse events sent to the canvas.
 *
 * A click on empty canvas sets the insertion point, including a click during which the cursor moves by a pixel, for
 * which no mouse-clicked event is sent. Alt-dragging drags out a duplicate of the selection and leaves the original
 * where it was; something that cannot be copied is moved instead, like any other drag.
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
import java.awt.Point
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.JDialog

class MouseEventHandlerTest {

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

    /** The canvas pixel showing a point given in network coordinates. */
    private fun NetworkPanel.pixelAt(networkPoint: Point2D): Point = onEdt {
        val p = canvas.camera.viewToLocal(Point2D.Double(networkPoint.x, networkPoint.y))
        Point(p.x.toInt(), p.y.toInt())
    }

    private fun NetworkPanel.send(id: Int, pixel: Point, altDown: Boolean = false) = onEdt {
        val modifiers = InputEvent.BUTTON1_DOWN_MASK or (if (altDown) InputEvent.ALT_DOWN_MASK else 0)
        val button = if (id == MouseEvent.MOUSE_DRAGGED) MouseEvent.NOBUTTON else MouseEvent.BUTTON1
        canvas.dispatchEvent(
            MouseEvent(canvas, id, System.currentTimeMillis(), modifiers, pixel.x, pixel.y, 1, false, button)
        )
    }

    /**
     * Presses at [networkPoint] and releases [travel] pixels to the right, as a hand that is not perfectly still
     * does. As on a real canvas, moving at all between press and release means no mouse-clicked event follows.
     */
    private fun NetworkPanel.pressAndRelease(networkPoint: Point2D, travel: Int, altDown: Boolean = false) {
        val press = pixelAt(networkPoint)
        val release = Point(press.x + travel, press.y)
        send(MouseEvent.MOUSE_PRESSED, press, altDown)
        if (travel != 0) send(MouseEvent.MOUSE_DRAGGED, release, altDown)
        send(MouseEvent.MOUSE_RELEASED, release, altDown)
        if (travel == 0) send(MouseEvent.MOUSE_CLICKED, release, altDown)
    }

    /**
     * Presses on the middle of the node of [model] with alt held, drags by ([dx], [dy]) in network coordinates in
     * several steps, and releases.
     */
    private fun NetworkPanel.altDrag(model: LocatableModel, dx: Double, dy: Double) {
        val start = onEdt { getNode(model)!!.globalFullBounds.let { point(it.centerX, it.centerY) } }
        fun pixelAfter(fraction: Double) = pixelAt(point(start.x + dx * fraction, start.y + dy * fraction))
        send(MouseEvent.MOUSE_PRESSED, pixelAfter(0.0), altDown = true)
        listOf(0.25, 0.5, 0.75, 1.0).forEach { send(MouseEvent.MOUSE_DRAGGED, pixelAfter(it), altDown = true) }
        send(MouseEvent.MOUSE_RELEASED, pixelAfter(1.0), altDown = true)
    }

    private fun assertNear(expected: Point2D, actual: Point2D?) {
        assertEquals(expected.x, actual!!.x, tolerance)
        assertEquals(expected.y, actual.y, tolerance)
    }

    @Test
    fun `a click on empty canvas sets the insertion point`() {
        val network = Network()
        val panel = panelFor(network, neuronAt(0.0, 0.0), neuronAt(600.0, 600.0))

        panel.pressAndRelease(point(300.0, 100.0), travel = 0)

        assertNear(point(300.0, 100.0), network.placementManager.insertionPoint)
    }

    @Test
    fun `a click during which the cursor moves slightly still sets the insertion point`() {
        val network = Network()
        val panel = panelFor(network, neuronAt(0.0, 0.0), neuronAt(600.0, 600.0))

        panel.pressAndRelease(point(300.0, 100.0), travel = 2)

        assertNear(point(300.0, 100.0), network.placementManager.insertionPoint)
    }

    @Test
    fun `dragging a marquee does not set the insertion point`() {
        val network = Network()
        val panel = panelFor(network, neuronAt(0.0, 0.0), neuronAt(600.0, 600.0))
        network.placementManager.insertionPoint = point(50.0, 500.0)

        panel.pressAndRelease(point(300.0, 100.0), travel = 60)

        assertEquals(point(50.0, 500.0), network.placementManager.insertionPoint)
    }

    @Test
    fun `dragging a marquee out and back to where it began does not set the insertion point`() {
        val network = Network()
        val panel = panelFor(network, neuronAt(0.0, 0.0), neuronAt(600.0, 600.0))
        network.placementManager.insertionPoint = point(50.0, 500.0)

        val press = panel.pixelAt(point(300.0, 100.0))
        panel.send(MouseEvent.MOUSE_PRESSED, press)
        panel.send(MouseEvent.MOUSE_DRAGGED, Point(press.x + 60, press.y + 40))
        panel.send(MouseEvent.MOUSE_DRAGGED, Point(press.x + 1, press.y))
        panel.send(MouseEvent.MOUSE_RELEASED, Point(press.x + 1, press.y))

        assertEquals(point(50.0, 500.0), network.placementManager.insertionPoint)
    }

    @Test
    fun `alt-clicking a neuron while the cursor moves slightly makes no copy`() {
        val network = Network()
        val source = neuronAt(0.0, 0.0)
        val panel = panelFor(network, source, neuronAt(600.0, 600.0))
        val clipboardBefore = Clipboard.contentVersion

        panel.pressAndRelease(point(0.0, 0.0), travel = 2, altDown = true)

        // A copy would be made off the event thread, so give one time to turn up
        Thread.sleep(300)
        assertEquals(2, network.getModels<Neuron>().size)
        assertEquals(clipboardBefore, Clipboard.contentVersion)
    }

    @Test
    fun `clicking a node does not set the insertion point`() {
        val network = Network()
        val neuron = neuronAt(0.0, 0.0)
        val panel = panelFor(network, neuron, neuronAt(600.0, 600.0))
        network.placementManager.insertionPoint = point(50.0, 500.0)

        panel.pressAndRelease(point(0.0, 0.0), travel = 0)

        assertEquals(listOf<NetworkModel>(neuron), onEdt { panel.selectionManager.selectedModels.toList() })
        assertEquals(point(50.0, 500.0), network.placementManager.insertionPoint)
    }

    @Test
    fun `a right button release with no press before it does not set the insertion point`() {
        val network = Network()
        val panel = panelFor(network, neuronAt(0.0, 0.0), neuronAt(600.0, 600.0))
        network.placementManager.insertionPoint = point(50.0, 500.0)

        // What the canvas gets when a popup menu took the press, or the release arrives after the menu has closed.
        // The canvas makes up the missing press, so its handlers see a press and release in the same place.
        val pixel = panel.pixelAt(point(300.0, 100.0))
        onEdt {
            panel.canvas.dispatchEvent(
                MouseEvent(
                    panel.canvas, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0,
                    pixel.x, pixel.y, 1, false, MouseEvent.BUTTON3
                )
            )
        }

        assertEquals(point(50.0, 500.0), network.placementManager.insertionPoint)
    }

    @Test
    fun `each paste goes to the click before it even when the clicks move slightly`() {
        val network = Network()
        val source = neuronAt(0.0, 0.0)
        val panel = panelFor(network, source, neuronAt(600.0, 600.0))
        onEdt { panel.selectionManager.set(panel.getNode(source)!!) }
        panel.copy()

        val clicks = listOf(point(300.0, 100.0), point(100.0, 450.0), point(450.0, 250.0))
        clicks.forEachIndexed { i, click ->
            panel.pressAndRelease(click, travel = if (i % 2 == 0) 1 else 2)
            runBlocking { panel.paste() }
            val pasted = network.getModels<Neuron>().last()
            assertEquals(3 + i, network.getModels<Neuron>().size)
            awaitUntil(message = "Paste ${i + 1} went to ${pasted.location} instead of the click at $click") {
                pasted.location.distance(click) < tolerance
            }
        }
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
