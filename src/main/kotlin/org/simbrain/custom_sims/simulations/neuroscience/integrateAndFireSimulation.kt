package org.simbrain.custom_sims.simulations.neuroscience

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.Sparse
import org.simbrain.network.connections.polarizeSynapses
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeuron
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.showNumericInputDialog
import org.simbrain.util.stats.distributions.NormalDistribution
import kotlin.math.sqrt

/**
 * A simulation of an integrate and fire network with sparse connectivity.
 * Converted from the original Bean script to Kotlin using the new simulation framework.
 */
val integrateAndFireSimulation = newSim {


    val numNeurons = showNumericInputDialog("Number of Neurons:", 49) ?: return@newSim

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
        repeat(numNeurons) {
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
        place(networkComponent, 210, 0, 600, 600)
        place(rasterPlot, 810, 0, 600, 600)
        createControlPanel("Controls", 0, 0) {
            addButton("Randomize Activations") {
                neuronCollection.randomize()
            }
            addButton("Sparsity") {
                // TODO
            }
            addButton("Excitatory Ratio") {
               // TODO
            }
        }
    }
    
    // Set up the coupling between the neuron collection and the raster plot
    with(couplingManager) {
        neuronCollection.getProducer(neuronCollection::spikes) couple
            rasterPlot.model.rasterConsumerList[0].getConsumer("setValues")
    }

    addSidebarInfo(
        """
            # Integrate-and-Fire Network with Raster Plot
            
            ## Overview
            
            This simulation allows users to explore the dynamics of a recurrent network of integrate-and-fire neurons. 
            It's also a good way to learn how **raster plots** can be used to visualize neural spike patterns and how 
            network structure influences those patterns.
            
            ## What You Can Do
            
            - **Observe spike dynamics** in a live raster plot.
            - **Adjust connectivity parameters** like sparsity and excitatory/inhibitory balance.
            - **Randomize neuron activations** to explore new initial states.
            - **Rebuild the network** to see how changes in structure affect emergent activity patterns.
            
            ## Key Concepts
            
            - **Integrate-and-Fire Neurons:** Simple neuron models that spike when their potential exceeds a threshold.
            - **Raster Plot:** A graphical display of spikes across neurons over time. Each row corresponds to a neuron, and each tick marks a spike.
            - **Sparsity:** Controls the proportion of possible synaptic connections that are actually created.
            - **Excitatory Ratio:** Specifies the percentage of synapses that are excitatory versus inhibitory.
            
            ## How to Use
            
            1. **Start the simulation** (click "Run" in the toolbar).
            2. Click **"Randomize Activations"** to explore different starting points.
            3. Adjust **sparsity** or **excitatory ratio** using the respective buttons. Each change rebuilds the network.
            4. Watch how changes affect the raster plot: 
               - Are the neurons spiking in synchrony?
               - Are patterns regular (limit cycles) or irregular (chaotic)?
            
            This hands-on tool provides an intuitive way to explore spiking neural dynamics and network connectivity.

            """.trimIndent()
    )
}