package org.simbrain.network.smile

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.smile.classifiers.SVMClassifier
import smile.data.Tuple
import smile.data.formula.Formula
import smile.data.type.DataType
import smile.data.type.StructField
import smile.data.type.StructType
import smile.io.Read
import smile.math.matrix.Matrix
import smile.regression.cart

class SmileClassifierTest {

    var net = Network()

    /**
     * Create a trained SVM (on xor) for testing. Use a weirdly shaped 3x2 xor for better tests.
     */
    var xorSVM = SmileClassifier(net, SVMClassifier(), 3, 2).apply {
        this.trainingInputs = arrayOf(
            doubleArrayOf(0.0, 0.0, 0.0),
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(1.0, 1.0, 0.0)
        )
        this.trainingTargets = intArrayOf(-1, 1, 1, -1)
        this.train()
    }

    @BeforeEach
    internal fun setUp() {
        net = Network()
        xorSVM.clear()
    }


    @Test
    fun testInit() {
        val classifier = SmileClassifier(net, SVMClassifier(), 4, 2)
        net.addNetworkModel(classifier)
        classifier.addInputs(Matrix(doubleArrayOf(1.0,2.0,3.0,4.0)))
        assertEquals(10.0, classifier.inputs.sum())
        net.update()
        assertEquals(2, classifier.outputs.size())
    }

    @Test
    fun `test SVM XOR`() {
        net.addNetworkModel(xorSVM)
        xorSVM.addInputs(Matrix(doubleArrayOf(0.0, 0.0, 0.0)))
        net.update()
        assertArrayEquals(doubleArrayOf(1.0, 0.0), xorSVM.outputs.col(0))
        xorSVM.addInputs(Matrix(doubleArrayOf(1.0, 0.0, 0.0)))
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 1.0), xorSVM.outputs.col(0))
        xorSVM.addInputs(Matrix(doubleArrayOf(0.0, 1.0, 0.0)))
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 1.0), xorSVM.outputs.col(0))
        xorSVM.addInputs(Matrix(doubleArrayOf(1.0, 1.0, 0.0)))
        net.update()
        assertArrayEquals(doubleArrayOf(1.0, 0.0), xorSVM.outputs.col(0))
    }

    @Test
    fun `test connections to neuron array`() {

        // Set up
        // inputNa -> wm1 -> xorSVM -> wm2 -> outputNa
        val inputNa = NeuronArray(net, 3)
        inputNa.clear() // neuron arrays are randomized by default
        val wm1 = WeightMatrix(net, inputNa, xorSVM)
        wm1.diagonalize()
        val outputNa = NeuronArray(net, 2)
        outputNa.clear()
        val wm2 = WeightMatrix(net, xorSVM, outputNa)
        wm2.diagonalize()
        net.addNetworkModels(listOf(inputNa, wm1, xorSVM, wm2, outputNa))

        // Set inputs
        inputNa.addInputs(Matrix(doubleArrayOf(0.0, 1.0, 0.0)))

        // Expected values after one update
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 1.0, 0.0),inputNa.activations.col(0), .001)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),xorSVM.inputs.col(0), .001)
        assertArrayEquals(doubleArrayOf(1.0, 0.0),xorSVM.outputs.col(0), .001)
        assertArrayEquals(doubleArrayOf(0.0, 0.0),outputNa.activations.col(0), .001)

        // Expected values after two updates
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),inputNa.activations.col(0), .001)
        // Inputs are immediately cleared. But an event is fired so that in the GUI the input would here
        // be seen as 0,1,0
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),xorSVM.inputs.col(0))
        assertArrayEquals(doubleArrayOf(0.0, 1.0),xorSVM.outputs.col(0))
        // (1,0) has propagated from last update
        assertArrayEquals(doubleArrayOf(1.0, 0.0),outputNa.activations.col(0))

        // Expected values after three updates
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),inputNa.activations.col(0))
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),xorSVM.inputs.col(0))
        assertArrayEquals(doubleArrayOf(1.0, 0.0),xorSVM.outputs.col(0))
        assertArrayEquals(doubleArrayOf(0.0, 1.0),outputNa.activations.col(0))

        // Expected values after four updates
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),inputNa.activations.col(0))
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),xorSVM.inputs.col(0))
        assertArrayEquals(doubleArrayOf(1.0, 0.0),xorSVM.outputs.col(0))
        assertArrayEquals(doubleArrayOf(1.0, 0.0),outputNa.activations.col(0))

        // TODO: A second version of this test using neurongroups or neuron collections
    }


    @Test
    fun `test decision tree`() {
        val iris = Read.arff("simulations/tables/iris.arff")
        val decisionTree = cart(Formula.of("class", "."), iris)
        (0 until iris.nrows()).forEach { i ->
            // println("${iris[i]} -> ${decisionTree.predict(iris.get(i))}")
            // println("${decisionTree.predict(iris.get(i))}")
        }
        val schema = StructType(
            StructField("sepallength", DataType.of(Int.javaClass)),
            StructField("sepalwidth", DataType.of(Int.javaClass)),
            StructField("petallength", DataType.of(Int.javaClass)),
            StructField("petalwidth", DataType.of(Int.javaClass))
        )
        // TODO: Get the label
        val result = decisionTree.predict(Tuple.of(doubleArrayOf(6.0,2.2,5.0,1.5), schema))
        println("result = $result")
    }
}