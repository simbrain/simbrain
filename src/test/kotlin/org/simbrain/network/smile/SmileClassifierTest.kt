package org.simbrain.network.smile

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.Utils
import org.simbrain.util.table.SmileDataFrame
import org.simbrain.util.toDoubleArray
import smile.classification.DecisionTree
import smile.classification.NaiveBayes
import smile.classification.SVM
import smile.data.Tuple
import smile.data.formula.Formula
import smile.io.Read
import smile.math.matrix.Matrix
import smile.read
import smile.stat.distribution.GaussianDistribution
import kotlin.random.Random

/**
 * Also see SmileTest.java and SmileRegressionTest.kt
 */
class SmileClassifierTest {

    var net = Network()

    /**
     * Create a trained SVM (on xor) for testing. Use a weirdly shaped 3x2 xor for better tests.
     */
    val svm = SVMClassifier(3).apply {
        this.trainingData.featureVectors = arrayOf(
            doubleArrayOf(0.0, 0.0, 0.0),
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(1.0, 1.0, 0.0)
        )
        this.trainingData.setIntegerTargets(intArrayOf(-1, 1, 1, -1))
    }
    var xorSVM = SmileClassifier(svm)

    @BeforeEach
    internal fun setUp() {
        net = Network()
        xorSVM.clear()
        xorSVM.train()
    }

    @Test
    fun testInit() {
        val classifier = SmileClassifier(SVMClassifier(4))
        net.addNetworkModel(classifier)
        classifier.addInputs(Matrix.column(doubleArrayOf(1.0,2.0,3.0,4.0)))
        assertEquals(10.0, classifier.inputs.sum())
        net.update()
        assertEquals(2, classifier.activations.size())
    }

    @Test
    fun `test SVM XOR`() {
        net.addNetworkModel(xorSVM)
        xorSVM.addInputs(Matrix.column(doubleArrayOf(0.0, 0.0, 0.0)))
        net.update()
        assertArrayEquals(doubleArrayOf(1.0, 0.0), xorSVM.activations.toDoubleArray())
        xorSVM.addInputs(Matrix.column(doubleArrayOf(1.0, 0.0, 0.0)))
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 1.0), xorSVM.activations.toDoubleArray())
        xorSVM.addInputs(Matrix.column(doubleArrayOf(0.0, 1.0, 0.0)))
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 1.0), xorSVM.activations.toDoubleArray())
        xorSVM.addInputs(Matrix.column(doubleArrayOf(1.0, 1.0, 0.0)))
        net.update()
        assertArrayEquals(doubleArrayOf(1.0, 0.0), xorSVM.activations.toDoubleArray())
    }

    @Test
    fun `test connections to neuron array`() {

        // Set up
        // inputNa -> wm1 -> xorSVM -> wm2 -> outputNa
        val inputNa = NeuronArray(3)
        inputNa.clear() // neuron arrays are randomized by default
        val wm1 = WeightMatrix(inputNa, xorSVM)
        wm1.diagonalize()
        val outputNa = NeuronArray(2)
        outputNa.clear()
        val wm2 = WeightMatrix(xorSVM, outputNa)
        wm2.diagonalize()
        net.addNetworkModels(listOf(inputNa, wm1, xorSVM, wm2, outputNa))

        // Set inputs
        inputNa.addInputs(Matrix.column(doubleArrayOf(0.0, 1.0, 0.0)))

        // Expected values after one update
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 1.0, 0.0),inputNa.activations.toDoubleArray(), .001)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),xorSVM.inputs.toDoubleArray(), .001)
        assertArrayEquals(doubleArrayOf(1.0, 0.0),xorSVM.activations.toDoubleArray(), .001)
        assertArrayEquals(doubleArrayOf(0.0, 0.0),outputNa.activations.toDoubleArray(), .001)

        // Expected values after two updates
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),inputNa.activations.toDoubleArray(), .001)
        // Inputs are immediately cleared. But an event is fired so that in the GUI the input would here
        // be seen as 0,1,0
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),xorSVM.inputs.toDoubleArray())
        assertArrayEquals(doubleArrayOf(0.0, 1.0),xorSVM.activations.toDoubleArray())
        // (1,0) has propagated from last update
        assertArrayEquals(doubleArrayOf(1.0, 0.0),outputNa.activations.toDoubleArray())

        // Expected values after three updates
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),inputNa.activations.toDoubleArray())
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),xorSVM.inputs.toDoubleArray())
        assertArrayEquals(doubleArrayOf(1.0, 0.0),xorSVM.activations.toDoubleArray())
        assertArrayEquals(doubleArrayOf(0.0, 1.0),outputNa.activations.toDoubleArray())

        // Expected values after four updates
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),inputNa.activations.toDoubleArray())
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0),xorSVM.inputs.toDoubleArray())
        assertArrayEquals(doubleArrayOf(1.0, 0.0),xorSVM.activations.toDoubleArray())
        assertArrayEquals(doubleArrayOf(1.0, 0.0),outputNa.activations.toDoubleArray())

        // TODO: A second version of this test using neurongroups or neuron collections
    }


    // @Test
    fun `test naive bayes`() {
        val nb = NaiveBayes(
            // Prior
            doubleArrayOf(.5,.5),
            // Class conditional distributions. I just made these numbers up
            arrayOf(
                // Men
                arrayOf(GaussianDistribution(5.9, 1.0), GaussianDistribution(180.0,
            1.0)),
                // Women
                arrayOf(GaussianDistribution(5.5, 1.0), GaussianDistribution(115.0,
                    1.0), )))


        // Do a prediction
        println(nb.predict(doubleArrayOf(5.0, 175.0)))
    }

    //@Test
    fun `test logistic regression`() {
        val inputs = arrayOf(
            doubleArrayOf(1.0,0.0,0.0),
            doubleArrayOf(0.0,1.0,0.1),
            doubleArrayOf(0.0,0.0,1.0)
        )
        // println(inputs.contentDeepToString())
        val targets = intArrayOf(1, 2, 3)

        val lr = LogisticRegClassifier(3, 3)
        lr.fit(inputs, targets)
        println(lr.predict(doubleArrayOf(1.0, 0.0, 0.0)))
        println(lr.outputProbabilities.contentToString())
        println(lr.predict(doubleArrayOf(0.0, 1.0, 0.0)))
        println(lr.outputProbabilities.contentToString())
    }

    /**
     * Predict Probability of 'Subscribing to a Term Deposit' based on 'Age, Balance, Duration, Campaign, Pay Days'
     * Based on https://towardsdatascience.com/building-a-logistic-regression-in-python-step-by-step-becd4d56c9c8
     * Dataset Source: http://archive.ics.uci.edu/ml/index.php
     */
    // @Test
    fun `test logistic regression with bank data`() {
        // csv(file: String, delimiter: Char = ',', header: Boolean = true, quote: Char = '"', escape: Char =
        // '\\', schema: StructType? = null): DataFram
        val data = SmileDataFrame(read.csv("simulations/tables/bank-full.csv", header = true))

        val targets = data.getIntColumn(data.columnCount-1)
        val inputs = data.get2DDoubleArray(listOf(0, 5, 11, 12, 13, 14))

        // Fit the model
        val lr = LogisticRegClassifier(inputs.size, 2)
        lr.fit(inputs, targets)
        println("\nModel Accuracy: ${Utils.round(lr.stats.toDouble(), 3)}\n")

        // Make some predictions
        fun predict(rowNum: Int) {
            val rowVector = inputs[rowNum]
            val tar = targets[rowNum]
            val pred = lr.predict(rowVector)
            println("Target $tar - Prediction  $pred = Error ${tar - pred}")
            println("Probabilities: ${lr.outputProbabilities.contentToString()}")
        }
        repeat(10) {
            predict(Random.nextInt(targets.size))
        }
        // println("Percent ones = ${targets.count { v -> v == 1 }.toDouble()/targets.size}")
    }

    @Test
    fun `test decision tree classifier`() {
        val data = SmileDataFrame(Read.arff("simulations/tables/iris.arff"))
        val decisionTree = DecisionTree.fit(Formula.of("class", "."), data.df)
        println(decisionTree.predict(Tuple.of(doubleArrayOf(5.4,3.9,1.3,0.4), data.df.schema())))
        // Use debugger to get a sense of what this has
        // Could find a way to make the inferred tree human readable, would be cool
    }

    // @Test
    fun `sandbox SVM Basic xor`() {
        val inputs = arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(1.0, 1.0)
        )
        val targets = intArrayOf(-1,1,1,-1)
        // val targets = intArrayOf(0,1,1,0) // Causes exception. Labels must be 1 or -1
        val svm = SVM.fit(inputs, targets, 1000.0, .001)
        var result = svm.predict(doubleArrayOf(1.0, 1.0))
        println(result)
        var result2 = svm.predict(doubleArrayOf(1.0, 0.0))
        println(result2)

    }
}