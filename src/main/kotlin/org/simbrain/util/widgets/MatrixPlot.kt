package org.simbrain.util.widgets

import kotlinx.coroutines.*
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Produce something like an R Corrplot.
 *
 * Cells grow to fill the available space and shrink with it down to [minCellSize], below which the plot
 * scrolls. Label margins are sized from the label text, and column labels turn vertical when they would
 * not fit across a cell. Paints directly without an off-screen buffer, and only the visible cells are
 * drawn, so it scales to large matrices. Hovering shows the row and column labels and the cell value.
 *
 * @param labels column and row headings
 * @param data the matrix data to represent
 * @param thumbnails optional images drawn beside each heading, for example a picture of the pattern that row
 * and column represent
 */
class MatrixPlot(
    private val labels: List<String>,
    private val data: Array<DoubleArray>,
    private val thumbnails: List<Image>? = null,
) : JPanel(), Scrollable {

    private val thumbnailSize = 36

    private val minCellSize = if (thumbnails != null) thumbnailSize + 4 else 28

    private val comfortableCellSize = 50

    private val labelGap = 6

    private val magnitude = data.flatten().maxOfOrNull { abs(it) }?.coerceAtLeast(1e-12) ?: 1.0

    var properties = MatrixPlotProperties()

    // The widest value decides whether values fit in a cell, and its sign and integer digits depend only on magnitude
    private val widestValueText = "%.2f".format(-magnitude)

    private val labelMetrics get() = getFontMetrics(font)

    private var measuredFont: Font? = null

    private var measuredLabelWidth = 0

    // Measured once per font rather than on every access, since layout math reads it many times per paint
    private val maxLabelWidth: Int
        get() {
            if (font != measuredFont) {
                measuredFont = font
                measuredLabelWidth = labels.maxOfOrNull { labelMetrics.stringWidth(it) } ?: 0
            }
            return measuredLabelWidth
        }

    private val verticalColumnLabels get() = maxLabelWidth > minCellSize - labelGap

    private val thumbnailBlock get() = if (thumbnails != null) thumbnailSize + labelGap else 0

    private val leftMargin get() = maxLabelWidth + 2 * labelGap + thumbnailBlock

    private val topMargin
        get() = thumbnailBlock + if (verticalColumnLabels) {
            maxLabelWidth + 2 * labelGap
        } else {
            labelMetrics.height + 2 * labelGap
        }

    private val cellSize: Int
        get() {
            val n = labels.size.coerceAtLeast(1)
            return max(minCellSize, min((width - leftMargin) / n, (height - topMargin) / n))
        }

    init {
        preferredSize = sizeForCell(minCellSize)
        ToolTipManager.sharedInstance().registerComponent(this)
    }

    private fun sizeForCell(cell: Int) = Dimension(leftMargin + labels.size * cell, topMargin + labels.size * cell)

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val clip = g2.clipBounds ?: Rectangle(0, 0, width, height)
        val cell = cellSize
        val left = leftMargin
        val top = topMargin

        // Determine visible cell range to avoid painting the entire matrix
        val colStart = max(0, (clip.x - left) / cell)
        val colEnd = min(labels.size - 1, (clip.x + clip.width - left) / cell)
        val rowStart = max(0, (clip.y - top) / cell)
        val rowEnd = min(labels.size - 1, (clip.y + clip.height - top) / cell)

        val colorRange = if (properties.fixedColorScale) {
            properties.minValue..properties.maxValue
        } else {
            -magnitude..magnitude
        }

        val valueFont = font.deriveFont((cell * 0.28f).coerceIn(10f, 18f))
        val valueMetrics = g2.getFontMetrics(valueFont)
        val showValues = valueMetrics.stringWidth(widestValueText) + 4 <= cell
        g2.font = valueFont
        for (i in rowStart..rowEnd) {
            for (j in colStart..colEnd) {
                val cx = left + j * cell
                val cy = top + i * cell
                val value = data[i][j]
                g2.color = Color(value.toSimbrainColor(colorRange))
                g2.fillRect(cx, cy, cell, cell)
                if (showValues) {
                    val text = "%.2f".format(value)
                    g2.color = Color.BLACK
                    g2.drawString(
                        text,
                        cx + (cell - valueMetrics.stringWidth(text)) / 2,
                        cy + (cell + valueMetrics.ascent - valueMetrics.descent) / 2
                    )
                }
            }
        }

        g2.font = font
        val metrics = labelMetrics
        val thumbBlock = thumbnailBlock
        val vertical = verticalColumnLabels
        for (i in rowStart..rowEnd) {
            val label = labels[i]
            g2.color = Theme.foreground
            g2.drawString(
                label,
                left - thumbBlock - labelGap - metrics.stringWidth(label),
                top + i * cell + (cell + metrics.ascent - metrics.descent) / 2
            )
            drawThumbnail(g2, i, left - thumbBlock, top + i * cell + (cell - thumbnailSize) / 2)
        }
        for (j in colStart..colEnd) {
            val label = labels[j]
            val centerX = left + j * cell + cell / 2
            drawThumbnail(g2, j, centerX - thumbnailSize / 2, top - thumbBlock)
            g2.color = Theme.foreground
            if (vertical) {
                val rotated = g2.create() as Graphics2D
                rotated.translate(centerX + (metrics.ascent - metrics.descent) / 2, top - thumbBlock - labelGap)
                rotated.rotate(-Math.PI / 2)
                rotated.drawString(label, 0, 0)
                rotated.dispose()
            } else {
                g2.drawString(
                    label,
                    centerX - metrics.stringWidth(label) / 2,
                    top - thumbBlock - labelGap - metrics.descent
                )
            }
        }
    }

    private fun drawThumbnail(g2: Graphics2D, index: Int, x: Int, y: Int) {
        val image = thumbnails?.getOrNull(index) ?: return
        g2.drawImage(image, x, y, thumbnailSize, thumbnailSize, null)
        g2.color = Color.GRAY
        g2.drawRect(x, y, thumbnailSize, thumbnailSize)
    }

    override fun getToolTipText(event: MouseEvent): String? {
        val cell = cellSize
        val col = Math.floorDiv(event.x - leftMargin, cell)
        val row = Math.floorDiv(event.y - topMargin, cell)
        val colLabel = labels.getOrNull(col)
        val rowLabel = labels.getOrNull(row)
        return when {
            rowLabel != null && colLabel != null -> "$rowLabel × $colLabel: ${"%.3f".format(data[row][col])}"
            event.x < leftMargin && event.y >= topMargin -> rowLabel
            event.y < topMargin && event.x >= leftMargin -> colLabel
            else -> null
        }
    }

    override fun getPreferredScrollableViewportSize(): Dimension = sizeForCell(comfortableCellSize).apply {
        width = min(width, 700)
        height = min(height, 700)
    }

    override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) = 10

    override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) =
        if (orientation == SwingConstants.VERTICAL) visibleRect.height else visibleRect.width

    /**
     * Fill the viewport while minimum-size cells fit in it; otherwise keep the minimum size and scroll.
     */
    private fun fitsViewport(): Boolean {
        val viewport = parent as? JViewport ?: return false
        return viewport.width >= preferredSize.width && viewport.height >= preferredSize.height
    }

    override fun getScrollableTracksViewportWidth() = fitsViewport()

    override fun getScrollableTracksViewportHeight() = fitsViewport()

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

/**
 * A [MatrixPlot] of pairwise comparisons between the rows of [data], with a toolbar for choosing the comparison
 * function. [thumbnails] are passed through to the plot.
 */
class CorrPlotPanel(
    private val labels: List<String>,
    private val data: Array<DoubleArray>,
    private val thumbnails: List<Image>? = null,
): JPanel(BorderLayout()) {

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
                matrixPlot = MatrixPlot(labels, result, thumbnails)
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