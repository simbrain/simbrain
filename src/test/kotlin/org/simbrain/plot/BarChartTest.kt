package org.simbrain.plot

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
}
