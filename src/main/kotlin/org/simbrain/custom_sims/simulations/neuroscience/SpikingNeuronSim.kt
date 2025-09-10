package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Create a spiking neuron, with an input, and graph its activity with a time series.
 */
val spikingNeuron = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val input = network.addNeuron {
        label = "Input"
        location = point(100, 100)
        increment = 2.0
        clamped = true
    }
    val spiking = network.addNeuron {
        updateRule = IzhikevichRule().apply {
            backgroundCurrent = 0.0
        }
        label = "Spiking"
        location = point(200, 100)
    }
    val postSpiking = network.addNeuron {
        label = "Post-Synaptic Response"
        location = point(300, 100)
    }

    network.addSynapse(input, spiking)
    network.addSynapse(spiking, postSpiking).apply {
        spikeResponder = JumpAndDecay()
    }

    withGui {
        place(networkComponent, 0, 0, 400,400)
    }

    val spikes = addTimeSeriesComponent("Spikes", seriesNames = listOf("Spiking"))
    val spikeResponses = addTimeSeriesComponent("Spike Responses", seriesNames = listOf("PSR"))

    with(couplingManager) {
        spiking couple spikes.model.timeSeriesList[0]
        postSpiking couple spikeResponses.model.timeSeriesList[0]
    }

    addSidebarInfo(
        """
        # Introduction

        This simulation allows you to explore the behavior of a [spiking neuron model](https://docs.simbrain.net/docs/network/spikingneurons.html), which responds to inputs in a realistic brain-inspired way. You can see how the neuron's behavior can be influenced by inputs, and how spikes trigger a post synaptic response or [PSR](https://en.wikipedia.org/wiki/Postsynaptic_potential).  
        
        Spiking neurons fire only when their internal signal becomes large enough. In this simulation, you provide input to a neuron, which then spikes and affects another neuron. The influence of each spike fades over time, mimicking how real brain cells communicate.

        The network has three neurons: an input neuron you control, a spiking neuron, and a post-synaptic response neuron that reacts to the spike.
       
        # Simulation Details

        - Clamped Input: A neuron held at a constant input level, which you can manually adjust
        - Izhikevich Neuron: A spiking neuron model capable of emulating bursting, chattering, tonic spiking, and more
        - Post Synaptic Response Neuron: A linear neuron that shows the time-decaying post-synaptic response (PSR)
        
        - Spikes Plot: A time series that shows the voltage trace of the spiking neuron. Spikes are visible in this plot
        - Spike Responses Plot: A time series showing the post-synaptic current produced by spikes. Increase the input so that the spiking neuron spikes, and observe the jump and decay response neuron over time
        
        This setup uses the [Izhikevich neuron model](https://docs.simbrain.net/docs/network/neurons/izhikevich.html), which is able to capture a wide variety of biologically realistic firing patterns. The Izhikevich model combines nonlinear membrane potential dynamics with a recovery variable to emulate diverse firing behaviors using just four parameters (A, B, C, D). It integrates user-driven input and exhibits dynamic spiking governed by a 2-variable system. 
                 
        When a spike occurs, a [jump-and-decay](https://docs.simbrain.net/docs/network/spikeresponders/jumpdecay.html) spike responder simulates how spikes affect downstream neurons, creating smooth post-synaptic potentials. Spike transmission is modeled by a spike responder, which applies a transient signal to the post-synaptic neuron.
        
        # What to Do

        1. Click `Run` to begin the simulation
        
        2. Click the Input neuron, then press the up/down arrow keys to raise or lower its activation
        
        3. Observe:
        - The spike activity in the "Spikes" plot
        - The smooth post-synaptic response in the "Spike Responses" plot

        Some things you can try:

        - Change the Izhikevich parameters: Double-click the `Spiking` neuron and modify A, B, C, D. Use the [Izhikevich documentation](https://docs.simbrain.net/docs/network/neurons/izhikevich.html) to explore different firing behaviors. The bottom of that page has a list of parameter settings you can try, for example, "Tonic Bursting" or "Phasic Spiking". Observe the different behaviors in the Spikes plot
        - **Experiment with spike responders**: Double-click the connection from `Spiking` to `Post-Synaptic Response`, then change to a different [spike responder](https://docs.simbrain.net/docs/network/spikeresponders/).
        - **Swap neuron models**: Try using a different spiking neuron, like the [Integrate-and-Fire neuron](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html), and compare behaviors.
        - **Explore related simulations**: Try the “Spike Responders” simulation to see different post-synaptic effects compared in one view.

        # Links

        - [Izhikevich Neuron](https://docs.simbrain.net/docs/network/neurons/izhikevich.html)
        - [Jump-and-Decay Spike Responder](https://docs.simbrain.net/docs/network/spikeresponders/jumpdecay.html)
        - [Spike Responders Overview](https://docs.simbrain.net/docs/network/spikeresponders/)
        - [Integrate-and-Fire Neuron](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html)

        # References

        Izhikevich, E. M. (2003). [Simple model of spiking neurons](https://doi.org/10.1109/TNN.2003.820440). _IEEE Transactions on Neural Networks_, _14_(6), 1569–1572.

        # Credits

        Jeff Yoshimi 
        Kanly Thao
        Elijah Olson
        """.trimIndent()
    )

    withGui {
        place(networkComponent, 0, 0, 400,400)
        place(spikes, 410, 0, 400,400)
        place(spikeResponses, 0, 409, 400, 400)
    }

    network.events.zoomToFitPage.fire()
}