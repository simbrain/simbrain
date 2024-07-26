package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTimeSeriesComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.util.*
import smile.math.matrix.Matrix
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random

/**
 * Simulate temporal xor in a simple recurrent network as described by Elman (1990).
 *
 * TODO: After ElmanSentences was fixed work on this dropped off. It was never made to work but getting it to work should not be too hard.
 */
val srnXORSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val srn = SRNNetwork(1, 2, 1)
    network.addNetworkModel(srn)

    // Load with xor data
    val xorInputs = generateTemporalXORData(1000)
    srn.trainingSet = MatrixDataset(xorInputs, xorInputs.shiftUpAndPadEndWithZero())
    srn.trainer.updateType = SupervisedTrainer.UpdateMethod.Stochastic()

    // Train
    with(network) {
        repeat(600) {
            srn.run { trainer.trainOnce() }
            if (it % 10 == 0) {
                println("iteration ${it}: ${srn.trainer.lossFunction.loss}")
            }
        }
    }

    val testData = generateTemporalXORData(1200 / 3)

    srn.inputLayer.inputData = testData

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

        val sumWindow = MutableList(12) { 0.0 }

        createControlPanel("Control Panel", 5, 10) {
            val actualText = addLabelledText("Actual Next: ", "0.000")
            val predictedText = addLabelledText("Predicted Next: ", "0.000")
            val errorText = addLabelledText("Error: ", "0.000")

            suspend fun test() {
                fun index() = counter % testData.nrow()
                srn.inputLayer.activations = testData.row(index()).toMatrix()
                counter += 1
                workspace.iterateSuspend()
                val output = srn.outputLayer.activations
                actualText.text = testData.row(index())[0].format(3)
                predictedText.text = output[0].format(3)
                val error = output rmse testData.row(index()).toMatrix()
                errorText.text = error.format(3)

                sumWindow[counter % 12] += error
                if (counter % 12 == 0) {
                    // println(sumWindow.map { it / max(1.0, floor(counter / 12.0)) }.map { it.format(3) })
                    timeSeries.model.timeSeriesList[0].series.clear()
                }
                timeSeries.model.timeSeriesList[0].series.add(counter % 12, sumWindow[counter % 12] / max(1.0, floor(counter / 12.0)))
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
fun generateTemporalXORData(n: Int): Matrix {
    // Initialize an array of size 3n to hold the bits
    val temporalXorMatrix = Matrix(3 * n, 1)

    // Fill the array with the triplets
    for (i in 0 until n) {
        val index = i * 3
        val bit1 = Random.nextInt(2).toDouble()
        val bit2 = Random.nextInt(2).toDouble()
        val xorBit = if (bit1 == bit2) 0.0 else 1.0
        temporalXorMatrix[index, 0] = bit1
        temporalXorMatrix[index + 1, 0] = bit2
        temporalXorMatrix[index + 2, 0] = xorBit
    }

    return temporalXorMatrix
}

fun main() {
    val xorData = generateTemporalXORData(3)
    println("Temporal XOR (input data):\t${xorData.transpose()}")
    println("Left-shifted Targets:\t\t${xorData.shiftUpAndPadEndWithZero().transpose()}")
}

