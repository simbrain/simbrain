package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.delay
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.OneToOne
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.util.*
import kotlin.random.Random
import org.simbrain.util.toGrayScaleImage
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow


val denisonNet = newSim {
    workspace.clearWorkspace()
    val netComponent = addNetworkComponent("Denison Net")
    val net = netComponent.network

    val currentStatus = NetworkTextObject("").apply { fontSize = 18 }
    val reportStatus = NetworkTextObject("").apply { fontSize = 18 }
    val modelDecision = NetworkTextObject("").apply {fontSize = 18}

    /*
    val inputs = NeuronArray(100*100).apply {
        label = "Inputs"
        isClamped = true
        gridMode = true
    }
    */

    val sensory1 = NeuronGroup(12).apply { label = "Sensory 1" }
    val sensory2 = NeuronGroup(12).apply { label = "Sensory 2" }
    val decision = NeuronGroup(2).apply { label = "Decision" }
    val vaLayer = NeuronGroup(1).apply { label = "VA" }
    val iaLayer = NeuronGroup(1).apply { label = "IA" }

    net.addNetworkModels( sensory1, sensory2, decision, vaLayer, iaLayer, currentStatus, reportStatus, modelDecision)
    val connector = OneToOne().apply { percentExcitatory = 100.0 }
    net.addNetworkModels(connector.connectNeurons(sensory1.neuronList, sensory2.neuronList))

    val component = addImageWorld("Gratings")
    val imageWorld = component.world
    imageWorld.loadImages(getFilesWithExtension("simulations/images/denisonGratings", "png"))

    val background = DoubleArray(10000) { 0.0 }.toGrayScaleImage(100, 100)
    imageWorld.imageAlbum.addImage(background)

    withGui {
        place(netComponent, 130, 15, 516, 556)
        place(component, 645, 15, 565, 675)
        var vaState = 0

        currentStatus.location = point(220, -240)
        reportStatus.location = point(220, -220)
        modelDecision.location = point(220, -200)
        currentStatus.text = "Paying Attention to Both"

        var reportTarget = 1  // 1 for T1, 2 for T2

        createControlPanel("Control Panel", 15, 15) {
            addButton("Cue T1") { vaState = 1; currentStatus.text = "Paying Attention to T1" }
            addButton("Cue T2") { vaState = 2; currentStatus.text = "Paying Attention to T2" }
            addButton("Cue Both") { vaState = 0; currentStatus.text = "Paying Attention to Both" }
            addSeparator()
            addButton("Report T1") { reportTarget = 1; reportStatus.text = "Reporting T1" }
            addButton("Report T2") { reportTarget = 2; reportStatus.text = "Reporting T2" }
            addSeparator()

            fun calculateStimulusInput(target: Int): DoubleArray {
                val degree = if (target <= 11) target * -0.1 - 1.4 else (target - 12) * 0.1 + 1.4
                val rad = Math.toRadians(degree)
                val orientations = DoubleArray(12) { Math.toRadians(-90 + it * 16.36) }
                return orientations.map { 0.0.coerceAtLeast(cos(rad - it)) }.toDoubleArray()
            }

            fun voluntaryGain(vaState: Int, soa: Int, wN: Double = 0.28, tR: Int = 918): Pair<DoubleArray, DoubleArray> {
                val gainT1 = DoubleArray(12) { if (vaState == 1) 1.0 else if (vaState == 0) wN else min(1.0, soa.toDouble() / tR) }
                val gainT2 = DoubleArray(12) { if (vaState == 2) 1.0 else if (vaState == 0) 1.0 - wN else min(1.0, soa.toDouble() / tR) }
                return gainT1 to gainT2
            }

            fun updateIA(prevIA: DoubleArray, s1: DoubleArray, dt: Double = 1.0, tau: Double = 2.0): DoubleArray {
                return DoubleArray(s1.size) { i ->
                    val dR = (-prevIA[i] + s1[i]) * (dt / tau)
                    prevIA[i] + dR
                }
            }

            fun updateS2(s1: DoubleArray, s2: DoubleArray,
                         n: Double = 1.5, sigma: Double = 1.4,
                         dt: Double = 1.0, tau: Double = 10.0): DoubleArray {
                val denom = s1.sumOf { it.pow(n) } + sigma.pow(n)
                return DoubleArray(s1.size) { i ->
                    val num = s1[i].pow(n)
                    val dR = (-s2[i] + num / denom) * (dt / tau)
                    s2[i] + dR
                }
            }

            fun nextGaussian(mean: Double = 0.0, std: Double = 1.0): Double {
                val u1 = Random.nextDouble()
                val u2 = Random.nextDouble()
                val r = kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
                return mean + std * r
            }

            suspend fun runTrial() {
                // --- Trial parameters ---
                val SOA = Random.nextInt(100, 801)
                val T1 = Random.nextInt(0, 24)
                val T2 = Random.nextInt(0, 24)
                val T1input = calculateStimulusInput(T1)
                val T2input = calculateStimulusInput(T2)
                val (vaT1, vaT2) = voluntaryGain(vaState, SOA)

                // --- State variables ---
                var ia = DoubleArray(12) { 0.0 }
                var s2 = DoubleArray(12) { 0.0 }
                var rT1 = 0.0   // decision neuron for T1
                var rT2 = 0.0   // decision neuron for T2
                decision.setActivations(doubleArrayOf(0.0, 0.0))

                val bVA = 40.0       // voluntary gain amplitude
                val bIA = 8.5        // involuntary gain amplitude
                val n    = 1.5       // normalization exponent
                val sigma = 1.4      // semi-saturation constant

                // --- Time control ---
                val start = System.currentTimeMillis()

                while (true) {
                    val t = System.currentTimeMillis() - start

                    when {
                        // Pre-trial fixation
                        t < 1000 -> {
                            sensory1.setActivations(DoubleArray(12))
                            imageWorld.setFrame(24)
                        }

                        // presentation window (30 ms)
                        t in 1000 until 1030 || t in (1030 + SOA) until (1060 + SOA)-> {
                            var neuron = if (t in 1000 until 1030) 1 else 2
                            val va = if (neuron == 1) vaT1 else vaT2

                            val s1 = DoubleArray(12) { i ->
                                val gain = (1 + bVA * va[i]) * (1 + bIA * ia[i])
                                if (neuron == 1) T1input[i] * gain else T2input[i] * gain
                            }
                            sensory1.setActivations(s1)
                            ia = updateIA(ia, s1)
                            s2 = updateS2(s1, s2, n, sigma)

                            val evidence = s2.slice(6..11).sum() - s2.slice(0..5).sum()

                            if(neuron == 1) {
                                rT1 += evidence + nextGaussian(0.0, 0.02)
                                imageWorld.setFrame(T1)
                                vaLayer.setActivations(doubleArrayOf(va.average()))
                            } else {
                                rT2 += evidence + nextGaussian(0.0, 0.02)
                                imageWorld.setFrame(T2)
                                vaLayer.setActivations(doubleArrayOf(va.average()))
                            }

                            iaLayer.setActivations(doubleArrayOf(ia.average()))
                            sensory2.setActivations(s2)
                            decision.setActivations(doubleArrayOf(rT1, rT2))
                        }

                        // Inter-stimulus interval
                        t in 1030 until (1030 + SOA) -> {
                            sensory1.setActivations(DoubleArray(12))
                            imageWorld.setFrame(24)
                        }

                        // Trial end (clear display and evaluate response)
                        else -> {
                            val finalDecision = if (reportTarget == 1) {
                                if (rT1 > 0) "CW" else "CCW"
                            } else {
                                if (rT2 > 0) "CW" else "CCW"
                            }
                            val reportedStim = if (reportTarget == 1) T1 else T2
                            val correctOrientation = if (reportedStim < 12) "CCW" else "CW"
                            val correctness = if (finalDecision == correctOrientation) "✔" else "✘"
                            modelDecision.text = "Reported $finalDecision | True: $correctOrientation $correctness"

                            // Reset sensory display
                            sensory1.setActivations(DoubleArray(12))
                            imageWorld.setFrame(24)
                            break
                        }
                    }
                    delay(1L)
                }
            }


            addButton("Start") { runTrial() }
        }
    }
    addSidebarInfo(
        """
        # Introduction
        This is a neural network simulation of visual attention, based off the paper "A dynamic normalization
        model of temporal attention" by Rachel Denison.
        
        In an experiment, participants were asked to pay attention to 2 rotating grates. The grates rotated
        either clockwise or counterclockwise, and the grates were shown 1 after the other. Participants
        were cued with a noise, which told them which grate to pay attention to. Afterwards, they were
        asked to report the rotation direction of one of the grates. For example, a participant would be
        cued with a high pitch, indicating them to pay attention to the second grate (T2), and then
        the researchers would ask them to report the rotation direction.
        
        The researchers found that when the cued noise matched the reporting grate, the overall response
        times were faster than if they were mismatched (Ex. T1 is cued and asked vs. T1 is cued but T2
        is asked). A model was then built simulating this.
        
        The model consists of 5 layers. 2 of the layers are input layers, 2 of them are attention layers
        (Involutary and Voluntary), and there is 1 decision layer.
        """.trimIndent()
    )
}
