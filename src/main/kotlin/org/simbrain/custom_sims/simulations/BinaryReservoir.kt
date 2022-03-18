package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.connections.FixedDegree
import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.neuron_update_rules.BinaryRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Create a reservoir simulation...
 */
val binaryReservoir = newSim {

    // TODO: Button for variance and implement.
    // U_bar
    // Measure of chaos, etc.

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Reservoir Sim")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val resNeurons = (0..100).map {
        val rule = BinaryRule()
        rule.threshold = .5
        val neuron = Neuron(network, rule)
        neuron
    }
    network.addNetworkModels(resNeurons)
    val reservoir = NeuronCollection(network, resNeurons)
    network.addNetworkModel(reservoir)
    reservoir.label = "Reservoir"
    reservoir.layout(GridLayout())
    reservoir.location = point(0, 0)

    var k = 2

    val conn = FixedDegree(degree = k)
    conn.connectNeurons(network, resNeurons, resNeurons)

    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

}