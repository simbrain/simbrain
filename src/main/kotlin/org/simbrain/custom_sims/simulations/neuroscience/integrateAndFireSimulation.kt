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
import org.simbrain.util.stats.distributions.PoissonDistribution
import kotlin.math.sqrt

/**
 * A simulation of an integrate and fire network with sparse connectivity.
 * Converted from the original Bean script to Kotlin using the new simulation framework.
 */
val integrateAndFireSimulation = newSim {


    val numNeurons = showNumericInputDialog("Number of Neurons:", 49) ?: return@newSim

    val gridSpace = 50.0
    val sparsity = 0.20 // Percent of possible connections to make, and change to alter synchronous firing
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
    
    // Randomize weights and delays
    val randDelay = PoissonDistribution(3.0)
    synapses.forEach { synapse ->
        if (synapse.strength > 0) {
            synapse.strength = exciteRand.sampleDouble()
        } else {
            synapse.strength = -inhibRand.sampleDouble()
        }
        synapse.delay = randDelay.sampleInt()
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

        # Introduction

        ## Basic
        This simulation lets you explore how a network of simple brain-like cells, called integrate-and-fire neurons, behave together. It's a great way to learn how raster plots help visualize when neurons "spike" or send signals, and how the structure of a recurrent neural network influences these spiking patterns over time.

        ## Advanced
        This simulation models a recurrent network of leaky integrate-and-fire neurons, demonstrating complex spiking behavior shaped by sparse, balanced excitatory and inhibitory connectivity.

        # Background

        ## Basic
        Integrate-and-fire neurons are simple, biologically-inspired models of brain cells that “spike” when their internal signal, or membrane potential, crosses a certain threshold. By adjusting how the neurons connect and how they’re activated, you can observe a wide range of network behaviors—from random, asynchronous firing to more organized bursts of activity.

        ## Advanced
        The neurons implement leaky integrate-and-fire dynamics: they integrate synaptic input currents, experience membrane potential decay over time, and emit spikes upon reaching threshold. Network connectivity is sparse (~5%), with balanced excitatory and inhibitory neuron populations influencing emergent firing patterns.

        # Simulation Details

        ## Neuron Model

        - Basic: Each neuron in the network adds up incoming signals and fires, or “spikes,” when it reaches a certain level, then resets. These are leaky integrate-and-fire neurons, meaning their internal signal also fades or “leaks” over time, creating more realistic, time-dependent behavior as they integrate input and respond when a threshold is crossed.
        - Advanced: Neurons follow leaky integrate-and-fire dynamics — integrating input currents, undergoing exponential decay, and firing spikes at threshold crossing, followed by a reset to resting potential.

        For more info: [Integrate-and-Fire documentation](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html).

        ## Network Structure

        - Basic: The network contains 49 neurons, each connected to about 5% of the others in a sparse, random pattern. Half of the neurons are excitatory, increasing activity in their targets, while the other half are inhibitory, decreasing it.
        - Advanced: The network has 49 neurons connected using sparse connectivity (`sparsity = 0.05`). Connections are unidirectional, fixed-weight, and the network is 50% excitatory and 50% inhibitory neurons, with no synaptic plasticity.

        More on sparse connections: [Sparse Connections documentation](https://docs.simbrain.net/docs/network/connections/sparse.html).

        ## Visualization

        - Basic: A raster plot shows when each neuron spikes over time—each row represents a neuron, and each tick marks a spike. If many ticks align vertically, it means neurons are firing together (synchrony), though in this simulation, the spikes are mostly scattered and asynchronous.
        - Advanced: Raster plots visualize precise spike timing and synchrony among neurons, revealing how network structure shapes dynamic patterns.

        Learn more: [Raster Plots](https://docs.simbrain.net/docs/plots/rasterPlot.html).

        ## Key Concepts

        - **Integrate-and-Fire Neurons**: Simple neuron models that spike when their membrane potential crosses a threshold.
        - **Raster Plot**: A graphical display showing when neurons fire over time; rows represent neurons, ticks represent spikes.
        - **Sparsity**: The fraction of possible synaptic connections actually formed in the network.
        - **Excitatory Ratio**: The percentage of connections that excite neurons versus inhibit them.

        # What to Do

        ## How to Use

        1. Click `Run` to start the simulation.
        2. Press `Randomize Activations` to explore different starting points.
        3. Adjust `sparsity` or `excitatory ratio` with the buttons; each change rebuilds the network.
        4. Watch the raster plot and ask:
        - Are neurons spiking together (synchrony)?
        - Are firing patterns regular (limit cycles) or irregular (chaotic)?

        ## Try This

        - Observe spike dynamics live on the raster plot.
        - Change connectivity settings like sparsity and excitatory/inhibitory balance.
        - Randomize neuron activations to explore different network states.
        - Rebuild the network and observe how changes in structure affect emergent activity.

        # Links

        - [Integrate-and-Fire Neuron Docs](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html) – Detailed neuron model information.
        - [Sparse Connectivity](https://docs.simbrain.net/docs/network/connections/sparse.html) – Explanation of sparse synaptic connections.
        - [Raster Plot](https://docs.simbrain.net/docs/plots/rasterPlot.html) – Details on spike timing visualization.

        # References

        Brunel, N. (2000). [Dynamics of sparsely connected networks of excitatory and inhibitory spiking neurons](https://doi.org/10.1023/A:1008925309027). _Journal of Computational Neuroscience_, _8_(3), 183–208.

        # Credits

        Jeff Yoshimi, 
        Elijah Olson
        """.trimIndent()
    )
}