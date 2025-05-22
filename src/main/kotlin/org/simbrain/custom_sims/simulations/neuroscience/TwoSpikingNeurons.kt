package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Simulation with two neurons connecting to a single spiking neuron to explore basic
 * neural dynamics relative to excitatory and inhibitory currents.
 */
val spikingNeuronTwoInputs = newSim {

    workspace.clearWorkspace()

    val netComponent = addNetworkComponent("Simple Integrate and Fire")
    val net = netComponent.network

    // Create clamped input neurons
    val excitatoryInput = net.addNeuron {
        label = "Excitatory Input"
        location = point(50, 100)
        clamped = true
        increment = 1.0
    }

    val inhibitoryInput = net.addNeuron {
        label = "Inhibitory Input"
        location = point(50, 200)
        clamped = true
    }

    // Create the integrate-and-fire neuron
    val spikingNeuron = net.addNeuron {
        label = "Spiking neuron"
        location = point(200, 150)
        updateRule = IzhikevichRule().apply {
            threshold = 5.0
        }
    }

    // Connect input neurons to the spiking neuron
    net.addSynapse(excitatoryInput, spikingNeuron).strength = 5.0
    net.addSynapse(inhibitoryInput, spikingNeuron).strength = -5.0

    // Time series plot for spiking neuron
    val timeSeriesPlot = addTimeSeriesComponent("Spiking Activity", listOf("Spiking neuron"))

    // Coupling the spiking neuron's activation to the plot
    with(couplingManager) {
        spikingNeuron couple timeSeriesPlot.model.timeSeriesList[0]
    }

    // GUI layout
    withGui {
        place(netComponent, 0, 0, 400, 400)
        place(timeSeriesPlot, 420, 0, 400, 400)
    }

    addSidebarInfo(
        """
    # Simple Integrate-and-Fire Neuron

    ## Overview

    This simulation demonstrates a basic spiking neuron model receiving input from two clamped sources:

    - One **excitatory input** (positive influence)
    - One **inhibitory input** (negative influence)

    The output neuron's spiking behavior is visualized over time using a **time series plot**.

    ## How It Works

    - When the membrane potential of the spiking neuron exceeds a threshold, it emits a spike and resets.
    - The excitatory input increases its potential.
    - The inhibitory input decreases it.
    - You can adjust the input activations in the GUI by selecting an input neuron and pressing the arrow keys (up/down).

    ## Instructions

    1. **Press Run** to start the simulation.
    2. **Select an input neuron** and use the arrow keys to adjust its activation.
    3. **Observe** how the spiking neuron responds in the time series plot.

    ## Concepts Illustrated

    - **Integrate-and-fire model:** A simplified model capturing the essence of neuronal spiking.
    - **Excitatory and inhibitory inputs:** Fundamental building blocks of neural computation.
    - **Spike trains:** Repeated spiking activity shown in a plot can resemble real neuron recordings.

    ## Try This

    - Increase excitatory input and see regular spiking.
    - Increase inhibitory input to suppress spiking.
    - Balance both inputs to finely control the spiking rate.
    - Change the type of the neuron from Izkhikevich to another type of spiking neuron, like Integrate and Fire or Adex.
    """.trimIndent()
    )


}