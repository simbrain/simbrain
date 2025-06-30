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
        # Spike Responders

        # Introduction

        ## Basic
        This simulation shows how different **spike responders** work in Simbrain by applying them to a single spiking neuron. It lets you see how spikes from one neuron influence downstream neurons in various ways.

        ## Advanced
        A single **Izhikevich neuron** receives input from a clamped source and sends spikes to four target neurons, each connected with a different spike responder type. This highlights how different biological synaptic dynamics can be modeled and compared side-by-side.

        # Background

        ## Basic
        Spike responders translate presynaptic spikes into effects on postsynaptic neurons. They mimic various types of synaptic transmission seen in real brains.

        ## Advanced
        Simbrain includes multiple spike responders modeling distinct post-synaptic dynamics:

        - **Step Responder:** Holds a fixed input value for a duration after each spike.
        - **Jump and Decay:** Produces an immediate jump in input followed by exponential decay.
        - **Rise and Decay:** Smoothly rises then decays more slowly, resembling chemical synaptic transmission.
        - **Short-Term Plasticity:** Changes response based on spike history, modeling facilitation or depression.

        Learn more: [Spike Responders Overview](https://docs.simbrain.net/docs/network/spikeresponders/).

        # Simulation Details

        ## Neuron Model

        - Basic: A clamped input neuron (‘clamped’ meaning its activation level is fixed and controlled directly by the user) activates an Izhikevich neuron, which then spikes.
        - Advanced: The Izhikevich neuron model captures realistic spiking patterns; it connects to four downstream neurons with distinct spike responders.

        ## Network Structure

        - Four downstream neurons each receive input via a different spike responder type.
        - A time series tracks spikes and post-synaptic responses.

        ## Visualization

        - Basic: A simple time series shows when the Izhikevich neuron fires.
        - Advanced: Plots display both spiking activity and how each spike responder shapes downstream neuron signals over time.

        # What to Do

        ## How to Use

        1. Click **Run** to start the simulation.
        2. Select the **Input** neuron and adjust its activation using the up/down arrow keys.
        3. Observe the Izhikevich neuron spike in response.
        4. Watch the **Spike Responders** plot to compare how each responder reacts to spikes.

        ## Try This

        - Modify the input activation to change spike frequency.
        - Double-click the Izhikevich neuron to alter its spiking behavior (e.g., bursting).
        - Change synapse types by double-clicking connections and selecting different spike responders.
        - Add new output neurons or try different spiking neuron models.

        # Spike Responder Types

        - 🔵 **[Step](https://docs.simbrain.net/docs/network/spikeresponders/step.html)**  
        Fixed output for a set time after a spike; simple and direct.

        - 🔴 **[Jump and Decay](https://docs.simbrain.net/docs/network/spikeresponders/jumpdecay.html)**  
        Immediate jump in input followed by exponential decay; mimics fast synaptic response.

        - 🟡 **[Rise and Decay](https://docs.simbrain.net/docs/network/spikeresponders/riseAndDecay.html)**  
        Smooth rise then slower decay; models chemical synaptic transmission.

        - 🟢 **[Short-Term Plasticity](https://docs.simbrain.net/docs/network/spikeresponders/shorttermplasticity.html)**  
        Dynamic response adapting to spike history; simulates facilitation or depression.

        # Links

        - [Spike Responders Overview](https://docs.simbrain.net/docs/network/spikeresponders/)
        - [Izhikevich Neuron](https://docs.simbrain.net/docs/network/neurons/izhikevich.html)
        - [Spiking Neurons](https://docs.simbrain.net/docs/network/spikingneurons.html)
        - [Synaptic Transmission (Wikipedia)](https://en.wikipedia.org/wiki/Synaptic_transmission)

        # Credits

        Jeff Yoshimi,  
        Kanly Thao,
        Elijah Olson
        """.trimIndent()
    )
}