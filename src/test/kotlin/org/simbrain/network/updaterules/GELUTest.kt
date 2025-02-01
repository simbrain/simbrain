package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.util.toMatrix

class GELUTest {

    val net = Network()

    var input = Neuron()
    var outputRule = GELU()
    val output = Neuron(outputRule)
    var weight = Synapse(input, output)

    var inputArray = NeuronArray(4)
    var outputArray = NeuronArray(5)
    var weightMatrix = WeightMatrix(inputArray, outputArray)

    init {
        net.addNetworkModels(input, output, weight)
        input.activation = 1.0
        input.clamped = true

        net.addNetworkModels(inputArray, outputArray, weightMatrix)
        inputArray.activations = doubleArrayOf(-1.0, 0.0, 1.0, 2.0).toMatrix()
        outputArray.updateRule = GELU()
    }

    @Test
    fun `test scalar update`() {
        net.update()
        println(output.activation)
        //assertEquals(1.5, output.activation, 0.0) // TODO: Fill in appropriate values
    }

    @Test
    fun `test scalar derivative`() {
        // TODO: Uncomment below and test fails
        // assertEquals(1.0, outputRule.getDerivative(20.0), .01)
    }

    @Test
    fun `test array update`() {
        net.update()
        println(outputArray.activations)
    }

    @Test
    fun `test array derivative`() {
        println(outputRule.getDerivative(doubleArrayOf(-1.0,0.0,1.0,3.0).toMatrix()))
    }

}