package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.*
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.setLabels
import org.simbrain.network.layouts.GridLayout
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel
import org.simbrain.util.*
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.updater.UpdateCoupling
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*
import kotlin.math.*
import kotlin.random.Random

enum class Layer {S1, S2, DECISION, VA, IA}

// MATLAB-style prefilter implementation
fun prefilter(
    xHistory: List<DoubleArray>, // past activations (each entry = S1 activations at a timestep)
    w: DoubleArray,              // temporal kernel
    n: Double,                   // exponent for nonlinearity
    dt: Double,                  // time step (seconds)
    idx: Int                     // current time index
): DoubleArray {
    val phw = doubleArrayOf(1.0, -1.0)
    val numNeurons = xHistory.firstOrNull()?.size ?: return DoubleArray(0)
    val y = Array(numNeurons) { DoubleArray(w.size) { Double.NaN } }

    // Build temporal window
    for (i in 0 until numNeurons) {
        for (j in w.indices) {
            val t = idx - w.size + j
            val historyRow = xHistory.getOrNull(t)
            if (historyRow != null && i < historyRow.size) {
                y[i][j] = historyRow[i]
            }
        }
    }

    // Filter across ON and OFF phases
    val inp = Array(numNeurons) { DoubleArray(2) }
    for (phase in 0..1) {
        val filtered = DoubleArray(numNeurons)
        for (i in 0 until numNeurons) {
            var convSum = 0.0
            for (j in w.indices) {
                val xVal = y[i][j]
                if (!xVal.isNaN()) {
                    convSum += xVal * w[j] * phw[phase]
                }
            }
            val rectified = max(0.0, convSum * dt).pow(n)
            filtered[i] = rectified
        }
        for (i in 0 until numNeurons) inp[i][phase] = filtered[i]
    }

    // ON minus OFF
    return DoubleArray(numNeurons) { i -> inp[i][0] - inp[i][1] }
}

// Main simulation definition
val denisonNet = newSim {
    workspace.clearWorkspace()
    val netComponent = addNetworkComponent("Denison Net")
    val net = netComponent.network

    val trialStatusText = NetworkTextObject("").apply { fontSize = 18 }
    val finalReportStatusText = NetworkTextObject("").apply { fontSize = 18 }

    val sensory1 = net.addNeuronCollection(12).apply {
        label = "Sensory"
        setLabels((0..11).map { "${it * 30}°" })
        layout(GridLayout())
    }
    val sensory2 = net.addNeuronCollection(12).apply {
        label = "Sustained Response"
        setLabels((0..11).map { "${it * 30}°" })
        layout(GridLayout())
    }
    val decision = net.addNeuronCollection(2).apply { label = "Decisions" }.apply {
        setLabels(listOf("Target 1", "Target 2"))
        layout(GridLayout(120.0, 50.0, 2))
    }
    val vaLayer = net.addNeuronCollection(1).apply { label = "Voluntary Attention" }
    val iaLayer = net.addNeuronCollection(1).apply { label = "Involuntary Attention" }

    sensory1.setUpperBound(0.1)
    sensory2.setUpperBound(0.1)
    decision.setLowerBound(-0.000002)
    decision.setUpperBound(0.000002)
    vaLayer.setLowerBound(-0.02)
    vaLayer.setUpperBound(0.02)
    iaLayer.setLowerBound(-0.02)
    iaLayer.setUpperBound(0.02)

    net.addNetworkModels(sensory1, sensory2, decision, vaLayer, iaLayer, trialStatusText, finalReportStatusText)

    val imageWorldComponent = addImageWorld("Gratings")
    val imageWorld = imageWorldComponent.world
    imageWorld.loadImages(getFilesWithExtension("simulations/images/denisonGratings", "png"))
    val background = DoubleArray(10000) { 0.0 }.toGrayScaleImage(100, 100)
    imageWorld.imageAlbum.addImage(background)

    val (attentionPlot, IASeries, VASeries, SensoryMeanSeries, SustainedMeanSeries) = addTimeSeries(
        "Attention",
        seriesNames = listOf("Involuntary Attention", "Voluntary Attention", "Sensory", "Sustained")
    )
    attentionPlot.apply {
        model.isAutoRange = true
        model.fixedWidth = false
    }

    val (decisionPlot, Decision1Series, Decision2Series) = addTimeSeries("Evidence (CW + / CCW -)", seriesNames = listOf("Target 1", "Target 2"))
    decisionPlot.apply {
        model.isAutoRange = true
        model.fixedWidth = false
    }

    val IAPlot = couplingManager.createCoupling(iaLayer.getNeuron(0), IASeries)
    val VAPlot = couplingManager.createCoupling(vaLayer.getNeuron(0), VASeries)
    val Decision1Plot = couplingManager.createCoupling(decision.getNeuron(0), Decision1Series)
    val Decision2Plot = couplingManager.createCoupling(decision.getNeuron(1), Decision2Series)

    val m = 2 * sensory1.size - 1
    val pref_orientations = DoubleArray(12) { i -> i * PI / 12 }
    val neg_orientations = DoubleArray(12) { i -> (-2.5 + i * 0.1) * PI / 180 + PI / 2 }
    val pos_orientations = DoubleArray(12) { i -> (1.4 + i * 0.1) * PI / 180 + PI / 2 }
    val grat_orientations = neg_orientations + pos_orientations

    val w_i = Array(12) { i ->
        DoubleArray(24) { j -> abs(cos(grat_orientations[j] - pref_orientations[i]).pow(m)) }
    }

    var w_D = DoubleArray(12) { 0.0 }
    var SOA = 300
    var T1 = 0
    var T2 = 0
    var currentTarget = -1
    var vaState = 0
    var reportMode = 0
    var reportTarget = 1
    var totalAttention = 0.0
    var T1Allocation = 0.0
    var T2Allocation = 0.0
    var modelTime = 0
    val dt = 2.0

    var init_t = 200 //default is 1000

    // rolling S1 history and temporal kernel for IA layer
    val maxHist = 50
    val s1History = mutableListOf<DoubleArray>()
    val s1HistoryLock = Any()
    val wFilter = DoubleArray(15) { i -> exp(-i / 5.0) } // temporal kernel
    var trialPrepared = false
    val sweepFrames = mutableListOf<JInternalFrame>()

    val sweepSeriesNames = listOf("T1 valid", "T1 neutral", "T1 invalid", "T2 valid", "T2 neutral", "T2 invalid")
    val sweepConditionSeriesNames = listOf("valid", "neutral", "invalid")
    val sweepTrialsPerCondition = 2

    data class MenuOption(val name: String, val state: Int) {
        override fun toString() = name
    }

    val cueTypes = listOf(
        MenuOption("Both Targets", 0),
        MenuOption("Target 1", 1),
        MenuOption("Target 2", 2)
    )

    val SOADurs = listOf(
        MenuOption("100", 100),
        MenuOption("200", 200),
        MenuOption("300", 300),
        MenuOption("400", 400),
        MenuOption("500", 500),
        MenuOption("600", 600),
        MenuOption("700", 700),
        MenuOption("800", 800),
    )

    val reportTargets = listOf(
        MenuOption("Random", 0),
        MenuOption("Target 1", 1),
        MenuOption("Target 2", 2)
    )

    fun halfWave(n: Double): Double = max(0.0, n)


    fun getValidityText(cueState: Int = vaState, target: Int = reportTarget) = when {
        cueState == 0 -> "neutral"
        cueState == target -> "valid"
        else -> "invalid"
    }

    fun getReportTargetText(target: Int = reportTarget) = if (target == 1) "Target 1 (T1)" else "Target 2 (T2)"

    fun getReportTargetShortText(target: Int = reportTarget) = if (target == 1) "T1" else "T2"

    fun getCueShortText() = when (vaState) {
        1 -> "Target 1"
        2 -> "Target 2"
        else -> "both"
    }

    fun directionSign(targetIndex: Int) = if (targetIndex < 12) -1.0 else 1.0

    fun finalEvidenceFor(target: Int = reportTarget) = decision.getNeuron(target - 1).activation

    fun finalReportText(target: Int = reportTarget): String {
        val evidence = finalEvidenceFor(target)
        return if (evidence > 0) "Clockwise" else if (evidence < 0) "Counterclockwise" else "Unclear"
    }

    fun currentEvidenceText(activation: Double) = when {
        activation > 0 -> "clockwise"
        activation < 0 -> "counterclockwise"
        else -> "-"
    }

    fun setDecisionLabel(index: Int, label: String) {
        val neuron = decision.getNeuron(index)
        if (neuron.label != label) {
            neuron.label = label
        }
    }

    fun updateDecisionLabels() {
        setDecisionLabel(0, "T1: ${currentEvidenceText(decision.getNeuron(0).activation)}")
        setDecisionLabel(1, "T2: ${currentEvidenceText(decision.getNeuron(1).activation)}")
    }

    fun updateTrialStatusText() {
        trialStatusText.text = "Pay attention to ${getCueShortText()} and report on ${getReportTargetShortText()} (${getValidityText()})"
    }

    fun updateReportText(final: Boolean = false) {
        finalReportStatusText.text = if (final) "Final report: \"${finalReportText()}\"" else ""
    }

    suspend fun resetTrial(clearPlots: Boolean = true, updateDisplay: Boolean = true, resetWorkspace: Boolean = true) {
        if (clearPlots) {
            attentionPlot.model.clearData()
            decisionPlot.model.clearData()
        }
        trialPrepared = false
        currentTarget = -1
        T1Allocation = 0.0
        T2Allocation = 0.0
        totalAttention = 0.0
        decision.setActivations(doubleArrayOf(0.0, 0.0))
        sensory1.setActivations(DoubleArray(12) { 0.0 })
        sensory2.setActivations(DoubleArray(12) { 0.0 })
        vaLayer.setActivations(doubleArrayOf(0.0))
        iaLayer.setActivations(doubleArrayOf(0.0))
        synchronized(s1HistoryLock) {
            s1History.clear()
        }
        if (updateDisplay) {
            updateTrialStatusText()
            updateReportText(final = false)
            updateDecisionLabels()
            imageWorld.setFrame(24)
        }
        modelTime = 0
        if (resetWorkspace) {
            workspace.resetTime()
            workspace.stop()
        }
    }

    fun excitatoryDrive(layer: Layer, n: Double = 1.5, c: Double = 0.64, b_VA: Double = 40.0, b_IA: Double = 8.5): DoubleArray {
        var drive = DoubleArray(12) { 0.0 }
        when (layer) {
            Layer.S1 -> {
                if (currentTarget >= 0) {
                    val a_i = halfWave(1 + b_VA * vaLayer.activationArray[0]) * halfWave(1 + b_IA * iaLayer.activationArray[0])
                    drive = DoubleArray(12) { i -> a_i * (w_i[i][currentTarget] * c).pow(n) }
                }
            }
            Layer.S2 -> drive = DoubleArray(12) { i -> sensory1.activations[i].pow(n) }
            Layer.DECISION -> {
                drive = doubleArrayOf(0.0, 0.0)
                if (modelTime >= init_t / dt && modelTime < (init_t + 30 + SOA) / dt) {
                    drive[0] = w_D.zip(sensory2.activationArray) { x, y -> x * y }.sum()
                } else if (modelTime >= (init_t + 30 + SOA) / dt) {
                    drive[1] = w_D.zip(sensory2.activationArray) { x, y -> x * y }.sum()
                }
            }
            Layer.VA -> {
                val t_VAOn = -34.0
                val t_VADur = 124.0
                drive = when {
                    modelTime >= (init_t + t_VAOn) / dt && modelTime < (init_t + t_VAOn + t_VADur) / dt ->
                        doubleArrayOf(T1Allocation.pow(n))
                    modelTime >= (init_t + 30 + SOA + t_VAOn) / dt && modelTime < (init_t + 30 + SOA + t_VAOn + t_VADur) / dt ->
                        doubleArrayOf(T2Allocation.pow(n))
                    else -> doubleArrayOf(0.0)
                }
            }
            Layer.IA -> {
                val historySnapshot = synchronized(s1HistoryLock) {
                    s1History.map { it.copyOf() }
                }
                if (historySnapshot.isNotEmpty()) {
                    val preOut = prefilter(historySnapshot, wFilter, n, dt, historySnapshot.size - 1)
                    val summed = preOut.sum()
                    drive = doubleArrayOf(summed)
                } else {
                    drive = doubleArrayOf(0.0)
                }
            }
        }
        return drive
    }

    fun calculateActivations(layer: Layer, c: Double = 0.64, n: Double = 1.5): DoubleArray {
        var r_tprev = DoubleArray(12) { 0.0 }
        var tau = 0.0
        var sigma = 0.0
        when (layer) {
            Layer.S1 -> { r_tprev = sensory1.activations.toDoubleArray(); tau = 52.0; sigma = 1.4 }
            Layer.S2 -> { r_tprev = sensory2.activations.toDoubleArray(); tau = 100.0; sigma = 0.1 }
            Layer.DECISION -> { r_tprev = decision.activations.toDoubleArray(); tau = 1e5; sigma = 0.7 }
            Layer.VA -> { r_tprev = vaLayer.activations.toDoubleArray(); tau = 50.0; sigma = 20.0 }
            Layer.IA -> { r_tprev = iaLayer.activations.toDoubleArray(); tau = 2.0; sigma = 20.0 }
        }
        val e_t = excitatoryDrive(layer, c = c)
        val s_t = e_t.sum()
        return DoubleArray(r_tprev.size) { i -> r_tprev[i] + dt / tau * (-r_tprev[i] + e_t[i] / (s_t + sigma.pow(n))) }
    }

    fun prepareTrial(updateDisplay: Boolean = true) {
        if (updateDisplay) {
            updateTrialStatusText()
        }
        T1 = Random.nextInt(0, 24)
        T2 = Random.nextInt(0, 24)
        reportTarget = if (reportMode == 0) Random.nextInt(1, 3) else reportMode

        val t_R = 918.0
        totalAttention = 1 + min(SOA.toDouble() / t_R, 1.0)

        if (vaState == 0) {
            val w_N = 0.28
            T1Allocation = w_N * totalAttention
            T2Allocation = (1 - w_N) * totalAttention
        } else if (vaState == 1) {
            T1Allocation = 1.0
            T2Allocation = totalAttention - 1.0
        } else if (vaState == 2) {
            T2Allocation = 1.0
            T1Allocation = totalAttention - 1.0
        }
        trialPrepared = true
        if (updateDisplay) {
            updateTrialStatusText()
            updateReportText()
        }
    }

    suspend fun stepTrial(updateDisplay: Boolean = true, updateWorkspace: Boolean = true): Boolean {
        currentCoroutineContext().ensureActive()
        if (modelTime >= (init_t + 1100) / dt) {
            workspace.stop()
            trialPrepared = false
            return false
        }

        synchronized(s1HistoryLock) {
            s1History.add(sensory1.activations.toDoubleArray())
            if (s1History.size > maxHist) s1History.removeAt(0)
        }

        if (modelTime < init_t / dt) {
            currentTarget = -1
            if (updateDisplay) imageWorld.setFrame(24)
        } else if (modelTime < (init_t + 30) / dt) {
            currentTarget = T1
            if (updateDisplay) imageWorld.setFrame(T1)
        } else if (modelTime < (init_t + 30 + SOA) / dt) {
            currentTarget = -1
            if (updateDisplay) imageWorld.setFrame(24)
        } else if (modelTime < (init_t + 60 + SOA) / 2) {
            currentTarget = T2
            if (updateDisplay) imageWorld.setFrame(T2)
        } else {
            currentTarget = -1
            if (updateDisplay) imageWorld.setFrame(24)
        }

        val newS1 = calculateActivations(Layer.S1)
        val newS2 = calculateActivations(Layer.S2)
        val newDecision = calculateActivations(Layer.DECISION)
        val newVA = calculateActivations(Layer.VA)
        val newIA = calculateActivations(Layer.IA)

        sensory1.setActivations(newS1)
        sensory2.setActivations(newS2)
        decision.setActivations(newDecision)
        vaLayer.setActivations(newVA)
        iaLayer.setActivations(newIA)

        if (updateDisplay) {
            updateDecisionLabels()
        }

        if (updateWorkspace) {
            workspace.iterateSuspend()
        }
        modelTime += 1

        val stillRunning = modelTime < (init_t + 1100) / dt
        if (!stillRunning) {
            workspace.stop()
            trialPrepared = false
            if (updateDisplay) {
                updateReportText(final = true)
            }
        }
        return stillRunning
    }

    /**
     * Run one headless trial and return signed correct evidence for the reported target.
     *
     * Positive values mean the final decision-node activation points in the target's true direction
     * (clockwise or counterclockwise), negative values mean it points the wrong way, and larger
     * positive values indicate stronger correct evidence.
     *
     */
    suspend fun runTrialForProxy(cueState: Int, soaValue: Int, targetToReport: Int): Double {
        resetTrial(clearPlots = false, updateDisplay = false, resetWorkspace = false)
        vaState = cueState
        SOA = soaValue
        reportMode = targetToReport
        prepareTrial(updateDisplay = false)
        val maxHeadlessSteps = ((init_t + 1100) / dt).toInt() + 5
        var steps = 0
        while (steps < maxHeadlessSteps && stepTrial(updateDisplay = false, updateWorkspace = false)) {
            steps += 1
        }
        val targetIndex = if (targetToReport == 1) T1 else T2
        return finalEvidenceFor(targetToReport) * directionSign(targetIndex)
    }

    suspend fun runSweep(
        trialsPerCondition: Int = sweepTrialsPerCondition,
        onProgress: (completed: Int, total: Int, status: String) -> Unit = { _, _, _ -> },
        shouldCancel: () -> Boolean = { false }
    ): Pair<TimeSeriesModel, TimeSeriesModel>? {
        workspace.stop()
        fun newSweepModel() = TimeSeriesModel().apply {
            isAutoRange = true
            fixedWidth = false
            sweepConditionSeriesNames.forEach { addTimeSeries(it) }
        }
        val t1Model = newSweepModel()
        val t2Model = newSweepModel()
        val originalCue = vaState
        val originalSOA = SOA
        val originalReportMode = reportMode
        val conditions = listOf(
            1 to 1,
            0 to 1,
            2 to 1,
            2 to 2,
            0 to 2,
            1 to 2
        )
        val totalSteps = SOADurs.size * conditions.size * trialsPerCondition
        var completedSteps = 0
        try {
            SOADurs.forEach { soaOption ->
                conditions.forEachIndexed { seriesIndex, (cueState, targetToReport) ->
                    val proxyValues = mutableListOf<Double>()
                    repeat(trialsPerCondition) {
                        currentCoroutineContext().ensureActive()
                        if (shouldCancel()) {
                            return null
                        }
                        proxyValues.add(runTrialForProxy(cueState, soaOption.state, targetToReport))
                        completedSteps += 1
                        onProgress(
                            completedSteps,
                            totalSteps,
                            "SOA ${soaOption.state} ms, ${sweepSeriesNames[seriesIndex]}: $completedSteps / $totalSteps"
                        )
                    }
                    val (model, conditionIndex) = if (seriesIndex < 3) {
                        t1Model to seriesIndex
                    } else {
                        t2Model to seriesIndex - 3
                    }
                    model.addData(conditionIndex, soaOption.state.toDouble(), proxyValues.average())
                }
            }
            return t1Model to t2Model
        } finally {
            vaState = originalCue
            SOA = originalSOA
            reportMode = originalReportMode
            resetTrial(clearPlots = true, updateDisplay = true)
        }
    }

    // compute w_D by precomputing responses
    sensory1.setActivations(DoubleArray(12) { 0.0 })
    sensory2.setActivations(DoubleArray(12) { 0.0 })
    for (i in 1..15) {
        currentTarget = 0
        sensory1.setActivations(calculateActivations(Layer.S1, c = 1.0))
        sensory2.setActivations(calculateActivations(Layer.S2, c = 1.0))
    }
    val fullCCW = DoubleArray(12) { i -> sensory2.activationArray[i] }

    sensory1.setActivations(DoubleArray(12) { 0.0 })
    sensory2.setActivations(DoubleArray(12) { 0.0 })
    for (i in 1..15) {
        currentTarget = 23
        sensory1.setActivations(calculateActivations(Layer.S1, c = 1.0))
        sensory2.setActivations(calculateActivations(Layer.S2, c = 1.0))
    }
    val fullCW = DoubleArray(12) { i -> sensory2.activationArray[i] }
    w_D = DoubleArray(12) { i -> fullCW[i] - fullCCW[i] }

    sensory1.setActivations(DoubleArray(12) { 0.0 })
    sensory2.setActivations(DoubleArray(12) { 0.0 })

    withGui {
        place(netComponent, 200, 15, 492, 447)
        place(imageWorldComponent, 690, 15, 503, 447)
        place(attentionPlot, 195, 450, 500, 300)
        place(decisionPlot, 693, 448, 500, 300)

        trialStatusText.location = point(185.0, -200.0)
        finalReportStatusText.location = point(185.0, -250.0)
        updateTrialStatusText()
        updateReportText(final = false)
        updateDecisionLabels()

        sensory1.location = point(-10.0, 100.0)
        sensory2.location = point(230.0, 100.0)
        decision.location = point(440.0, 100.0)
        vaLayer.location = point(44.0, -97.0)
        iaLayer.location = point(370.0, -97.0)

        fun makeSweepFrame(title: String, model: TimeSeriesModel, x: Int, y: Int) =
            JInternalFrame(title, true, true, true, true).apply {
                layout = BorderLayout()
                val plotPanel = TimeSeriesPlotPanel(model).apply {
                    chartPanel.chart.xyPlot.domainAxis.label = "SOA"
                    chartPanel.chart.xyPlot.rangeAxis.label = "Sensitivity"
                }
                add(plotPanel, BorderLayout.CENTER)
                setBounds(x, y, 600, 420)
                defaultCloseOperation = JInternalFrame.DISPOSE_ON_CLOSE
                isVisible = true
            }

        fun showSweepFrames(t1Model: TimeSeriesModel, t2Model: TimeSeriesModel) {
            sweepFrames.filter { it.isDisplayable }.forEach { it.dispose() }
            sweepFrames.clear()
            val frames = listOf(
                makeSweepFrame("Sensitivity Proxy by SOA — T1", t1Model, 60, 120),
                makeSweepFrame("Sensitivity Proxy by SOA — T2", t2Model, 670, 120)
            )
            frames.forEach {
                sweepFrames.add(it)
                addInternalFrame(it)
                it.isIcon = false
                it.toFront()
                it.isSelected = true
            }
        }

        createControlPanel("Control Panel", 15, 15) {
            addLabel("Attention Cue:")
            addComboBox("", cueTypes, cueTypes[0]) { selectedCue ->
                vaState = selectedCue.state
                updateTrialStatusText()
            }
            addLabel("Report Target")
            addComboBox("", reportTargets, reportTargets[0]) { selectedReportTarget ->
                reportMode = selectedReportTarget.state
                reportTarget = if (reportMode == 0) reportTarget else reportMode
                updateTrialStatusText()
            }
            addSeparator()
            addLabel("SOA")
            addComboBox("", SOADurs, SOADurs[2]) {selectedSOA ->
                SOA = selectedSOA.state
            }
            addSeparator()
            addButton("Run SOA Sweep") {
                val sweepButton = this
                val cancelRequested = AtomicBoolean(false)
                val totalSteps = SOADurs.size * sweepSeriesNames.size * sweepTrialsPerCondition
                val progressWindow = withContext(Dispatchers.Swing) {
                    sweepButton.isEnabled = false
                    ProgressWindow(totalSteps, "SOA sweep: 0 / $totalSteps").apply progressWindow@{
                        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
                        val cancelButton = JButton("Cancel").apply {
                            addActionListener {
                                cancelRequested.set(true)
                                isEnabled = false
                                text = "Cancelling..."
                                this@progressWindow.text = "Cancelling after current trial..."
                            }
                        }
                        val cancelButtonPanel = JPanel(BorderLayout()).apply {
                            border = BorderFactory.createEmptyBorder(0, 10, 10, 10)
                            add(cancelButton, BorderLayout.CENTER)
                        }
                        add(cancelButtonPanel, BorderLayout.SOUTH)
                        addWindowListener(object : WindowAdapter() {
                            override fun windowClosing(e: WindowEvent) {
                                cancelRequested.set(true)
                            }
                        })
                        pack()
                        setLocationRelativeTo(null)
                    }
                }
                try {
                    val models = runSweep(
                        onProgress = { completed, total, status ->
                            SwingUtilities.invokeLater {
                                progressWindow.value = completed
                                progressWindow.text = status
                                progressWindow.progressBar.string = "$completed / $total"
                            }
                        },
                        shouldCancel = { cancelRequested.get() }
                    )
                    withContext(Dispatchers.Swing) {
                        progressWindow.close()
                        if (models != null) {
                            val (t1Model, t2Model) = models
                            showSweepFrames(t1Model, t2Model)
                        }
                    }
                } finally {
                    withContext(Dispatchers.Swing) {
                        sweepButton.isEnabled = true
                        if (progressWindow.isDisplayable) {
                            progressWindow.close()
                        }
                    }
                }
            }
            addSeparator()

            workspace.updater.updateManager.clear()
            workspace.updater.updateManager.addAction(updateAction("Temporal attention step") {
                stepTrial(updateDisplay = true, updateWorkspace = false)
            })
            workspace.updater.updateManager.addAction(updateAction("Sensory/sustained means") {
                SensoryMeanSeries.setValue(sensory1.activationArray.average())
                SustainedMeanSeries.setValue(sensory2.activationArray.average())
            })
            workspace.updater.updateManager.addAction(UpdateCoupling(VAPlot))
            workspace.updater.updateManager.addAction(UpdateCoupling(IAPlot))
            workspace.updater.updateManager.addAction(UpdateCoupling(Decision1Plot))
            workspace.updater.updateManager.addAction(UpdateCoupling(Decision2Plot))

            addButton("Start") {
                if (workspace.updater.isRunning) return@addButton
                resetTrial()
                prepareTrial()
                workspace.run()
            }
            addButton("Step 10") {
                if (workspace.updater.isRunning) return@addButton
                if (!trialPrepared) {
                    resetTrial()
                    prepareTrial()
                }
                workspace.iterateSuspend(10)
            }
            addButton("Reset") {
                workspace.stop()
                resetTrial()
            }
        }
    }

addSidebarInfo(
        """
        # Temporal Attention Model

        A neural network model of visual attention based on the paper ["A dynamic normalization model of temporal attention"](https://www.nature.com/articles/s41562-021-01129-1) by Rachel Denison, Marisa Carrasco, and David Heeger. The main goal of this simulation is to make the model inspectable: you can run or step through a trial and watch the stimulus, sensory responses, attention traces, sustained activity, and decision evidence unfold over time. The model approximates some qualitative effects from the experiment, via the `SOA Sweep` button, discussed below.

        # Simulation Details

        In the experiment, participants viewed two tilted grating targets presented one after the other: Target 1 (`T1`) and Target 2 (`T2`). Each target was rotated either clockwise or counterclockwise. Before the targets appeared, participants received an auditory cue telling them to pay attention to `T1`, `T2`, or both target times. After viewing the targets, they were asked to report the rotation direction for one target or the other, depending on the trial.

        Key finding in the paper: When participants were cued to attend to a specific target and then asked to report on that same target, their response times were faster and more accurate than when cued to one target but asked about the other. This demonstrates that voluntary attention enhances perceptual processing of attended stimuli. The paper also replicates earlier results showing that perceptual sensitivity varies with time between stimuli and is more difficult for earlier stimuli.

        The paper's main behavioral measure is perceptual sensitivity, or `d'`. A quick way to think about `d'` is "how separable are signal and noise?" If you can reliably tell a real phone buzz from background vibration, spot a faint star against visual noise, or distinguish two similar wines, sensitivity is high. If not, sensitivity is low.

        In this simulation, the analogous question is how strongly the model's final evidence separates clockwise from counterclockwise for the reported target. Strong evidence in the correct direction is like higher sensitivity; weak or ambiguous evidence is like lower sensitivity. In Simbrain this corresponds to how strongly the decision nodes are activated. 

        ## Control Panel Settings

        `Attention Cue` sets how voluntary attention is allocated before the two targets appear:

        - `Both Targets`: Divides attention across the two target times, similar to the neutral cue condition in the experiment. On screen this appears as `Cue: attend both targets`.
        - `Target 1`: Prioritizes the first target (`T1`), producing stronger voluntary gain around the first stimulus. On screen this appears as `Cue: attend Target 1 (T1)`.
        - `Target 2`: Prioritizes the second target (`T2`), producing stronger voluntary gain around the second stimulus. On screen this appears as `Cue: attend Target 2 (T2)`.

        `SOA` sets the stimulus onset asynchrony: the delay, in milliseconds, between the first and second target. Short SOAs create stronger competition for the limited voluntary attention resource; longer SOAs allow voluntary attention to recover before the second target.

        `Report Target` sets which target the model is asked to report after the two targets appear. `Random` chooses either `T1` or `T2` for each trial. A trial is `valid` when the attention cue matches the report target, `invalid` when it cues the other target, and `neutral` when the cue is `Both Targets`.

        To set up the main trial conditions:

        - `T1 valid`: set `Attention Cue` to `Target 1` and `Report Target` to `Target 1`.
        - `T1 invalid`: set `Attention Cue` to `Target 2` and `Report Target` to `Target 1`.
        - `T2 valid`: set `Attention Cue` to `Target 2` and `Report Target` to `Target 2`.
        - `T2 invalid`: set `Attention Cue` to `Target 1` and `Report Target` to `Target 2`.
        - `Neutral`: set `Attention Cue` to `Both Targets` and `Report Target` to `Random`.

        `Start` runs one trial with randomly selected orientations for `T1` and `T2`. While a trial is running it has no effect; use the workspace `Stop` control to halt the run, or `Reset` to clear the network state and plots before starting a new trial.

        `Step 10` advances the current trial by 10 workspace iterations. If no trial is prepared, it starts a new trial using the current cue and `SOA` settings and then advances 10 iterations.

        `Reset` cancels any running trial and returns the model to a blank baseline state.

        `Run SOA Sweep` runs a small batch of trials for every `SOA`, report target, and validity condition. When the sweep completes, it opens two `Sensitivity Proxy by SOA` popups side by side, one for `T1` and one for `T2`. Each plots the `valid`, `neutral`, and `invalid` curves against `SOA` (x-axis) and the sensitivity proxy (y-axis). For a clockwise target, positive decision activation counts as correct; for a counterclockwise target, negative decision activation is flipped so it also counts as correct. Larger values in the appropriate direction are stronger correct evidence and thus are a proxy for d'.

        The Simbrain simulations captures some, but not all, of the paper's results. It shows that for the earlier target `T1` sensitivity is dependent on SOA but `T2` us not. Valid trials should often produce stronger evidence than invalid trials, but because this is a small stochastic simulation and not a full fit to behavioral data, that ordering is not guaranteed in every sweep.

       The two decision nodes are best read as evidence traces for what the model would say if asked about `T1` or `T2`.
       
       # Model structure and time series plots

        The model starts with a raw stimulus: a tilted grating in the image world. While the grating is on screen, it drives orientation-tuned activity in the sensory layer. That raw sensory response is then transformed into a slower sustained response, which is what the decision layer reads out as evidence for clockwise or counterclockwise tilt.

        The model consists of five layers that process visual input and make decisions.

        - `Sensory Layer`: 12 neurons, each tuned to a different orientation (0°, 30°, 60°, etc.). When a grating pattern appears, the neuron matching that orientation activates most strongly, with nearby orientations showing weaker responses. This is the model's immediate response to the raw stimulus.

        - `Sustained Response Layer`: 12 neurons that maintain prolonged activation after the sensory layer responds. These sustained activations are a slower echo of the raw sensory response, and they are what get accumulated in the decisions layer.

        - `Decisions Layer`: 2 neurons that accumulate evidence for each target. Positive activation indicates clockwise rotation, negative indicates counterclockwise. The strength of activation reflects the model's confidence.

        - `Voluntary Attention`: A single neuron controlled by the attention cue selected in the control panel. It enhances processing in the sensory layer based on which target is cued and when that target is expected.

        - `Involuntary Attention`: A single neuron driven by the stimulus itself. It responds automatically to pattern onsets regardless of the cue, and also enhances processing in the sensory layer.

        ## How Attention Works in the Model

        Both attention systems modulate the gain of the sensory layer: they multiply the overall activation level before the sustained response and decision evidence are computed.

        Voluntary attention is determined by the cue and by the known timing of the two targets. A `Target 1` cue makes the voluntary trace larger around the first target, a `Target 2` cue makes it larger around the second target, and a neutral cue divides attention across both target times. The available voluntary attention is limited over short intervals and recovers as SOA increases, so cueing one target can reduce the gain available for the other.

        Involuntary attention is stimulus-driven. It is computed from recent sensory-layer activity, so it rises after a grating appears even when that target was not cued. Because it is driven by S1 activity, it can be larger when the stimulus response has already been boosted by voluntary attention.

        ## Time Series

        The `Attention` plot contains four traces:

        - `Involuntary Attention`: the automatic, stimulus-driven gain signal produced by recent sensory activity.
        - `Voluntary Attention`: the cue-driven gain signal timed around the expected target onsets.
        - `Sensory`: the average activity of the raw orientation-tuned sensory layer.
        - `Sustained`: the average activity of the slower sustained response layer that feeds decision evidence.

        Read these four traces together (we suggest starting by stepping through a trial). A target first produces a sensory response; that response feeds a sustained response; voluntary and involuntary attention change the gain of the sensory response; and the decision units accumulate the sustained activity in separate windows for `T1` and `T2`.

        # What to Do

        ## Run a Trial

        1. Select an `Attention Cue`, `SOA`, and `Report Target` from the control panel. Generally it's best to use one of the 5 possibilities noted above: valid T1, valid T2, etc.
        2. Click `Start` to run a trial
        3. Use `Step 10` instead of `Start` if you want to advance through the trial manually
        4. Click `Reset` if you want to stop the current trial and clear the display

        ## Trial Timeline

        The simulation begins with a short blank interval. This is intentional: the model starts from a baseline state before the first target appears, so the attention and evidence traces have a visible pre-stimulus period.

        ```text
        0 ms        200 ms      230 ms          200 + SOA ms     230 + SOA ms
        |-----------|===========|---------------|================|----------->
         blank       T1 shown    blank delay     T2 shown         response tails
        ```

        During the blank periods, the image world shows a gray background and the sensory layers should remain near baseline. Around `T1` and `T2`, watch for sensory activity, involuntary attention transients, voluntary gain determined by `Attention Cue`, and decision-layer evidence.

        ## What to Observe

        `Sensory Responses`: Watch the sensory layer activate when patterns appear, and the sustained response layer show a fading echo of sensory activations. The neuron corresponding to the pattern's orientation will activate most strongly.

        `Attention Dynamics`: The `Attention` time series plot shows how voluntary attention, involuntary attention, the raw sensory response, and the sustained response unfold over time. Notice:
        - When involuntary attention spikes (at pattern onsets)
        - How voluntary attention is allocated to one target or the other based on the cue you selected
        - How the sensory mean rises quickly to the current grating
        - How the sustained mean lingers after the sensory response
        - The interaction between the two attention systems

        `Evidence Formation`: The `Evidence (CW + / CCW -)` time series plot shows how the two decision neurons accumulate evidence for each target over time. Positive activation indicates clockwise rotation, negative indicates counterclockwise. The decision node labels also show the current evidence direction directly as `T1: clockwise` or `T2: counterclockwise`.
        
        `Behavior`: Compact text at the top of the network shows the current cue, report target, and validity. At the end of a trial, a second line shows the final clockwise/counterclockwise report. The exact randomly selected target orientations are intentionally not shown as text, so the focus stays on the network activity, image display, and evidence traces.

        `Sensitivity Proxy by SOA`: Click `Run SOA Sweep` to run the sweep and open two popups side by side, one for `T1` and one for `T2`. In each, compare the valid, neutral, and invalid curves, with `SOA` on the x-axis and the sensitivity proxy on the y-axis. Higher values mean the model's final evidence is farther in the correct direction for the reported target.
        
        ## Experiment

        1. Try different attention cues and observe how they affect:
            - The evidence trace activation strengths (model confidence)
            - The voluntary attention trace in the `Attention` plot
            - What the model would report if asked about each target

        2. Run multiple trials with the same cue to see how random variation in stimulus patterns affects the model's performance.

        3. Try different `SOA` (stimulus onset asynchrony) values to see how the timing between the two targets affects attention allocation and evidence strength.

        4. Click `Run SOA Sweep` and compare the sensitivity proxy curves for valid, neutral, and invalid trials in the side-by-side `T1` and `T2` plots.

        # References

        Denison, R. N., Carrasco, M., & Heeger, D. J. (2021). [_A dynamic normalization model of temporal attention_](https://www.nature.com/articles/s41562-021-01129-1). _Nature Human Behaviour_, _5_(12), 1674–1685.

        # Credits

        [Jensen Guo](https://www.linkedin.com/in/jensen-guo/)
        
        [Jeff Yoshimi](https://www.jeffyoshimi.net)

        """.trimIndent()
    )
}
