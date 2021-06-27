package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTimeSeries
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Create a spiking neuron, with an input, and graph its activity with a time series.
 */
val spikingNetwork = newSim {

    val networkComponent = addNetworkComponent("Just a Neuron")

    val network = networkComponent.network

    val input = network.addNeuron {
        label = "Input"
        location = point(100, 100)
        isClamped = true
    }
    val spiking = network.addNeuron {
        updateRule = SpikingThresholdRule()
        label = "Spiking"
        location = point(200, 100)
    }

    val synapse = network.addSynapse(input, spiking)

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
    }

}