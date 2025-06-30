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
        # Izhikevich Spiking Neuron Network

        # Introduction

        ## Basic
        This simulation shows how a spiking neuron responds to input using a realistic brain-inspired model. You'll see how a spike can influence downstream neurons through a decaying signal, helping you understand how timing and firing behavior work in spiking neural networks.

        ## Advanced
        This setup uses the Izhikevich neuron model, which efficiently captures a wide variety of biologically realistic firing patterns. A jump-and-decay spike responder simulates how spikes affect downstream neurons, creating smooth post-synaptic potentials.

        # Background

        ## Basic
        Spiking neurons fire only when their internal signal becomes large enough. In this simulation, you provide input to a neuron, which then spikes and affects another neuron. The influence of each spike fades over time, mimicking how real brain cells communicate.

        ## Advanced
        The Izhikevich model combines nonlinear membrane potential dynamics with a recovery variable to emulate diverse firing behaviors using just four parameters (A, B, C, D). Spike transmission is modeled by a spike responder (Jump-and-Decay), which applies a transient signal to the post-synaptic neuron.

        # Simulation Details

        ## Neuron Model

        - Basic: The central neuron spikes when it gets enough input. Its firing then causes a response in the next neuron, with the effect gradually fading.
        - Advanced: The spiking neuron uses an [IzhikevichRule](https://docs.simbrain.net/docs/network/neurons/izhikevich.html) update rule, initialized with zero background current. It integrates user-driven input and exhibits dynamic spiking governed by a 2-variable system. Spikes are processed downstream using a [Jump-and-Decay](https://docs.simbrain.net/docs/network/spikeresponders/jumpdecay.html) spike responder.

        ## Network Structure

        - Basic: The network has three neurons: an input neuron you control, a spiking neuron, and a post-synaptic response neuron that reacts to the spike.
        - Advanced: A feedforward network with one manually activated input neuron connected to a central Izhikevich neuron, which in turn connects to a downstream neuron via a fixed-weight synapse with a spike responder. No feedback or plasticity is included.

        ## Visualization

        - Basic: The “Spikes” plot shows when the spiking neuron fires. The “Spike Responses” plot shows how each spike affects the response neuron over time.
        - Advanced: Time series graphs show the voltage trace of the spiking neuron and the time-decaying post-synaptic response (PSR), enabling visualization of spike timing and effect propagation.

        ## Key Concepts

        - **Izhikevich Neuron**: A spiking neuron model capable of emulating bursting, chattering, tonic spiking, and more.
        - **Spike Responder**: A function that simulates how a spike affects a target neuron. Here, Jump-and-Decay creates a smooth, fading post-synaptic signal.
        - **Time Series Plot**: Graphs showing neuron activity over time.
        - **Clamped Input**: A neuron held at a constant input level, which you can manually adjust.

        # What to Do

        ## How to Use

        1. Click `Run` to begin the simulation.
        2. Click the **Input** neuron, then press the up/down arrow keys to raise or lower its activation.
        3. Observe:
        - The spike activity in the “Spikes” plot.
        - The smooth post-synaptic response in the “Spike Responses” plot.

        ## Try This

        - **Change the Izhikevich parameters**: Double-click the `Spiking` neuron and modify A, B, C, D. Use the [Izhikevich documentation](https://docs.simbrain.net/docs/network/neurons/izhikevich.html) to explore different firing behaviors.
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

        Jeff Yoshimi,  
        Kanly Thao,
        Eliah Olson
        """.trimIndent()
    )

    withGui {
        place(networkComponent, 0, 0, 400,400)
        place(spikes, 410, 0, 400,400)
        place(spikeResponses, 0, 409, 400, 400)
    }

    network.events.zoomToFitPage.fire()
}