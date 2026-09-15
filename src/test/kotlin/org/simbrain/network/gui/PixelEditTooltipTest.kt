package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.nodes.NeuronArrayNode
import org.simbrain.network.gui.nodes.WeightMatrixNode

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

        panel.canvas.toolTipText = "stale summary"
        node.pixelSelection = setOf(0)
        assertNull(panel.canvas.toolTipText)
    }
}
