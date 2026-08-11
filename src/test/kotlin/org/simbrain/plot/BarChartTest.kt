package org.simbrain.plot

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.plot.barchart.BarChartComponent
import org.simbrain.workspace.Workspace

class BarChartTest {

    @Test
    fun `bars are named after neuron labels and follow renames without an iteration`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Network")
        workspace.addWorkspaceComponent(networkComponent)
        val network = networkComponent.network
        val neurons = List(2) { Neuron() }
        neurons.forEach { network.addNetworkModelAsync(it) }
        neurons[0].label = "Left"
        neurons[1].label = "Right"
        val collection = NeuronCollection(neurons).apply { isClamped = true }
        network.addNetworkModelAsync(collection)

        val barChartComponent = BarChartComponent("Bars")
        workspace.addWorkspaceComponent(barChartComponent)
        workspace.couplingManager.createCoupling(collection, barChartComponent.model)

        collection.activationArray = doubleArrayOf(0.25, 0.75)
        workspace.simpleIterate()
        awaitUntil(message = "Bars were not named after neuron labels") {
            barChartComponent.model.getDataset().columnKeys == listOf("Left", "Right")
        }
        assertEquals(0.75, barChartComponent.model.getDataset().getValue(0, 1))

        neurons[0].label = "West"
        awaitUntil(message = "Bar name did not follow the neuron rename without an iteration") {
            barChartComponent.model.getDataset().columnKeys == listOf("West", "Right")
        }
        assertEquals(0.25, barChartComponent.model.getDataset().getValue(0, 0))
    }

    @Test
    fun `deleting a neuron removes its bar rather than renumbering it, and undoing brings it back`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Network")
        workspace.addWorkspaceComponent(networkComponent)
        val network = networkComponent.network
        val neurons = List(3) { Neuron() }
        neurons.forEach { network.addNetworkModelAsync(it) }
        neurons[0].label = "Alpha"
        neurons[1].label = "Beta"
        neurons[2].label = "Gamma"
        val collection = NeuronCollection(neurons).apply { isClamped = true }
        network.addNetworkModelAsync(collection)

        val barChartComponent = BarChartComponent("Bars")
        workspace.addWorkspaceComponent(barChartComponent)
        workspace.couplingManager.createCoupling(collection, barChartComponent.model)

        collection.activationArray = doubleArrayOf(1.0, 2.0, 3.0)
        workspace.simpleIterate()
        awaitUntil { barChartComponent.model.getDataset().columnKeys == listOf("Alpha", "Beta", "Gamma") }

        runBlocking { neurons[1].delete() }

        // Without waiting for another update: the deleted neuron's bar goes, rather than staying behind
        // under the stand-in number it would get once it no longer has a name
        awaitUntil(message = "The deleted neuron's bar was not removed") {
            barChartComponent.model.getDataset().columnKeys == listOf("Alpha", "Gamma")
        }

        // Undoing a deletion restores collection membership through restoreNeuron
        network.addNetworkModelAsync(neurons[1])
        collection.restoreNeuron(neurons[1])

        awaitUntil(message = "The restored neuron's bar did not come back") {
            barChartComponent.model.getDataset().columnKeys == listOf("Alpha", "Gamma", "Beta")
        }
    }

    @Test
    fun `an array producer without labels leaves the bars numbered`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Network")
        workspace.addWorkspaceComponent(networkComponent)
        val network = networkComponent.network
        val neurons = List(2) { Neuron() }
        neurons.forEach { network.addNetworkModelAsync(it) }
        neurons[0].label = "Left"
        val collection = NeuronCollection(neurons).apply { isClamped = true }
        network.addNetworkModelAsync(collection)

        val barChartComponent = BarChartComponent("Bars")
        workspace.addWorkspaceComponent(barChartComponent)
        with(workspace.couplingManager) {
            // "inputArray" declares no arrayComponentsMethod, so it supplies no per-bar names at all and
            // must not have the attribute's own name applied to the first bar
            collection.getProducer("getInputArray") couple barChartComponent.model.getConsumer("setBarValues")
        }

        workspace.simpleIterate()
        assertEquals(listOf("1", "2"), barChartComponent.model.getDataset().columnKeys)
    }
}
