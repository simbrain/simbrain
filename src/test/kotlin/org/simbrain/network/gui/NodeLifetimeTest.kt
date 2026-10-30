/**
 * A node that leaves the canvas, and a panel whose view ends, leave no subscriptions behind on the models they showed,
 * which outlive them: undo reuses model instances, and a network outlives its window. A synapse leaving the canvas
 * also gives its symmetric partner back the part of the line it had trimmed to clear this synapse's circle.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.addNeurons
import org.simbrain.network.gui.nodes.TinyLanguageModelNode
import org.simbrain.network.gui.nodes.createTrainingDialog
import org.simbrain.network.llm.TinyLanguageModel
import org.simbrain.network.llm.TinyLmConfig
import org.simbrain.util.UiWork
import org.simbrain.util.onEdt
import org.simbrain.util.point
import org.simbrain.util.subscriberTotal

class NodeLifetimeTest {

    @Test
    fun `a detached neuron array node leaves no subscriptions on its array`() = runBlocking {
        val network = Network()
        val array = NeuronArray(4).also { network.addNetworkModel(it) }
        val component = NetworkComponent("test", network)
        val withoutNode = array.events.subscriberTotal()
        val panel = NetworkPanel(component)
        val node = panel.getNode(array)
        assertTrue(array.events.subscriberTotal() > withoutNode)

        onEdt { panel.detachNode(node) }

        assertEquals(withoutNode, array.events.subscriberTotal())
    }

    @Test
    fun `detached weight matrix and array nodes leave no subscriptions on the matrix or its arrays`() = runBlocking {
        val network = Network()
        val source = NeuronArray(3).also { network.addNetworkModel(it) }
        val target = NeuronArray(2).also { network.addNetworkModel(it) }
        val matrix = WeightMatrix(source, target).also { network.addNetworkModel(it) }
        fun total() = matrix.events.subscriberTotal() + source.events.subscriberTotal() + target.events.subscriberTotal()
        val component = NetworkComponent("test", network)
        val withoutNodes = total()
        val panel = NetworkPanel(component)
        val nodes = listOf(source, target, matrix).map { panel.getNode(it) }
        UiWork.awaitIdle()

        onEdt { nodes.forEach { panel.detachNode(it) } }

        assertEquals(withoutNodes, total())
    }

    @Test
    fun `a detached tiny language model node leaves no subscriptions on the model or its trainer`() = runBlocking {
        val network = Network()
        val model = TinyLanguageModel(
            TinyLmConfig(contextSize = 6, embedDim = 12, numHeads = 3, hiddenDim = 16, vocabSize = 5, numLayers = 1)
        ).also { network.addNetworkModel(it) }
        val component = NetworkComponent("test", network)
        val withoutNode = model.events.subscriberTotal() + model.trainer.events.subscriberTotal()
        val panel = NetworkPanel(component)
        val node = panel.getNode(model) as TinyLanguageModelNode
        UiWork.awaitIdle()

        onEdt { panel.detachNode(node) }

        assertEquals(withoutNode, model.events.subscriberTotal() + model.trainer.events.subscriberTotal())
    }

    @Test
    fun `a closed tiny language model training dialog leaves no subscriptions on the trainer`() = runBlocking {
        val model = TinyLanguageModel(
            TinyLmConfig(contextSize = 6, embedDim = 12, numHeads = 3, hiddenDim = 16, vocabSize = 5, numLayers = 1)
        )
        val withoutDialog = model.trainer.events.subscriberTotal()
        val dialog = onEdt { model.createTrainingDialog(null) }
        assertTrue(model.trainer.events.subscriberTotal() > withoutDialog)

        onEdt { dialog.dispose() }

        assertEquals(withoutDialog, model.trainer.events.subscriberTotal())
    }

    @Test
    fun `a panel whose view ends leaves no subscriptions on its network`() = runBlocking {
        val network = Network()
        network.addNeurons(2)
        // The component outlives its panel's view, so its own subscriptions stay
        val component = NetworkComponent("test", network)
        val withoutPanel = network.events.subscriberTotal()
        val panel = NetworkPanel(component)
        assertTrue(network.events.subscriberTotal() > withoutPanel)

        panel.viewScope.cancel()

        assertEquals(withoutPanel, network.events.subscriberTotal())
    }

    @Test
    fun `a detached synapse gives its symmetric partner back its full line`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val (a, b) = network.addNeurons(2)
        a.location = point(0.0, 0.0)
        b.location = point(200.0, 0.0)
        val forward = Synapse(a, b).also { network.addNetworkModel(it) }
        val backward = Synapse(b, a).also { network.addNetworkModel(it) }
        val forwardNode = panel.getNode(forward)
        val backwardNode = panel.getNode(backward)
        UiWork.awaitIdle()
        val trimmedWidth = onEdt { backwardNode.globalFullBounds.width }

        // Undo's stale-node pass detaches nodes this way, without the panel's deleted-model path
        onEdt { panel.detachNode(forwardNode) }

        val width = onEdt { backwardNode.globalFullBounds.width }
        assertTrue(width > trimmedWidth, "the partner kept its trimmed line after the synapse left: $trimmedWidth -> $width")
    }
}
