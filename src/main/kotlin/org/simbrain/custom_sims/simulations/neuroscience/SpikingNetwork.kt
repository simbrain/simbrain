package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTimeSeries
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.updaterules.SpikingThresholdRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Create a spiking neuron, with an input, and graph its activity with a time series.
 */
val spikingNetwork = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val input = network.addNeuron {
        label = "Input"
        location = point(100, 100)
        clamped = true
    }
    val spiking = network.addNeuron {
        updateRule = SpikingThresholdRule()
        label = "Spiking"
        location = point(200, 100)
    }
    val postSpiking = network.addNeuron {
        label = "Post-Synaptic Response"
        location = point(200, 200)
    }

    network.addSynapse(input, spiking)
    network.addSynapse(spiking, postSpiking).apply {
        spikeResponder = JumpAndDecay()
    }

    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    val timeSeriesComponent = addTimeSeries("Spikes")

    withGui {
        place(timeSeriesComponent) {
            location = point(410,0)
            width = 400
            height = 400
        }
    }

    with(couplingManager) {
        spiking couple timeSeriesComponent.model.timeSeriesList[0]
        postSpiking couple timeSeriesComponent.model.timeSeriesList[1]
    }

}