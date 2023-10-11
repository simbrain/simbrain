package org.simbrain.util.widgets

import org.simbrain.util.displayInDialog
import org.simbrain.util.toSimbrainColor
import smile.math.matrix.Matrix
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.random.Random

/**
 * Produce something like an R Corrplot.
 *
 * @param labels column and row headings
 * @param data the matrix data to represent
 */
class MatrixPlot(private val labels: List<String>, private val data: Array<DoubleArray>) : JPanel() {

    private val cellSize = 50
    private val labelOffset = cellSize

    /**
     * Construct with a Smile Matrix
     */
    constructor(labels: List<String>, data: Matrix): this(labels, data.toArray())

    init {
        if (labels.size != data.size) {
            throw IllegalArgumentException("Number of labels (${labels.size}) does not match the size of data (${data.size}).")
        }
        preferredSize = Dimension((labels.size + 2) * 50, (labels.size + 2) * 50)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        // Drawing the matrix cells
        for (i in labels.indices) {
            for (j in labels.indices) {
                val value = data[i][j]
                g.color = Color(value.toSimbrainColor())
                g.fillRect(labelOffset + j * cellSize, labelOffset + i * cellSize, cellSize, cellSize)
                g.color = abs(value).let { if (it > 0.75) Color.WHITE else Color.BLACK }
                g.drawString("%.2f".format(value), labelOffset + j * cellSize + 10, labelOffset + i * cellSize + 30)
            }
        }

        // Drawing the labels
        for (i in labels.indices) {
            g.color = Color.BLACK
            g.drawString(labels[i], i * cellSize + 10 + labelOffset, labelOffset - 10)
            g.drawString(labels[i], 10, i * cellSize + 30 + labelOffset)
        }
    }
}

fun main() {
    val labels = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
    val data = Array(10) { DoubleArray(10) { Random.nextDouble(-1.0, 1.0) } }
    MatrixPlot(labels, data).displayInDialog()
}

// fun main() {
//
//     val text = "The cat can run. The dog can run. The cat eats food. The dog eats food. Please bring lunch to the " +
//             "table."
//     val coc = generateCooccurrenceMatrix(text, 2, true)
//     MatrixPlot(coc.first, coc.second).displayInDialog()
//
// }