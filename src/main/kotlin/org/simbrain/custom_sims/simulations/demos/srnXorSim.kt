package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTimeSeriesComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.*
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random

/**
 * Simulate temporal xor in a simple recurrent network as described by Elman (1990).
 *
 * TODO: Still not working as expected.
 */
val srnXORSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val srn = SRNNetwork(1, 4, 1)
    network.addNetworkModelAsync(srn)
    srn.layers.filterIsInstance<NeuronArray>().forEach { it.circleMode = true }

    // Load with xor data
    val xorInputs = generateTemporalXORData(1000)
    
    // Create shifted targets (shift up and pad last with zeros)
    val xorTargets = xorInputs.shiftUpAndPadEndWithZero()
    
    srn.trainingSet = TrainingDataset(xorInputs, xorTargets)
    srn.trainerConfig.updateType = SupervisedTrainer.UpdateMethod.Epoch()

    val trainer = SupervisedTrainer(network, srn)
    trainer.config.learningRate = .01

    // Train
    repeat(10) {
        trainer.trainOnce()
        println("iteration ${it}: ${trainer.lastTrainingError}")
    }

    val testData = generateTemporalXORData(1200 / 3)
    // TODO use srn.testingSet

    // Convert to Matrix for inputData compatibility
    val testDataMatrix = testData.toMatrix()
    srn.inputLayer.inputData = testDataMatrix

    var counter = 0

    withGui {
        place(networkComponent) {
            location = point(200, 10)
            width = 500
            height = 550
        }

        val timeSeries = addTimeSeriesComponent("Errors", seriesNames = listOf("error"))

        place(timeSeries) {
            location = point(700, 10)
            width = 500
            height = 550
        }

        createControlPanel("Control Panel", 5, 10) {
            val actualText = addLabelledText("Actual Next: ", "0.000")
            val predictedText = addLabelledText("Predicted Next: ", "0.000")
            val errorText = addLabelledText("Error: ", "0.000")

            // TODO: Could add a message or text that says "predictable" every third iteration

            val timeSeriesWindowLength = 30 * 3
            // A rolling window used to accumulate errors; sized to match the window length
            val sumWindow = MutableList(timeSeriesWindowLength) { 0.0 }

            suspend fun test() {
                fun index() = counter % testDataMatrix.nrow()
                srn.inputLayer.activations = testDataMatrix.row(index()).toColumnVector()
                counter += 1
                workspace.iterateSuspend()
                val output = srn.outputLayer.activations
                actualText.text = testDataMatrix.row(index())[0].format(3)
                predictedText.text = output[0].format(3)
                val error = output rmse testDataMatrix.row(index()).toColumnVector()
                errorText.text = error.format(3)

                sumWindow[counter % timeSeriesWindowLength] += error
                if (counter % timeSeriesWindowLength == 0) {
                    // println(sumWindow.map { it / max(1.0, floor(counter / timeSeriesWindowLength.0)) }.map { it.format(3) })
                    timeSeries.model.timeSeriesList[0].series.clear()
                }
                timeSeries.model.timeSeriesList[0].series.add(counter % timeSeriesWindowLength, sumWindow[counter % timeSeriesWindowLength] / max(1.0, floor(counter / timeSeriesWindowLength.toDouble())))
            }

            addButton("Test") {
                test()
            }

            addButton("Test 1200") {
                repeat(1200) {
                    test()
                }
            }

        }
    }

}

/**
 * Generates a sequence of 3n bits where each triplet consists of two random bits
 * and a third bit that is the XOR of the first two, using doubles to represent bits.
 * @param n The number of triplets to generate.
 * @return A DoubleArray representing the sequence of bits.
 */
fun generateTemporalXORData(n: Int): MutableList<MutableList<Double>> {
    // Initialize list to hold the bits
    val temporalXorData = mutableListOf<MutableList<Double>>()

    // Fill the list with the triplets
    for (i in 0 until n) {
        val bit1 = Random.nextInt(2).toDouble()
        val bit2 = Random.nextInt(2).toDouble()
        val xorBit = if (bit1 == bit2) 0.0 else 1.0
        temporalXorData.add(mutableListOf(bit1))
        temporalXorData.add(mutableListOf(bit2))
        temporalXorData.add(mutableListOf(xorBit))
    }

    return temporalXorData
}

fun main() {
    val xorData = generateTemporalXORData(3)
    println("Temporal XOR (input data):\t${xorData.map { it[0] }}")
    
    // Create shifted targets using the utility function
    val targets = xorData.shiftUpAndPadEndWithZero()
    println("Left-shifted Targets:\t\t${targets.map { it[0] }}")
}

