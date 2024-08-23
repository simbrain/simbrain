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
             
            This is a[spiking neuron model](https://docs.simbrain.net/docs/network/spikingneurons.html) using the [Izhekevich neuron](https://docs.simbrain.net/docs/network/neurons/izhikevich.html) model neuron and a [bump and decay](https://docs.simbrain.net/docs/network/spikeresponders/jumpdecay.html) spike responder to show how biologically realistic models can be used in Simbrain.
            
            # What to do
            
            For a quick sense of how this simulations works, press `run` in the desktop toolbar, select the input neuron, and press the up and down arrows to adjust the input neuron's activation. Then observe how the spike rate of the spiking neuron changes as a result. You can also observe how the neuron downstrea from the spiking neuron reacts. Dealing with spike responses is complicated, and requires the use of [spike responders](https://docs.simbrain.net/docs/network/spikeresponders/).
            
            Some things you can do with this situation include changing the parameters of the neuron, changing the  type of spiking neuron, changing the parameters of the spike responder, and changing the type of spike responder
            
            # Izhekevich Neuron
            
            The [Izhekevich neuron](https://docs.simbrain.net/docs/network/neurons/izhikevich.html) neuron is notable for having four parameters, `A`, `B`, `C`, and `D` that can be used to produce different types of neural behavior. See the table at the bottom of the help page for Izhekevich neurons linked above.
             
            # Post-Synaptic Response 
             
            To change the post-synaptic response, double click on the weight from the `Spiking` to the  `Post-Synaptic Response` neuron. This can be tricky but you can lasso over the line and then use `Command/Ctrl-E` to open the editor dialog. Then you can either change to a different node or edit that one.
            
            To see a comparison between different types of post-synaptic responses, open the `Spike Responders` simulation, a simulation showing different post-synaptic responses in one time series.
             
            # Other Spiking Neuron Models
             
             To explore different spiking neuron models, double click on the spiking neuron and change its update rule to another rule. An example spiking neuron update rule would be the [Integrate And Fire Rule](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html). The Integrate and Fire model is a more easily interpretable spiking.  Other spiking neuron models are listed [here](https://docs.simbrain.net/docs/network/spikingneurons.html).
                        
        """.trimIndent()
    )

    withGui {
        place(networkComponent, 0, 0, 400,400)
        place(spikes, 410, 0, 400,400)
        place(spikeResponses, 410, 410, 400,400)
        place(docViewer, 0, 410, 400,400)
    }

    network.events.zoomToFitPage.fire()


}