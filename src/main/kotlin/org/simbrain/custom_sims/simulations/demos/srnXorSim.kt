package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.trainers.IterableTrainer
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.shiftUpAndPadEndWithZero
import smile.math.matrix.Matrix
import kotlin.random.Random

/**
 * Simulate temporal xor in a simple recurrent network as described by Elman (1990).
 */
val srnXORSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val srn = SRNNetwork(network, 1, 2, 1)
    network.addNetworkModel(srn)

    // Load with xor data
    val xorInputs = generateTemporalXORData(100)
    srn.trainingSet = MatrixDataset(xorInputs, xorInputs.shiftUpAndPadEndWithZero())
    srn.trainer.updateType = IterableTrainer.UpdateMethod.STOCHASTIC

    // Train
    repeat(20) {
        srn.trainer.iterate()
        if (it % 10 == 0) {
            println("iteration ${it}: ${srn.trainer.error}")
        }
    }

    // Load input data into input array
    srn.inputLayer.inputData = xorInputs

    // TODO. Run a performance test where all the input is run back through, or new inputs are used, and the
    // output error / rmse is shown

    withGui {
        place(networkComponent) {
            location = point(460, 0)
            width = 500
            height = 550
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

