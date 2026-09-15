/** Explores connectivity-driven dynamics of two ordinary tanh neurons with selectable recurrence plots. */
package org.simbrain.custom_sims.simulations.dynamical_systems

import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.plot.timeseries.RecurrenceView
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.math.SigmoidFunctionEnum
import org.simbrain.util.place
import org.simbrain.util.showWarningDialog
import org.simbrain.workspace.Workspace

val twoNeuronDynamics = newSim("two-neuron-dynamics") {
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Two-Neuron Network")
    val neurons = networkComponent.network.addTwoNeuronDynamicsNetwork()
    val plotModel = TimeSeriesModel().apply {
        isAutoRange = false
        rangeLowerBound = -1.0
        rangeUpperBound = 1.0
        windowSize = 300
        fixedWidth = true
        recurrenceView = RecurrenceView.BOTH
        recurrenceThreshold = 0.05
        recurrenceEmbeddingDimension = 3
        recurrenceEmbeddingDelay = 1
    }
    val timeSeries = TimeSeriesPlotComponent("Two-Neuron Dynamics", plotModel).apply {
        addTimeSeries("Neuron 1")
        addTimeSeries("Neuron 2")
    }
    workspace.addWorkspaceComponent(timeSeries)
    with(couplingManager) {
        neurons.neuronList.forEachIndexed { index, neuron ->
            neuron couple timeSeries.model.timeSeriesList[index]
        }
    }
    setupTwoNeuronControls(workspace)
    addSidebarInfo(
        """
        # Two-Neuron Dynamics

        Two ordinary discrete sigmoidal neurons generate periodic and chaotic dynamics
        through four recurrent connections. Neither neuron is an activity generator.
        Both activations appear in the time series; select `Neuron 1` or `Neuron 2`
        in the recurrence tabs to examine either signal.

        # Simulation Details

        Both neurons use `Tanh`, bounds `-1` and `1`, slope `1`, zero bias, and no noise.
        The standard synchronous network update implements:

        - `x(t+1) = tanh(g (4.43 x(t) - 4.74 y(t)))`
        - `y(t+1) = tanh(g (1.97 x(t) - 1.10 y(t)))`

        The feedback gain `g` scales all four weights. Neuron 1 excites both neurons;
        neuron 2 inhibits both. Initial activations are `0.2` and `-0.1`.
        The initial gain is `1.0`.

        Presets were selected by numerical exploration of this map. At gain `1.0`,
        a largest Lyapunov exponent estimate is approximately `0.17` per update,
        supporting chaos rather than merely an irregular-looking trace. This is
        numerical evidence, not a mathematical proof. Periodic presets describe
        settled behavior from the supplied initial state, not every possible initial state.

        ## Control Panel Settings

        Presets restore the tanh rules, zero biases, and all four weights, then reset
        initial activations and clear both series. Use the workspace toolbar to run or pause.

        - `Fixed point (0.3)`: decay toward zero.
        - `Period 6 (0.8)`: settle into a six-step orbit.
        - `Chaotic (1.0)`: explore irregular deterministic dynamics.
        - `Period 5 (1.04)`: enter a periodic window beyond the chaotic preset.
        - `Period 10 (1.07)`: see the five-step orbit double its period.
        - `Period 20 (1.09)`: see another period doubling.
        - `Feedback gain`: enter a finite gain between `0` and `1.3`.
        - `Apply gain`: apply the entered gain to the base weight pattern, restore
          the neuron rules, and restart. This replaces manual weight edits.
        - `Reset initial conditions`: restart at `0.2`, `-0.1` and clear the plot,
          preserving current weights and rules.
        - `Perturb neuron 1`: add `0.000001` to its current activation, within bounds,
          without clearing the plot. This does not create a second comparison trajectory.
        - `Clear plot`: discard samples while leaving the network state unchanged.

        # What to Do

        1. Run `Period 6 (0.8)` for at least `1000` updates, then `Clear plot` to
           separate the settled orbit from its transient. Observe regularly spaced
           diagonals in both recurrence tabs.
        2. Try `Chaotic (1.0)`. Compare the irregular short diagonal segments with
           the periodic pattern. Similar states need not remain close indefinitely.
        3. Compare gains `1.04`, `1.07`, and `1.09` to see period doubling. The plot
           retains `300` samples, enough to display multiple repetitions.
        4. Explore nearby gains, or edit individual synapses. For a reproducible
           initial condition, pause before selecting a preset or resetting.
        5. Reset the chaotic preset and compare runs with and without an immediate
           perturbation. Early activity is similar, but later trajectories can diverge.

        Use the recurrence plot and time series to get a feel for how different dynamical regimes behave.
        
        The recurrence plot initially compares three-sample trajectories with delay `1`.
        Try embedding dimension `1` to compare individual values instead. Threshold `0.05`
        is relative to the largest distance in the current window. The main diagonal
        always appears and does not establish periodicity. Tiny numerical residuals
        near the zero fixed point can affect a relative threshold. Recurrence structure
        alone does not establish chaos; long transients can also appear irregular.
        """.trimIndent()
    )
    networkComponent.network.events.zoomToFitPage.fire()
}.registerReopenFunction { workspace -> setupTwoNeuronControls(workspace) }

internal suspend fun Network.addTwoNeuronDynamicsNetwork(): NeuronCollection {
    val neurons = addNeuronCollection(2).apply { label = "Recurrent pair" }
    neurons.neuronList.forEachIndexed { index, neuron ->
        neuron.label = "Neuron ${index + 1}"
        neuron.setLocation(index * 160.0, 0.0)
    }
    neurons.neuronList.forEach { source ->
        neurons.neuronList.forEach { target -> addSynapse(source, target) }
    }
    configureTwoNeuronDynamics(neurons, 1.0)
    return neurons
}

internal fun configureTwoNeuronDynamics(neurons: NeuronCollection, gain: Double) {
    val weights = listOf(listOf(4.43, -4.74), listOf(1.97, -1.10))
    neurons.neuronList.forEachIndexed { targetIndex, target ->
        target.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.TANH
            lowerBound = -1.0
            upperBound = 1.0
            slope = 1.0
            addNoise = false
        }
        target.bias = 0.0
        target.clamped = false
        target.activation = if (targetIndex == 0) 0.2 else -0.1
        neurons.neuronList.forEachIndexed { sourceIndex, source ->
            requireNotNull(getSynapse(source, target)).strength = gain * weights[targetIndex][sourceIndex]
        }
    }
}

private suspend fun SimulationScope.setupTwoNeuronControls(workspace: Workspace) {
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().first()
    val neurons = networkComponent.network.getModelByLabel<NeuronCollection>("Recurrent pair")
    val timeSeries = workspace.componentList.filterIsInstance<TimeSeriesPlotComponent>().first()
    val firstNeuron = neurons.neuronList[0]
    val secondNeuron = neurons.neuronList[1]
    withGui {
        val controlPanel = createControlPanel("Two-Neuron Presets", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            val gainField = addTextField(
                "Feedback gain", (requireNotNull(getSynapse(firstNeuron, firstNeuron)).strength / 4.43).toString(),
                toolTip = "Scale the base weight pattern by a gain from 0 to 1.3; click Apply gain."
            )
            fun applyGain(gain: Double) {
                configureTwoNeuronDynamics(neurons, gain)
                gainField.text = gain.toString()
                timeSeries.model.clearData()
            }
            addButton("Apply gain") {
                val gain = gainField.text.toDoubleOrNull()
                if (gain == null || !gain.isFinite() || gain !in 0.0..1.3) {
                    showWarningDialog("Enter a finite feedback gain between 0 and 1.3.")
                } else {
                    applyGain(gain)
                }
            }.toolTipText = "Restore the base tanh network with the entered gain and restart both series."
            addSeparator()
            listOf(
                "Fixed point (0.3)" to 0.3,
                "Period 6 (0.8)" to 0.8,
                "Chaotic (1.0)" to 1.0,
                "Period 5 (1.04)" to 1.04,
                "Period 10 (1.07)" to 1.07,
                "Period 20 (1.09)" to 1.09
            ).forEach { (label, gain) ->
                addButton(label) { applyGain(gain) }.toolTipText =
                    "Restore tanh neurons, set feedback gain to $gain, reset activations, and clear the plot."
            }
            addSeparator()
            addButton("Reset initial conditions") {
                firstNeuron.activation = 0.2
                secondNeuron.activation = -0.1
                timeSeries.model.clearData()
            }.toolTipText = "Reset to 0.2 and -0.1 without changing weights or rules; clear both series."
            addButton("Perturb neuron 1") {
                firstNeuron.activation = (firstNeuron.activation + 0.000001).coerceIn(-1.0, 1.0)
            }.toolTipText = "Add a tiny perturbation to neuron 1 without resetting or clearing data."
            addButton("Clear plot") { timeSeries.model.clearData() }.toolTipText =
                "Clear both recorded series without changing the network state."
        }.awaitLayout()
        val networkWidth = 360
        place(networkComponent, SIM_WINDOW_GAP, controlPanel.bottomEdgeWithGap(), networkWidth, 240)
        val plotX = maxOf(controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP + networkWidth + SIM_WINDOW_GAP)
        place(timeSeries, plotX, SIM_WINDOW_GAP, 800, 700)
    }
}
