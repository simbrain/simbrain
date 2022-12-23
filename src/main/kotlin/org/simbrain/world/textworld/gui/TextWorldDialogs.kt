package org.simbrain.world.textworld.gui

import net.miginfocom.swing.MigLayout
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.display
import org.simbrain.util.embeddingSimilarity
import org.simbrain.world.textworld.TextWorld
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

fun TextWorld.showComparisonDialog(): StandardDialog {
    return StandardDialog().apply {

        title = "Compare Word Vectors"
        contentPane = JPanel()
        layout = MigLayout("Debug")

        val word1cb = JComboBox(tokenVectorMap.tokensMap.keys.toTypedArray())
        val word2cb = JComboBox(tokenVectorMap.tokensMap.keys.toTypedArray())
        val similarity = JLabel("Similarity: ")

        val compareWords = createAction {
            val vec1 = tokenVectorMap.get(word1cb.selectedItem as String)
            val vec2 = tokenVectorMap.get(word2cb.selectedItem as String)
            println("Vector 1: ${vec1.contentToString()}")
            println("Vector 2: ${vec2.contentToString()}")
            similarity.text = "Similarity: ${embeddingSimilarity(vec1, vec2)}"
        }
        word1cb.action = compareWords
        word2cb.action = compareWords

        contentPane.add(word1cb)
        contentPane.add(JLabel("and"))
        contentPane.add(word2cb, "wrap")
        contentPane.add(similarity)
    }
}

fun main() {
    val world = TextWorld()
    world.showComparisonDialog().display()
}