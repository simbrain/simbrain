/**
 * Wand strokes on the network canvas, made with mouse events sent to the canvas.
 *
 * A stroke applies the palette's selected action, or its secondary action if Shift is held when the stroke begins,
 * and keeps that action until the mouse is released. Between strokes the cursor shows the secondary action while
 * Shift is held. The palette is the application-wide one, so each test swaps its own actions in and puts the
 * previous ones back; nothing is written to the saved palette.
 */
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
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.awaitUntil
import org.simbrain.util.onEdt
import org.simbrain.util.point
import java.awt.Dimension
import java.awt.Point
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.KeyStroke

class WandStrokeTest {

    private val drawn = 0.7

    private fun draw() = AdjustValueAction(amount = AdjustValueAmount.Value(drawn))

    private fun erase() = AdjustValueAction(amount = AdjustValueAmount.Value(0.0))

    /**
     * Runs [block] with the wand palette holding [selected] and, if given, [secondary] as its secondary action.
     */
    private fun withWandPalette(selected: WandAction, secondary: WandAction?, block: () -> Unit) {
        val palette = NetworkPreferences.wandPalette
        val previousActions = palette.actions.toList()
        val previousSelected = palette.selectedIndex
        val previousSecondary = palette.secondaryIndex
        palette.setActions(listOfNotNull(selected, secondary), if (secondary != null) 1 else -1)
        try {
            block()
        } finally {
            palette.setActions(previousActions, previousSecondary)
            palette.selectAction(previousSelected)
        }
    }

    /** A panel in wand mode, laid out at a fixed size and validated, showing [network] with [models] added to it. */
    private fun wandPanelFor(network: Network, vararg models: NetworkModel): NetworkPanel {
        val panel = NetworkPanel(NetworkComponent("test", network))
        runBlocking { models.forEach { network.addNetworkModel(it, usePlacementManager = false) } }
        onEdt {
            panel.preferredSize = Dimension(800, 800)
            JDialog().apply { contentPane = panel; pack() }
            panel.autoZoom = false
        }
        panel.validateCanvas()
        onEdt {
            panel.canvas.camera.animateViewToCenterBounds(panel.canvas.layer.fullBounds, true, 0)
            panel.mouseCursor = MouseEventHandler.MouseCursor.Wand
        }
        panel.validateCanvas()
        return panel
    }

    /** Neurons in a row, far enough apart that the brush touches one at a time. */
    private fun neuronRow(count: Int, activation: Double = 0.0) = List(count) {
        Neuron().apply {
            location = point(it * 200.0, 0.0)
            this.activation = activation
        }
    }

    /** The canvas pixel at the middle of the node of [model]. */
    private fun NetworkPanel.pixelOf(model: NetworkModel): Point = onEdt {
        val bounds = getNode(model)!!.globalFullBounds
        val p = canvas.camera.viewToLocal(Point2D.Double(bounds.centerX, bounds.centerY))
        Point(p.x.toInt(), p.y.toInt())
    }

    private fun NetworkPanel.send(id: Int, pixel: Point, shiftDown: Boolean) = onEdt {
        val modifiers = InputEvent.BUTTON1_DOWN_MASK or (if (shiftDown) InputEvent.SHIFT_DOWN_MASK else 0)
        val button = if (id == MouseEvent.MOUSE_DRAGGED) MouseEvent.NOBUTTON else MouseEvent.BUTTON1
        canvas.dispatchEvent(
            MouseEvent(canvas, id, System.currentTimeMillis(), modifiers, pixel.x, pixel.y, 1, false, button)
        )
    }

    /**
     * Presses on the first of [pixels], drags through the rest and releases on the last. Shift is held at the press
     * if [shiftAtPress], and for the rest of the stroke if [shiftAfterPress].
     */
    private fun NetworkPanel.stroke(
        pixels: List<Point>,
        shiftAtPress: Boolean = false,
        shiftAfterPress: Boolean = shiftAtPress
    ) {
        send(MouseEvent.MOUSE_PRESSED, pixels.first(), shiftAtPress)
        pixels.drop(1).forEach { send(MouseEvent.MOUSE_DRAGGED, it, shiftAfterPress) }
        send(MouseEvent.MOUSE_RELEASED, pixels.last(), shiftAfterPress)
    }

    private fun NetworkPanel.strokeOver(
        models: List<NetworkModel>,
        shiftAtPress: Boolean = false,
        shiftAfterPress: Boolean = shiftAtPress
    ) = stroke(models.map { pixelOf(it) }, shiftAtPress, shiftAfterPress)

    private fun awaitActivations(neurons: List<Neuron>, expected: Double) =
        awaitUntil(message = "Expected $expected but activations are ${neurons.map { it.activation }}") {
            neurons.all { it.activation == expected }
        }

    /** Waits for the stroke to be finished off, which is when it becomes one undoable action. */
    private fun NetworkPanel.awaitUndoableActions(count: Int) =
        awaitUntil(message = "Expected $count undoable actions but there are ${undoManager.undoStack.size}") {
            undoManager.undoStack.size == count
        }

    @Test
    fun `a stroke applies the selected action`() = withWandPalette(draw(), erase()) {
        val neurons = neuronRow(3)
        val panel = wandPanelFor(Network(), *neurons.toTypedArray())

        panel.strokeOver(neurons)

        awaitActivations(neurons, drawn)
    }

    @Test
    fun `a stroke begun with shift held applies the secondary action`() = withWandPalette(draw(), erase()) {
        val neurons = neuronRow(3, activation = drawn)
        val panel = wandPanelFor(Network(), *neurons.toTypedArray())

        panel.strokeOver(neurons, shiftAtPress = true)

        awaitActivations(neurons, 0.0)
    }

    @Test
    fun `releasing shift part way through a stroke does not change its action`() = withWandPalette(draw(), erase()) {
        val neurons = neuronRow(3, activation = drawn)
        val panel = wandPanelFor(Network(), *neurons.toTypedArray())

        panel.strokeOver(neurons, shiftAtPress = true, shiftAfterPress = false)

        awaitActivations(neurons, 0.0)
    }

    @Test
    fun `pressing shift part way through a stroke does not change its action`() = withWandPalette(draw(), erase()) {
        val neurons = neuronRow(3)
        val panel = wandPanelFor(Network(), *neurons.toTypedArray())

        panel.strokeOver(neurons, shiftAtPress = false, shiftAfterPress = true)

        awaitActivations(neurons, drawn)
    }

    @Test
    fun `a stroke with shift held applies the selected action when there is no secondary action`() =
        withWandPalette(draw(), null) {
            val neurons = neuronRow(3)
            val panel = wandPanelFor(Network(), *neurons.toTypedArray())

            panel.strokeOver(neurons, shiftAtPress = true)

            awaitActivations(neurons, drawn)
        }

    @Test
    fun `a click applies its action when the release is already waiting as the press is dealt with`() =
        withWandPalette(draw(), erase()) {
            val neurons = neuronRow(1)
            val panel = wandPanelFor(Network(), *neurons.toTypedArray())
            val pixel = panel.pixelOf(neurons[0])

            // Both events in one turn of the event queue, so that the work the press sets going runs after the release
            onEdt {
                panel.send(MouseEvent.MOUSE_PRESSED, pixel, shiftDown = false)
                panel.send(MouseEvent.MOUSE_RELEASED, pixel, shiftDown = false)
            }

            awaitActivations(neurons, drawn)
            panel.awaitUndoableActions(1)
        }

    @Test
    fun `a stroke is finished off when another tool is switched to before the mouse is released`() =
        withWandPalette(draw(), erase()) {
            val neurons = neuronRow(2)
            val panel = wandPanelFor(Network(), *neurons.toTypedArray())
            val pixels = neurons.map { panel.pixelOf(it) }

            panel.send(MouseEvent.MOUSE_PRESSED, pixels[0], shiftDown = false)
            panel.send(MouseEvent.MOUSE_DRAGGED, pixels[1], shiftDown = false)
            onEdt { panel.mouseCursor = MouseEventHandler.MouseCursor.Selection }
            panel.send(MouseEvent.MOUSE_RELEASED, pixels[1], shiftDown = false)

            awaitActivations(neurons, drawn)
            panel.awaitUndoableActions(1)
        }

    @Test
    fun `each stroke is undone as a whole with the action it applied`() = withWandPalette(draw(), erase()) {
        val neurons = neuronRow(3)
        val panel = wandPanelFor(Network(), *neurons.toTypedArray())

        panel.strokeOver(neurons)
        panel.awaitUndoableActions(1)
        panel.strokeOver(neurons.take(2), shiftAtPress = true)
        panel.awaitUndoableActions(2)
        assertEquals(listOf(0.0, 0.0, drawn), neurons.map { it.activation })

        runBlocking { panel.undoManager.undo() }
        assertEquals(listOf(drawn, drawn, drawn), neurons.map { it.activation })

        runBlocking { panel.undoManager.undo() }
        assertEquals(listOf(0.0, 0.0, 0.0), neurons.map { it.activation })
    }

    /**
     * Runs what the panel has bound to Shift going down or coming up, found from a key event shaped as the real
     * one is: Shift counts among the modifiers of its own press and not of its release.
     */
    private fun NetworkPanel.shiftKey(pressed: Boolean) = onEdt {
        val event = KeyEvent(
            this,
            if (pressed) KeyEvent.KEY_PRESSED else KeyEvent.KEY_RELEASED,
            System.currentTimeMillis(),
            if (pressed) InputEvent.SHIFT_DOWN_MASK else 0,
            KeyEvent.VK_SHIFT,
            KeyEvent.CHAR_UNDEFINED
        )
        val binding = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStrokeForEvent(event))
        actionMap.get(binding).actionPerformed(null)
    }

    @Test
    fun `the cursor shows the secondary action while shift is held`() = withWandPalette(draw(), erase()) {
        val panel = wandPanelFor(Network(), *neuronRow(1).toTypedArray())

        panel.shiftKey(pressed = true)
        assertTrue(panel.wandEventHandler.cursorShowsSecondary)

        panel.shiftKey(pressed = false)
        assertFalse(panel.wandEventHandler.cursorShowsSecondary)
    }

    @Test
    fun `the cursor follows shift as the mouse moves when the key itself was not seen`() =
        withWandPalette(draw(), erase()) {
            val panel = wandPanelFor(Network(), *neuronRow(1).toTypedArray())
            fun move(shiftDown: Boolean) = onEdt {
                val modifiers = if (shiftDown) InputEvent.SHIFT_DOWN_MASK else 0
                panel.canvas.dispatchEvent(
                    MouseEvent(
                        panel.canvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), modifiers, 50, 50, 0,
                        false, MouseEvent.NOBUTTON
                    )
                )
            }

            move(shiftDown = true)
            assertTrue(panel.wandEventHandler.cursorShowsSecondary)

            move(shiftDown = false)
            assertFalse(panel.wandEventHandler.cursorShowsSecondary)
        }

    @Test
    fun `the cursor keeps showing a stroke's action when shift is released part way`() =
        withWandPalette(draw(), erase()) {
            val neurons = neuronRow(2, activation = drawn)
            val panel = wandPanelFor(Network(), *neurons.toTypedArray())
            val pixels = neurons.map { panel.pixelOf(it) }

            panel.send(MouseEvent.MOUSE_PRESSED, pixels[0], shiftDown = true)
            panel.shiftKey(pressed = false)
            panel.send(MouseEvent.MOUSE_DRAGGED, pixels[1], shiftDown = false)
            assertTrue(panel.wandEventHandler.cursorShowsSecondary)

            panel.send(MouseEvent.MOUSE_RELEASED, pixels[1], shiftDown = false)
            assertFalse(panel.wandEventHandler.cursorShowsSecondary)
            awaitActivations(neurons, 0.0)
        }

    @Test
    fun `a click applies the action to the neuron array cell under the brush`() = withWandPalette(draw(), erase()) {
        val array = NeuronArray(4).apply {
            location = point(0.0, 0.0)
            activationArray = DoubleArray(4)
        }
        val panel = wandPanelFor(Network(), array)
        val cells = onEdt {
            panel.getNode(array)!!.getAllNodes(object : PNodeFilter {
                override fun accept(node: PNode) = node.isPixelTarget
                override fun acceptChildrenOf(node: PNode) = true
            }, null).filterIsInstance<PNode>().first { it !== panel.getNode(array) }
        }
        // The middle of the first of the four cells, which are far wider on screen than the brush
        val firstCell = onEdt {
            val bounds = cells.globalFullBounds
            val p = panel.canvas.camera.viewToLocal(
                Point2D.Double(bounds.x + bounds.width * 0.125, bounds.centerY)
            )
            Point(p.x.toInt(), p.y.toInt())
        }

        panel.stroke(listOf(firstCell))

        awaitUntil(message = "Activations are ${array.activationArray.toList()}") {
            array.activationArray[0] == drawn
        }
        assertEquals(listOf(0.0, 0.0, 0.0), array.activationArray.drop(1))
    }
}
