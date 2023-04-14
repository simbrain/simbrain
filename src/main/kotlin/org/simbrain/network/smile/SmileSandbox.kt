package org.simbrain.network.smile

import org.simbrain.util.Utils
import smile.io.Read
import smile.math.matrix.Matrix
import smile.nlp.normalizer.SimpleNormalizer
import smile.nlp.stemmer.PorterStemmer
import smile.nlp.tokenizer.SimpleSentenceSplitter
import smile.nlp.tokenizer.SimpleTokenizer
import smile.plot.swing.BoxPlot

// To see examples of basic Smile matrix operations see
// SmileNLPSandbox, SmileTest, SmileTestKt, SmileUtils, SpikeResponderMatrixTest

fun main() {
    // boxPlot()
    // nlpBasics()
    matrixBasics()
}

fun matrixBasics() {
    val zeros = Matrix(5,4)
    println("zeros: $zeros")
    val filled = Matrix(5,4, 2.0)
    println("filled: $filled")
    val diagonal = Matrix.eye(5)
    println("diagonal: $diagonal")
    val specific = Matrix.of(arrayOf(
        doubleArrayOf(1.0, 2.0, 3.0),
        doubleArrayOf(4.0, 5.0, 6.0),
        doubleArrayOf(7.0, 8.0, 9.0)
    ))
    println("specific: $specific")
}


fun nlpBasics() {
    val speech = Utils.getTextFromURL("https://radiochemistry.org/speech_archives/text/king.shtml")
    var text = SimpleNormalizer.getInstance().normalize(speech)
    var sentences = SimpleSentenceSplitter.getInstance().split(text)
    // println(sentences.joinToString("\n"))

    // TODO: Below failed
    var tokenizer = SimpleTokenizer(true)
    var words =  sentences
        .map { s -> tokenizer.split(s) }
        // .filter{w -> !(EnglishStopWords.DEFAULT.contains(w.lo) || EnglishPunctuations.getInstance().contains(w))}
        .toTypedArray()
    println(words.contentDeepToString())

    // Word2Vec(words, arrayOf(floatArrayOf(0f,0f)))

    var porter = PorterStemmer()
    // println(porter.stem("Awesomeness"))

}

fun boxPlot() {
    val iris = Read.arff("simulations/tables/iris.arff")
    // val canvas = ScatterPlot.of(iris, "sepallength", "sepalwidth", "class", '*').canvas();
    val canvas = BoxPlot.of(
        iris.floatVector(0).toDoubleArray(),
        iris.floatVector(1).toDoubleArray(),
        iris.floatVector(2).toDoubleArray(),
        iris.floatVector(3).toDoubleArray()
    ).canvas();
    // canvas.setAxisLabels("sepallength", "sepalwidth")
    canvas.window()
}