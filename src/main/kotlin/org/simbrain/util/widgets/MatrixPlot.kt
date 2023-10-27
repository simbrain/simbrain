package org.simbrain.util.widgets

import org.simbrain.util.displayInDialog
import org.simbrain.util.flatten
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.toSimbrainColor
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JPanel
import javax.swing.JScrollPane
import kotlin.math.abs
import kotlin.random.Random

/**
 * Produce something like an R Corrplot.
 *
 * @param labels column and row headings
 * @param data the matrix data to represent
 */
class MatrixPlot(private val labels: List<String>, private val data: Array<DoubleArray>) : JPanel() {

    private val maxCellSize = 100
    private val minCellSize = 50
    private var currentCellSize = 50

    private val magnitude = data.flatten().maxOf { abs(it) }

    var properties = MatrixPlotProperties()

    init {
        adjustCellSize()
    }

    private fun adjustCellSize() {
        val frameSize = parent?.width ?: 800  // Assuming default width if parent not yet available
        currentCellSize = (frameSize / (labels.size + 2)).coerceIn(minCellSize, maxCellSize)
        val totalSize = (labels.size + 2) * currentCellSize
        preferredSize = Dimension(totalSize, totalSize)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        adjustCellSize()  // Adjust cell size dynamically based on the frame's current size

        // Drawing the matrix cells
        for (i in labels.indices) {
            for (j in labels.indices) {
                val value = data[i][j]
                g.color = Color(value.toSimbrainColor(
                    if (properties.fixedColorScale) {
                        properties.minValue..properties.maxValue
                    } else {
                        -magnitude..magnitude
                    }
                ))
                g.fillRect(currentCellSize + j * currentCellSize, currentCellSize + i * currentCellSize, currentCellSize, currentCellSize)
                g.color = Color.BLACK
                if (currentCellSize >= 40) {
                    g.drawString("%.2f".format(value), currentCellSize + j * currentCellSize + 10, currentCellSize + i * currentCellSize + 30)
                }
            }
        }

        // Drawing the labels
        for (i in labels.indices) {
            g.drawString(labels[i], i * currentCellSize + 10 + currentCellSize, currentCellSize - 10)
            g.drawString(labels[i], 10, i * currentCellSize + 30 + currentCellSize)
        }
    }
}

class MatrixPlotProperties: EditableObject {
    var fixedColorScale by GuiEditable(
        initValue = false,
        description = "If false use min and max values of data to set the color ranges of the heat map."
    )

    var minValue by GuiEditable(
        initValue = -1.0,
        max = 0.0,
        conditionallyEnabledBy = MatrixPlotProperties::fixedColorScale
    )

    var maxValue by GuiEditable(
        initValue = 1.0,
        min = 0.0,
        conditionallyEnabledBy = MatrixPlotProperties::fixedColorScale
    )
}

fun main() {
    val labels = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
    val data = Array(labels.size) { DoubleArray(10) { Random.nextDouble(-1.0, 1.0) } }
    JScrollPane(MatrixPlot(labels, data)).apply { border = null }.displayInDialog()
}

// fun main() {
//
//     val text = "The cat can run. The dog can run. The cat eats food. The dog eats food. Please bring lunch to the " +
//             "table."
//     val coc = generateCooccurrenceMatrix(text, 2, true)
//     MatrixPlot(coc.first, coc.second).displayInDialog()
//
// }