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

    [STDP](http://www.scholarpedia.org/article/Spike-timing_dependent_plasticity) is a form of synaptic plasticity
    in which the timing of spikes determines how synaptic weights are modified. If a presynaptic neuron ("pre") fires 
    shortly before a postsynaptic neuron ("post"), the synapse is typically strengthened (LTP). If the post fires before 
    the pre, the synapse is weakened (LTD). The closer in time the spikes occur, the stronger the effect.

    This mechanism reinforces causal relationships between neurons: if pre tends to cause post to fire, the connection 
    strengthens; if post tends to fire before pre, the connection weakens.

    # What to Do 

    In this simulation, the pre and post neurons are driven by the two columns in the table below. 
    Currently, the table is set up so that pre fires before post, which causes the connection to strengthen. 
    To observe weakening, adjust the table so post fires before pre.

    You can experiment with the time lag between pre and post and observe how this affects synaptic learning.

    **Note:** Because the data table loops (repeats), spikes from the end of the sequence can influence the beginning 
    of the next cycle. For instance, a post spike at the end and a pre spike at the beginning may still produce LTD. 
    This is why the time constants in the STDP rule are set very small: to restrict updates to spikes that occur close 
    together in time and avoid these wraparound effects.

    You can inspect and modify the STDP parameters by double-clicking the synapse between pre and post. This lets you 
    change the time constants, learning rates, and other properties to see how they affect plasticity behavior.
    """.trimIndent()
    )
}