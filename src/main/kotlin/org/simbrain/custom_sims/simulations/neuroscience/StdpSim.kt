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
    val syn = network.addSynapse(pre, post).apply {
        strength = 0.0
        learningRule = STDPRule().apply {
            tauMinus = 60.0
            tauPlus = 30.0
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
        whereby when one neuron ("pre") fires before another to which it's connected ("post"), the synapse is strengthened,
        whereas when post fires before pre, the synapse is weakened.

        In this simulation, the pre and post neurons are coupled to the first and second columns of the table below.
        Currently the table is set so the connection will strengthen (pre fires before post). To see weakening,
        adjust the table so post fires before pre. You can also adjust the time-lag between the signals
        and observe its effect on learning. To edit the STDP synapse, double-click it.
        """.trimIndent()
    )
}