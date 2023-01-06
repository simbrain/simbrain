package org.simbrain.world.textworld.gui

import net.miginfocom.layout.CC
import net.miginfocom.swing.MigLayout
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.display
import org.simbrain.util.embeddingSimilarity
import org.simbrain.world.textworld.TextWorld
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

fun Double.format(digits: Int) = "%.${digits}f".format(this) // Not sure where to put this.

fun TextWorld.showComparisonDialog(): StandardDialog {
    return StandardDialog().apply {

        title = "Compare Word Vectors"
        contentPane = JPanel()
        // layout = MigLayout("Debug")
        layout = MigLayout()

        val word1cb = JComboBox(tokenVectorMap.tokensMap.keys.toTypedArray())
        val word2cb = JComboBox(tokenVectorMap.tokensMap.keys.toTypedArray())
        val similarityMethod = JComboBox(arrayOf("Cosine Similarity", "Dot Product"))
        // val calculateButton = JButton("Calculate!") // Text not showing when line 41 is active?
        val similarity = JLabel("Similarity: ")

        val compareWords = createAction {
            val vec1 = tokenVectorMap.get(word1cb.selectedItem as String)
            val vec2 = tokenVectorMap.get(word2cb.selectedItem as String)
            val methodType = similarityMethod.selectedItem as String
            println(methodType)
            println("Vector 1: ${vec1.contentToString()}")
            println("Vector 2: ${vec2.contentToString()}")
            similarity.text = "Similarity: ${embeddingSimilarity(vec1, vec2, methodType == "Cosine Similarity").format(3)}"
        }
        word1cb.action = compareWords
        word2cb.action = compareWords
        similarityMethod.action = compareWords
        // calculateButton.action = compareWords

        val componentConstraints = CC()
        componentConstraints.alignX("center").spanX()

        contentPane.add(word1cb)
        contentPane.add(JLabel("and"), "center")
        contentPane.add(word2cb, "wrap")
        contentPane.add(JLabel("Method:"))
        contentPane.add(similarityMethod, "wrap")
        // contentPane.add(JLabel(""),"wrap")
        // contentPane.add(similarity, componentConstraints)
        contentPane.add(similarity, "wrap")
        // contentPane.add(calculateButton)
    }
}

fun main() {
    val world = TextWorld()
    world.showComparisonDialog().display()
}