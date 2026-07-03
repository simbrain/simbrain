package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapseAsync
import org.simbrain.network.learningrules.STDPRule
import org.simbrain.network.spikeresponders.JumpAndDecay
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
    network.addNetworkModelAsync(nc)

    network.addSynapseAsync(input1, pre)
    network.addSynapseAsync(input2, post)

    // STDP synapse from pre to post
    // Use a small time window (tauMinu/tauPlus) to avoid interference from spikes at the end of the data looping around to the beginning.
    val syn = network.addSynapseAsync(pre, post).apply {
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
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 400, 300)
        placeComponent(plot, 400 + 2 * SIM_WINDOW_GAP, SIM_WINDOW_GAP, 400, 300)
        placeComponent(dataWorldComponent, SIM_WINDOW_GAP, 300 + 2 * SIM_WINDOW_GAP, 400, 300)
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

        This simulation shows how the precise timing of spikes between neurons affects synaptic strength. If a [**presynaptic neuron** fires just before a **postsynaptic neuron**](https://www.geeksforgeeks.org/biology/difference-between-presynaptic-neuron-and-postsynaptic-neuron/),
        the connection strengthens—a process known as long-term potentiation (LTP). If the order is reversed, the connection weakens—called long-term depression (LTD). This reflects
        the principle of *fire together, wire together*, modified by timing.

        Two spiking neurons (`Pre` and `Post`) are activated using inputs from a looping data table. Their connection uses the [STDP learning rule](https://docs.simbrain.net/docs/network/synapses/stdp.html)
        and changes to the synaptic weight are displayed in a live time series plot.

        ## Background

        Spike Timing Dependent Plasticity (STDP) is a biologically inspired learning mechanism that adjusts synaptic strength according to the timing difference between presynaptic
        and postsynaptic spikes. If the presynaptic neuron spikes slightly before the postsynaptic neuron, the synapse strengthens (LTP). If the postsynaptic neuron spikes first,
        the synapse weakens (LTD).

        The rule is parameterized by:

        - `tauPlus`: Time constant for potentiation (Pre before Post)
        - `tauMinus`: Time constant for depression (Post before Pre)
        - `wPlus`: Maximum weight increase
        - `wMinus`: Maximum weight decrease
        - `learningRate`: Controls how quickly weights update

        To learn more about the theory behind this rule, see the [Scholarpedia article](http://www.scholarpedia.org/article/Spike-timing_dependent_plasticity) on STDP.

        # Simulation Details

        - `SpikingThreshold Neurons`: The Pre and Post neurons are [SpikingThreshold neurons](https://docs.simbrain.net/docs/network/spikingneurons.html), which spike when they
        receive enough input.
        - `Data Table Input`: Two linear input neurons inject current at specific times, defined in a looping data table.
        - `STDP Synapse`: A single synapse connects the Pre to the Post neuron using the [STDP rule](https://docs.simbrain.net/docs/network/synapses/stdp.html), which updates weight
         based on spike timing.
        - `Jump-and-Decay Responder`: The synapse includes a [Jump-and-Decay spike responder](https://docs.simbrain.net/docs/network/spikeresponders/jumpdecay.html) to model fast,
         decaying post-synaptic effects.
        - `Synapse Strength Plot`: A time series graph shows how the Pre→Post connection strength changes over time.

        # What to Do

        1. Click `Run` to begin the simulation.
        2. Inspect the data table to see when the Pre and Post neurons receive input.
        3. Observe how the Pre → Post synaptic weight changes in the time series plot.
        4. Try reversing the spike order to watch the synapse weaken.

        ## Exploring the model

        - Modify the data table to change the spike timing between Pre and Post.
        - Double-click the synapse to adjust parameters like `tauPlus`, `tauMinus`, `wPlus`, `wMinus`, and `learningRate`.
        - Add delays, multiple spikes, or repeat patterns in the input to simulate bursting behavior.
            1. Reduce the time constants to explore more sensitive or rapid learning responses.
            2. Observe how changes in spike order and timing influence the pattern of plasticity.

        # Credits

        Elijah Olson

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        Kanly Thao

        """.trimIndent()
    )
}
