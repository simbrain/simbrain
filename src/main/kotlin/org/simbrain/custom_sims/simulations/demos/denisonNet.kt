package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.delay
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.OneToOne
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.util.*
import kotlin.random.Random
import org.simbrain.network.core.NeuronArray
import org.simbrain.util.toGrayScaleImage
import kotlin.math.min
import kotlin.math.pow

val denisonNet = newSim {
    workspace.clearWorkspace()
    val net = addNetworkComponent("Denison Net").network

    val currentStatus = NetworkTextObject("").apply { fontSize = 18 }
    val reportStatus = NetworkTextObject("").apply { fontSize = 18 }

    val inputs = NeuronArray(100*100).apply {
        label = "Inputs"
        isClamped = true
        gridMode = true
    }
    val sensory1 = NeuronGroup(12).apply { label = "Sensory 1" }
    val sensory2 = NeuronGroup(12).apply { label = "Sensory 2" }
    val decision = NeuronGroup(2).apply { label = "Decision" }
    val vaLayer = NeuronGroup(1).apply { label = "Voluntary Attention" }
    val iaLayer = NeuronGroup(1).apply { label = "Involuntary Attention" }

    net.addNetworkModels(inputs, sensory1, sensory2, decision, vaLayer, iaLayer, currentStatus, reportStatus)
    val connector = OneToOne().apply { percentExcitatory = 100.0 }
    net.addNetworkModels(connector.connectNeurons(sensory1.neuronList, sensory2.neuronList))

    val component = addImageWorld("Gratings")
    val imageWorld = component.world
    imageWorld.loadImages(getFilesWithExtension("simulations/images/denisonGratings", "png"))

    val background = DoubleArray(10000) { 0.0 }.toGrayScaleImage(100, 100)
    imageWorld.imageAlbum.addImage(background)

    withGui {
        place(component, 393, 10, 565, 675)
        var vaState = 0

        currentStatus.location = point(220, -240)
        reportStatus.location = point(220, -220)
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
                return orientations.map { Math.max(0.0, Math.cos(rad - it)) }.toDoubleArray()
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

            fun updateS2(s1: DoubleArray, s2: DoubleArray, n: Double = 1.5, sigma: Double = 1.4, dt: Double = 1.0, tau: Double = 100.0): DoubleArray {
                val s = s1.sumOf { it.pow(n) }
                val denom = s + sigma.pow(n)
                return DoubleArray(s1.size) { i ->
                    val num = s1[i].pow(n)
                    val dR = (-s2[i] + num / denom) * (dt / tau)
                    s2[i] + dR
                }
            }

            fun updateDecision(decision: DoubleArray, s2: DoubleArray, dt: Double = 1.0): DoubleArray {
                return doubleArrayOf(
                    decision[0] + s2.slice(0..5).sum() * dt,
                    decision[1] + s2.slice(6..11).sum() * dt
                )
            }

            suspend fun runTrial() {
                val SOA = Random.nextInt(100, 801)
                val T1 = Random.nextInt(0, 24)
                val T2 = Random.nextInt(0, 24)
                val T1input = calculateStimulusInput(T1)
                val T2input = calculateStimulusInput(T2)
                val (vaT1, vaT2) = voluntaryGain(vaState, SOA)

                var ia = DoubleArray(12) { 0.0 }
                var s2 = DoubleArray(12) { 0.0 }
                var decisionActs = DoubleArray(2) { 0.0 }
                val start = System.currentTimeMillis()

                while (true) {
                    val t = System.currentTimeMillis() - start
                    if (t < 1000) {
                        sensory1.setActivations(DoubleArray(12))
                        imageWorld.setFrame(24)
                    } else if (t < 1030) {
                        val s1 = DoubleArray(12) { i -> T1input[i] * vaT1[i] * (1 + ia[i]) }
                        sensory1.setActivations(s1)
                        val s1Acts = sensory1.activations.toDoubleArray()
                        ia = updateIA(ia, s1Acts)
                        s2 = updateS2(s1Acts, s2)
                        decisionActs = updateDecision(decisionActs, s2)

                        iaLayer.setActivations(doubleArrayOf(ia.average()))
                        sensory2.setActivations(s2)
                        decision.setActivations(decisionActs)
                        imageWorld.setFrame(T1)
                        vaLayer.setActivations(doubleArrayOf(vaT1.average()))
                    } else if (t < 1030 + SOA) {
                        sensory1.setActivations(DoubleArray(12))
                        imageWorld.setFrame(24)
                    } else if (t < 1060 + SOA) {
                        val s1 = DoubleArray(12) { i -> T2input[i] * vaT2[i] * (1 + ia[i]) }
                        sensory1.setActivations(s1)
                        val s1Acts = sensory1.activations.toDoubleArray()
                        ia = updateIA(ia, s1Acts)
                        s2 = updateS2(s1Acts, s2)
                        decisionActs = updateDecision(decisionActs, s2)

                        iaLayer.setActivations(ia)
                        sensory2.setActivations(s2)
                        decision.setActivations(decisionActs)
                        imageWorld.setFrame(T2)
                        vaLayer.setActivations(doubleArrayOf(vaT2.average()))
                    } else {
                        // Trial complete. Evaluate decision based on report target
                        val finalDecision = if (decisionActs[0] > decisionActs[1]) "CCW" else "CW"
                        val reportedStim = if (reportTarget == 1) T1 else T2
                        val correctOrientation = if (reportedStim < 12) "CCW" else "CW"
                        val correctness = if (finalDecision == correctOrientation) "✔" else "✘"
                        reportStatus.text = "Reported $finalDecision | True: $correctOrientation $correctness"

                        sensory1.setActivations(DoubleArray(12))
                        imageWorld.setFrame(24)

                        break
                    }
                    delay(1L)
                }
            }

            addButton("Start") { runTrial() }
        }
    }
}





/*
package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.delay
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.OneToOne
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.util.*
import kotlin.random.Random
import org.simbrain.network.core.NeuronArray
import org.simbrain.util.toGrayScaleImage
import smile.math.matrix.Matrix
import kotlin.math.min
import kotlin.math.pow


val denisonNet = newSim {

    workspace.clearWorkspace()
    //Network
    val networkComponent = addNetworkComponent("Denison Net")
    val net = networkComponent.network

    val currentStatus = NetworkTextObject("").apply {
        fontSize = 18
    }

    val reportStatus = NetworkTextObject("").apply {
        fontSize = 18
    }
    val inputs = NeuronArray(100*100).apply {
        label = "Inputs"
        isClamped = true
        gridMode = true
    }
    val sensory1 = NeuronGroup(12).apply {
        label = "Sensory 1"
    }
    val sensory2 = NeuronGroup(12).apply{
        label = "Sensory 2"
    }
    val decision = NeuronGroup(2).apply {
        label = "Decision"
    }
    //also voluntary attention and involuntary attention layers

    net.addNetworkModels(inputs, sensory1, sensory2, decision, currentStatus, reportStatus)
    val connector = OneToOne().apply {
        percentExcitatory = 100.0
    }
    net.addNetworkModels(connector.connectNeurons(sensory1.neuronList, sensory2.neuronList))

    //World
    val component = addImageWorld("Gratings")
    val imageWorld = component.world

    imageWorld.loadImages(getFilesWithExtension("simulations/images/denisonGratings", "png"))

    val background = DoubleArray(10000) { 0.0 }.toGrayScaleImage(100, 100)
    imageWorld.imageAlbum.addImage(background)

    withGui{
        place(component,393, 10, 565, 675)

        var VA_stat = 0

        currentStatus.location = point(220, -240)
        reportStatus.location = point(220, -220)
        currentStatus.text = "Paying Attention to Both"
        createControlPanel("Control Panel", 15, 15) {
            addButton("Cue T1"){
                VA_stat = 1
                currentStatus.text = "Paying Attention to T1"
            }
            addButton("Cue T2"){
                VA_stat = 2
                currentStatus.text = "Paying Attention to T2"
            }
            addButton("Cue Both"){
                VA_stat = 0
                currentStatus.text = "Paying Attention to Both"
            }
            addSeparator()

            addButton("Report T1"){
                reportStatus.text = "Reporting T1"
            }
            addButton("Report T2"){
                reportStatus.text = "Reporting T2"
            }

            addSeparator()

            fun calculateInputs(target: Double):DoubleArray{
                var degree = 0.0
                degree = if (target <= 11) {
                    target*-0.1-1.4
                } else{
                    (target-12)*0.1+1.4
                }
                val rad = Math.toRadians(degree)
                val orientations = doubleArrayOf(-90.0, -73.63636364, -57.27272727, -40.90909091, -24.54545455,
                    -8.18181818,   8.18181818,  24.54545455,  40.90909091,  57.27272727, 73.63636364,  90.0) //maybe there's a linspace function?
                val inputs = orientations.map{ orientation ->
                    val orientationRad = Math.toRadians(orientation)
                    maxOf(0.0, Math.cos(rad - orientationRad))
                }.toDoubleArray()

                return inputs
            }

            fun VAControl(VA_stat:Int, SOA:Int, wN:Double = 0.28, tR:Int = 918): DoubleArray{
                val amp = doubleArrayOf(0.0, 0.0) //index 0 is T1, index 1 is T1
                if (VA_stat == 0){
                    amp[0] = wN
                    amp[1] = 1.0 - wN
                }
                else if (VA_stat == 1){
                    amp[0] = 1.0
                    amp[1] = min(1.0, (SOA/tR).toDouble())
                }
                else if (VA_stat == 2){
                    amp[0] = min(1.0, (SOA/tR).toDouble())
                    amp[1] = 1.0
                }
                return amp
            }

            fun updateS2(s1Acts: DoubleArray, s2Acts: DoubleArray, dt: Double = 1.0, tau: Double = 100.0, n: Double = 1.5, sigma: Double = 1.4): DoubleArray {
                val newActs = DoubleArray(s1Acts.size)
                val s = s1Acts.sumOf { it.pow(n) }
                val denom = s + sigma.pow(n)

                for (i in s1Acts.indices) {
                    val num = s1Acts[i].pow(n)
                    val dR = (-s2Acts[i] + num / denom) * (dt / tau)
                    newActs[i] = s2Acts[i] + dR
                }

                return newActs
            }

            fun updateDecision(
                decisionActs: DoubleArray,
                s2Acts: DoubleArray,
                dt: Double = 1.0
            ): DoubleArray {
                val newActs = decisionActs.copyOf()
                val ccwSum = s2Acts.slice(0..5).sum()
                val cwSum = s2Acts.slice(6..11).sum()

                newActs[0] += ccwSum * dt  // CCW unit
                newActs[1] += cwSum * dt   // CW unit

                return newActs
            }

            suspend fun runTrial(){
                //experiment parameters
                val SOA = Random.nextInt(100,801)

                //0-11 is CCW, 12-23 is CW
                val T1 = Random.nextInt(0,24)
                val T2 = Random.nextInt(0, 24)

                val T1input = calculateInputs(T1.toDouble())
                val T2input = calculateInputs(T2.toDouble())

                val amplitudes = VAControl(VA_stat, SOA)

                val vaGainT1 = DoubleArray(12) { amplitudes[0] }
                val vaGainT2 = DoubleArray(12) { amplitudes[1] }

                val T1inputWithVA = T1input.zip(vaGainT1) { stim, gain -> stim * gain }.toDoubleArray()
                val T2inputWithVA = T2input.zip(vaGainT2) { stim, gain -> stim * gain }.toDoubleArray()

                //trial vars
                var trialComplete = false
                val startTime = System.currentTimeMillis()
                val buffer = 1L
                var decisionActs = DoubleArray(2) { 0.0 }

                //run the model
                while(!trialComplete){
                    var t = (System.currentTimeMillis()-startTime)
                    if (t < 1000){
                        sensory1.setActivations(DoubleArray(12) { 0.0 })
                        imageWorld.setFrame(24)
                    }
                    else if(t < 1030){
                        val iaBoost = DoubleArray(12) { 1.2 }
                        val input = T1input.zip(vaGainT1).zip(iaBoost.toList()) { (stim, va), ia -> stim * va * ia }.toDoubleArray()
                        sensory1.setActivations(input)

                        val s1Acts = sensory1.activations.toDoubleArray()
                        val s2Acts = sensory2.activations.toDoubleArray()
                        val updatedS2 = updateS2(s1Acts, s2Acts)
                        sensory2.setActivations(updatedS2)

                        decisionActs = updateDecision(decisionActs, sensory2.activations.toDoubleArray())
                        decision.setActivations(decisionActs)

                        imageWorld.setFrame(T1)
                    }
                    else if (t < 1030 + SOA){
                        sensory1.setActivations(DoubleArray(12) { 0.0 })
                        imageWorld.setFrame(24)
                    }
                    else if (t < 1060 + SOA){
                        val iaBoost = DoubleArray(12) { 1.2 }
                        val input = T2input.zip(vaGainT2).zip(iaBoost.toList()) { (stim, va), ia -> stim * va * ia }.toDoubleArray()
                        sensory1.setActivations(input)

                        val s1Acts = sensory1.activations.toDoubleArray()
                        val s2Acts = sensory2.activations.toDoubleArray()
                        val updatedS2 = updateS2(s1Acts, s2Acts)
                        sensory2.setActivations(updatedS2)

                        decisionActs = updateDecision(decisionActs, sensory2.activations.toDoubleArray())
                        decision.setActivations(decisionActs)

                        imageWorld.setFrame(T2)
                    }
                    else if (t < 1090 + SOA) {
                        sensory1.setActivations(DoubleArray(12) { 0.0 })
                        imageWorld.setFrame(24)
                    }
                    else {
                        trialComplete = true
                    }
                    delay(buffer)
                }
            }



            addButton("Start"){
                runTrial()
            }
        }
    }

/*
    with(couplingManager) {
        createCoupling(
            imageWorld.filterCollection.currentFilter.getProducer(imageWorld.filterCollection.currentFilter::brightness),
            inputs.getConsumer(inputs::setActivations)
        )
    }
 */

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
*/