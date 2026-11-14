/**
 * A single Leabra point neuron driven by excitatory and inhibitory inputs, after the neuron simulation in chapter 2
 * of Computational Cognitive Neuroscience (https://github.com/CompCogNeuro/sims/tree/master/ch2/neuron).
 */
package org.simbrain.custom_sims.simulations.leabra

import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.updaterules.PointNeuronRule
import org.simbrain.network.updaterules.PointNeuronScalarData
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.updateAction
import org.simbrain.workspace.Workspace

val pointNeuronSim = newSim("leabra_point_neuron") {
    workspace.clearWorkspace()
    exposeTypes(PointNeuronRule::class)

    val networkComponent = addNetworkComponent("Point neuron")
    val network = networkComponent.network

    val excitatory = Neuron().apply {
        label = EXCITATORY
        clamped = true
        activation = 0.0
        location = point(0, 60)
    }
    val inhibitory = Neuron().apply {
        label = INHIBITORY
        clamped = true
        activation = 0.0
        location = point(100, 60)
    }
    val pointNeuron = Neuron(PointNeuronRule().apply {
        excitatoryMaxConductance = 0.3
        leakConductance = 0.3
    }).apply {
        label = POINT_NEURON
        location = point(50, -60)
    }
    network.addNetworkModels(
        excitatory, inhibitory, pointNeuron,
        Synapse(excitatory, pointNeuron).apply { strength = 1.0 },
        Synapse(inhibitory, pointNeuron).apply { strength = -1.0 },
        usePlacementManager = false
    )

    addTimeSeriesComponent(PLOT, listOf("Membrane potential", "Activation"))

    setUpPointNeuronSim(workspace)

    addSidebarInfo(
        """
        # Point neuron

        The basic unit of [Leabra](https://github.com/emer/leabra) is a point neuron: a neuron reduced to a single
        point whose membrane potential is pulled up and down by three kinds of ion channels.

        # Simulation Details

        Excitatory channels pull the membrane potential toward 1, inhibitory channels toward 0.25, and leak channels
        toward 0.3. Each channel's pull is its conductance, the fraction of channels open, times its distance from
        the membrane potential. The membrane potential settles where these pulls balance.

        Activation stands for firing rate. It is zero until excitation is strong enough to push the membrane
        potential past threshold (0.5), then rises steeply and saturates near 1.

        Here the excitatory and inhibitory input neurons set the excitatory and inhibitory conductances directly.

        ## Control Panel Settings

        - `Excitatory input`: Fraction of excitatory channels open.
        - `Inhibitory input`: Fraction of inhibitory channels open.
        - `Leak conductance`: Strength of the leak channels, which pull the neuron back to rest.

        The plot shows membrane potential and activation over time. Press play to run.

        # Activities

        ## Activity 1: Find the threshold
        1. Press play, then slowly raise `Excitatory input` from 0.
            - The membrane potential rises right away, but activation stays at 0 until input passes about 0.4.
            - Past that point activation jumps up and then saturates.

        ## Activity 2: Inhibition and leak
        1. Press play if the simulation is not running, and set `Excitatory input` to 0.6.
        2. Raise `Inhibitory input`.
            - The membrane potential falls and activation shuts off.
        3. Return inhibition to 0 and raise `Leak conductance`. It has a similar effect.

        ## Activity 3: Leak sets the resting state
        1. Press play if the simulation is not running, and set both inputs to 0.
            - The membrane potential settles at 0.3, the leak's equilibrium. This is the neuron's resting state.
        2. Set `Leak conductance` to 0.1, then raise `Excitatory input` slowly from 0.
            - Activation now starts at about 0.15 instead of 0.4. With less leak pulling it back to rest, the neuron
              responds to weaker input.

        # References

        O'Reilly, R. C., Munakata, Y., Frank, M. J., Hazy, T. E., & Contributors. (2012).
        [_Computational cognitive neuroscience_](https://compcogneuro.org). Wiki Book.
        """.trimIndent(),
        width = 330
    )
}.registerReopenFunction { workspace -> setUpPointNeuronSim(workspace) }

private const val EXCITATORY = "Excitatory"
private const val INHIBITORY = "Inhibitory"
private const val POINT_NEURON = "Point neuron"
private const val PLOT = "Point neuron activity"

private suspend fun SimulationScope.setUpPointNeuronSim(workspace: Workspace) {
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().first()
    val network = networkComponent.network
    fun neuron(name: String) = network.freeNeurons.first { it.label == name }
    val excitatory = neuron(EXCITATORY)
    val inhibitory = neuron(INHIBITORY)
    val pointNeuron = neuron(POINT_NEURON)
    val rule = pointNeuron.updateRule as PointNeuronRule
    val plot = workspace.componentList.filterIsInstance<TimeSeriesPlotComponent>().first { it.name == PLOT }

    network.updateManager.addAction(updateAction("Plot point neuron") {
        val membranePotential = (pointNeuron.dataHolder as PointNeuronScalarData).membranePotential
        plot.model.addData(0, network.time, membranePotential)
        plot.model.addData(1, network.time, pointNeuron.activation)
    })

    withGui {
        val controlPanel = createControlPanel("Inputs", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addSlider("Excitatory input", 0.0, 1.0, excitatory.activation, 0.05, toolTip = "Fraction of excitatory channels open.") {
                excitatory.activation = it
            }
            addSlider("Inhibitory input", 0.0, 1.0, inhibitory.activation, 0.05, toolTip = "Fraction of inhibitory channels open.") {
                inhibitory.activation = it
            }
            addSlider("Leak conductance", 0.0, 1.0, rule.leakConductance, 0.05, toolTip = "Strength of the leak channels.") {
                rule.leakConductance = it
            }
        }.awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 300, 320)
        place(plot, controlPanel.rightEdgeWithGap() + 300 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 450, 320)
    }
}
