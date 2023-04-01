package org.simbrain.world.textworld.gui

import net.miginfocom.swing.MigLayout
import org.simbrain.util.*
import org.simbrain.world.textworld.TextWorld
import smile.math.MathEx
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

// TODO: Enum for distance types
fun TextWorld.showComparisonDialog(): StandardDialog {
    return StandardDialog().apply {

        title = "Compare Word Vectors"
        contentPane = JPanel()
        layout = MigLayout("ins 0, gap 20px 20px")

        val filteredTerms = removeStopWords(tokenVectorMap.tokensMap.keys.toList())

        var distanceFunction: (DoubleArray, DoubleArray) -> Double = MathEx::cos

        val word1cb = JComboBox(filteredTerms.toTypedArray())
        val word2cb = JComboBox(filteredTerms.toTypedArray())

        val similarityMethod = JComboBox(arrayOf("Cosine Similarity", "Dot Product", "Euclidean Distance"))
        val similarity = JLabel("Similarity: ")

        fun updatePanel() {
            val vec1 = tokenVectorMap.get(word1cb.selectedItem as String)
            val vec2 = tokenVectorMap.get(word2cb.selectedItem as String)
            when (similarityMethod.selectedItem as String) {
                "Cosine Similarity" -> {
                    distanceFunction = MathEx::cos
                }
                "Dot Product" -> {
                    distanceFunction = MathEx::dot
                }
                "Euclidean Distance" -> {
                    distanceFunction = MathEx::distance
                }
            }

            word1cb.toolTipText = vec1.format(2)
            word2cb.toolTipText = vec2.format(2)

            similarity.text = "Similarity: ${embeddingSimilarity(vec1, vec2, distanceFunction).format(3)}"
            pack()

        }
        val compareWords = createAction {
            updatePanel()
        }
        word1cb.action = compareWords
        word2cb.action = compareWords
        similarityMethod.action = compareWords
        updatePanel()

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