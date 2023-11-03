package org.simbrain.network.smile

import org.simbrain.network.core.Network
import smile.data.Tuple
import smile.data.formula.Formula
import smile.data.type.DataType
import smile.data.type.StructField
import smile.data.type.StructType
import smile.io.Read
import smile.regression.cart

class SmileRegressionTest {

    var net = Network()

    // @Test
    fun `test regresson tree`() {
        val iris = Read.arff("simulations/tables/iris.arff")
        val decisionTree = cart(Formula.of("class", "."), iris)
        // (0 until iris.nrow()).forEach { i ->
        //     println("${iris[i]} -> ${decisionTree.predict(iris.get(i))}")
        //     println("${decisionTree.predict(iris.get(i))}")
        // }
        val schema = StructType(
            StructField("sepallength", DataType.of(Double::class.java)),
            StructField("sepalwidth", DataType.of(Double::class.java)),
            StructField("petallength", DataType.of(Double::class.java)),
            StructField("petalwidth", DataType.of(Double::class.java))
        )
        // TODO: Get the label
        val result = decisionTree.predict(Tuple.of(doubleArrayOf(6.0,2.2,5.0,1.5), schema))
        println("result = $result")
    }


}