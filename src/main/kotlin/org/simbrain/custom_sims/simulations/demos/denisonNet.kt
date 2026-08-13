/**
 * Interactive implementation of Denison, Carrasco, and Heeger's temporal attention model.
 *
 * This is the canonical Temporal Attention Model simulation. Its paper-aligned dynamics live
 * here; the presentation and sidebar documentation make those dynamics inspectable.
 */
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

val denisonNet = newSim {
    workspace.clearWorkspace()
    val netComponent = addNetworkComponent("Temporal Attention Model")
    val net = netComponent.network

    val trialStatusText = NetworkTextObject("").apply { fontSize = 18 }
    val finalReportStatusText = NetworkTextObject("").apply { fontSize = 18 }
    val rfLabels = (1..12).map { "${((180 - it * 15) % 180)}°" }

    val sensory1 = net.addNeuronCollection(12).apply {
        label = "Sensory 1"
        setLabels(rfLabels)
        layout(GridLayout())
    }
    val sensory2 = net.addNeuronCollection(12).apply {
        label = "Sensory 2"
        setLabels(rfLabels)
        layout(GridLayout())
    }
    val sensory3 = net.addNeuronCollection(12).apply {
        label = "Sensory 3"
        setLabels(rfLabels)
        layout(GridLayout())
    }
    val decision = net.addNeuronCollection(2).apply { label = "Decision evidence" }.apply {
        setLabels(listOf("Target 1", "Target 2"))
        layout(GridLayout(120.0, 50.0, 2))
    }
    val vaLayer = net.addNeuronCollection(1).apply { label = "Voluntary Attention" }
    val iaLayer = net.addNeuronCollection(1).apply { label = "Involuntary Attention" }

    sensory1.setUpperBound(0.1)
    sensory2.setUpperBound(0.1)
    sensory3.setUpperBound(0.1)
    decision.setLowerBound(-0.00002)
    decision.setUpperBound(0.00002)
    vaLayer.setLowerBound(-0.05)
    vaLayer.setUpperBound(0.05)
    iaLayer.setLowerBound(-0.1)
    iaLayer.setUpperBound(0.1)

    net.addNetworkModels(sensory1, sensory2, sensory3, decision, vaLayer, iaLayer, trialStatusText, finalReportStatusText)

    val imageWorldComponent = addImageWorld("Gratings")
    val imageWorld = imageWorldComponent.world
    imageWorld.loadImages(
        arrayOf(
            getFileFromRoot("simulations/images/denisonGratingsV2/v_ccw_2deg.png"),
            getFileFromRoot("simulations/images/denisonGratingsV2/v_cw_2deg.png"),
            getFileFromRoot("simulations/images/denisonGratingsV2/h_ccw_2deg.png"),
            getFileFromRoot("simulations/images/denisonGratingsV2/h_cw_2deg.png"),
        )
    )
    val blankFrameIndex = 4
    val background = DoubleArray(10000) { 0.0 }.toGrayScaleImage(100, 100)
    imageWorld.imageAlbum.addImage(background)

    val (attentionPlot, IASeries, VASeries, SensoryMeanSeries, SustainedMeanSeries, LateSensoryMeanSeries) = addTimeSeries(
        "Attention",
        seriesNames = listOf("Involuntary Attention", "Voluntary Attention", "Sensory 1 Mean", "Sensory 2 Mean", "Sensory 3 Mean")
    )
    attentionPlot.apply {
        model.isAutoRange = true
        model.fixedWidth = false
        useScalarCouplingNames = false
    }

    val (decisionPlot, Decision1Series, Decision2Series) = addTimeSeries("Evidence (CW + / CCW -)", seriesNames = listOf("Target 1", "Target 2"))
    decisionPlot.apply {
        model.isAutoRange = true
        model.fixedWidth = false
        useScalarCouplingNames = false
    }

    val IAPlot = couplingManager.createCoupling(iaLayer.getNeuron(0), IASeries)
    val VAPlot = couplingManager.createCoupling(vaLayer.getNeuron(0), VASeries)
    val Decision1Plot = couplingManager.createCoupling(decision.getNeuron(0), Decision1Series)
    val Decision2Plot = couplingManager.createCoupling(decision.getNeuron(1), Decision2Series)
    attentionPlot.model.renameTimeSeries(
        listOf("Involuntary Attention", "Voluntary Attention", "Sensory 1 Mean", "Sensory 2 Mean", "Sensory 3 Mean")
    )
    decisionPlot.model.renameTimeSeries(listOf("Target 1", "Target 2"))

    val m = 2 * sensory1.size - 1
    val tilt = 2 * PI / 180
    val grat_orientations = doubleArrayOf(PI / 2 + tilt, PI / 2 - tilt, tilt, PI - tilt)
    val stimulusConditions = listOf(
        intArrayOf(1, 0),
        intArrayOf(1, 1),
        intArrayOf(1, 2),
        intArrayOf(1, 3)
    )
    val stimSequences = (0 until grat_orientations.size).flatMap { t1 ->
        (0 until grat_orientations.size).map { t2 -> intArrayOf(t1, t2) }
    }
    val defaultStimSequence = stimulusConditions[3]

    fun randomStimSequence() = stimSequences.random()

    val w_i = Array(12) { i ->
        DoubleArray(grat_orientations.size) { j -> abs(cos(grat_orientations[j] + (i + 1) * PI / sensory1.size).pow(m)) }
    }

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

    var init_t = 500

    val maxHist = 500
    val s1History = mutableListOf<DoubleArray>()
    val s1HistoryLock = Any()
    val wFilter = denisonPrefilterKernel(dt)
    var trialPrepared = false
    val sweepFrames = mutableListOf<JInternalFrame>()

    val sweepSeriesNames = listOf("T1 valid", "T1 invalid", "T1 neutral", "T2 valid", "T2 invalid", "T2 neutral")
    val sweepConditionSeriesNames = listOf("valid", "invalid", "neutral")
    val sweepTrialsPerCondition = 1

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
        MenuOption("150", 150),
        MenuOption("200", 200),
        MenuOption("250", 250),
        MenuOption("300", 300),
        MenuOption("350", 350),
        MenuOption("400", 400),
        MenuOption("450", 450),
        MenuOption("500", 500),
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

    fun directionSign(targetIndex: Int) = if (targetIndex % 2 == 0) -1.0 else 1.0

    fun templateResponses(targetIndex: Int): Pair<DoubleArray, DoubleArray> {
        val firstTemplate = if (targetIndex < 2) 0 else 2
        return DoubleArray(12) { i -> w_i[i][firstTemplate] } to DoubleArray(12) { i -> w_i[i][firstTemplate + 1] }
    }

    fun decodeEvidence(response: DoubleArray, targetIndex: Int): Double {
        val (ccwTemplate, cwTemplate) = templateResponses(targetIndex)
        return response.indices.sumOf { i -> (cwTemplate[i] - ccwTemplate[i]) * response[i] }
    }

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
        sensory3.setActivations(DoubleArray(12) { 0.0 })
        vaLayer.setActivations(doubleArrayOf(0.0))
        iaLayer.setActivations(doubleArrayOf(0.0))
        synchronized(s1HistoryLock) {
            s1History.clear()
        }
        if (updateDisplay) {
            updateTrialStatusText()
            updateReportText(final = false)
            updateDecisionLabels()
            imageWorld.setFrame(blankFrameIndex)
        }
        modelTime = 0
        if (resetWorkspace) {
            workspace.resetTime()
            workspace.stop()
        }
    }

    fun excitatoryDrive(layer: DenisonLayer, n: Double = 1.5, c: Double = 0.64, b_VA: Double = 40.0, b_IA: Double = 8.5): DoubleArray {
        var drive = DoubleArray(12) { 0.0 }
        when (layer) {
            DenisonLayer.S1 -> {
                if (currentTarget >= 0) {
                    val a_i = halfWave(1 + b_VA * vaLayer.activationArray[0]) * halfWave(1 + b_IA * iaLayer.activationArray[0])
                    drive = DoubleArray(12) { i -> a_i * (w_i[i][currentTarget] * c).pow(n) }
                }
            }
            DenisonLayer.S2 -> drive = DoubleArray(12) { i -> sensory1.activations[i].pow(n) }
            DenisonLayer.S3 -> drive = DoubleArray(12) { i -> sensory2.activations[i].pow(n) }
            DenisonLayer.DECISION -> {
                drive = doubleArrayOf(0.0, 0.0)
                if (modelTime >= init_t / dt && modelTime < (init_t + SOA) / dt) {
                    drive[0] = decodeEvidence(sensory2.activationArray, T1)
                } else if (modelTime >= (init_t + SOA) / dt) {
                    drive[1] = decodeEvidence(sensory2.activationArray, T2)
                }
                drive = drive.map { if (abs(it) < 1e-3) 0.0 else it }.toDoubleArray()
            }
            DenisonLayer.VA -> {
                val t_VAOn = -34.0
                val t_VADur = 124.0
                val t1Active = modelTime >= (init_t + t_VAOn) / dt && modelTime < (init_t + t_VAOn + t_VADur) / dt
                val t2Active = modelTime >= (init_t + SOA + t_VAOn) / dt && modelTime < (init_t + SOA + t_VAOn + t_VADur) / dt
                val input = max(
                    if (t1Active) T1Allocation else 0.0,
                    if (t2Active) T2Allocation else 0.0
                )
                drive = doubleArrayOf(sensory1.size * denisonHalfExp(input, n))
            }
            DenisonLayer.IA -> {
                val historySnapshot = synchronized(s1HistoryLock) {
                    s1History.map { it.copyOf() }
                }
                if (historySnapshot.isNotEmpty()) {
                    val preOut = denisonPrefilter(historySnapshot, wFilter, n, dt, historySnapshot.size - 1)
                    val summed = preOut.sum()
                    drive = doubleArrayOf(summed)
                } else {
                    drive = doubleArrayOf(0.0)
                }
            }
        }
        return drive
    }

    fun calculateActivations(layer: DenisonLayer, c: Double = 0.64, n: Double = 1.5): DoubleArray {
        var r_tprev = DoubleArray(12) { 0.0 }
        var tau = 0.0
        var sigma = 0.0
        var normalizationPoolMultiplier = 1.0
        when (layer) {
            DenisonLayer.S1 -> { r_tprev = sensory1.activations.toDoubleArray(); tau = 52.0; sigma = 1.4 }
            DenisonLayer.S2 -> { r_tprev = sensory2.activations.toDoubleArray(); tau = 100.0; sigma = 0.1 }
            DenisonLayer.S3 -> { r_tprev = sensory3.activations.toDoubleArray(); tau = 2.0; sigma = 0.3 }
            DenisonLayer.DECISION -> { r_tprev = decision.activations.toDoubleArray(); tau = 1e5; sigma = 0.7 }
            DenisonLayer.VA -> {
                r_tprev = vaLayer.activations.toDoubleArray()
                tau = 50.0
                sigma = 20.0
                normalizationPoolMultiplier = sensory1.size.toDouble()
            }
            DenisonLayer.IA -> {
                r_tprev = iaLayer.activations.toDoubleArray()
                tau = 2.0
                sigma = 20.0
                normalizationPoolMultiplier = sensory1.size.toDouble()
            }
        }
        val e_t = excitatoryDrive(layer, c = c)
        val s_t = normalizationPoolMultiplier * e_t.sumOf { abs(it) }
        return DoubleArray(r_tprev.size) { i -> r_tprev[i] + dt / tau * (-r_tprev[i] + e_t[i] / (s_t + sigma.pow(n))) }
    }

    fun prepareTrial(updateDisplay: Boolean = true, stimulusSequence: IntArray = randomStimSequence()) {
        if (updateDisplay) {
            updateTrialStatusText()
        }
        T1 = stimulusSequence[0]
        T2 = stimulusSequence[1]
        reportTarget = if (reportMode == 0) Random.nextInt(1, 3) else reportMode

        val t_R = 918.0
        totalAttention = 1 + min(SOA.toDouble() / t_R, 1.0)

        if (vaState == 0) {
            val w_N = 0.5
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
        if (modelTime >= 2100 / dt) {
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
            if (updateDisplay) imageWorld.setFrame(blankFrameIndex)
        } else if (modelTime < (init_t + 30) / dt) {
            currentTarget = T1
            if (updateDisplay) imageWorld.setFrame(T1)
        } else if (modelTime < (init_t + SOA) / dt) {
            currentTarget = -1
            if (updateDisplay) imageWorld.setFrame(blankFrameIndex)
        } else if (modelTime < (init_t + SOA + 30) / dt) {
            currentTarget = T2
            if (updateDisplay) imageWorld.setFrame(T2)
        } else {
            currentTarget = -1
            if (updateDisplay) imageWorld.setFrame(blankFrameIndex)
        }

        val newS1 = calculateActivations(DenisonLayer.S1)
        sensory1.setActivations(newS1)
        val newS2 = calculateActivations(DenisonLayer.S2)
        sensory2.setActivations(newS2)
        val newS3 = calculateActivations(DenisonLayer.S3)
        sensory3.setActivations(newS3)
        val newDecision = calculateActivations(DenisonLayer.DECISION)
        decision.setActivations(newDecision)
        val newVA = calculateActivations(DenisonLayer.VA)
        vaLayer.setActivations(newVA)
        val newIA = calculateActivations(DenisonLayer.IA)
        iaLayer.setActivations(newIA)

        if (updateDisplay) {
            updateDecisionLabels()
        }

        if (updateWorkspace) {
            workspace.iterateSuspend()
        }
        modelTime += 1

        val stillRunning = modelTime < 2100 / dt
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
    suspend fun runTrialForProxy(cueState: Int, soaValue: Int, targetToReport: Int, stimulusSequence: IntArray = randomStimSequence()): Double {
        resetTrial(clearPlots = false, updateDisplay = false, resetWorkspace = false)
        vaState = cueState
        SOA = soaValue
        reportMode = targetToReport
        prepareTrial(updateDisplay = false, stimulusSequence = stimulusSequence)
        val maxHeadlessSteps = (2100 / dt).toInt() + 5
        var steps = 0
        while (steps < maxHeadlessSteps && stepTrial(updateDisplay = false, updateWorkspace = false)) {
            steps += 1
        }
        val targetIndex = if (targetToReport == 1) T1 else T2
        return finalEvidenceFor(targetToReport) * directionSign(targetIndex) * 1e5
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
            2 to 1,
            0 to 1,
            2 to 2,
            1 to 2,
            0 to 2
        )
        val totalSteps = SOADurs.size * conditions.size * trialsPerCondition * stimSequences.size
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
                        stimSequences.forEach { stimulusSequence ->
                            proxyValues.add(runTrialForProxy(cueState, soaOption.state, targetToReport, stimulusSequence))
                            completedSteps += 1
                            onProgress(
                                completedSteps,
                                totalSteps,
                                "SOA ${soaOption.state} ms, ${sweepSeriesNames[seriesIndex]}: $completedSteps / $totalSteps"
                            )
                        }
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

    sensory1.setActivations(DoubleArray(12) { 0.0 })
    sensory2.setActivations(DoubleArray(12) { 0.0 })
    sensory3.setActivations(DoubleArray(12) { 0.0 })

    withGui {
        trialStatusText.location = point(250.0, -200.0)
        finalReportStatusText.location = point(250.0, -250.0)
        updateTrialStatusText()
        updateReportText(final = false)
        updateDecisionLabels()

        sensory1.location = point(-35.0, 100.0)
        sensory2.location = point(165.0, 100.0)
        sensory3.location = point(365.0, 100.0)
        decision.location = point(575.0, 100.0)
        vaLayer.location = point(44.0, -97.0)
        iaLayer.location = point(370.0, -97.0)

        fun makeSweepFrame(title: String, model: TimeSeriesModel, x: Int, y: Int) =
            JInternalFrame(title, true, true, true, true).apply {
                layout = BorderLayout()
                val plotPanel = TimeSeriesPlotPanel(model).apply {
                    chartPanel.chart.xyPlot.domainAxis.label = "SOA"
                    chartPanel.chart.xyPlot.rangeAxis.label = "Sensitivity (d′ proxy)"
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
                makeSweepFrame("Sensitivity (d′ proxy) by SOA — T1", t1Model, 60, 120),
                makeSweepFrame("Sensitivity (d′ proxy) by SOA — T2", t2Model, 670, 120)
            )
            frames.forEach {
                sweepFrames.add(it)
                addInternalFrame(it)
                it.isIcon = false
                it.toFront()
                it.isSelected = true
            }
        }

        val controlPanel = createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addLabel("Attention Cue:").apply {
                toolTipText = "Choose the cue that initiates voluntary attention before the targets appear."
            }
            addComboBox("", cueTypes, cueTypes[0]) { selectedCue ->
                vaState = selectedCue.state
                updateTrialStatusText()
            }.apply { toolTipText = "Select which target time receives voluntary attention." }
            addLabel("Report Target").apply {
                toolTipText = "Choose which target's evidence is used for the final report."
            }
            addComboBox("", reportTargets, reportTargets[0]) { selectedReportTarget ->
                reportMode = selectedReportTarget.state
                reportTarget = if (reportMode == 0) reportTarget else reportMode
                updateTrialStatusText()
            }.apply { toolTipText = "Select which target's evidence is reported after the trial." }
            addSeparator()
            addLabel("SOA").apply {
                toolTipText = "Stimulus onset asynchrony: the delay between the first and second target."
            }
            addComboBox("", SOADurs, SOADurs[4]) {selectedSOA ->
                SOA = selectedSOA.state
            }.apply { toolTipText = "Set the delay, in milliseconds, between Target 1 and Target 2." }
            addSeparator()
            suspend fun JButton.runSweepAction() {
                val sweepButton = this
                val cancelRequested = AtomicBoolean(false)
                val totalSteps = SOADurs.size * sweepSeriesNames.size * sweepTrialsPerCondition * stimSequences.size
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

            workspace.updater.updateManager.clear()
            workspace.updater.updateManager.addAction(updateAction("Temporal attention step") {
                stepTrial(updateDisplay = true, updateWorkspace = false)
            })
            workspace.updater.updateManager.addAction(updateAction("Sensory-stage means") {
                SensoryMeanSeries.setValue(sensory1.activationArray.average())
                SustainedMeanSeries.setValue(sensory2.activationArray.average())
                LateSensoryMeanSeries.setValue(sensory3.activationArray.average())
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
            }.apply { toolTipText = "Reset the state and run one trial with the selected settings." }
            addButton("Step 10") {
                if (workspace.updater.isRunning) return@addButton
                if (!trialPrepared) {
                    resetTrial()
                    prepareTrial()
                }
                workspace.iterateSuspend(10)
            }.apply { toolTipText = "Advance the prepared trial by 10 simulation steps." }
            addButton("Reset") {
                workspace.stop()
                resetTrial()
            }.apply { toolTipText = "Stop the simulation and return the network and plots to baseline." }
            addSeparator()
            addButton("Run SOA Sweep") {
                runSweepAction()
            }.apply {
                toolTipText = "Run every paper stimulus condition across SOAs and plot the d′-like sensitivity proxy."
            }
        }.awaitLayout()
        val gridX = controlPanel.rightEdgeWithGap()
        place(netComponent, gridX, SIM_WINDOW_GAP, 700, 447)
        place(imageWorldComponent, gridX + 700 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 503, 447)
        place(attentionPlot, gridX, SIM_WINDOW_GAP + 447 + SIM_WINDOW_GAP, 500, 300)
        place(decisionPlot, gridX + 500 + SIM_WINDOW_GAP, SIM_WINDOW_GAP + 447 + SIM_WINDOW_GAP, 500, 300)
    }

addSidebarInfo(
    """
    # Temporal Attention Model

    This interactive neural network model brings the temporal-attention model from Denison et al.'s ["A dynamic normalization model of temporal attention"](https://www.nature.com/articles/s41562-021-01129-1) to life. Run or step through trials, watch the stimulus, sensory responses, attention traces, and decision evidence unfold, and experiment with timing and cues to develop an intuitive feel for the model.

    In Denison et al.'s experiment, participants viewed two tilted grating targets in sequence: Target 1 (`T1`) and Target 2 (`T2`). Each was rotated clockwise or counterclockwise. An auditory cue appeared before the targets and initiated voluntary attention to `T1`, `T2`, or both target times. Participants then reported the rotation direction for one target.

    ## Model and attention

    The model contains six processing layers.

    - `Sensory 1`: 12 orientation-tuned neurons. The degree labels are their receptive-field preferences, not the randomly selected stimulus orientation.
    - `Sensory 2`: a slower, sustained transformation of Sensory 1. The current decision decoder reads this stage.
    - `Sensory 3`: the next, late-normalized stage after Sensory 2. It is calculated and plotted so you can observe the full sensory cascade from the paper architecture, but it is currently a terminal display stage: nothing downstream reads it, so it does not affect the decision or sweep results.
    - `Decision evidence`: two target-specific decisions, one for each target time. The `T1` node reports the evidence for Target 1 and the `T2` node reports the evidence for Target 2; positive values mean clockwise and negative values mean counterclockwise. **The labels inside these two network nodes show the current decision directly:** for example, `T1: clockwise` means the current Target 1 evidence is positive.
    - `Voluntary Attention`: the temporally targeted gain signal initiated by the cue. A `T1` or `T2` cue favors that target time, and a neutral cue divides the available resource.
    - `Involuntary Attention`: a stimulus-driven gain signal computed from recent Sensory 1 activity. It rises after a grating onset regardless of the cue.

    Both attention systems modulate Sensory 1 gain: higher gain makes its orientation-tuned neurons more responsive to the same grating input. The `Attention` plot shows the two attention signals plus the mean activity of all three sensory stages. Read it as a sequence: a target drives Sensory 1, activity propagates to Sensory 2 and Sensory 3, and Sensory 2 supplies target-specific decision evidence.

    ## Control panel

    `Attention Cue` selects the instruction that initiates voluntary attention:

    - `Both Targets`: divides voluntary attention across the two target times (neutral).
    - `Target 1`: prioritizes the first target (`T1`).
    - `Target 2`: prioritizes the second target (`T2`).

    `SOA` is the stimulus onset asynchrony: the delay, in milliseconds, between `T1` and `T2`. Short SOAs create stronger competition for limited voluntary attention; longer SOAs allow it to recover.

    `Report Target` selects the target whose final evidence is evaluated. `Random` chooses one on each trial. A trial is `valid` when cue and report target match, `invalid` when the cue favors the other target, and `neutral` when the cue is `Both Targets`.

    - `T1 valid`: cue `Target 1`; report `Target 1`.
    - `T1 invalid`: cue `Target 2`; report `Target 1`.
    - `T2 valid`: cue `Target 2`; report `Target 2`.
    - `T2 invalid`: cue `Target 1`; report `Target 2`.
    - `Neutral`: cue `Both Targets`; report `Random`.

    `Start` runs one trial. `Step 10` advances a prepared trial by ten workspace iterations. `Reset` stops the simulation and returns the network and plots to baseline.

    `Run SOA Sweep` evaluates every paper stimulus condition across the available SOAs and opens `T1` and `T2` plots. The curves show signed correct decision evidence, averaged across the four stimulus conditions, as a `d′`-like sensitivity proxy: positive values indicate evidence in the correct direction, values near zero are ambiguous, and negative values indicate evidence in the wrong direction. This is not formal behavioral `d′`, which requires hit and false-alarm rates; it is a model-level measure that lets you directly inspect the paper's SOA-dependent qualitative effects.

    <img src="//localfiles/simulations/images/denison/denison-soa-results.png" width="300" alt="Denison et al.'s SOA performance results" />

    Denison et al.'s result has a distinctive shape: `T1` performance rises with SOA and then levels off, while `T2` shows an early dip before recovering. The sweep recreates this broad qualitative pattern, along with the separation between valid, neutral, and invalid cue conditions. Compare the two sweep windows to the figure rather than treating their evidence scale as a direct numerical reproduction of behavioral `d′`.

    ## What to do

    **Simulate an individual trial.** Select an `Attention Cue`, `SOA`, and `Report Target`, then click `Start`. Change `SOA` between trials to see how the gap between targets affects attention allocation and decision evidence. Use `Step 10` repeatedly to watch the trial unfold and observe each event along this timeline:

    ```text
    0 ms        500 ms      530 ms          500 + SOA ms     530 + SOA ms
    |-----------|===========|---------------|================|----------->
     blank       T1 shown    blank delay     T2 shown         response tails
    ```

    Watch Sensory 1 respond at each grating onset, Sensory 2 sustain that response, Sensory 3 show later normalization, and the decision-evidence nodes accumulate signed tilt information.

    **Replicate paper conditions with an SOA sweep.** Use `Run SOA Sweep` to evaluate every paper stimulus condition directly and compare valid, neutral, and invalid curves. Look for voluntary-attention tradeoffs, SOA-dependent `T1` sensitivity, and an attentional-blink-like dip for `T2`.

    For better performance during a trial or SOA sweep, minimize the `Attention` and `Evidence (CW + / CCW -)` windows.

    ## Reference

    Denison, R. N., Carrasco, M., & Heeger, D. J. (2021). [_A dynamic normalization model of temporal attention_](https://www.nature.com/articles/s41562-021-01129-1). _Nature Human Behaviour_, _5_(12), 1674–1685.

    ## Credits

    [Jensen Guo](https://www.linkedin.com/in/jensen-guo/)

    [Jeff Yoshimi](https://www.jeffyoshimi.net)
    """.trimIndent()
)
}

enum class DenisonLayer {S1, S2, S3, DECISION, VA, IA}

fun denisonHalfExp(value: Double, exponent: Double): Double = max(0.0, value).pow(exponent)

fun denisonGamma(value: Double): Double {
    val coefficients = doubleArrayOf(
        676.5203681218851,
        -1259.1392167224028,
        771.32342877765313,
        -176.61502916214059,
        12.507343278686905,
        -0.13857109526572012,
        9.9843695780195716e-6,
        1.5056327351493116e-7
    )
    if (value < 0.5) return PI / (sin(PI * value) * denisonGamma(1 - value))
    var x = 0.99999999999980993
    val adjusted = value - 1
    coefficients.forEachIndexed { index, coefficient ->
        x += coefficient / (adjusted + index + 1)
    }
    val t = adjusted + coefficients.size - 0.5
    return sqrt(2 * PI) * t.pow(adjusted + 0.5) * exp(-t) * x
}

fun denisonGammaKernel(x: Double, shape: Double, scale: Double, amplitude: Double = 1.0): Double {
    if (shape.isNaN() || scale.isNaN()) return 0.0
    return amplitude * x.pow(shape - 1) * exp(-x / scale) / (denisonGamma(shape) * scale.pow(shape))
}

fun denisonPrefilterKernel(dt: Double): DoubleArray {
    val posShape = 2.2
    val posScale = 0.023
    val negShape = Double.NaN
    val negScale = Double.NaN
    val ampNeg = 0.0
    return DoubleArray((0.8 / (dt / 1000.0)).roundToInt() + 1) { i ->
        val x = i * dt / 1000.0
        denisonGammaKernel(x, posShape, posScale) - denisonGammaKernel(x, negShape, negScale, ampNeg)
    }
}

fun denisonPrefilter(
    xHistory: List<DoubleArray>,
    w: DoubleArray,
    n: Double,
    dt: Double,
    idx: Int
): DoubleArray {
    val phw = doubleArrayOf(1.0, -1.0)
    val numNeurons = xHistory.firstOrNull()?.size ?: return DoubleArray(0)
    val y = Array(numNeurons) { DoubleArray(w.size) { Double.NaN } }

    for (i in 0 until numNeurons) {
        for (j in w.indices) {
            val historyRow = xHistory.getOrNull(idx - w.size + j)
            if (historyRow != null && i < historyRow.size) y[i][j] = historyRow[i]
        }
    }

    val inp = Array(numNeurons) { DoubleArray(2) }
    for (phase in 0..1) {
        val filtered = DoubleArray(numNeurons)
        for (i in 0 until numNeurons) {
            var convolution = 0.0
            for (j in w.indices) {
                val xValue = y[i][j]
                if (!xValue.isNaN()) convolution += xValue * w[w.lastIndex - j] * phw[phase]
            }
            filtered[i] = max(0.0, convolution * dt).pow(n)
        }
        for (i in 0 until numNeurons) inp[i][phase] = filtered[i]
    }

    return DoubleArray(numNeurons) { i -> inp[i][0] - inp[i][1] }
}
