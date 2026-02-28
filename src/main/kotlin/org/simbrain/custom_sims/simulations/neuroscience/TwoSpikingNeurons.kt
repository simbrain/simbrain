package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapseAsync
import org.simbrain.network.updaterules.AdExIFRule
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
        updateRule = AdExIFRule()
    }

    // Connect input neurons to the spiking neuron
    net.addSynapseAsync(excitatoryInput, spikingNeuron).strength = 5.0
    net.addSynapseAsync(inhibitoryInput, spikingNeuron).strength = -5.0

    // Time series plot for spiking neuron
    val timeSeriesPlot = addTimeSeriesComponent("Membrane potential", listOf("Spiking neuron"))

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
        # Two-Input Spiking Neuron

        This simulation demonstrates how a single neuron responds to excitatory and inhibitory inputs using a biologically realistic spiking model. 
        You can manually adjust both input levels and observe how they interact to control the neuron’s firing. When the excitatory signal is strong enough, the neuron spikes; 
        when inhibition dominates, it may be silenced. This gives you a hands-on way to explore how neurons integrate multiple sources of input.

        The spiking neuron uses the [AdEx model](https://docs.simbrain.net/docs/network/neurons/adaptiveExIntegAndFire.html#adex-integrate-and-fire) 
        shows the neuron's activity over time, letting you visualize how input balance affects firing.

        # Simulation Details

        Spiking neurons integrate signals from multiple sources and fire only when their internal voltage crosses a threshold. In this setup:

        - The excitatory neuron sends current with a synaptic strength of `+5.0`.
        - The inhibitory neuron sends current with strength `-5.0`.
        - These inputs combine in the Izhikevich neuron, which computes its next state using a nonlinear system of equations.
        - If the net input drives the membrane potential above the spike threshold, the neuron fires and resets.

        This simple network has no recurrent or lateral connections—only forward input into a single neuron—so you can clearly see how input balance shapes output behavior.

        # What to Do

        1. Press `Run` to start the simulation.
        2. Click either the `Excitatory Input` or `Inhibitory Input` neuron.
        3. Use the arrow keys (`↑` / `↓`) to raise or lower the activation level.
        4. Observe how the spiking neuron's spiking behavior changes in the time series plot.

        ## Exploration of the impacts of input activations

        - Raise only the `Excitatory Input`: The neuron should spike consistently.
        - Raise only the `Inhibitory Input`: The neuron may stop firing.
        - Adjust both inputs to fine-tune whether and when spikes occur.
        
        ## Exploration of other spiking neuron models
        
        You can also change the spiking neuron model to a different model (i.e., Izhikevich, [Integrate-and-Fire](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html),
        etc) to see other learning model behaviors by double-clicking on `Spiking neuron` to `Update Rule`.

        # Credits

        Elijah Olson  
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        Kanly Thao
        
        """.trimIndent()
    )
}