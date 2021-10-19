package org.simbrain.network.smile

import org.simbrain.util.Utils
import smile.io.Read
import smile.nlp.normalizer.SimpleNormalizer
import smile.nlp.stemmer.PorterStemmer
import smile.nlp.tokenizer.SimpleSentenceSplitter
import smile.nlp.tokenizer.SimpleTokenizer
import smile.plot.swing.BoxPlot

fun main() {
    // boxPlot()
    nlpBasics() // Possibly move to SmileNLPSandbox
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