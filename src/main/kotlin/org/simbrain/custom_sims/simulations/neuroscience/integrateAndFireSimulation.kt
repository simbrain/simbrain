package org.simbrain.custom_sims.simulations.neuroscience

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.connections.Sparse
import org.simbrain.network.connections.polarizeSynapses
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeuron
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.stats.distributions.NormalDistribution
import kotlin.math.sqrt

/**
 * A simulation of an integrate and fire network with sparse connectivity.
 * Converted from the original Bean script to Kotlin using the new simulation framework.
 */
val integrateAndFireSimulation = newSim {

    // Parameters 
    val numNeurons = 49
    val gridSpace = 50.0
    val sparsity = 0.05 // Percent of possible connections to make
    val excitatoryRatio = 5.0 // Percent of connections that will be excitatory
    
    // Setup workspace and create network component
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Integrate and Fire Network")
    val network = networkComponent.network
    
    // Create layout for neurons
    val layout = GridLayout(gridSpace, gridSpace, (sqrt(numNeurons.toDouble())).toInt())
    
    // Create neurons with integrate and fire rules
    val neurons = buildList {
        val neuron = runBlocking {
            network.addNeuron {
                updateRule = IntegrateAndFireRule().apply {
                    timeConstant = 5.0
                    resetPotential = 2.0
                    threshold = 11.0
                }
            }
        }
        add(neuron)
    }

    val neuronCollection = NeuronCollection(neurons)

    network.addNetworkModel(neuronCollection)
    
    // Apply layout to neurons
    layout.layoutNeurons(neurons)
    
    // Create sparse connections between neurons
    val sparse = Sparse(sparsity, false, true)
    val synapses = sparse.connectNeurons(neurons, neurons)
    
    // Setup randomizers for excitatory and inhibitory weights
    val exciteRand = NormalDistribution().apply {
        mean = 1.0
        standardDeviation = 0.1
    }
    
    val inhibRand = NormalDistribution().apply {
        mean = 5.0
        standardDeviation = 0.1
    }
    
    // Polarize synapses according to specified excitatory ratio
    polarizeSynapses(synapses, excitatoryRatio)
    
    // Randomize weights with appropriate distributions
    synapses.forEach { synapse ->
        if (synapse.strength > 0) {
            synapse.strength = exciteRand.sampleDouble()
        } else {
            synapse.strength = -inhibRand.sampleDouble()
        }
    }

    network.addNetworkModels(synapses)
    
    // Randomize neuron activations
    neurons.forEach { it.randomize() }
    
    // Create a raster plot to visualize the spikes
    val rasterPlot = RasterPlotComponent("Spike Raster Plot")
    workspace.addWorkspaceComponent(rasterPlot)
    
    // Position components in the GUI
    withGui {
        place(networkComponent) {
            location = point(10, 10)
            width = 500
            height = 500
        }
        
        place(rasterPlot) {
            location = point(520, 10)
            width = 500
            height = 500
        }
    }
    
    // Set up the coupling between the neuron collection and the raster plot
    with(couplingManager) {
        neuronCollection.getProducer(neuronCollection::spikes) couple
            rasterPlot.model.rasterConsumerList[0].getConsumer("setValues")
    }
}