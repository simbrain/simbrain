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

    addSidebarInfo(
        """ 
        # Introduction
         
        This is a simple [spiking neuron model](https://docs.simbrain.net/docs/network/spikingneurons.html) that uses a [spike responder](https://docs.simbrain.net/docs/network/spikeresponders/)
        to show how biologically realistic models can be used in Simbrain.
        
        # Simulation Details
        
        This simulation uses an [Izhikevich neuron](https://docs.simbrain.net/docs/network/neurons/izhikevich) and a [bump and decay](https://docs.simbrain.net/docs/network/spikeresponders/jumpdecay) 
        spike responder to create a spiking neuron model. The Izhikevich neuron model is notable for having four parameters, `A`, `B`, `C`, and `D` that can be used to produce different 
        types of neural behavior. There are many neural behaviors that you can explore, some of which are specified in the link embedded in _Izhikevich neuron_. 
        
        Although this simulation uses the Izhikevich neuron and the bump and decay spike responder, you can change the spiking neuron and the spike responder to other ones. More details
        are explained in the `What to Do` section.
        
        # What to Do
        
        For a quick sense of how this simulation works:
        
        1) Press `run`.
        2) Left-click the `Input` neuron, then press the up and down arrows to adjust its activation.
        3) Then observe how the spike rate of the `Spiking` neuron changes as a result. You can also observe how the neuron downstream from the `Spiking` neuron reacts.
        
        Some additional things you can do with this simulation include changing the parameters of the neuron, changing the  type of spiking neuron, changing the parameters of the
        spike responder, and changing the type of spike responder.
        
        ## Exploring the Izhikevich Neuron
        
        1) Double left-click on the `Spiking` neuron.
        2) There, you see all the parameters of the Izhekevic neuron. You can start experimenting with the Izhevich neuron by changing the parameters using the table in the
        link embedded in _Izhikevich neuron_ to see different neural behaviors.
         
        ## Changing the Post-Synaptic Response 
         
        1) To change the post-synaptic response, double left-click on the weight from the `Spiking` neuron to the `Post-Synaptic Response` neuron. 
        2) Then, you can change to a different spike responder.
        3) Now, observe changes in the post-synaptic response.
        
        To see a comparison between different types of post-synaptic responses, open the `Spike Responders` simulation, a simulation showing different post-synaptic responses in 
        one time series.
         
        ## Other Spiking Neuron Models
         
        To explore different spiking neuron models, double left-click on the `Spiking` neuron and change its update rule to another rule. An example spiking neuron update rule would be
        the [Integrate And Fire Rule](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html). The Integrate and Fire model is a more easily interpretable spiking
        neuron. More information can be found in the `Integrate and Fire Network` simulation.
        
        # Credits
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        Kanly Thao
                    
        """.trimIndent()
    )

    withGui {
        place(networkComponent, 0, 0, 400,400)
        place(spikes, 410, 0, 400,400)
        place(spikeResponses, 0, 409, 400, 400)
    }

    network.events.zoomToFitPage.fire()


}