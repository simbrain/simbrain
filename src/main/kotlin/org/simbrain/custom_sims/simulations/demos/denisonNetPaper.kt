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

enum class DenisonPaperLayer {S1, S2, S3, DECISION, VA, IA}

fun denisonPaperHalfExp(value: Double, exponent: Double): Double = max(0.0, value).pow(exponent)

fun denisonPaperGamma(value: Double): Double {
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
    if (value < 0.5) {
        return PI / (sin(PI * value) * denisonPaperGamma(1 - value))
    }
    var x = 0.99999999999980993
    val adjusted = value - 1
    coefficients.forEachIndexed { index, coefficient ->
        x += coefficient / (adjusted + index + 1)
    }
    val t = adjusted + coefficients.size - 0.5
    return sqrt(2 * PI) * t.pow(adjusted + 0.5) * exp(-t) * x
}

fun denisonPaperGammaKernel(x: Double, shape: Double, scale: Double, amplitude: Double = 1.0): Double {
    if (shape.isNaN() || scale.isNaN()) {
        return 0.0
    }
    return amplitude * x.pow(shape - 1) * exp(-x / scale) / (denisonPaperGamma(shape) * scale.pow(shape))
}

fun denisonPaperPrefilterKernel(dt: Double): DoubleArray {
    val posShape = 2.2
    val posScale = 0.023
    val negShape = Double.NaN
    val negScale = Double.NaN
    val ampNeg = 0.0
    return DoubleArray((0.8 / (dt / 1000.0)).roundToInt() + 1) { i ->
        val x = i * dt / 1000.0
        denisonPaperGammaKernel(x, posShape, posScale) -
            denisonPaperGammaKernel(x, negShape, negScale, ampNeg)
    }
}

fun denisonPaperPrefilter(
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
            val t = idx - w.size + j
            val historyRow = xHistory.getOrNull(t)
            if (historyRow != null && i < historyRow.size) {
                y[i][j] = historyRow[i]
            }
        }
    }

    val inp = Array(numNeurons) { DoubleArray(2) }
    for (phase in 0..1) {
        val filtered = DoubleArray(numNeurons)
        for (i in 0 until numNeurons) {
            var convSum = 0.0
            for (j in w.indices) {
                val xVal = y[i][j]
                if (!xVal.isNaN()) {
                    convSum += xVal * w[w.lastIndex - j] * phw[phase]
                }
            }
            val rectified = max(0.0, convSum * dt).pow(n)
            filtered[i] = rectified
        }
        for (i in 0 until numNeurons) inp[i][phase] = filtered[i]
    }

    return DoubleArray(numNeurons) { i -> inp[i][0] - inp[i][1] }
}

val denisonNetPaper = newSim {
    workspace.clearWorkspace()
    val netComponent = addNetworkComponent("Denison Paper Net")
    val net = netComponent.network

    val trialStatusText = NetworkTextObject("").apply { fontSize = 18 }
    val finalReportStatusText = NetworkTextObject("").apply { fontSize = 18 }
    val rfLabels = (1..12).map { "${((180 - it * 15) % 180)}°" }

    val sensory1 = net.addNeuronCollection(12).apply {
        label = "Sensory"
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
    val decision = net.addNeuronCollection(2).apply { label = "Decisions" }.apply {
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
    val tilt = 2 * PI / 180
    val grat_orientations = doubleArrayOf(PI / 2 + tilt, PI / 2 - tilt, tilt, PI - tilt)
    val paperStimSequences = listOf(
        intArrayOf(1, 0),
        intArrayOf(1, 1),
        intArrayOf(1, 2),
        intArrayOf(1, 3)
    )
    val stimSequences = (0 until grat_orientations.size).flatMap { t1 ->
        (0 until grat_orientations.size).map { t2 -> intArrayOf(t1, t2) }
    }
    val defaultStimSequence = paperStimSequences[3]

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
    val wFilter = denisonPaperPrefilterKernel(dt)
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

    fun excitatoryDrive(layer: DenisonPaperLayer, n: Double = 1.5, c: Double = 0.64, b_VA: Double = 40.0, b_IA: Double = 8.5): DoubleArray {
        var drive = DoubleArray(12) { 0.0 }
        when (layer) {
            DenisonPaperLayer.S1 -> {
                if (currentTarget >= 0) {
                    val a_i = halfWave(1 + b_VA * vaLayer.activationArray[0]) * halfWave(1 + b_IA * iaLayer.activationArray[0])
                    drive = DoubleArray(12) { i -> a_i * (w_i[i][currentTarget] * c).pow(n) }
                }
            }
            DenisonPaperLayer.S2 -> drive = DoubleArray(12) { i -> sensory1.activations[i].pow(n) }
            DenisonPaperLayer.S3 -> drive = DoubleArray(12) { i -> sensory2.activations[i].pow(n) }
            DenisonPaperLayer.DECISION -> {
                drive = doubleArrayOf(0.0, 0.0)
                if (modelTime >= init_t / dt && modelTime < (init_t + SOA) / dt) {
                    drive[0] = decodeEvidence(sensory2.activationArray, T1)
                } else if (modelTime >= (init_t + SOA) / dt) {
                    drive[1] = decodeEvidence(sensory2.activationArray, T2)
                }
                drive = drive.map { if (abs(it) < 1e-3) 0.0 else it }.toDoubleArray()
            }
            DenisonPaperLayer.VA -> {
                val t_VAOn = -34.0
                val t_VADur = 124.0
                val t1Active = modelTime >= (init_t + t_VAOn) / dt && modelTime < (init_t + t_VAOn + t_VADur) / dt
                val t2Active = modelTime >= (init_t + SOA + t_VAOn) / dt && modelTime < (init_t + SOA + t_VAOn + t_VADur) / dt
                val input = max(
                    if (t1Active) T1Allocation else 0.0,
                    if (t2Active) T2Allocation else 0.0
                )
                drive = doubleArrayOf(sensory1.size * denisonPaperHalfExp(input, n))
            }
            DenisonPaperLayer.IA -> {
                val historySnapshot = synchronized(s1HistoryLock) {
                    s1History.map { it.copyOf() }
                }
                if (historySnapshot.isNotEmpty()) {
                    val preOut = denisonPaperPrefilter(historySnapshot, wFilter, n, dt, historySnapshot.size - 1)
                    val summed = preOut.sum()
                    drive = doubleArrayOf(summed)
                } else {
                    drive = doubleArrayOf(0.0)
                }
            }
        }
        return drive
    }

    fun calculateActivations(layer: DenisonPaperLayer, c: Double = 0.64, n: Double = 1.5): DoubleArray {
        var r_tprev = DoubleArray(12) { 0.0 }
        var tau = 0.0
        var sigma = 0.0
        var normalizationPoolMultiplier = 1.0
        when (layer) {
            DenisonPaperLayer.S1 -> { r_tprev = sensory1.activations.toDoubleArray(); tau = 52.0; sigma = 1.4 }
            DenisonPaperLayer.S2 -> { r_tprev = sensory2.activations.toDoubleArray(); tau = 100.0; sigma = 0.1 }
            DenisonPaperLayer.S3 -> { r_tprev = sensory3.activations.toDoubleArray(); tau = 2.0; sigma = 0.3 }
            DenisonPaperLayer.DECISION -> { r_tprev = decision.activations.toDoubleArray(); tau = 1e5; sigma = 0.7 }
            DenisonPaperLayer.VA -> {
                r_tprev = vaLayer.activations.toDoubleArray()
                tau = 50.0
                sigma = 20.0
                normalizationPoolMultiplier = sensory1.size.toDouble()
            }
            DenisonPaperLayer.IA -> {
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

        val newS1 = calculateActivations(DenisonPaperLayer.S1)
        sensory1.setActivations(newS1)
        val newS2 = calculateActivations(DenisonPaperLayer.S2)
        sensory2.setActivations(newS2)
        val newS3 = calculateActivations(DenisonPaperLayer.S3)
        sensory3.setActivations(newS3)
        val newDecision = calculateActivations(DenisonPaperLayer.DECISION)
        decision.setActivations(newDecision)
        val newVA = calculateActivations(DenisonPaperLayer.VA)
        vaLayer.setActivations(newVA)
        val newIA = calculateActivations(DenisonPaperLayer.IA)
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
        sensory2.location = point(180.0, 100.0)
        sensory3.location = point(370.0, 100.0)
        decision.location = point(560.0, 100.0)
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
            addComboBox("", SOADurs, SOADurs[4]) {selectedSOA ->
                SOA = selectedSOA.state
            }
            addSeparator()
            addButton("Run SOA Sweep") {
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
            addSeparator()

            workspace.updater.updateManager.clear()
            workspace.updater.updateManager.addAction(updateAction("Temporal attention step") {
                stepTrial(updateDisplay = true, updateWorkspace = false)
            })
            workspace.updater.updateManager.addAction(updateAction("Sensory/sustained means") {
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

        A neural network model of visual attention based on the paper ["A dynamic normalization model of temporal attention"](https://www.nature.com/articles/s41562-021-01129-1) by Rachel Denison, Marisa Carrasco, and David Heeger. The simulation demonstrates how voluntary and involuntary attention interact to detect and classify briefly presented visual patterns.

        # Simulation Details

        In Denison's experiment, participants viewed two tilted grating targets presented one after the other: Target 1 (`T1`) and Target 2 (`T2`). Each target was rotated either clockwise or counterclockwise. Before the targets appeared, participants received an auditory cue telling them to pay attention to `T1`, `T2`, or both target times. After viewing the targets, they were asked to report the rotation direction for one target or the other, depending on the trial.

        The key finding: When participants were cued to attend to a specific target and then asked to report on that same target, their response times were faster and more accurate than when cued to one target but asked about the other. This demonstrates that voluntary attention (the cue) enhances perceptual processing of attended stimuli.

        ## Theoretical Background

        The paper's main behavioral measure is perceptual sensitivity, or `d'`. A quick way to think about `d'` is "how separable are signal and noise?" If you can reliably tell a real phone buzz from background vibration, spot a faint star against visual noise, or distinguish two similar wines, sensitivity is high. If not, sensitivity is low.

        In this simulation, the analogous question is how strongly the model's final evidence separates clockwise from counterclockwise for the reported target. Strong evidence in the correct direction is like higher sensitivity; weak or ambiguous evidence is like lower sensitivity. This Simbrain model does not compute behavioral `d'` directly, but instead uses a sensitivity proxy based on signed correct evidence: negative values mean the final evidence points the wrong way, values near zero are ambiguous, and larger positive values mean stronger evidence in the correct direction.
        
        This copy is closer to the paper model than the original Simbrain demo: it uses the paper's four orientation conditions, SOA list, three sensory stages, gamma-shaped involuntary attention prefilter, balanced neutral cue allocation, template-based decision decoding, and absolute-value normalization pool. It still uses the existing Simbrain visualization and a sensitivity proxy rather than a full behavioral `d'` fitting pipeline.

        ## Control Panel Settings

        `Attention Cue` sets how voluntary attention is allocated before the two targets appear:

        - `Both Targets`: Divides attention across the two target times, similar to the neutral cue condition in the experiment. On screen this appears as `Cue: attend both targets`.
        - `Target 1`: Prioritizes the first target (`T1`), producing stronger voluntary gain around the first stimulus. On screen this appears as `Cue: attend Target 1 (T1)`.
        - `Target 2`: Prioritizes the second target (`T2`), producing stronger voluntary gain around the second stimulus. On screen this appears as `Cue: attend Target 2 (T2)`.

        `SOA` sets the stimulus onset asynchrony: the delay, in milliseconds, between the first and second target. Short SOAs create stronger competition for the limited voluntary attention resource; longer SOAs allow voluntary attention to recover before the second target.

        `Report Target` sets which target the model is asked to report after the two targets appear. `Random` chooses either `T1` or `T2` for each trial. A trial is `valid` when the attention cue matches the report target, `invalid` when it cues the other target, and `neutral` when the cue is `Both Targets`.

        `Start` runs one trial with randomly selected orientations for `T1` and `T2`. While a trial is running it has no effect; use the workspace `Stop` control to halt the run, or `Reset` to clear the network state and plots before starting a new trial.

        `Step 10` advances the current trial by 10 workspace iterations. If no trial is prepared, it starts a new trial using the current cue and `SOA` settings and then advances 10 iterations.

        `Reset` cancels any running trial and returns the model to a blank baseline state.

        `Run SOA Sweep` runs a small batch of trials for every `SOA`, report target, and validity condition, showing progress in a separate window with a `Cancel` button. When the sweep completes, it opens two `Sensitivity Proxy by SOA` popups side by side, one for `T1` and one for `T2`. Each plots the `valid`, `neutral`, and `invalid` curves against `SOA` (x-axis) and the sensitivity proxy (y-axis). For a clockwise target, positive decision activation counts as correct; for a counterclockwise target, negative decision activation is flipped so it also counts as correct. Larger values in the appropriate direction are stronger correct evidence and thus are a proxy for d'.

        Two internal target-specific decisions are made, but only one behavioral report would be requested in the experiment. In this simulation, the two decision nodes are best read as evidence traces for what the model would say if asked about `T1` or `T2`.

        The model consists of six layers that process visual input and make decisions.

        - `Sensory 1`: 12 neurons, each tuned to a different orientation (0°, 30°, 60°, etc.). When a grating pattern appears, the neuron matching that orientation activates most strongly, with nearby orientations showing weaker responses.

        - `Sensory 2`: 12 neurons that maintain prolonged activation after the first sensory layer responds. These activations are decoded by the decisions layer in the default paper model.

        - `Sensory 3`: A third normalized sensory stage included to match the paper model architecture and support late-combination variants.

        - `Decisions Layer`: 2 neurons that accumulate evidence for each target. Positive activation indicates clockwise rotation, negative indicates counterclockwise. The strength of activation reflects the model's confidence.

        - `Voluntary Attention`: A single neuron controlled by the attention cue selected in the control panel. It enhances processing in the sensory layer based on which target is cued.

        - `Involuntary Attention`: A single neuron driven by the stimulus itself. It responds automatically to pattern onsets regardless of the cue, and also enhances processing in the sensory layer.

        ## How Attention Works in the Model

        Both attention systems modulate the gain of the sensory layer: they multiply the overall activation level. Voluntary attention is determined by your cue selection. Involuntary attention is stimulus-driven and responds to any visual pattern onset.

        # What to Do

        ## Run a Trial

        1. Select an `Attention Cue`, `SOA`, and `Report Target` from the control panel.
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

        `Attention Dynamics`: The `Attention` time series plot shows how voluntary and involuntary attention unfold over time. Notice:
        - When involuntary attention spikes (at pattern onsets)
        - How voluntary attention is allocated to one target or the other based on the cue you selected
        - The interaction between the two attention systems

        `Evidence Formation`: The `Evidence (CW + / CCW -)` time series plot shows how the two decision neurons accumulate evidence for each target over time. Positive activation indicates clockwise rotation, negative indicates counterclockwise. The decision node labels also show the current evidence direction directly as `T1: clockwise` or `T2: counterclockwise`.
        
        `Behavior`: Compact text at the top of the network shows the current cue, report target, and validity. At the end of a trial, a second line shows the final clockwise/counterclockwise report. The exact randomly selected target orientations are intentionally not shown as text, so the focus stays on the network activity, image display, and evidence traces.

        `Sensitivity Proxy by SOA`: Click `Run SOA Sweep` to run the sweep and open two popups side by side, one for `T1` and one for `T2`. In each, compare the valid, neutral, and invalid curves, with `SOA` on the x-axis and the sensitivity proxy on the y-axis. Higher values mean the model's final evidence is farther in the correct direction for the reported target. The goal is to look for qualitative signatures from the paper, such as voluntary attentional tradeoffs, larger cueing effects at intermediate SOAs, masking-like improvement for `T1`, or an attentional-blink-like dip for `T2`. The current plots are a model evidence proxy, not a direct reproduction of the paper's behavioral `d'` values.

        ## Experiment

        1. Try different attention cues and observe how they affect:
            - The evidence trace activation strengths (model confidence)
            - The voluntary attention trace in the `Attention` plot
            - What the model would report if asked about each target

        2. Run multiple trials with the same cue to see how random variation in stimulus patterns affects the model's performance.

        3. Try different `SOA` (stimulus onset asynchrony) values to see how the timing between the two targets affects attention allocation and evidence strength.

        4. Click `Run SOA Sweep` and compare the sensitivity proxy curves for valid, neutral, and invalid trials in the side-by-side `T1` and `T2` plots.

        ## Performance Tip

        For better performance during trials or the SOA sweep, minimize the time series windows (`Attention` and `Evidence (CW + / CCW -)`). The simulation will run faster when these plots are not actively rendering.

        # References

        Denison, R. N., Carrasco, M., & Heeger, D. J. (2021). [_A dynamic normalization model of temporal attention_](https://www.nature.com/articles/s41562-021-01129-1). _Nature Human Behaviour_, _5_(12), 1674–1685.

        # Credits

        [Jensen Guo](https://www.linkedin.com/in/jensen-guo/)
        
        [Jeff Yoshimi](https://www.jeffyoshimi.net)

        """.trimIndent()
    )
}
