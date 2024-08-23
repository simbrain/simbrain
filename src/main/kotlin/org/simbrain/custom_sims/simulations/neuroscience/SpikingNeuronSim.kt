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
        increment = 2.0
        clamped = true
    }
    val spiking = network.addNeuron {
        updateRule = IzhikevichRule().apply {
            backgroundCurrent = 0.0
        }
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
            
            This is a **[spiking neuron model](https://docs.simbrain.net/docs/network/spikingneurons.html)** using the Izhekevich neuron model to model the firing events of real neurons. In this spiking neuron simulation, you can explore different
             dynamical systems of neural firing through different parameter settings.
            
            # Spiking Neuron
            
            To explore the neuron model, double click on the **spiking neuron** and change its parameters. To get a feel of the parameters, the online doc for **[Izhekevich neuron](https://docs.simbrain.net/docs/network/neurons/izhikevich.html)** 
            describes many parameter settings that produce different, yet interesting dynamical systems that is expressed in the top-right time series. This online doc can be accessed through the link attached before 
            or, through the **help icon** after double clicking on the spiking neuron.
            
            # Post-Synaptic Response Neuron
            
            To change the post-synaptic response, double click on the weight from the spiking neuron to the post-synaptic response neuron. This post-synaptic response is expressed in the bottom-right time series. To see a comparison between 
            different types of post-synaptic responses, open the **Spike Responders** simulation, a simulation showing different post-synaptic responses in one time series.
            
            # Other Spiking Neuron Models
            
            To explore different spiking neuron models, double click on the spiking neuron and change its update rule to another rule. An example spiking neuron update rule would be the
            **[Integrate And Fire Rule](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html)**. The Integrate and Fire model is an easier, interpretable spiking neuron in comparison to the
            Izhekevich neuron model, an alternative model. To look at other spiking neuron models, the link attached in the _Introduction_ section mentions about other examples of spiking neuron models.
            
            # Other Things To Change and Observe
            
            An additional option is to increase or decrease the activation of the **input neuron** to influence the spike rate of the spiking neuron, and observe the resulting changes in the post-synaptic response in the time series.
            
        """.trimIndent()
    )

    withGui {
        place(networkComponent, 0, 0, 400,400)
        place(spikes, 410, 0, 400,400)
        place(spikeResponses, 410, 410, 400,400)
        place(docViewer, 0, 410, 400,400)
    }

}