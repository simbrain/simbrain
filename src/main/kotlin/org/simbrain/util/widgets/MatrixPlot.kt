package org.simbrain.util.widgets

import org.simbrain.util.displayInDialog
import org.simbrain.util.flatten
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.toSimbrainColor
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
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

    private var buffer: BufferedImage? = null

    init {
        adjustCellSize()
    }

    private fun adjustCellSize() {
        val frameSize = parent?.width ?: 800  // Assuming default width if parent not yet available
        currentCellSize = (frameSize / (labels.size + 2)).coerceIn(minCellSize, maxCellSize)
        val totalSize = (labels.size + 2) * currentCellSize
        preferredSize = Dimension(totalSize, totalSize)
    }

    private fun rebuildBuffer() {
        // Adjust cell size and other calculations as needed before rebuilding the buffer
        adjustCellSize()

        val transform = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.defaultTransform
        val widthScaled = (width * transform.scaleX).toInt()
        val heightScaled = (height * transform.scaleY).toInt()

        buffer = BufferedImage(widthScaled, heightScaled, BufferedImage.TYPE_INT_ARGB)
        val g = buffer!!.createGraphics()
        g.transform = transform

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

        g.dispose() // Dispose of the graphics context to release resources
    }

    private fun shouldRebuildBuffer(): Boolean {
        val transform = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.defaultTransform
        val widthScaled = (width * transform.scaleX).toInt()
        val heightScaled = (height * transform.scaleY).toInt()
        return buffer == null ||
                buffer!!.width != widthScaled ||
                buffer!!.height != heightScaled // Add other conditions as needed
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (buffer == null || shouldRebuildBuffer()) { // Check if the buffer needs to be rebuilt
            rebuildBuffer()
        }

        g.drawImage(buffer, 0, 0, width, height, this)
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
    val size = 50
    val labels = List(size) { "$it" }
    val data = Array(labels.size) { DoubleArray(size) { Random.nextDouble(-1.0, 1.0) } }
    JScrollPane(MatrixPlot(labels, data)).apply {
        border = null
        verticalScrollBar.unitIncrement = 10
        horizontalScrollBar.unitIncrement = 10
    }.displayInDialog()
}

// fun main() {
//
//     val text = "The cat can run. The dog can run. The cat eats food. The dog eats food. Please bring lunch to the " +
//             "table."
//     val coc = generateCooccurrenceMatrix(text, 2, true)
//     MatrixPlot(coc.first, coc.second).displayInDialog()
//
// }