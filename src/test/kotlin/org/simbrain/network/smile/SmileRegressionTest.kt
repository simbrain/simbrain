package org.simbrain.network.smile

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.smile.classifiers.LogisticRegClassifier

class SmileRegressionTest {

    var net = Network()

    @Test
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


}