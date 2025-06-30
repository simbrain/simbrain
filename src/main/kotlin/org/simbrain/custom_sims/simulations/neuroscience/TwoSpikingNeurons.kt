package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Simulation with two neurons connecting to a single spiking neuron to explore basic
 * neural dynamics relative to excitatory and inhibitory currents.
 */
val spikingNeuronTwoInputs = newSim {

    workspace.clearWorkspace()

    val netComponent = addNetworkComponent("Simple Integrate and Fire")
    val net = netComponent.network

    // Create clamped input neurons
    val excitatoryInput = net.addNeuron {
        label = "Excitatory Input"
        location = point(50, 100)
        clamped = true
        increment = 1.0
    }

    val inhibitoryInput = net.addNeuron {
        label = "Inhibitory Input"
        location = point(50, 200)
        clamped = true
    }

    // Create the integrate-and-fire neuron
    val spikingNeuron = net.addNeuron {
        label = "Spiking neuron"
        location = point(200, 150)
        updateRule = IzhikevichRule().apply {
            threshold = 5.0
        }
    }

    // Connect input neurons to the spiking neuron
    net.addSynapse(excitatoryInput, spikingNeuron).strength = 5.0
    net.addSynapse(inhibitoryInput, spikingNeuron).strength = -5.0

    // Time series plot for spiking neuron
    val timeSeriesPlot = addTimeSeriesComponent("Spiking Activity", listOf("Spiking neuron"))

    // Coupling the spiking neuron's activation to the plot
    with(couplingManager) {
        spikingNeuron couple timeSeriesPlot.model.timeSeriesList[0]
    }

    // GUI layout
    withGui {
        place(netComponent, 0, 0, 400, 400)
        place(timeSeriesPlot, 420, 0, 400, 400)
    }

    addSidebarInfo(
        """
        # Two-Input Spiking Neuron

        # Introduction

        ## Basic
        This simulation shows how a simple spiking neuron responds to two types of input: one excitatory and one inhibitory. It’s a hands-on way to understand how neurons add up signals and decide when to fire (spike), and how opposing inputs can either trigger or prevent that firing.

        ## Advanced
        A single Izhikevich spiking neuron receives input from two clamped sources — one excitatory, one inhibitory. This setup demonstrates how membrane potential dynamics and spike threshold interact with balanced or unbalanced inputs to shape neural spiking patterns.

        # Background

        ## Basic
        Neurons fire (or “spike”) when their internal voltage gets high enough. Excitatory inputs push that voltage up, while inhibitory inputs push it down. In this simulation, you can see how increasing or decreasing each type of input changes the spiking pattern of the neuron.

        ## Advanced
        The spiking neuron uses the Izhikevich model, which captures rich firing behaviors with a simple set of equations. The excitatory input sends a strong positive current; the inhibitory input sends a strong negative current. Their net effect determines whether the neuron crosses the spike threshold and resets.

        # Simulation Details

        ## Neuron Model

        - Basic: The main neuron collects signals from both inputs. When its voltage goes over a threshold, it spikes and resets.
        - Advanced: The neuron uses the [Izhikevich model](https://docs.simbrain.net/docs/network/neurons/izhikevich), with a manually set threshold of `5.0`. It integrates incoming synaptic input and spikes based on internal voltage dynamics.

        ## Network Structure

        - Basic: Two clamped input neurons connect to one spiking neuron.
        - Excitatory Input: Raises the voltage of the spiking neuron.
        - Inhibitory Input: Lowers it.
        - Advanced: The network includes:
        - One clamped excitatory neuron (`+5.0` synaptic strength).
        - One clamped inhibitory neuron (`-5.0` synaptic strength).
        - One Izhikevich neuron integrating both inputs.
        - No recurrent or lateral connections.

        ## Visualization

        - Basic: A time series plot shows when the spiking neuron fires over time. Peaks in the graph indicate spikes.
        - Advanced: The time series plot reflects the neuron's voltage (or spiking state), illustrating how dynamic input patterns shape firing behavior.

        Learn more: [Time Series Plot Docs](https://docs.simbrain.net/docs/plots/timeseries.html)

        ## Key Concepts

        - **Izhikevich Neuron**: A biologically inspired spiking model that simulates a variety of neuron types.
        - **Excitatory Input**: Adds positive current, pushing the neuron toward spiking.
        - **Inhibitory Input**: Adds negative current, making spiking less likely.
        - **Spike Threshold**: The membrane potential value at which a spike occurs.

        # What to Do

        ## How to Use

        1. Press `Run` to start the simulation.
        2. Select `Excitatory Input` or `Inhibitory Input`.
        3. Use the arrow keys (`↑`/`↓`) to adjust each neuron's activation.
        4. Observe how the spiking neuron's activity changes in the plot.

        ## Try This

        - Increase only the excitatory input: The neuron should spike regularly.
        - Increase only the inhibitory input: The neuron may stop spiking altogether.
        - Adjust both inputs to balance spiking vs. suppression.
        - Double-click the spiking neuron to:
        - Explore Izhikevich parameters.
        - Change its type to something simpler, like [Integrate and Fire](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html).

        # Links

        - [Izhikevich Neuron Docs](https://docs.simbrain.net/docs/network/neurons/izhikevich) – Explore the neuron model.
        - [Integrate-and-Fire Model](https://docs.simbrain.net/docs/network/neurons/integrateAndFire.html) – A simpler alternative.
        - [Time Series Plot](https://docs.simbrain.net/docs/plots/timeseries.html) – Info on visualizing activity.

        # Credits

        Jeff Yoshimi,  
        Kanly Thao,
        Elijah Olson
        """.trimIndent()
    )
}