package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Create a spiking neuron, with an input, and graph its activity with a time series.
 */
val spikingNeuron = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val input = network.addNeuron {
        label = "Input"
        location = point(100, 100)
        clamped = true
    }
    val spiking = network.addNeuron {
        updateRule = IzhikevichRule()
        label = "Spiking"
        location = point(200, 100)
    }
    val postSpiking = network.addNeuron {
        label = "Post-Synaptic Response"
        location = point(300, 100)
    }

    network.addSynapse(input, spiking)
    network.addSynapse(spiking, postSpiking).apply {
        spikeResponder = JumpAndDecay()
    }

    withGui {
        place(networkComponent, 0, 0, 400,400)
    }

    val spikes = addTimeSeriesComponent("Spikes", seriesNames = listOf("Spiking"))
    val spikeResponses = addTimeSeriesComponent("Spike Responses", seriesNames = listOf("PSR"))

    with(couplingManager) {
        spiking couple spikes.model.timeSeriesList[0]
        postSpiking couple spikeResponses.model.timeSeriesList[0]
    }

    val docViewer = addDocViewer(
        "Spiking Neuron",
        """ 
            # Introduction
            Increase the activation on the input neuron to increase the spike rate of the spiking neuron, and observe
            changes in the post-synaptic response.
            
            To change the parameters of the neuron double click on it. The online docs for Izhekevich describe many parmaeter
            settings that produce different and interesting dynamics.
            
            To change the post-synaptic response double click on the weight from the spiking neuron to the post-synaptic
             response neuron and edit or change the spike responder.
            
        """.trimIndent()
    )

    withGui {
        place(networkComponent, 0, 0, 400,400)
        place(spikes, 410, 0, 400,400)
        place(spikeResponses, 410, 410, 400,400)
        place(docViewer, 0, 410, 400,400)
    }

}