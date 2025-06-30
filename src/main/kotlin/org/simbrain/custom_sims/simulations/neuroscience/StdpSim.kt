package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.learningrules.STDPRule
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SpikingThresholdRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Create a simulation to illustrate spike timing dependent plasticity.
 */
val stdpSim = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("STDP Network")
    val network = networkComponent.network

    // Data
    val numRows = 20
    val stimAmount = 1.0
    val dataWorldComponent = addDataWorldComponent("Pre/Post Activity Table", numRows, 2)
    dataWorldComponent.dataWorld.dataModel.setValueAt(stimAmount, 5, 0)
    dataWorldComponent.dataWorld.dataModel.setValueAt(stimAmount, 7, 1)


    // Pre and Post neurons (Integrate and Fire)
    val pre = network.addNeuron {
        label = "Pre"
        updateRule = SpikingThresholdRule()
        location = point(137, 94)
    }

    val post = network.addNeuron {
        label = "Post"
        updateRule = SpikingThresholdRule()
        location = point(208, 94)
    }

    val input1 = network.addNeuron {
        updateRule = LinearRule()
        location = point(137, 170)
    }

    val input2 = network.addNeuron {
        updateRule = LinearRule()
        location = point(208, 170)
    }

    val nc = NeuronCollection(listOf(input1, input2))
    network.addNetworkModel(nc)

    network.addSynapse(input1, pre)
    network.addSynapse(input2, post)

    // STDP synapse from pre to post
    // Use a small time window (tauMinu/tauPlus) to avoid interference from spikes at the end of the data looping around to the beginning.
    val syn = network.addSynapse(pre, post).apply {
        strength = 0.0
        learningRule = STDPRule().apply {
            tauMinus = .1 // Make a small time window so we don't get "roll-around" effects
            tauPlus = .1
            wPlus = 10.0
            wMinus = 10.0
            learningRate = 0.01
            isHebbian = true
        }
        spikeResponder = JumpAndDecay().apply {
            timeConstant = .1
        }
    }

    // Add time series
    val (plot, series) = addTimeSeries("STDP Plot", seriesNames = listOf("Synapse"))

    withGui {
        place(networkComponent, 10, 10, 400, 300)
        placeComponent(plot, 420, 10, 400, 300)
        placeComponent(dataWorldComponent, 10, 320, 400, 300)
    }

    // Coupling
    with(couplingManager) {
        dataWorldComponent.dataWorld couple nc
        //data.dataWorld couple input1
        //data.column(1) couple input2
        syn couple series
    }

    addSidebarInfo(
        """
        # Spike Timing Dependent Plasticity (STDP)

        # Introduction

        ## Basic
        This simulation demonstrates how the timing between neuron spikes affects synaptic strength. If a presynaptic neuron fires just before a postsynaptic neuron, the connection strengthens. If the order is reversed, the connection weakens.

        ## Advanced
        This is a minimal example of **STDP**, a biologically plausible learning rule where synaptic weights are updated based on the temporal relationship between spikes. Two spiking neurons ("pre" and "post") are stimulated by inputs from a data table. A synapse connecting them uses an STDP learning rule, and changes in its strength are plotted over time.

        # Background

        ## Basic
        STDP mimics a key learning mechanism in the brain: **"fire together, wire together"** — but with timing. The direction and timing of spikes determine whether a connection is strengthened or weakened.

        ## Advanced
        STDP modifies synaptic weights according to the relative timing of pre- and postsynaptic spikes. If the **pre** neuron fires just before the **post**, long-term potentiation (LTP) occurs. If the **post** fires first, long-term depression (LTD) happens. Simbrain’s STDP implementation includes tunable parameters: time constants (`tauPlus`, `tauMinus`), learning rates, and weights (`wPlus`, `wMinus`) for potentiation and depression.

        Learn more: [STDP Overview](http://www.scholarpedia.org/article/Spike-timing_dependent_plasticity)

        # Simulation Details

        ## Neuron Model

        - Basic: Two **SpikingThreshold** neurons (pre and post) receive input from two linear input neurons.
        - Advanced: Inputs are defined via a looping data table that injects current at specific time steps to cause spikes.

        ## Network Structure

        - Two main neurons: **Pre** and **Post**, each connected to an input.
        - One plastic synapse from Pre to Post governed by the **STDP learning rule**.
        - The synapse includes a **Jump and Decay** spike responder to model fast, decaying influence on the postsynaptic neuron.

        ## Visualization

        - Basic: A **time series plot** shows the strength of the Pre→Post synapse over time.
        - Advanced: A data table defines when each neuron receives stimulation, allowing precise control over spike timing.

        # What to Do

        ## How to Use

        1. Click **Run** to start the simulation.
        2. Inspect the **data table** to see when Pre and Post neurons are activated.
        3. Observe the synaptic strength between Pre and Post in the plot.
        4. Try reversing the spike order to see the synapse weaken.

        ## Try This

        - Change the stimulation time so the **Post** neuron fires *before* the **Pre**.
        - Edit the STDP synapse by double-clicking it to modify learning rates and time constants.
        - Add delays or multiple spikes in the data table to explore how complex timing affects plasticity.
        - Observe wraparound effects and reduce time constants if needed.

        # STDP Rule Parameters

        - **tauPlus**: Decay time constant for LTP (Pre before Post)
        - **tauMinus**: Decay time constant for LTD (Post before Pre)
        - **wPlus**: Maximum weight increment for potentiation
        - **wMinus**: Maximum weight decrement for depression
        - **Learning Rate**: Controls the speed of weight updates

        # Links

        - [STDP in Simbrain](https://docs.simbrain.net/docs/network/synapses/stdp.html)
        - [Spiking Neurons](https://docs.simbrain.net/docs/network/spikingneurons.html)
        - [Jump and Decay Responder](https://docs.simbrain.net/docs/network/spikeresponders/jumpdecay.html)
        - [STDP (Scholarpedia)](http://www.scholarpedia.org/article/Spike-timing_dependent_plasticity)

        # Credits

        Jeff Yoshimi,  
        Kanly Thao,  
        Elijah Olson
        """.trimIndent()
    )
}