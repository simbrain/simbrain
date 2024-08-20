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

}