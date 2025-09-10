package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.spikeresponders.RiseAndDecay
import org.simbrain.network.spikeresponders.ShortTermPlasticity
import org.simbrain.network.spikeresponders.StepResponder
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Create a spiking neuron, with an input, and graph its activity and spike responders with a time series.
 */
val spikeResponderSim = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val input = network.addNeuron {
        label = "Input"
        location = point(100, 80)
        clamped = true
        increment = 1.0
        activation = 10.0
    }
    val spiking = network.addNeuron {
        updateRule = IzhikevichRule().apply {
            setiBg(0.0)
        }
        label = "Izhikevich"
        location = point(200, 80)
    }
    network.addSynapse(input, spiking)

    val stepResponder = network.addNeuron {
        label = "Step Responder"
        location = point(290, 10)
    }
    network.addSynapse(spiking, stepResponder).apply {
        spikeResponder = StepResponder()
    }

    val jumpAndDecay = network.addNeuron {
        label = "Jump and Decay"
        location = point(290, 60)
    }
    network.addSynapse(spiking, jumpAndDecay).apply {
        spikeResponder = JumpAndDecay()
    }

    val riseAndDecay = network.addNeuron {
        label = "Rise And Decay"
        location = point(290, 110)
    }
    network.addSynapse(spiking, riseAndDecay).apply {
        spikeResponder = RiseAndDecay()
    }

    val stp = network.addNeuron {
        label = "Short Term Plasticity"
        location = point(290, 160)
    }
    network.addSynapse(spiking, stp).apply {
        spikeResponder = ShortTermPlasticity()
    }

    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    val (spikePlot, izhikevichSeries) = addTimeSeries("Spikes", seriesNames = listOf("Izhikevich"))
    val (spikeResponderPlot, stepSeries, jumpSeries, riseSeries, stpSeries) = addTimeSeries("Spike Responders", seriesNames = listOf("Step", "Jump and Decay", "Rise and Decay", "Short Term Plasticity"))

    withGui {
        placeComponent(spikePlot, 410, 0, 400, 400)
        placeComponent(spikeResponderPlot, 410, 410, 400, 400)
    }

    with(couplingManager) {
        spiking couple izhikevichSeries
        stepResponder couple stepSeries
        jumpAndDecay couple jumpSeries
        riseAndDecay couple riseSeries
        stp couple stpSeries
    }

    addSidebarInfo(
        """
        # Introduction

        This simulation shows how different types of spike responders affect downstream neurons. A [spiking neuron](https://docs.simbrain.net/docs/network/spikingneurons.html) sends signals to four targets, each with a different spike responder, so you can visually compare how spikes are transmitted through different synapse models.

        The setup includes an [Izhikevich neuron](https://docs.simbrain.net/docs/network/neurons/izhikevich.html) triggered by a clamped input, connected to four downstream neurons. These outputs respond differently depending on the spike responder type. This lets you explore different biological models of [synaptic transmission](https://en.wikipedia.org/wiki/Synaptic_transmission), including step responses, exponential decay, and plasticity effects.

        # Simulation Details

        - Clamped Input: A user-controlled input neuron, which you adjust with arrow keys
        - Izhikevich Neuron: A biologically inspired spiking model that drives the system
        - [Spike Responders](https://docs.simbrain.net/docs/network/spikeresponders/): Each connection uses a different spike responder to shape the signal:
        - [Step](https://docs.simbrain.net/docs/network/spikeresponders/step.html): Fixed response for a set duration
        - [Jump and Decay](https://docs.simbrain.net/docs/network/spikeresponders/jumpdecay.html): Instant rise with exponential decay
        - [Rise and Decay](https://docs.simbrain.net/docs/network/spikeresponders/riseAndDecay.html): Gradual rise and fall, mimicking slower chemical transmission
        - [Short-Term Plasticity](https://docs.simbrain.net/docs/network/spikeresponders/shortTermPlasticity.html): History-dependent responses that adapt over time
        - Spike Responders Plot: A time series that visualizes each downstream neuron's post-synaptic response to spikes

        Spike responders convert spikes into post-synaptic signals, modeling how neurons influence one another across synapses. Simbrain includes several types, each representing a different kind of [synaptic dynamic](https://www.sciencedirect.com/topics/computer-science/synaptic-dynamic).

        In this simulation, a clamped input neuron triggers an Izhikevich neuron, which exhibits dynamic spiking behavior. Each spike travels to four downstream neurons through distinct spike responders, allowing you to see side-by-side how each model transforms the spike signal.

        These responders help simulate key phenomena:
        - Step: Simple, constant post-spike effect
        - Jump-and-Decay: Immediate response that fades over time
        - Rise-and-Decay: Slower, more gradual influence
        - Short-Term Plasticity: Adapts based on recent spike history, mimicking facilitation or depression seen in real neurons

        Learn more in the [Spike Responders Overview](https://docs.simbrain.net/docs/network/spikeresponders/).

        # What to Do

        1. Click `Run` to start the simulation
        
        2. Select the Input neuron and use the up/down arrow keys to change its activation
        
        3. Watch the Izhikevich neuron fire in response
        
        4. Observe each downstream neuron's unique post-synaptic response in the Spike Responders plot

        # Try This

        - Adjust input strength to change spiking frequency.
        - Double-click the **Izhikevich neuron** to modify its spiking parameters (e.g., tonic spiking, bursting).
        - Click on connections to switch spike responders and experiment with different signal types.
        - Add new neurons or try different neuron models like [Integrate-and-Fire](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html) to see how different spiking behaviors affect responders.

        # Credits

        Jeff Yoshimi  
        Kanly Thao  
        Elijah Olson
        """.trimIndent()
    )
}