package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import org.simbrain.custom_sims.*
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.addNeuronGroup
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

    val currentStatus = NetworkTextObject("").apply { fontSize = 18 }
    val modelDecision = NetworkTextObject("").apply { fontSize = 18 }

    val sensory1 = net.addNeuronGroup(12).apply {
        label = "Sensory"
        setLabels((0..11).map { "${it * 30}°" })
    }
    val sensory2 = net.addNeuronGroup(12).apply {
        label = "Sustained Response"
        setLabels((0..11).map { "${it * 30}°" })
    }
    val decision = net.addNeuronGroup(2).apply { label = "Decision" }.apply {
        setLabels(listOf("Pattern 1", "Pattern 2"))
    }
    val vaLayer = net.addNeuronGroup(1).apply { label = "Voluntary Attention" }
    val iaLayer = net.addNeuronGroup(1).apply { label = "Involuntary Attention" }

    sensory1.setUpperBound(0.1)
    sensory2.setUpperBound(0.1)
    decision.setLowerBound(-0.000002)
    decision.setUpperBound(0.000002)
    vaLayer.setLowerBound(-0.02)
    vaLayer.setUpperBound(0.02)
    iaLayer.setLowerBound(-0.02)
    iaLayer.setUpperBound(0.02)

    net.addNetworkModels(sensory1, sensory2, decision, vaLayer, iaLayer, modelDecision, currentStatus)

    val imageWorldComponent = addImageWorld("Gratings")
    val imageWorld = imageWorldComponent.world
    imageWorld.loadImages(getFilesWithExtension("simulations/images/denisonGratings", "png"))
    val background = DoubleArray(10000) { 0.0 }.toGrayScaleImage(100, 100)
    imageWorld.imageAlbum.addImage(background)

    val (plot, IASeries, VASeries) = addTimeSeries("Attention", seriesNames = listOf("Involuntary Attention", "Voluntary Attention"))
    plot.apply {
        model.isAutoRange = true
        model.fixedWidth = false
    }

    val IAPlot = couplingManager.createCoupling(iaLayer.getNeuron(0), IASeries)
    val VAPlot = couplingManager.createCoupling(vaLayer.getNeuron(0), VASeries)

    val m = 2 * sensory1.size - 1
    val pref_orientations = DoubleArray(12) { i -> i * PI / 12 }
    val neg_orientations = DoubleArray(12) { i -> (-2.5 + i * 0.1) * PI / 180 + PI / 2 }
    val pos_orientations = DoubleArray(12) { i -> (1.4 + i * 0.1) * PI / 180 + PI / 2 }
    val grat_orientations = neg_orientations + pos_orientations

    val w_i = Array(12) { i ->
        DoubleArray(24) { j -> abs(cos(grat_orientations[j] - pref_orientations[i]).pow(m)) }
    }

    var w_D = DoubleArray(12) { 0.0 }
    var SOA = 0
    var T1 = 0
    var T2 = 0
    var currentTarget = -1
    var vaState = 0
    var reportTarget = 1
    var totalAttention = 0.0
    var T1Allocation = 0.0
    var T2Allocation = 0.0
    val dt = 2.0

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
                if (workspace.time >= 1000 / dt && workspace.time < (1030 + SOA) / dt) {
                    drive[0] = w_D.zip(sensory2.activationArray) { x, y -> x * y }.sum()
                } else if (workspace.time >= (1030 + SOA) / dt) {
                    drive[1] = w_D.zip(sensory2.activationArray) { x, y -> x * y }.sum()
                }
            }
            Layer.VA -> {
                val t_VAOn = -34.0
                val t_VADur = 124.0
                drive = when {
                    workspace.time >= (1000 + t_VAOn) / dt && workspace.time < (1000 + t_VAOn + t_VADur) / dt ->
                        doubleArrayOf(T1Allocation.pow(n))
                    workspace.time >= (1030 + SOA + t_VAOn) / dt && workspace.time < (1030 + SOA + t_VAOn + t_VADur) / dt ->
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
        place(imageWorldComponent, 690, 15, 503, 446)
        place(plot, 303, 442, 500, 300)

        currentStatus.location = point(145, -240)
        modelDecision.location = point(145, -180)

        sensory1.location = point(0.0, 100.0)
        sensory2.location = point(220.0, 100.0)
        decision.location = point(440.0, 100.0)
        vaLayer.location = point(44.0, -97.0)
        iaLayer.location = point(370.0, -97.0)

        // Define cue types
        data class CueType(val name: String, val state: Int) {
            override fun toString() = name
        }

        val cueTypes = listOf(
            CueType("Cue Pattern 1", 0),
            CueType("Cue Pattern 2", 1),
            CueType("Cue Both", 2)
        )

        createControlPanel("Control Panel", 15, 15) {
            addLabel("Attention Cue:")
            addComboBox("", cueTypes, cueTypes[0]) { selectedCue ->
                vaState = selectedCue.state
            }
            addSeparator()

            workspace.updater.updateManager.clear()
            workspace.updater.updateManager.addAction(UpdateCoupling(VAPlot))
            workspace.updater.updateManager.addAction(UpdateCoupling(IAPlot))

            addButton("Start") {
                workspace.launch {
                    plot.model.clearData()
                    currentStatus.text = ""
                    modelDecision.text = ""

                    SOA = Random.nextInt(100, 801)
                    T1 = Random.nextInt(0, 24)
                    T2 = Random.nextInt(0, 24)

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

                    while (workspace.time < 2100 / dt) {
                        // Record sensory1 history
                        s1History.add(sensory1.activations.toDoubleArray())
                        if (s1History.size > maxHist) s1History.removeAt(0)

                        if (workspace.time < 1000 / dt) {
                            currentStatus.text = "No stimulus"
                            currentTarget = -1; imageWorld.setFrame(24)
                        } else if (workspace.time < 1030 / dt) {
                            currentStatus.text = "Pattern 1 (${grat_orientations[T1].toDegrees().roundTo(2)}°)"
                            currentTarget = T1; imageWorld.setFrame(T1)
                        } else if (workspace.time < (1030 + SOA) / dt) {
                            currentStatus.text = "No stimulus"
                            currentTarget = -1; imageWorld.setFrame(24)
                        } else if (workspace.time < (1060 + SOA) / 2) {
                            currentStatus.text = "Pattern 2 (${grat_orientations[T2].toDegrees().roundTo(2)}°)"
                            currentTarget = T2; imageWorld.setFrame(T2)
                        } else {
                            currentStatus.text = "No stimulus"
                            currentTarget = -1; imageWorld.setFrame(24)
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

                        workspace.iterateSuspend()
                    }

                    val T1GroundTruth = if (T1 < 12) "Counterclockwise" else "Clockwise"
                    val T2GroundTruth = if (T2 < 12) "Counterclockwise" else "Clockwise"
                    val T1decision = if (decision.getNeuron(0).activation > 0) "Clockwise" else "Counterclockwise"
                    val T2decision = if (decision.getNeuron(1).activation > 0) "Clockwise" else "Counterclockwise"

                    modelDecision.text = """
                        Pattern 1: $T1GroundTruth       Pattern 1 decision: $T1decision
                        Pattern 2: $T2GroundTruth       Pattern 2 decision: $T2decision
                    """
                }
            }
        }
    }

addSidebarInfo(
        """
        # Introduction
        
        This is a neural network simulation of visual attention, based on the paper ["A dynamic normalization
        model of temporal attention"](https://www.nature.com/articles/s41562-021-01129-1) by Rachel Denison.
        
        In an experiment, participants were asked to pay attention to 2 tilted grating patterns. The patterns were rotated
        either clockwise or counterclockwise, and were shown one after the other. Participants
        were cued with a noise, which told them which pattern to pay attention to (first, second, or both). Afterwards, they were
        asked to report the rotation direction of one of the patterns.
        
        The researchers found that when the cued pattern the matched they were asked to report on, the overall response
        times were faster than if they were mismatched. In this simulation they are asked to report both, so there is no
        congruence effect.
        
        The model consists of 5 layers. 2 of the layers are input layers, 2 of them are attention layers
        (Involuntary and Voluntary), and there is 1 decision layer.
        
        # What to Do
        
        In the control panel, choose a trial type. Run the trial and observe what happens.
        
        When a grating appears in the panel to the right, the neurons in the first sensory layer pick it up. Each neuron
        in this layer is tuned to a different orientation. Thus a given pattern produces a maximal response in one neuron
        and fading activations in nearby neurons. Another network shows sustained echoes of the pattern in the sensory network.
        The sustained activations are accumulated in the decision layer.
        
        The sensory neurons influence involuntary attention, and is thus stimulus-based. Voluntary attention is determimed by the cue type.

        The attention neurons adjust the gain (the overall activation) of the sensory layer.
                
        The decision neurons correspond to the first and second pattern. Positive activations signify clockwise orientation,
        while negative activations signify counterclockwise.
        
        The first decision neuron accumulates evidence for the first pattern,
        and similarly for the second. Observe the strength of the neurons in the decision layer (which correspond to
         how "sure" the model is of its answer) and how the different cueings affect it.
        
        # Credits 
        [Jensen Guo](https://www.linkedin.com/in/jensen-guo/)
        
        """.trimIndent()
    )
}
