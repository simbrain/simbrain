package org.simbrain.network.smile

import smile.io.Read
import smile.plot.swing.BoxPlot

fun main() {
    val iris = Read.arff("src/main/resources/iris.arff")
    // val canvas = ScatterPlot.of(iris, "sepallength", "sepalwidth", "class", '*').canvas();
    val canvas = BoxPlot.of(iris.floatVector(0).toDoubleArray(),
        iris.floatVector(1).toDoubleArray(),
        iris.floatVector(2).toDoubleArray(),
        iris.floatVector(3).toDoubleArray()
    ).canvas();
    // canvas.setAxisLabels("sepallength", "sepalwidth")
    canvas.window()
}