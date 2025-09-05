package org.simbrain.custom_sims.simulations.psychology

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.updaterules.BinaryRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * McCulloch-Pitts Model of the Heat Illusion
 * 
 * A reproduction of McCulloch and Pitts' (1943) account of the heat illusion.
 * This simulation demonstrates how the removal of cold stimulation can paradoxically 
 * create a brief perception of heat.
 */
val heatColdSim = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Heat-Cold Network")
    val network = networkComponent.network

    // Create the neurons based on the XML structure
    val heatReceptor = network.addNeuron {
        label = "Heat Receptor"
        location = point(18.5, -73.3)
        updateRule = BinaryRule().apply {
            threshold = 0.5
            increment = 1.0
        }
        clamped = true
        activation = 0.0
    }

    val coldReceptor = network.addNeuron {
        label = "Cold Receptor"  
        location = point(17.3, 70.5)
        updateRule = BinaryRule().apply {
            threshold = 0.5
            increment = 1.0
        }
        clamped = true
        activation = 0.0
    }

    val heatSensation = network.addNeuron {
        label = "Heat Sensation"
        location = point(154.5, -74.0)
        updateRule = BinaryRule().apply {
            threshold = 1.0
            increment = 0.1
        }
    }

    val coldSensation = network.addNeuron {
        label = "Cold Sensation"
        location = point(149.1, 70.3)
        updateRule = BinaryRule().apply {
            threshold = 1.0
            increment = 0.1
        }
    }

    // Intermediate neurons A and B
    val neuronA = network.addNeuron {
        label = "A"
        location = point(82.9, -33.6)
        updateRule = BinaryRule().apply {
            threshold = 1.0
            increment = 0.1
        }
    }

    val neuronB = network.addNeuron {
        label = "B"
        location = point(83.1, 26.4)
        updateRule = BinaryRule().apply {
            threshold = 1.0
            increment = 0.1
        }
    }

    // Create synapses based on the XML structure
    // Heat Receptor -> Heat Sensation (strength 2.0)
    network.addSynapse(heatReceptor, heatSensation).apply {
        strength = 2.0
    }

    // Cold Receptor -> Cold Sensation (strength 2.0) 
    network.addSynapse(coldReceptor, coldSensation).apply {
        strength = 2.0
    }

    // Cold Receptor -> B (strength 2.0)
    network.addSynapse(coldReceptor, neuronB).apply {
        strength = 2.0
    }

    // B -> A (strength 2.0)
    network.addSynapse(neuronB, neuronA).apply {
        strength = 2.0
    }

    // B -> Cold Sensation (strength 1.0)
    network.addSynapse(neuronB, coldSensation).apply {
        strength = 1.0
    }

    // A -> Heat Sensation (strength 2.0)
    network.addSynapse(neuronA, heatSensation).apply {
        strength = 2.0
    }

    // Cold Receptor -> A (inhibitory, strength -1.0)
    network.addSynapse(coldReceptor, neuronA).apply {
        strength = -1.0
    }

    withGui {
        place(networkComponent, 0, 0, 500, 400)
    }

    addSidebarInfo(
        """
        # McCulloch-Pitts Model of the Heat Illusion

        A reproduction of McCulloch and Pitts' (1943) account of the heat illusion. This simulation demonstrates a fascinating perceptual phenomenon where the removal of a cold stimulus can create a brief sensation of heat.

        ## The Basic Illusion

        The illusion arises when the cold receptor is activated just once. As the inhibition to node A is removed, the activation to node B propagates forward and creates a brief perception of heat.

        To see this:
        1. Click the **Cold Receptor** neuron and press the up arrow to activate it
        2. Press **Space** to iterate the simulation once
        3. Click the Cold Receptor again and press the down arrow to deactivate it
        4. Keep iterating and observe activation propagate to heat sensation

        You can see this mechanism in action in [this animation](https://twitter.com/JeffYoshimi/status/1583922612924665856).

        ## Unintended Consequences

        This same mechanism will produce a perception of heat at *any* cold offset (like holding a piece of ice and letting go of it). When a cold stimulus is removed, inhibition to node A ceases instantly, so that the activation in node B can propagate.

        This was likely unintentional in the original model. It shows how even experts fluent in formal logic can miss consequences that become immediately evident in a visualization.

        ## Network Architecture

        The network consists of:
        - **Heat Receptor**: Input for heat stimuli (clamped)
        - **Cold Receptor**: Input for cold stimuli (clamped)  
        - **Heat Sensation**: Output representing heat perception
        - **Cold Sensation**: Output representing cold perception
        - **Neurons A & B**: Intermediate processing nodes that create the illusion

        The key mechanism is that the Cold Receptor inhibits neuron A while also exciting neuron B. When the cold stimulus is removed, this inhibition ceases, allowing B's activation to propagate through A to the Heat Sensation.

        ## What to Do

        1. **Observe normal heat sensation**: Activate the Heat Receptor and run the simulation
        2. **Observe normal cold sensation**: Activate the Cold Receptor and run the simulation  
        3. **Create the heat illusion**: 
           - Activate the Cold Receptor briefly
           - Deactivate it and continue running
           - Watch the Heat Sensation activate paradoxically
        4. **Experiment**: Try different patterns of activation and observe the network dynamics

        ## Historical Context

        This model comes from Warren McCulloch and Walter Pitts' pioneering 1943 paper "A Logical Calculus of the Ideas Immanent in Nervous Activity," which laid the foundation for both artificial neural networks and computational neuroscience.

        ## Source and Acknowledgement

        Thanks to Gualtiero Piccinini, whose [paper](https://link.springer.com/article/10.1023/B:SYNT.0000043018.52445.3e) on the topic led to this reproduction of the circuit.

        ## References

        McCulloch, W. S., & Pitts, W. (1943). A logical calculus of the ideas immanent in nervous activity. *Bulletin of Mathematical Biophysics*, *5*(4), 115-133.

        ## Credits

        Jeff Yoshimi
        """.trimIndent()
    )

    network.events.zoomToFitPage.fire()
}
