package org.simbrain.network.smile

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import org.simbrain.util.Utils.round
import org.simbrain.util.table.DataFrameWrapper
import smile.read
import kotlin.random.Random

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
        // csv(file: String, delimiter: Char = ',', header: Boolean = true, quote: Char = '"', escape: Char =
        // '\\', schema: StructType? = null): DataFram
        val data = DataFrameWrapper(read.csv("simulations/tables/bank-full.csv", header = true))

        val targets = data.getIntColumn(data.columnCount-1)
        val inputs = data.get2DDoubleArray(listOf(0, 5, 11, 12, 13, 14))

        // Fit the model
        val lr = LogisticRegClassifier()
        lr.fit(inputs, targets)
        println("\nModel Accuracy: ${round(lr.stats.toDouble(), 3)}\n")

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

}