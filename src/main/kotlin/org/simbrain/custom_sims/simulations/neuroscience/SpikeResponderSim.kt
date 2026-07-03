package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapseAsync
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
    network.addSynapseAsync(input, spiking)

    val stepResponder = network.addNeuron {
        label = "Step responder"
        location = point(290, 10)
    }
    network.addSynapseAsync(spiking, stepResponder).apply {
        spikeResponder = StepResponder()
    }

    val jumpAndDecay = network.addNeuron {
        label = "Jump and Decay"
        location = point(290, 60)
    }
    network.addSynapseAsync(spiking, jumpAndDecay).apply {
        spikeResponder = JumpAndDecay()
    }

    val riseAndDecay = network.addNeuron {
        label = "Rise and decay"
        location = point(290, 110)
    }
    network.addSynapseAsync(spiking, riseAndDecay).apply {
        spikeResponder = RiseAndDecay()
    }

    val stp = network.addNeuron {
        label = "Short term plasticity"
        location = point(290, 160)
    }
    network.addSynapseAsync(spiking, stp).apply {
        spikeResponder = ShortTermPlasticity()
    }

    withGui {
        place(networkComponent) {
            location = point(SIM_WINDOW_GAP, SIM_WINDOW_GAP)
            width = 400
            height = 400
        }
    }

    val (spikePlot, izhikevichSeries) = addTimeSeries("Spikes", seriesNames = listOf("Izhikevich"))
    val (spikeResponderPlot, stepSeries, jumpSeries, riseSeries, stpSeries) = addTimeSeries("Spike Responders", seriesNames = listOf("Step", "Jump and Decay", "Rise and Decay", "Short Term Plasticity"))

    withGui {
        placeComponent(spikePlot, SIM_WINDOW_GAP + 400 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 400, 400)
        placeComponent(spikeResponderPlot, SIM_WINDOW_GAP + 400 + SIM_WINDOW_GAP, SIM_WINDOW_GAP + 400 + SIM_WINDOW_GAP, 400, 400)
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
        # Spike Responders

        This simulation shows how different types of spike responders affect downstream neurons. A [spiking neuron](https://docs.simbrain.net/docs/network/spikingneurons.html) sends signals to four targets, each with a different spike responder, so you can visually compare how spikes are transmitted through different synapse models.

        The setup includes an [Izhikevich neuron](https://docs.simbrain.net/docs/network/neurons/izhikevich.html) triggered by a clamped input, connected to four downstream neurons. These outputs respond differently depending on the spike responder type. This lets you explore different biological models of [synaptic transmission](https://en.wikipedia.org/wiki/Synaptic_transmission), including step responses, exponential decay, and plasticity effects.

        # Simulation Details

        - `Clamped Input`: A user-controlled input neuron, which you adjust with arrow keys.
        - `Izhikevich Neuron`: A biologically inspired spiking model that drives the system.
        - Four [`Spike Responders`](https://docs.simbrain.net/docs/network/spikeresponders/): Each connection uses a different spike responder to shape the signal.
            1. [Step](https://docs.simbrain.net/docs/network/spikeresponders/step.html): Fixed response for a set duration.
            2. [Jump and Decay](https://docs.simbrain.net/docs/network/spikeresponders/jumpdecay.html): Instant rise with exponential decay.
            3. [Rise and Decay](https://docs.simbrain.net/docs/network/spikeresponders/riseAndDecay.html): Gradual rise and fall, mimicking slower chemical transmission.
            4. [Short-Term Plasticity](https://docs.simbrain.net/docs/network/spikeresponders/shortTermPlasticity.html): History-dependent responses that adapt over time.
        - `Spike Responders Plot`: A time series that visualizes each downstream neuron's post-synaptic response to spikes

        Spike responders convert spikes into post-synaptic signals, modeling how neurons influence one another across synapses. Simbrain includes several types, each representing 
        a different kind of [synaptic dynamic](https://www.sciencedirect.com/topics/computer-science/synaptic-dynamic).

        In this simulation, a clamped `Input neuron` triggers an `Izhikevich neuron`, which exhibits dynamic spiking behavior. Each spike travels to four downstream neurons
        through distinct spike responders, allowing you to see side-by-side how each model transforms the spike signal. The spike frequency can be adjusted by the input activation
        from the `Input neuron`.

        These responders help simulate key phenomena:
        
        1. Step: Simple, constant post-spike effect
        2. Jump-and-Decay: Immediate response that fades over time
        3. Rise-and-Decay: Slower, more gradual influence
        4. Short-Term Plasticity: Adapts based on recent spike history, mimicking facilitation or depression seen in real neurons

        Learn more in the [Spike Responders Overview](https://docs.simbrain.net/docs/network/spikeresponders/).

        # What to Do

        1. Click `Run` to start the simulation. 
        2. Select the `Input neuron` and use the up/down arrow keys to change its activation. 
        3. Watch the `Izhikevich neuron` fire in response.
        4. Observe each downstream neuron's unique post-synaptic response in the Spike Responders plot.
        
         ## Exploration of other spiking neuron models
        
        You can also change the spiking neuron model to a different model (i.e., Izhikevich, [Integrate-and-Fire](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html),
        etc) to see how different learning models interact with different spike responders.

        # Credits

        Elijah Olson  
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html) 
        
        Kanly Thao
        
        """.trimIndent()
    )
}
