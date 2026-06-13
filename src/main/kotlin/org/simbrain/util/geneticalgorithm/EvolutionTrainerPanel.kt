package org.simbrain.util.geneticalgorithm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.jfree.chart.plot.ValueMarker
import org.jfree.chart.ui.RectangleAnchor
import org.jfree.chart.ui.TextAnchor
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotActions
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.widgets.ToggleButton
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.*
import javax.swing.*

/**
 * Trainer-style controls for an [EvolutionRunner]: Step, Run/Stop toggle, Properties,
 * Express, plus generation/metric labels and a fitness time series plot. Modeled on
 * the supervised `TrainerControls`.
 *
 * The optional [onExpress] callback is invoked when the Express button is pressed. Use it
 * to visualize the current best genome in the workspace. If `null`, the button is hidden.
 */
class EvolutionTrainerControls(
    private val runner: EvolutionRunner,
    private val evaluatorParams: EvaluatorParams,
    private val onExpress: (suspend () -> Unit)? = null
) : JPanel(), CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.Swing + job

    private val generationLabel = JLabel(runner.generation.toString())
    private val metricLabel = JLabel("-")

    private val runAction = createAction(
        name = "Run",
        iconPath = "menu_icons/Play.png",
        description = "Run evolution until stop is pressed"
    ) {
        runner.startEvolving()
    }

    private val stopAction = createAction(
        name = "Stop",
        iconPath = "menu_icons/Stop.png",
        description = "Pause evolution"
    ) {
        runner.stopEvolving()
    }

    private val stepAction = createAction(
        description = "Evolve one generation",
        iconPath = "menu_icons/Step.png",
        initBlock = {
            runner.events.beginEvolution.on { isEnabled = false }
            runner.events.endEvolution.on { isEnabled = true }
        }
    ) {
        runner.evolveOnce()
    }

    fun openPropsDialog() {
        val editor = AnnotatedPropertyEditor(evaluatorParams)
        editor.displayInDialog(title = "Evolution Properties") { editor.commitChanges() }
    }

    private val propsAction = createAction(
        name = "Evolution properties",
        description = "Edit evolution parameters",
        iconPath = "menu_icons/Tools.png"
    ) {
        openPropsDialog()
    }

    private val expressAction = onExpress?.let { express ->
        createAction(
            name = "Express current best",
            description = "Visualize the current best genome in the workspace",
            iconPath = "menu_icons/Trophy.png",
            coroutineContext = Dispatchers.Default
        ) {
            express()
        }
    }
    val expressButton: JButton? = expressAction?.let(::JButton)

    init {
        layout = MigLayout("ins 0, gap 0px 0px")

        val runTools = JPanel(MigLayout("nogrid, ins 0"))
        runTools.add(JButton(stepAction))
        runTools.add(
            ToggleButton(listOf(runAction, stopAction)).apply {
                setAction("Run")
                runner.events.beginEvolution.on(Dispatchers.Swing) {
                    this@EvolutionTrainerControls.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                    setAction("Stop")
                }
                runner.events.endEvolution.on(Dispatchers.Swing) {
                    this@EvolutionTrainerControls.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    setAction("Run")
                }
            }
        )
        runTools.add(JButton(propsAction))
        runTools.add(JPanel(MigLayout("ins 0, gap 0px 0px")).apply {
            expressButton?.let { add(it) }
        }, "wrap")

        val labelPanel = LabelledItemPanel()
        labelPanel.addItem("Generation:", generationLabel)
        val metricName = evaluatorParams.stoppingCondition.name
        labelPanel.addItem("$metricName (${evaluatorParams.evaluationPercentile}th pct):", metricLabel)
        runTools.add(labelPanel, "span")

        val plot = FitnessTimeSeries(runner, evaluatorParams)
        val plotPanel = JPanel(MigLayout("ins 0, gap 0px 0px, fillx, wrap")).apply {
            add(plot, "grow, push, wrap")
            val btns = JPanel(MigLayout("ins 0, gap 0px 0px"))
            btns.add(JButton(TimeSeriesPlotActions.getClearGraphAction(plot.graphPanel)))
            btns.add(JButton(TimeSeriesPlotActions.getPropertiesDialogAction(plot.graphPanel)))
            add(btns, "align center")
        }

        add(runTools, "growy")
        add(plotPanel, "grow, push, h 240!")

        runner.events.generationUpdated.on(Dispatchers.Swing) { state ->
            generationLabel.text = state.generation.toString()
            val value = state.nthPercentileFitness(evaluatorParams.evaluationPercentile)
            metricLabel.text = value.format(4)
        }
    }
}

/**
 * Fitness time series plot: tracks the target metric (percentile fitness/error) over generations.
 */
class FitnessTimeSeries(runner: EvolutionRunner, evaluatorParams: EvaluatorParams) : JPanel() {

    val graphPanel: TimeSeriesPlotPanel

    init {
        layout = MigLayout("ins 0, gap 0px 0px")
        preferredSize = Dimension(500, 200)

        val model = TimeSeriesModel()
        model.fixedWidth = true
        model.windowSize = 1000
        model.isAutoRange = true
        model.useAutoRangeMinimumUpperBound = true
        model.autoRangeMinimumUpperBound = 1.0
        graphPanel = TimeSeriesPlotPanel(model)
        graphPanel.chartPanel.chart.setTitle("")
        graphPanel.chartPanel.chart.xyPlot.domainAxis.label = "Generation"
        graphPanel.chartPanel.chart.xyPlot.rangeAxis.label = evaluatorParams.stoppingCondition.name
        graphPanel.removeAllButtonsFromToolBar()
        add(graphPanel, "grow, push")

        model.addTimeSeries("${evaluatorParams.evaluationPercentile}th pct")

        val targetMarker = ValueMarker(evaluatorParams.targetMetric).apply {
            paint = Color.RED
            stroke = BasicStroke(
                1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
                floatArrayOf(6f, 6f), 0f
            )
            label = "Target"
            labelPaint = Color.RED
            labelAnchor = RectangleAnchor.TOP_RIGHT
            labelTextAnchor = TextAnchor.BOTTOM_RIGHT
        }
        graphPanel.addRangeMarker(targetMarker)

        val maxGenMarker = ValueMarker(evaluatorParams.maxGenerations.toDouble()).apply {
            paint = Color.GRAY
            stroke = BasicStroke(
                1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
                floatArrayOf(6f, 6f), 0f
            )
            label = "Max gen"
            labelPaint = Color.GRAY
            labelAnchor = RectangleAnchor.TOP_LEFT
            labelTextAnchor = TextAnchor.TOP_RIGHT
        }
        graphPanel.addDomainMarker(maxGenMarker)

        runner.events.generationUpdated.on(Dispatchers.Swing) { state ->
            val value = state.nthPercentileFitness(evaluatorParams.evaluationPercentile)
            model.addData(0, state.generation.toDouble(), value)
            if (targetMarker.value != evaluatorParams.targetMetric) {
                targetMarker.value = evaluatorParams.targetMetric
            }
            if (maxGenMarker.value != evaluatorParams.maxGenerations.toDouble()) {
                maxGenMarker.value = evaluatorParams.maxGenerations.toDouble()
            }
        }
    }
}

/**
 * An entry in the [ExpressionHistory] — one "expression" of a genome, possibly consisting of several
 * workspace components (e.g. one network per cow).
 *
 * Callbacks (typically) show/hide/remove those components from the workspace.
 */
class ExpressionEntry(
    val label: String,
    val restore: () -> Unit,
    val minimize: () -> Unit,
    val close: () -> Unit,
) {
    companion object {
        /**
         * Build an entry whose [restore]/[minimize]/[close] act on a list of workspace components
         * (fire `componentMinimized` events and call [Workspace.removeWorkspaceComponent]). This is
         * the standard packaging of expressed genome components; use it instead of hand-writing
         * the three lambdas per sim.
         */
        fun forComponents(
            workspace: Workspace,
            components: List<WorkspaceComponent>,
            label: String
        ) = ExpressionEntry(
            label = label,
            restore = { components.forEach { it.events.componentMinimized.fire(false) } },
            minimize = { components.forEach { it.events.componentMinimized.fire(true) } },
            close = { components.forEach { workspace.removeWorkspaceComponent(it) } }
        )
    }
}

class ExpressionHistoryEvents : FlowEvents() {
    val changed = NoArgEvent()
}

/**
 * Registry of past expressions. Sims add an entry each time `expressBest()` is called, typically after
 * minimizing the previous entries (see [minimizeAll]). Bound by [ExpressionHistoryPanel] in the trainer dialog.
 */
class ExpressionHistory {
    private val _entries = mutableListOf<ExpressionEntry>()
    val entries: List<ExpressionEntry> get() = _entries.toList()

    val events = ExpressionHistoryEvents()

    fun add(entry: ExpressionEntry) {
        _entries.add(entry)
        events.changed.fire()
    }

    fun remove(entry: ExpressionEntry) {
        _entries.remove(entry)
        events.changed.fire()
    }

    fun minimizeAll() = _entries.forEach { it.minimize() }
}

/**
 * Simple list view of [ExpressionHistory] entries: one row per entry with restore/minimize/close buttons.
 * Scrollable so a long history doesn't push the rest of the trainer dialog off-screen.
 */
class ExpressionHistoryPanel(private val history: ExpressionHistory) : JPanel() {

    private val listPanel = JPanel(MigLayout("ins 4, wrap, fillx, gapy 6"))

    init {
        layout = BorderLayout()
        val scroll = JScrollPane(listPanel).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            border = null
            preferredSize = Dimension(600, 130)
        }
        add(scroll, BorderLayout.CENTER)
        history.events.changed.on(Dispatchers.Swing) { rebuild() }
        rebuild()
    }

    private fun rebuild() {
        listPanel.removeAll()
        if (history.entries.isEmpty()) {
            listPanel.add(JLabel("(none yet)"))
        } else {
            for (entry in history.entries) {
                listPanel.add(buildRow(entry), "growx")
            }
        }
        listPanel.revalidate()
        listPanel.repaint()
    }

    private fun buildRow(entry: ExpressionEntry): JPanel {
        return JPanel(MigLayout("ins 2 4 2 4, gap 6px")).apply {
            add(JLabel(entry.label), "growx, push")
            add(JButton("Restore").apply {
                addActionListener { entry.restore() }
            })
            add(JButton("Minimize").apply {
                addActionListener { entry.minimize() }
            })
            add(JButton("Close").apply {
                addActionListener {
                    entry.close()
                    history.remove(entry)
                }
            })
        }
    }
}

/**
 * Long-lived trainer state that outlives any one dialog. Build once per evolution run and reuse it
 * across open/close cycles of the trainer dialog so closing the dialog doesn't wipe the runner,
 * history, gene displays, or metrics.
 *
 * Event handlers are registered during construction. The dialog created by
 * [createEvolutionTrainerDialog] just arranges the pre-built UI pieces into a shell that can be
 * disposed without affecting session state.
 */
class EvolutionTrainerSession(
    val runner: EvolutionRunner,
    val evaluatorParams: EvaluatorParams,
    extras: List<JComponent> = emptyList(),
    onExpress: (suspend () -> Unit)? = null,
    val history: ExpressionHistory? = null,
    val title: String = "Evolution Trainer"
) {
    val controls = EvolutionTrainerControls(
        runner = runner,
        evaluatorParams = evaluatorParams,
        onExpress = onExpress
    )
    val historyPanel: DetailTrianglePanel? = history?.let { h ->
        DetailTrianglePanel(
            contentPanel = ExpressionHistoryPanel(h),
            defaultOpen = false,
            upLabel = "Expressed genomes",
            downLabel = "Expressed genomes",
            topPanelComponent = controls.expressButton
        ).also { panel ->
            var openedOnce = false
            h.events.changed.on(Dispatchers.Swing) {
                if (!openedOnce && h.entries.isNotEmpty()) {
                    openedOnce = true
                    panel.setOpen(true)
                }
            }
        }
    }
    val extraScrollPanes: List<JScrollPane> = extras.map { extra ->
        JScrollPane(extra).apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            preferredSize = Dimension(800, 300)
        }
    }
}

/**
 * Build a non-modal [StandardDialog] around a pre-built [EvolutionTrainerSession]. Disposing the
 * dialog leaves the session intact, so re-invoking this function with the same session restores
 * the UI with all state preserved (runner generation, history entries, plot data, etc.).
 *
 * Non-modal so evolution can run while users inspect the workspace.
 */
fun SimbrainDesktop.createEvolutionTrainerDialog(session: EvolutionTrainerSession): StandardDialog {
    return StandardDialog(frame, session.title).apply {
        modalityType = Dialog.ModalityType.MODELESS
        val content = JPanel(MigLayout("ins 10, wrap, fill"))
        content.add(session.controls, "growx")
        session.historyPanel?.let { content.add(it.withTopSeparator(), "growx") }
        session.extraScrollPanes.forEach { pane -> content.add(pane.withTopSeparator(), "grow, push") }
        contentPane = content
        setAsDoneDialog()
        addCloseTask {
            session.runner.launch { session.runner.stopEvolving() }
        }
    }
}

private fun JComponent.withTopSeparator(): JPanel {
    return JPanel(BorderLayout()).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                1,
                0,
                0,
                0,
                UIManager.getColor("Separator.foreground") ?: Color.LIGHT_GRAY
            ),
            BorderFactory.createEmptyBorder(6, 0, 0, 0)
        )
        add(this@withTopSeparator, BorderLayout.CENTER)
    }
}
