package org.simbrain.custom_sims.simulations.neuroscience

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.Sparse
import org.simbrain.network.connections.polarizeSynapses
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.network.updaterules.SpikingThresholdRule
import org.simbrain.network.updaterules.activity_generators.SinusoidalRule
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.util.place
import org.simbrain.util.showNumericInputDialog
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.PoissonDistribution
import javax.swing.JOptionPane
import kotlin.math.sqrt

/**
 * A simulation of a spiking network with a raster plot that can be customized for exploration.
 */
val spikingNetworkSimulation = newSim {

    val numNeurons = showNumericInputDialog("Number of Neurons:", 49) ?: return@newSim

    val gridSpace = 50.0
    val sparsity = 0.20 // Percent of possible connections to make, and change to alter synchronous firing
    val excitatoryRatio = 5.0 // Percent of connections that will be excitatory
    
    // Pacemaker neuron tracking
    var pacemakerNeuron: Neuron? = null
    
    // Setup workspace and create network component
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Integrate and Fire Network")
    val network = networkComponent.network
    
    // Create layout for neurons
    val layout = GridLayout(gridSpace, gridSpace, (sqrt(numNeurons.toDouble())).toInt())

    // Default params for Integrate and Fire Rules
    fun IntegrateAndFireRule.setIntFireParams() {
        timeConstant = 5.0
        resetPotential = 2.0
        threshold = 11.0
    }

    // Create neurons with integrate and fire rules
    val neurons = buildList {
        repeat(numNeurons) {
            val neuron = runBlocking {
                network.addNeuron {
                    updateRule = IntegrateAndFireRule().apply {
                        setIntFireParams()
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
                val newSparsity = showNumericInputDialog("Enter new sparsity (0.0 to 1.0):", sparsity) ?: return@addButton
                
                // Remove existing synapses
                network.flatSynapseList.filter { synapse ->
                    synapse.source in neurons && synapse.target in neurons
                }.forEach { synapse ->
                    runBlocking { synapse.delete() }
                }
                
                // Create new sparse connections with updated sparsity
                val newSparse = Sparse(newSparsity, false, true)
                val newSynapses = newSparse.connectNeurons(neurons, neurons)
                
                // Polarize synapses according to current excitatory ratio
                polarizeSynapses(newSynapses, excitatoryRatio)
                
                // Randomize weights and delays
                newSynapses.forEach { synapse ->
                    if (synapse.strength > 0) {
                        synapse.strength = exciteRand.sampleDouble()
                    } else {
                        synapse.strength = -inhibRand.sampleDouble()
                    }
                    synapse.delay = randDelay.sampleInt()
                }
                
                // Add new synapses to network
                network.addNetworkModels(newSynapses)
            }
            addButton("Excitatory Ratio") {
                val newExcitatoryRatio = showNumericInputDialog("Enter new excitatory ratio (0.0 to 100.0):", excitatoryRatio) ?: return@addButton
                
                // Remove existing synapses
                network.flatSynapseList.filter { synapse ->
                    synapse.source in neurons && synapse.target in neurons
                }.forEach { synapse ->
                    runBlocking { synapse.delete() }
                }
                
                // Create new sparse connections with current sparsity
                val newSparse = Sparse(sparsity, false, true)
                val newSynapses = newSparse.connectNeurons(neurons, neurons)
                
                // Polarize synapses according to new excitatory ratio
                polarizeSynapses(newSynapses, newExcitatoryRatio)
                
                // Randomize weights and delays
                newSynapses.forEach { synapse ->
                    if (synapse.strength > 0) {
                        synapse.strength = exciteRand.sampleDouble()
                    } else {
                        synapse.strength = -inhibRand.sampleDouble()
                    }
                    synapse.delay = randDelay.sampleInt()
                }
                
                // Add new synapses to network
                network.addNetworkModels(newSynapses)
            }
            addButton("Delays") {
                val newDelayMean = showNumericInputDialog("Enter new mean synaptic delay in milliseconds (Poisson parameter):", randDelay.p) ?: return@addButton
                
                // Update the Poisson distribution parameter
                randDelay.p = newDelayMean
                
                // Update delays for all existing synapses
                network.flatSynapseList.filter { synapse ->
                    synapse.source in neurons && synapse.target in neurons
                }.forEach { synapse ->
                    synapse.delay = randDelay.sampleInt()
                }
            }
            addButton("Neuron Type") {
                val options = arrayOf("Integrate and Fire", "Izhikevich", "Spiking Threshold")
                val selectedOption = JOptionPane.showOptionDialog(
                    null,
                    "Select neuron update rule:",
                    "Change Neuron Type",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
                )
                
                if (selectedOption != JOptionPane.CLOSED_OPTION) {
                    // Change all neurons to the selected update rule
                    neurons.forEach { neuron ->
                        neuron.updateRule = when (selectedOption) {
                            0 -> IntegrateAndFireRule().apply {
                                setIntFireParams()
                            }
                            1 -> IzhikevichRule().apply {
                                // "Subthreshold" params, which hover around rest with little voltage ripples but never fire unless you add enough current to push them into the spiking regime.
                                a = 0.05
                                b = 0.26
                                c = -60.0
                                d = -1.0
                                backgroundCurrent = 0.0
                            }
                            2 -> SpikingThresholdRule().apply {
                                threshold = 0.5
                            }
                            else -> IntegrateAndFireRule().apply {
                                setIntFireParams()
                            }
                        }
                    }
                }
            }
            addButton("Add Pacemaker") {
                if (pacemakerNeuron == null) {
                    val frequency = showNumericInputDialog("Enter pacemaker frequency (Hz):", 0.5) ?: return@addButton
                    
                    // Create pacemaker neuron with sinusoidal activity
                    pacemakerNeuron = runBlocking {
                        network.addNeuron {
                            updateRule = SinusoidalRule().apply {
                                this.frequency = frequency
                                upperBound = 1.0
                                lowerBound = -1.0
                                phase = 0.0
                            }
                            // Position off to the side
                            x = neuronCollection.bound.minX
                            y = neuronCollection.bound.maxY + 100.0
                        }
                    }
                    
                    // Connect pacemaker to all neurons (one-to-all)
                    val pacemakerSynapses = buildList {
                        neurons.forEach { targetNeuron ->
                            val synapse = runBlocking {
                                network.addSynapse(pacemakerNeuron!!, targetNeuron)
                            }
                            synapse.strength = 2.0
                            //synapse.delay = 1
                            add(synapse)
                        }
                    }
                    
                    network.addNetworkModels(pacemakerSynapses)
                } else {
                    // Update existing pacemaker frequency
                    val currentFreq = (pacemakerNeuron!!.updateRule as SinusoidalRule).frequency
                    val newFreq = showNumericInputDialog("Enter new pacemaker frequency (Hz):", currentFreq) ?: return@addButton
                    (pacemakerNeuron!!.updateRule as SinusoidalRule).frequency = newFreq
                }
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
        # Spiking Network with Raster Plot

        This simulation lets you explore how a network of spiking neurons behave. You can modify parameters of the models with the buttons and see how this impacts network properties like synchrony.
                
        # Background

        Integrate-and-fire neurons are  biologically-inspired models of brain cells that "spike" when their internal signal, or membrane potential, crosses a certain threshold. By adjusting how the neurons connect and how they're activated, you can observe a wide range of network behaviors—from random, asynchronous firing to more organized bursts of activity.

        The neurons implement leaky integrate-and-fire dynamics: they integrate synaptic input currents, experience membrane potential decay over time, and emit spikes upon reaching threshold. Network connectivity is sparse (~5%), with balanced excitatory and inhibitory neuron populations influencing emergent firing patterns.

        Each neuron in the network adds up incoming signals and fires, or "spikes," when it reaches a certain level, then resets. These are leaky integrate-and-fire neurons, meaning their internal signal also fades or "leaks" over time, creating more realistic, time-dependent behavior as they integrate input and respond when a threshold is crossed.

        Different features of the network like [sparsity](https://docs.simbrain.net/docs/network/connections/sparse.html), the fraction of possible synaptic connections actually formed in the network,
        and excitatory ratio (the percentage of connections that excite neurons versus inhibit them) can have an impact on network dynamics.

        # Visualization

        A [raster plot](https://docs.simbrain.net/docs/plots/rasterPlot.html). shows when each neuron spikes over time—each row represents a neuron, and each tick marks a spike. If many ticks align vertically, it means neurons are firing together (synchrony), though in this simulation, the spikes are mostly scattered and asynchronous.
        Raster plots visualize precise spike timing and synchrony among neurons, revealing how network structure shapes dynamic patterns.

        # What to Do

        1. Click `Run` to start the simulation.
        2. Press `Randomize Activations` to explore different starting points.
        3. Adjust `sparsity` or `excitatory ratio` with the buttons; each change rebuilds the network.
        4. Watch the raster plot and ask:
        - Are neurons spiking together (synchrony)?
        - Are firing patterns regular (limit cycles) or irregular (probably chaotic)?

        Other things you can try:

        - Switch between different neuron models (Integrate-and-Fire, Izhikevich, Spiking Threshold) to see how they affect network dynamics.
        - Adjust synaptic delays to explore how temporal dynamics influence firing patterns.
        - With neurons as Izhikevich try parameters (`N` and double click to edit all) and use parameers from the [Izhikevich docs](https://docs.simbrain.net/docs/network/neurons/izhikevich.html) at the bottom to create specific types of node
        - Add a pacemaker neuron with sinusoidal activity that drives the entire network at a regular frequency - observe how this affects synchrony and firing patterns.
        
        # References

        Brunel, N. (2000). [Dynamics of sparsely connected networks of excitatory and inhibitory spiking neurons](https://doi.org/10.1023/A:1008925309027). _Journal of Computational Neuroscience_, _8_(3), 183–208.

        # Credits

        [Jeff Yoshimi](www.jeffyoshimi.net) 
        Elijah Olson
        """.trimIndent()
    )
}