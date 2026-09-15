/** Demonstrates logistic-map regimes with a single neuron, preset controls, and recurrence plots. */
package org.simbrain.custom_sims.simulations.dynamical_systems

import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.updaterules.activity_generators.LogisticRule
import org.simbrain.plot.timeseries.RecurrenceView
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.place
import org.simbrain.workspace.Workspace

val logisticMap = newSim("logistic-map") {
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Logistic Map")
    val neuron = networkComponent.network.addNeuron {
        label = "Logistic state"
        updateRule = LogisticRule().apply {
            lowerBound = 0.0
            upperBound = 1.0
            growthRate = 3.9
        }
        activation = 0.2
    }
    val timeSeries = addTimeSeriesComponent("Logistic Dynamics", "Activation")
    timeSeries.model.apply {
        isAutoRange = false
        rangeLowerBound = 0.0
        rangeUpperBound = 1.0
        windowSize = 250
        fixedWidth = true
        recurrenceView = RecurrenceView.BOTH
        recurrenceThreshold = 0.05
        recurrenceEmbeddingDimension = 3
        recurrenceEmbeddingDelay = 1
    }
    with(couplingManager) {
        neuron couple timeSeries.model.timeSeriesList[0]
    }
    setupLogisticControls(workspace)
    addSidebarInfo(
        """
        # Logistic Map

        A simple model using a single logistic neuron to demonstrate fixed points, periodicity, and chaos.

        # Simulation Details

        The `Logistic` activity generator iterates `x(t+1) = r x(t) (1 - x(t))`,
        with bounds `0` and `1` and initial activation `0.2`. No connections or noise
        are needed: the dynamics are in the update rule, not emergent network activity.
        Initially `r = 3.9` (chaotic).

        ## Control Panel Settings

        Each preset sets the growth rate, resets activation to `0.2`, and clears the plot.
        Use the workspace toolbar to run or pause.

        - `Fixed point (2.8)`: converge to a constant activation.
        - `Period 2 (3.2)`: alternate between two values after the transient.
        - `Period 4 (3.5)`: repeat a four-step cycle.
        - `Period 8 (3.55)`: repeat an eight-step cycle.
        - `Chaotic (3.9)`: bounded, irregular deterministic activity.
        - `Period 3 window (3.83)`: return to periodicity within the chaotic region.
        - `Reset initial condition`: restart at `0.2` and clear the plot, keeping the current rule settings.
        - `Clear plot`: discard recorded samples without changing the neuron or growth rate.

        # What to Do

        1. Select `Period 2 (3.2)` and press Play. Compare it with `Period 4 (3.5)`
           and `Period 8 (3.55)` to see period doubling.
        2. After about `200` updates, use `Clear plot` to examine settled dynamics
           without the initial transient. The plot retains the latest `250` samples.
        3. Try `Chaotic (3.9)`, then `Period 3 window (3.83)`. Chaos is not simply
           increasing disorder as the growth rate increases.
        4. Double-click the neuron to explore other growth rates between `0` and `4`.
           Avoid initial activations at the bounds, which lead to the zero fixed point.

        The recurrence plot can be used to visualize how different dynamical regimes look.
        
        Periodic dynamics produce regularly spaced long diagonals in the recurrence plot.
        Chaotic dynamics produce irregular structures and shorter diagonal segments:
        similar states can evolve similarly for a while before separating.
        The main diagonal is always present; it does not establish periodicity.
        A recurrence picture alone does not prove chaos.

        In the plot's `Recurrence` settings, the initial embedding dimension `3` and
        delay `1` compare three-sample trajectories. Try dimension `1` to compare
        individual activation values instead. The threshold `0.05` is a fraction of
        the largest pairwise distance in the current window, not an absolute distance.
        At a settled fixed point all states coincide and the recurrence plot fills in;
        tiny floating-point residuals can affect this relative threshold.
        """.trimIndent()
    )
    networkComponent.network.events.zoomToFitPage.fire()
}.registerReopenFunction { workspace -> setupLogisticControls(workspace) }

private suspend fun SimulationScope.setupLogisticControls(workspace: Workspace) {
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().first()
    val neuron = networkComponent.network.getModelByLabel<Neuron>("Logistic state")
    val timeSeries = workspace.componentList.filterIsInstance<TimeSeriesPlotComponent>().first()

    fun reset() {
        neuron.activation = 0.2
        timeSeries.model.clearData()
    }

    withGui {
        val controlPanel = createControlPanel("Logistic Presets", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            listOf(
                "Fixed point (2.8)" to 2.8,
                "Period 2 (3.2)" to 3.2,
                "Period 4 (3.5)" to 3.5,
                "Period 8 (3.55)" to 3.55,
                "Chaotic (3.9)" to 3.9,
                "Period 3 window (3.83)" to 3.83
            ).forEach { (label, growthRate) ->
                addButton(label) {
                    (neuron.updateRule as LogisticRule).growthRate = growthRate
                    reset()
                }.toolTipText = "Set growth rate to $growthRate, reset activation to 0.2, and clear the plot."
            }
            addSeparator()
            addButton("Reset initial condition") { reset() }.toolTipText =
                "Restart at activation 0.2 and clear the plot without changing the growth rate."
            addButton("Clear plot") { timeSeries.model.clearData() }.toolTipText =
                "Clear recorded samples while preserving the current neuron state."
        }.awaitLayout()
        place(networkComponent, SIM_WINDOW_GAP, controlPanel.bottomEdgeWithGap(), 280, 220)
        val plotX = maxOf(controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP + 280 + SIM_WINDOW_GAP)
        place(timeSeries, plotX, SIM_WINDOW_GAP, 800, 600)
    }
}
