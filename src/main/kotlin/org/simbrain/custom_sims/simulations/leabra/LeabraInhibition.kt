/**
 * A layer of Leabra point neurons with feedforward-feedback (FFFB) inhibition, showing how inhibition keeps the
 * layer's activity sparse whatever the strength of its input. Settling is handled by [LeabraTrainer].
 */
package org.simbrain.custom_sims.simulations.leabra

import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.trainers.LeabraProjection
import org.simbrain.network.trainers.LeabraTrainer
import org.simbrain.network.trainers.leabraLayer
import org.simbrain.network.trainers.leabraProjection
import org.simbrain.network.updaterules.LeabraMatrixData
import org.simbrain.network.updaterules.LeabraRule
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.updateAction
import org.simbrain.workspace.Workspace

val leabraInhibition = newSim("leabra_inhibition") {
    workspace.clearWorkspace()
    exposeTypes(LeabraRule::class)

    val networkComponent = addNetworkComponent("Leabra network")
    val network = networkComponent.network

    val input = leabraLayer(LAYER_SIZE, INPUT).apply {
        gridMode = true
        gridColumns = 7
        isClamped = true
        location = point(0, 200)
    }
    val layer = leabraLayer(LAYER_SIZE, LAYER).apply {
        gridMode = true
        gridColumns = 7
        location = point(0, -200)
    }
    val projection = leabraProjection(input, layer)
    projection.weights.label = "Weights"
    network.addNetworkModels(input, layer, projection.weights, usePlacementManager = false)

    addTimeSeriesComponent(PLOT, listOf("Percent active", "Inhibition x 100"))

    setUpLeabraInhibition(workspace)

    addSidebarInfo(
        """
        # Layer inhibition

        In the brain, inhibitory interneurons keep any one area from becoming too active. In
        [Leabra](https://github.com/emer/leabra) this is modeled by computing inhibition for a whole layer at once.
        The result is that only a small fraction of units in a layer are active at a time, however strong the input.

        This simulation builds on `Point neuron`. Each unit is that neuron, and layer inhibition reaches it as its
        inhibitory conductance.

        # Simulation Details

        The `Input` layer projects to `Layer` through random weights. Each unit in `Layer` is a point neuron with
        the same inhibitory conductance, computed from two sources:

        - Feedforward inhibition grows with the average excitatory input to the layer. It anticipates how active
          the layer is about to become.
        - Feedback inhibition grows with the average activity of the layer itself, with a short delay.

        Their sum, times the overall inhibition gain, is the layer's inhibitory conductance. Only units whose
        excitatory input is strong enough to overcome it become active, so the units with the most input win.

        The inhibitory neurons themselves are not shown. Leabra does not simulate them as a separate layer;
        their effect is computed directly in each layer's update rule. The plot's `Inhibition x 100` line is the
        only place it appears.

        ## Control Panel Settings

        - `Input strength`: Scales the input pattern.
        - `Inhibition gain`: Overall strength of inhibition. 0 turns inhibition off.
        - `Feedforward`: Weight of feedforward inhibition.
        - `Feedback`: Weight of feedback inhibition.
        - `New input pattern`: Choose a new random input pattern and let the layer settle again from rest.

        The plot shows the percent of units in `Layer` that are clearly active (above 0.5), and the layer's
        inhibitory conductance times 100.

        # Activities

        ## Activity 1: Sparse activity at any input strength
        1. Press play.
            - About 15% of `Layer` becomes active.
        2. Move `Input strength` between 0.5 and 1.
            - Inhibition rises and falls with the input. The percent active grows only a little, from under 10% to
              about 15%.

        ## Activity 2: Inhibition gain
        1. Press play if the simulation is not running, and set `Input strength` to 1.
        2. Set `Inhibition gain` to 0.
            - Every unit in `Layer` becomes active, so the layer no longer picks out the units that best match the
              input.
        3. Raise `Inhibition gain` step by step.
            - Fewer and fewer units stay active: about 40% at 1.2, 15% at 1.8, and none at 3.
        4. Return `Inhibition gain` to 1.8.

        ## Activity 3: Feedforward and feedback
        1. Press play if the simulation is not running.
        2. Set `Feedback` to 0.
            - Nearly every unit becomes active. Feedforward inhibition alone is not enough.
        3. Return `Feedback` to 1 and set `Feedforward` to 0.
            - About half the units become active. Feedback inhibition alone is not enough either.
        4. Return `Feedforward` to 1.
            - Together they keep activity sparse: feedforward inhibition responds to the input, and feedback
              inhibition corrects for how active the layer actually becomes.

        ## Activity 4: New inputs
        1. Press play if the simulation is not running, and click `New input pattern` a few times.
            - Different units win each time, but about the same fraction of the layer is active.

        # References

        O'Reilly, R. C., Munakata, Y., Frank, M. J., Hazy, T. E., & Contributors. (2012).
        [_Computational cognitive neuroscience_](https://compcogneuro.org). Wiki Book.
        """.trimIndent(),
        width = 330
    )
}.registerReopenFunction { workspace -> setUpLeabraInhibition(workspace) }

private const val INPUT = "Input"
private const val LAYER = "Layer"
private const val PLOT = "Layer activity"
private const val LAYER_SIZE = 49
private const val INPUT_DENSITY = 0.3

/**
 * A random binary pattern with [INPUT_DENSITY] of its units on.
 */
private fun randomInputPattern(): DoubleArray {
    val on = (0 until LAYER_SIZE).shuffled().take((INPUT_DENSITY * LAYER_SIZE).toInt()).toSet()
    return DoubleArray(LAYER_SIZE) { if (it in on) 1.0 else 0.0 }
}

private suspend fun SimulationScope.setUpLeabraInhibition(workspace: Workspace) {
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().first()
    val network = networkComponent.network
    val arrays = network.getModels<NeuronArray>()
    val input = arrays.first { it.label == INPUT }
    val layer = arrays.first { it.label == LAYER }
    val rule = layer.updateRule as LeabraRule
    val projection = LeabraProjection(network.getModels<WeightMatrix>().first())
    val trainer = LeabraTrainer(network, listOf(input, layer), listOf(projection), input, layer).apply {
        expectedActivity[input] = INPUT_DENSITY
    }
    val plot = workspace.componentList.filterIsInstance<TimeSeriesPlotComponent>().first { it.name == PLOT }

    var strength = input.activationArray.max().takeIf { it > 0 } ?: 1.0
    var pattern = input.activationArray.map { if (it > 0) 1.0 else 0.0 }.toDoubleArray()
    if (pattern.sum() == 0.0) pattern = randomInputPattern()
    fun applyInput() = trainer.clamp(input, DoubleArray(LAYER_SIZE) { pattern[it] * strength })
    applyInput()

    network.updateManager.clear()
    network.updateManager.addAction(updateAction("Leabra cycle") {
        trainer.cycle()
        val percentActive = 100.0 * layer.activationArray.count { it > 0.5 } / layer.size
        plot.model.addData(0, network.time, percentActive)
        plot.model.addData(1, network.time, 100 * (layer.dataHolder as LeabraMatrixData).gi)
    })

    withGui {
        val controlPanel = createControlPanel("Inhibition", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addSlider("Input strength", 0.0, 1.0, strength, 0.05, toolTip = "Scales the input pattern.") {
                strength = it
                applyInput()
            }
            addSlider("Inhibition gain", 0.0, 3.0, rule.inhibitionGain, 0.1, toolTip = "Overall strength of inhibition.") {
                rule.inhibitionGain = it
            }
            addSlider("Feedforward", 0.0, 2.0, rule.feedforward, 0.1, toolTip = "Weight of feedforward inhibition.") {
                rule.feedforward = it
            }
            addSlider("Feedback", 0.0, 2.0, rule.feedback, 0.1, toolTip = "Weight of feedback inhibition.") {
                rule.feedback = it
            }
            addButton("New input pattern") {
                pattern = randomInputPattern()
                applyInput()
                trainer.resetActivity()
            }.apply { toolTipText = "Choose a new random input pattern and let the layer settle again from rest." }
        }.awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 360, 520)
        place(plot, controlPanel.rightEdgeWithGap() + 360 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 450, 320)
    }
}
