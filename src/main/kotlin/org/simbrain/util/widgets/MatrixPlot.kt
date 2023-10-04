package org.simbrain.util.widgets

import org.simbrain.util.toSimbrainColor
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.random.Random

class MatrixPlot(private val labels: List<String>, private val data: List<List<Double>>) : JPanel() {
    private val cellSize = 50
    private val labelOffset = cellSize

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
    val data = List(10) {
        List(10) {
            Random.nextDouble(-1.0, 1.0)
        }
    }

    val frame = JFrame("Matrix Plot")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.size = Dimension((labels.size + 2) * 50, (labels.size + 2) * 50)
    frame.add(MatrixPlot(labels, data))
    frame.isVisible = true
}