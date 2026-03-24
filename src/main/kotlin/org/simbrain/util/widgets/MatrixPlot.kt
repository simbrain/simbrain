package org.simbrain.util.widgets

import kotlinx.coroutines.*
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import java.awt.*
import javax.swing.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Produce something like an R Corrplot.
 *
 * Paints directly without an off-screen buffer. When placed in a JScrollPane,
 * only the visible cells are drawn, so it scales to large matrices.
 *
 * @param labels column and row headings
 * @param data the matrix data to represent
 */
class MatrixPlot(private val labels: List<String>, private val data: Array<DoubleArray>) : JPanel() {

    private val cellSize = 50

    private val magnitude = data.flatten().maxOfOrNull { abs(it) }?.coerceAtLeast(1e-12) ?: 1.0

    var properties = MatrixPlotProperties()

    init {
        val totalSize = (labels.size + 2) * cellSize
        preferredSize = Dimension(totalSize, totalSize)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val clip = g2.clipBounds ?: Rectangle(0, 0, width, height)

        // Determine visible cell range to avoid painting the entire matrix
        val colStart = max(0, (clip.x - cellSize) / cellSize - 1)
        val colEnd = min(labels.size - 1, (clip.x + clip.width) / cellSize)
        val rowStart = max(0, (clip.y - cellSize) / cellSize - 1)
        val rowEnd = min(labels.size - 1, (clip.y + clip.height) / cellSize)

        val colorRange = if (properties.fixedColorScale) {
            properties.minValue..properties.maxValue
        } else {
            -magnitude..magnitude
        }

        for (i in rowStart..rowEnd) {
            for (j in colStart..colEnd) {
                val cx = cellSize + j * cellSize
                val cy = cellSize + i * cellSize
                val value = data[i][j]
                g2.color = Color(value.toSimbrainColor(colorRange))
                g2.fillRect(cx, cy, cellSize, cellSize)
                g2.color = Color.BLACK
                g2.drawString("%.2f".format(value), cx + 5, cy + cellSize / 2 + 5)
            }
        }

        // Draw labels only if they intersect the clip region
        for (i in labels.indices) {
            val topLabelX = cellSize + i * cellSize + 5
            if (topLabelX + cellSize >= clip.x && topLabelX <= clip.x + clip.width && clip.y < cellSize) {
                g2.drawString(labels[i], topLabelX, cellSize - 10)
            }
            val leftLabelY = cellSize + i * cellSize + cellSize / 2 + 5
            if (leftLabelY + 15 >= clip.y && leftLabelY - 15 <= clip.y + clip.height && clip.x < cellSize) {
                g2.drawString(labels[i], 5, leftLabelY)
            }
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

class CorrPlotPanel(private val labels: List<String>, private val data: Array<DoubleArray>): JPanel(BorderLayout()) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var computeJob: Job? = null
    private var displayedFunction = "Correlation"
    private var suppressComboAction = false

    var matrixPlot: MatrixPlot? = null

    private val progressBar = JProgressBar(0, 100).apply {
        isStringPainted = true
        string = "Computing..."
        isVisible = false
    }

    private val cancelButton = JButton("Cancel").apply {
        isVisible = false
        addActionListener { computeJob?.cancel() }
    }

    private val progressPanel = JPanel(BorderLayout()).apply {
        add(progressBar, BorderLayout.CENTER)
        add(cancelButton, BorderLayout.EAST)
        isVisible = false
    }

    val matrixPlotPanel = JScrollPane().apply {
        border = null
        verticalScrollBar.unitIncrement = 10
        horizontalScrollBar.unitIncrement = 10
        minimumSize = Dimension(400, 400)
    }

    private val functionComboBox = JComboBox(
        arrayOf("Correlation", "Cosine Similarity", "Covariance", "Dot Product", "Euclidean Distance")
    ).apply {
        addActionListener {
            if (suppressComboAction) return@addActionListener
            val selected = selectedItem as? String ?: return@addActionListener
            computeAndDisplay(selected)
        }
        maximumSize = Dimension(150, preferredSize.height)
    }

    val toolbar = JToolBar().apply {
        isFloatable = false
        add(JLabel("Comparison Function: "))
        add(functionComboBox)
    }

    init {
        preferredSize = Dimension(700, 700)
        add(toolbar, BorderLayout.NORTH)
        add(progressPanel, BorderLayout.SOUTH)
        add(matrixPlotPanel, BorderLayout.CENTER)
        computeAndDisplay("Correlation")
    }

    private fun computeAndDisplay(functionName: String) {
        computeJob?.cancel()
        progressPanel.isVisible = true
        progressBar.isVisible = true
        progressBar.value = 0
        cancelButton.isVisible = true
        functionComboBox.isEnabled = false

        computeJob = scope.launch {
            val onProgress: (Int) -> Unit = { pct ->
                SwingUtilities.invokeLater {
                    progressBar.value = pct
                    progressBar.string = "Computing $functionName... $pct%"
                }
            }
            try {
                val result = when (functionName) {
                    "Correlation" -> computeCorrelationMatrix(data, onProgress)
                    "Covariance" -> computeCovarianceMatrix(data, onProgress)
                    "Cosine Similarity" -> computeCosineSimilarityMatrix(data, onProgress)
                    "Euclidean Distance" -> computeSimilarityMatrix(data, onProgress)
                    "Dot Product" -> computeDotProductMatrix(data, onProgress)
                    else -> computeCorrelationMatrix(data, onProgress)
                }
                matrixPlot = MatrixPlot(labels, result)
                matrixPlotPanel.viewport.view = matrixPlot
                displayedFunction = functionName
                revalidate()
            } catch (_: CancellationException) {
                suppressComboAction = true
                functionComboBox.selectedItem = displayedFunction
                suppressComboAction = false
            } finally {
                progressPanel.isVisible = false
                functionComboBox.isEnabled = true
            }
        }
    }

    fun cancelComputation() {
        computeJob?.cancel()
        scope.cancel()
    }
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