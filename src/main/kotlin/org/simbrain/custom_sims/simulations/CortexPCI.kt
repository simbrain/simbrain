package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.connections.RadialSimple
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.createNeurons
import org.simbrain.network.core.networkUpdateAction
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.neuron_update_rules.KuramotoRule
import org.simbrain.util.Utils
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.toDoubleArray
import java.io.File

/**
 * Create a simulation of Cortex...
 */
val cortexPCI = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Cortex Simulation")
    val network = networkComponent.network

    // Template for Kuramoto neuron
    fun Neuron.kuramotoTemplate() {
        updateRule = KuramotoRule().apply {
            naturalFrequency = 100 * Math.random()
        }
        upperBound = 5.0
    }

    // Sparse connectivity
    val sparse = Sparse().apply {
        connectionDensity = .3
        excitatoryRatio = .2
    }

    // Radial connectivity
    val radial = RadialSimple().apply {
        excitatoryRadius = 150.0
        excitatoryProbability = .4
        inhibitoryRadius = 150.0
        inhibitoryProbability = .9
    }

    // Subnetwork 1
    val region1neurons = network.createNeurons(5) { kuramotoTemplate() }
    val region1 = NeuronCollection(network, region1neurons)
    network.addNetworkModel(region1)
    region1.apply {
        label = "Region 1"
        layout(GridLayout())
        location = point(-100, -100)
    }
    radial.connect(network, region1neurons, region1neurons)


    // Region 2
    val region2neurons = network.createNeurons(10) { kuramotoTemplate() }
    val region2 = NeuronCollection(network, region2neurons)
    network.addNetworkModel(region2)
    region2.apply {
       label = "Region 2"
       layout(GridLayout())
       location = point(100, 100)
    }
    radial.connect(network, region2neurons, region2neurons)

    // Region 3
    val region3neurons = network.createNeurons(10) { kuramotoTemplate() }
    val region3 = NeuronCollection(network, region3neurons)
    network.addNetworkModel(region3)
    region3.apply {
        label = "Region 3"
        layout(GridLayout())
        location = point(400, -100)
    }
    radial.connect(network, region3neurons, region3neurons)

    // Make connections between regions
    sparse.connect(network, region2neurons, region1neurons)
    sparse.connect(network, region1neurons, region2neurons)
    sparse.connect(network, region2neurons, region3neurons)
    sparse.connect(network, region3neurons, region2neurons)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    // ----- Add pulse and record activations  ------
    // (Note the pulse has not been actually added yet)

    val activations = mutableListOf<List<Double>>()
    region1.randomize()
    val recordActivations = networkUpdateAction("Record activations") {
        val acts = network.looseNeurons.map { n -> n.activation }
        activations.add(acts)
    }
    network.addUpdateAction(recordActivations)
    workspace.iterate(10)
    network.removeUpdateAction(recordActivations)

    // Save activations
    Utils.writeMatrix(activations.toDoubleArray(), File("activations.csv"))
}
