package org.simbrain.util.geneticalgorithm

import org.simbrain.custom_sims.createControlPanel
import org.simbrain.util.ControlPanelKt
import org.simbrain.util.format
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.JButton
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Wraps [EvaluatorParams] with a control panel and progress window for evolution sims.
 *
 * Usage in a sim:
 * ```
 * val controlPanel = EvolutionControlPanel(evaluatorParams)
 *
 * // In runSim():
 * controlPanel.bind(runner)
 * val result = runner.run()
 *
 * // In withGui:
 * val panel = controlPanel.show("Control Panel", 5, 10)
 * controlPanel.addButton("Evolve") { ... }
 * ```
 */
class EvolutionControlPanel(val evaluatorParams: EvaluatorParams) {

    private var panel: ControlPanelKt? = null

    /**
     * Show the control panel on the desktop with an [AnnotatedPropertyEditor] for the evaluator params.
     * Must be called inside a `withGui` block where `this` is [SimbrainDesktop].
     * Returns the [ControlPanelKt] so callers can add extra widgets (separators, property editors, etc.).
     */
    fun show(desktop: SimbrainDesktop, name: String, x: Int, y: Int): ControlPanelKt {
        val paramsEditor = AnnotatedPropertyEditor(evaluatorParams)
        val cp = desktop.createControlPanel(name, x, y) {
            addAnnotatedPropertyEditor(paramsEditor)
        }
        panel = cp
        return cp
    }

    /**
     * Add a button to the control panel.
     */
    fun addButton(
        label: String,
        context: CoroutineContext = EmptyCoroutineContext,
        task: suspend JButton.(ActionEvent) -> Unit
    ) {
        panel?.addButton(label, context, task = task)
    }

    /**
     * Bind a progress window to the [EvolutionRunner], automatically updating each generation
     * and closing when evolution completes via [EvolutionRunner.onComplete].
     */
    fun bind(runner: EvolutionRunner) {
        val pw = ProgressWindow(evaluatorParams.maxGenerations, "Progress").apply {
            minimumSize = Dimension(300, 100)
            setLocationRelativeTo(null)
        }
        runner.onGeneration { state ->
            val metricName = evaluatorParams.stoppingCondition.name
            val value = state.nthPercentileFitness(evaluatorParams.evalutationPercentile)
            pw.text = "$metricName: ${value.format(4)}"
            pw.value = state.generation
        }
        runner.onComplete {
            pw.close()
        }
    }
}
