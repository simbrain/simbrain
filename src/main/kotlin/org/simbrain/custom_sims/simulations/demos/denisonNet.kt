package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import org.simbrain.custom_sims.*
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.setLabels
import org.simbrain.util.*
import org.simbrain.workspace.updater.UpdateCoupling
import kotlin.math.*
import kotlin.random.Random

enum class Layer {S1, S2, DECISION, VA, IA}

// ------------------------------------------
// MATLAB-style prefilter implementation
// ------------------------------------------
fun prefilter(
    xHistory: List<DoubleArray>, // past activations (each entry = S1 activations at a timestep)
    w: DoubleArray,              // temporal kernel
    n: Double,                   // exponent for nonlinearity
    dt: Double,                  // time step (seconds)
    idx: Int                     // current time index
): DoubleArray {
    val phw = doubleArrayOf(1.0, -1.0)
    val numNeurons = xHistory[0].size
    val y = Array(numNeurons) { DoubleArray(w.size) { Double.NaN } }

    // Build temporal window
    for (i in 0 until numNeurons) {
        for (j in w.indices) {
            val t = idx - w.size + j
            if (t in xHistory.indices) y[i][j] = xHistory[t][i]
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

// ------------------------------------------
// Main simulation definition
// ------------------------------------------
val denisonNet = newSim {
    workspace.clearWorkspace()
    val netComponent = addNetworkComponent("Denison Net")
    val net = netComponent.network

    val t1Stimulus = NetworkTextObject("").apply { fontSize = 18 }
    val t1Decision = NetworkTextObject("").apply { fontSize = 18 }
    val t2Stimulus = NetworkTextObject("").apply { fontSize = 18 }
    val t2Decision = NetworkTextObject("").apply { fontSize = 18 }

    val sensory1 = net.addNeuronCollection(12).apply {
        label = "Sensory"
        setLabels((0..11).map { "${it * 30}°" })
    }
    val sensory2 = net.addNeuronCollection(12).apply {
        label = "Sustained Response"
        setLabels((0..11).map { "${it * 30}°" })
    }
    val decision = net.addNeuronCollection(2).apply { label = "Decision" }.apply {
        setLabels(listOf("Pattern 1", "Pattern 2"))
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

    net.addNetworkModels(sensory1, sensory2, decision, vaLayer, iaLayer, t1Stimulus, t1Decision, t2Stimulus, t2Decision)

    val imageWorldComponent = addImageWorld("Gratings")
    val imageWorld = imageWorldComponent.world
    imageWorld.loadImages(getFilesWithExtension("simulations/images/denisonGratings", "png"))
    val background = DoubleArray(10000) { 0.0 }.toGrayScaleImage(100, 100)
    imageWorld.imageAlbum.addImage(background)

    val (attentionPlot, IASeries, VASeries) = addTimeSeries("Attention", seriesNames = listOf("Involuntary Attention", "Voluntary Attention"))
    attentionPlot.apply {
        model.isAutoRange = true
        model.fixedWidth = false
    }

    val (decisionPlot, Decision1Series, Decision2Series) = addTimeSeries("Decisions", seriesNames = listOf("Pattern 1", "Pattern 2"))
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
    var reportTarget = 1
    var totalAttention = 0.0
    var T1Allocation = 0.0
    var T2Allocation = 0.0
    val dt = 2.0

    var init_t = 200 //default is 1000

    // rolling S1 history and temporal kernel for IA layer
    val maxHist = 50
    val s1History = mutableListOf<DoubleArray>()
    val wFilter = DoubleArray(15) { i -> exp(-i / 5.0) } // temporal kernel

    fun halfWave(n: Double): Double = max(0.0, n)

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
                if (workspace.time >= init_t / dt && workspace.time < (init_t + 30 + SOA) / dt) {
                    drive[0] = w_D.zip(sensory2.activationArray) { x, y -> x * y }.sum()
                } else if (workspace.time >= (init_t + 30 + SOA) / dt) {
                    drive[1] = w_D.zip(sensory2.activationArray) { x, y -> x * y }.sum()
                }
            }
            Layer.VA -> {
                val t_VAOn = -34.0
                val t_VADur = 124.0
                drive = when {
                    workspace.time >= (init_t + t_VAOn) / dt && workspace.time < (init_t + t_VAOn + t_VADur) / dt ->
                        doubleArrayOf(T1Allocation.pow(n))
                    workspace.time >= (init_t + 30 + SOA + t_VAOn) / dt && workspace.time < (init_t + 30 + SOA + t_VAOn + t_VADur) / dt ->
                        doubleArrayOf(T2Allocation.pow(n))
                    else -> doubleArrayOf(0.0)
                }
            }
            Layer.IA -> {
                if (s1History.isNotEmpty()) {
                    val preOut = prefilter(s1History, wFilter, n, dt, s1History.size - 1)
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
        place(attentionPlot, 80, 450, 500, 300)
        place(decisionPlot, 600, 450, 500, 300)

        // Text positioning variables for easy adjustment
        var textOffsetX = 0.0
        var textOffsetY = -240.0
        t1Stimulus.location = point(textOffsetX, textOffsetY)
        t1Decision.location = point(textOffsetX + 400, textOffsetY)
        t2Stimulus.location = point(textOffsetX, textOffsetY - 40)
        t2Decision.location = point(textOffsetX + 400, textOffsetY - 40)

        sensory1.location = point(-10.0, 100.0)
        sensory2.location = point(230.0, 100.0)
        decision.location = point(440.0, 100.0)
        vaLayer.location = point(44.0, -97.0)
        iaLayer.location = point(370.0, -97.0)

        // Define cue types
        data class menuMap(val name: String, val state: Int) {
            override fun toString() = name
        }

        val cueTypes = listOf(
            menuMap("Both", 0),
            menuMap("Pattern 1", 1),
            menuMap("Pattern 2", 2)
        )

        val SOADurs = listOf(
            menuMap("100", 100),
            menuMap("200", 200),
            menuMap("300", 300),
            menuMap("400", 400),
            menuMap("500", 500),
            menuMap("600", 600),
            menuMap("700", 700),
            menuMap("800", 800),
        )

        createControlPanel("Control Panel", 15, 15) {
            addLabel("Attention Cue:")
            addComboBox("", cueTypes, cueTypes[0]) { selectedCue ->
                vaState = selectedCue.state
            }
            addSeparator()
            addLabel("SOA")
            addComboBox("", SOADurs, SOADurs[2]) {selectedSOA ->
                SOA = selectedSOA.state
            }

            workspace.updater.updateManager.clear()
            workspace.updater.updateManager.addAction(UpdateCoupling(VAPlot))
            workspace.updater.updateManager.addAction(UpdateCoupling(IAPlot))
            workspace.updater.updateManager.addAction(UpdateCoupling(Decision1Plot))
            workspace.updater.updateManager.addAction(UpdateCoupling(Decision2Plot))

            addButton("Start") {
                workspace.launch {
                    attentionPlot.model.clearData()
                    decisionPlot.model.clearData()
                    t1Stimulus.text = ""
                    t1Decision.text = ""
                    t2Stimulus.text = ""
                    t2Decision.text = ""

                    T1 = Random.nextInt(0, 24)
                    T2 = Random.nextInt(0, 24)

                    val T1Direction = if (T1 < 12) "counterclockwise" else "clockwise"
                    val T2Direction = if (T2 < 12) "counterclockwise" else "clockwise"
                    val T1Angle = grat_orientations[T1].toDegrees().roundTo(2)
                    val T2Angle = grat_orientations[T2].toDegrees().roundTo(2)

                    t1Stimulus.text = "Stimulus (t1): $T1Direction (${T1Angle}°)"
                    t2Stimulus.text = "Stimulus (t2): $T2Direction (${T2Angle}°)"

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

                    decision.setActivations(doubleArrayOf(0.0, 0.0))
                    sensory1.setActivations(DoubleArray(12) { 0.0 })
                    sensory2.setActivations(DoubleArray(12) { 0.0 })
                    vaLayer.setActivations(doubleArrayOf(0.0))
                    iaLayer.setActivations(doubleArrayOf(0.0))
                    s1History.clear()

                    workspace.resetTime()

                    while (workspace.time < (init_t + 1100) / dt) {
                        s1History.add(sensory1.activations.toDoubleArray())
                        if (s1History.size > maxHist) s1History.removeAt(0)

                        if (workspace.time < init_t / dt) {
                            currentTarget = -1; imageWorld.setFrame(24)
                        } else if (workspace.time < (init_t + 30) / dt) {
                            currentTarget = T1; imageWorld.setFrame(T1)
                        } else if (workspace.time < (init_t + 30 + SOA) / dt) {
                            currentTarget = -1; imageWorld.setFrame(24)
                        } else if (workspace.time < (init_t + 60 + SOA) / 2) {
                            currentTarget = T2; imageWorld.setFrame(T2)
                        } else {
                            currentTarget = -1; imageWorld.setFrame(24)
                        }

                        val T1DecisionText = if (decision.getNeuron(0).activation > 0) "clockwise" 
                                        else if (decision.getNeuron(0).activation < 0) "counterclockwise"
                                        else "none"
                        val T2DecisionText = if (decision.getNeuron(1).activation > 0) "clockwise"
                                        else if (decision.getNeuron(1).activation < 0) "counterclockwise"
                                        else "none"

                        t1Decision.text = "Current decision: $T1DecisionText"
                        t2Decision.text = "Current decision: $T2DecisionText"

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

                        workspace.iterateSuspend()
                    }
                }
            }
        }
    }

addSidebarInfo(
        """
        # Temporal Attention Model

        A neural network model of visual attention based on the paper ["A dynamic normalization model of temporal attention"](https://www.nature.com/articles/s41562-021-01129-1) by Rachel Denison. The simulation demonstrates how voluntary and involuntary attention interact to detect and classify briefly presented visual patterns.

        ## Background

        In Denison's experiment, participants viewed two tilted grating patterns presented one after the other. Each pattern was rotated either clockwise or counterclockwise. Before the patterns appeared, participants received an auditory cue telling them to pay attention to the first pattern, the second pattern, or both. After viewing the patterns, they reported the rotation direction.

        The key finding: When participants were cued to attend to a specific pattern and then asked to report on that same pattern, their response times were faster and more accurate than when cued to one pattern but asked about the other. This demonstrates that voluntary attention (the cue) enhances perceptual processing of attended stimuli.

        # Simulation Details

        The model consists of five layers that process visual input and make decisions.

        ## Network Architecture

        - Sensory Layer: 12 neurons, each tuned to a different orientation (0°, 30°, 60°, etc.). When a grating pattern appears, the neuron matching that orientation activates most strongly, with nearby orientations showing weaker responses.

        - Sustained Response Layer: 12 neurons that maintain prolonged activation after the sensory layer responds. These sustained activations are what get accumulated in the decision layer.

        - Decision Layer: 2 neurons that accumulate evidence for each pattern. Positive activation indicates clockwise rotation, negative indicates counterclockwise. The strength of activation reflects the model's confidence.

        - Voluntary Attention: A single neuron controlled by the attention cue selected in the control panel. It enhances processing in the sensory layer based on which pattern is cued.

        - Involuntary Attention: A single neuron driven by the stimulus itself. It responds automatically to pattern onsets regardless of the cue, and also enhances processing in the sensory layer.

        ## How Attention Works in the Model

        Both attention systems modulate the gain of the sensory layer: they multiply the overall activation level. Voluntary attention is determined by your cue selection. Involuntary attention is stimulus-driven and responds to any visual pattern onset.

        # What to Do

        ## Run a Trial

        1. Select an `Attention Cue` and `SOA` (delay between stimuli) from the dropdown in the control panel.
        2. Click `Start` to run a trial
        3. Watch the simulation unfold

        ## What to Observe

        Sensory Responses: Watch the sensory layer activate when patterns appear, and the sustained response layer show a fading echo of sensory activations. The neuron corresponding to the pattern's orientation will activate most strongly.

        Attention Dynamics: The `Attention` time series plot shows how voluntary and involuntary attention unfold over time. Notice:
        - When involuntary attention spikes (at pattern onsets)
        - How voluntary attention is allocated to one pattern or the other based on the cue you selected
        - The interaction between the two attention systems

        Decision Formation: The `Decisions` time series plot shows how the two decision neurons accumulate evidence for each pattern over time. Positive activation indicates clockwise rotation, negative indicates counterclockwise. Watch how the decisions evolve as the model processes each stimulus and makes its judgment.
        
        Behavior: Text at the top of the network shows which stimulus patterns is presented at time 1 and time 2, along with the model's current decision for each pattern. 

        ## Experiment

        1. Try different attention cues and observe how they affect:
            - The decision layer activation strengths (model confidence)
            - The voluntary attention trace in the `Attention` plot
            - Whether the model makes correct decisions

        2. Run multiple trials with the same cue to see how random variation in stimulus patterns affects the model's performance.

        3. Try different `SOA` (stimulus onset asynchrony) values to see how the timing between the two patterns affects attention allocation and decision accuracy.

        ## Performance Tip

        For better performance during trials, minimize the time series windows (`Attention` and `Decisions`). The simulation will run faster when these plots are not actively rendering.

        # References

        Denison, R. N., Yuval-Greenberg, S., & Carrasco, M. (2021). [A dynamic normalization model of temporal attention](https://www.nature.com/articles/s41562-021-01129-1). _Nature Human Behaviour_, _5_(12), 1674–1685.

        # Credits

        [Jensen Guo](https://www.linkedin.com/in/jensen-guo/)
        
        [Jeff Yoshimi](https://www.jeffyoshimi.net)

        """.trimIndent()
    )
}
