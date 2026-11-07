package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.piccolo2d.event.PInputEvent
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.nodes.NeuronArrayNode
import org.simbrain.network.gui.nodes.TensorNode
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.UiWork
import java.awt.event.InputEvent
import java.awt.event.MouseEvent

class PixelEditTooltipTest {

    @Test
    fun `neuron array node has no tooltip while pixels are selected`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val array = NeuronArray(4)
        network.addNetworkModelsAsync(array)
        val node = panel.getNode(array) as NeuronArrayNode

        assertNotNull(node.toolTipText)
        node.pixelSelection = setOf(1)
        assertNull(node.toolTipText)
        node.pixelSelection = emptySet()
        assertNotNull(node.toolTipText)
    }

    @Test
    fun `weight matrix node has no tooltip while cells are selected`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val source = NeuronArray(3)
        val target = NeuronArray(2)
        val wm = WeightMatrix(source, target)
        network.addNetworkModelsAsync(source, target, wm)
        val node = panel.getNode(wm) as WeightMatrixNode

        assertNotNull(node.toolTipText)
        node.pixelSelection = setOf(0 to 1)
        assertNull(node.toolTipText)
    }

    @Test
    fun `selecting a pixel clears the canvas tooltip text`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val array = NeuronArray(4)
        network.addNetworkModelsAsync(array)
        val node = panel.getNode(array) as NeuronArrayNode
        // Let the post-add zoom land first: a camera change re-picks the node under the mouse and can set a tooltip
        UiWork.awaitIdle()

        panel.canvas.toolTipText = "stale summary"
        node.pixelSelection = setOf(0)
        assertNull(panel.canvas.toolTipText)
    }

    @Test
    fun `neuron array pixel tooltip shows the neuron index and value`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val array = NeuronArray(4)
        network.addNetworkModelsAsync(array)
        array.setActivations(doubleArrayOf(0.0, 0.73219, -12.5, 0.0))
        val node = panel.getNode(array) as NeuronArrayNode

        assertEquals("Neuron 1: 0.732", node.pixelTooltipText(1))
        assertEquals("Neuron 2: -12.5", node.pixelTooltipText(2))
        assertNull(node.pixelTooltipText(4))
    }

    @Test
    fun `weight matrix cell tooltip names source and target`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val source = NeuronArray(3)
        val target = NeuronArray(2)
        val wm = WeightMatrix(source, target)
        network.addNetworkModelsAsync(source, target, wm)
        wm.weights[1, 2] = -0.25
        val node = panel.getNode(wm) as WeightMatrixNode

        assertEquals("Source 2 → Target 1: -0.25", node.cellTooltipText(wm, 1, 2))
    }

    @Test
    fun `tensor pixel tooltip shows the current channel or the rgb composite`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val layer = TensorLayer(TensorShape(2, 2, 3))
        network.addNetworkModelsAsync(layer)
        layer[1, 0, 0] = 0.1
        layer[1, 0, 1] = 0.2
        layer[1, 0, 2] = 0.3
        val node = panel.getNode(layer) as TensorNode

        layer.thumbnailStripMode = false
        layer.currentChannel = 1
        assertEquals("Ch 2 (h 1, w 0): 0.2", node.pixelTooltipText(1, 0))
        layer.rgbComposite = true
        assertEquals("(h 1, w 0): R 0.1, G 0.2, B 0.3", node.pixelTooltipText(1, 0))
    }

    @Test
    fun `pixel tooltip is withheld while alt is held or pixels are selected`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        fun moveEvent(modifiers: Int) = PInputEvent(
            panel.canvas.root.defaultInputManager,
            MouseEvent(panel.canvas, MouseEvent.MOUSE_MOVED, 0L, modifiers, 0, 0, 0, false)
        )

        panel.showPixelTooltip(moveEvent(0), hasPixelSelection = false) { "Neuron 0: 1" }
        assertEquals("Neuron 0: 1", panel.canvas.toolTipText)
        panel.showPixelTooltip(moveEvent(InputEvent.ALT_DOWN_MASK), hasPixelSelection = false) { "Neuron 0: 1" }
        assertNull(panel.canvas.toolTipText)
        panel.showPixelTooltip(moveEvent(0), hasPixelSelection = true) { "Neuron 0: 1" }
        assertNull(panel.canvas.toolTipText)
    }
}
