package org.simbrain.world.textworld.gui

import net.miginfocom.layout.CC
import net.miginfocom.swing.MigLayout
import org.simbrain.util.*
import org.simbrain.world.textworld.TextWorld
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

// TO DO: Best way to specify distance function?
//        Capitalization issue

fun TextWorld.showComparisonDialog(): StandardDialog {
    return StandardDialog().apply {

        title = "Compare Word Vectors"
        contentPane = JPanel()
        layout = MigLayout("ins 0, gap 20px 20px")

        val filteredTerms = removeStopWords(tokenVectorMap.tokensMap.keys.toList())

        val word1cb = JComboBox(filteredTerms.toTypedArray())
        val word2cb = JComboBox(filteredTerms.toTypedArray())
        val similarityMethod = JComboBox(arrayOf("Cosine Similarity", "Dot Product", "Euclidean Distance"))
        val similarity = JLabel("Similarity: ")

        val compareWords = createAction {
            val vec1 = tokenVectorMap.get(word1cb.selectedItem as String)
            val vec2 = tokenVectorMap.get(word2cb.selectedItem as String)
            val methodType = similarityMethod.selectedItem as String
            // println(methodType)
            // println("Vector 1: ${vec1.contentToString()}")
            // println("Vector 2: ${vec2.contentToString()}")

            // smile.math.MathEx::cos, smile.math.MathEx::dot, smile.math.MathEx::distance
            similarity.text = "Similarity: ${embeddingSimilarity(vec1, vec2, smile.math.MathEx::cos).format(3)}"
            pack()
        }
        word1cb.action = compareWords
        word2cb.action = compareWords
        similarityMethod.action = compareWords

        contentPane.add(word1cb)
        contentPane.add(JLabel("and"), "center")
        contentPane.add(word2cb, "wrap")
        contentPane.add(JLabel("Method:"))
        contentPane.add(similarityMethod, "wrap")
        contentPane.add(similarity, "wrap")
    }
}

fun main() {
    val world = TextWorld()
    world.showComparisonDialog().display()
}