package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.connections.RadialGaussian
import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.neuron_update_rules.KuramotoRule
import org.simbrain.util.SimbrainConstants
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Create a simulation of Cortex...
 */
val cortexPCI = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Cortex Simulation")
    val network = networkComponent.network

    // Function to create nodes
    fun getNeurons(numNodes: Int): List<Neuron> {
        return (0..numNodes).map {
            val rule = KuramotoRule()
            rule.naturalFrequency = 100 * Math.random()
            val neuron = Neuron(network, rule)
            if (Math.random() < 0.5) {
                neuron.polarity = SimbrainConstants.Polarity.INHIBITORY
            } else {
                neuron.polarity = SimbrainConstants.Polarity.EXCITATORY
            }
            neuron.upperBound = 5.0
            neuron
        }
    }

    // Add a self-connected neuron array to the network
    val neuronList1 = getNeurons(5)
    network.addNetworkModels(neuronList1)
    val region1 = NeuronCollection(network, neuronList1)
    network.addNetworkModel(region1)
    region1.label = "Region 1"
    region1.layout(GridLayout())
    region1.location = point(-100,-100)
    var syns = RadialGaussian.connectRadialPolarized(neuronList1, neuronList1)
    print(syns.size)
    network.addNetworkModels(syns)

    val neuronList2 = getNeurons(10)
    network.addNetworkModels(neuronList2)
    val region2 = NeuronCollection(network, neuronList2)
    network.addNetworkModel(region2)
    region2.label = "Region 2"
    region2.layout(GridLayout())
    region2.location = point(0,0)
    syns = RadialGaussian.connectRadialPolarized(neuronList2, neuronList2)
    // syns = Sparse.connectSparse(neuronList2, neuronList2, .8, true, true, true)
    print(syns.size)
    network.addNetworkModels(syns)


    val neuronList3 = getNeurons(8)
    network.addNetworkModels(neuronList3)
    val region3 = NeuronCollection(network, neuronList3 )
    network.addNetworkModel(region3)
    region3.label = "Region 3"
    region3.layout(GridLayout())
    region3.location = point(100,100)

    // Make connections between regions
    syns = RadialGaussian.connectRadialPolarized(neuronList1, neuronList2)
    network.addNetworkModels(syns)
    syns = RadialGaussian.connectRadialPolarized(neuronList2, neuronList3)
    network.addNetworkModels(syns)

    // TODO: Code to create the pulse and log the data

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

}