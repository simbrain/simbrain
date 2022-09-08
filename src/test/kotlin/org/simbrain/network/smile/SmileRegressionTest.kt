package org.simbrain.network.smile

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import smile.io.Read
import java.math.RoundingMode

class SmileRegressionTest {

    var net = Network()

    //@Test
    fun `test logistic regression`() {
        val inputs = arrayOf(
            doubleArrayOf(1.0,0.0,0.0),
            doubleArrayOf(0.0,1.0,0.1),
            doubleArrayOf(0.0,0.0,1.0)
        )
        // println(inputs.contentDeepToString())
        val targets = intArrayOf(1, 2, 3)

        val lr = LogisticRegClassifier()
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
    @Test
    fun `test logistic regression with bank data`() {
        val data = Read.csv("simulations/tables/bank-full.csv")

        // Create Type Arrays
        var targets = IntArray(45211) { 1 }
        val inputs = Array<DoubleArray>(targets.size) { doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0) }

        // Replace input Array with data values
        for (i in 1 until targets.size) {
            inputs[i] = doubleArrayOf(
                data[i][0].toString().toDouble(),
                data[i][5].toString().toDouble(),
                data[i][11].toString().toDouble(),
                data[i][12].toString().toDouble(),
                data[i][13].toString().toDouble(),
                data[i][14].toString().toDouble()
            )
        }

        // Replace target array with data values
        for (i in 1 until targets.size) {
            targets[i] = data[i][17].toString().toInt()
        }

        // Print sizes
        println("Input Size: " + inputs.size)
        println("Target Size: " + targets.size)

        // Call Logistic Regression
        val lr = LogisticRegClassifier()
        // Fit Model
        lr.fit(inputs, targets)

        //Prediction test Function
        fun predictionTest(subjectNumber: Int): DoubleArray {
            return doubleArrayOf(
                data[subjectNumber][0].toString().toDouble(),
                data[subjectNumber][5].toString().toDouble(),
                data[subjectNumber][11].toString().toDouble(),
                data[subjectNumber][12].toString().toDouble(),
                data[subjectNumber][13].toString().toDouble(),
                data[subjectNumber][14].toString().toDouble()
            )
        }

        var accuracy = lr.stats.toDouble() * 100
        accuracy = accuracy.toBigDecimal().setScale(2, RoundingMode.UP).toDouble()

        println("\nModel Accuracy: $accuracy%\n")

        // Predict
        // Subject 87
        println("Target: 1")
        println("Prediction: ${lr.predict(predictionTest(87))}")
        println("Probability Array: ${lr.outputProbabilities.contentToString()}\n")

        // Subject 84
        println("Target: 1")
        println("Prediction: ${lr.predict(predictionTest(84))}")
        println("Probability Array: ${lr.outputProbabilities.contentToString()}\n")

        // Subject 88
        println("Target: 1")
        println("Prediction: ${lr.predict(predictionTest(88))}")
        println("Probability Array: ${lr.outputProbabilities.contentToString()}\n")

        // Subject 169
        println("Target: 1")
        println("Prediction: ${lr.predict(predictionTest(169))}")
        println("Probability Array: ${lr.outputProbabilities.contentToString()}\n")
    }

}