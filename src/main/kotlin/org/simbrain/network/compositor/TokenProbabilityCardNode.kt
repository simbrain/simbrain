package org.simbrain.network.compositor

import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.network.gui.ArrowDirection
import org.simbrain.network.gui.createArrowButton
import org.simbrain.network.gui.nodes.NEURON_DIAMETER
import org.simbrain.network.gui.nodes.NeuronCircleNode
import org.simbrain.util.NetworkTheme
import org.simbrain.util.Theme
import java.awt.geom.Point2D

data class TokenProbabilityCardStyle(
    val title: String = "next-token probabilities",
    val width: Double = 230.0,
    val height: Double = 300.0,
    val columns: Int = 5,
    val visibleRows: Int = 4,
)

/** Compact canvas card for a copied next-token distribution, one neuron circle per token. */
class TokenProbabilityCardNode(
    private val tokenLabel: (Int) -> String,
    private val style: TokenProbabilityCardStyle = TokenProbabilityCardStyle(),
    private val scalingFactor: () -> Double = { 1.0 },
) : PNode() {

    val cardWidth get() = style.width
    val cardHeight get() = style.height

    private val background = PPath.createRectangle(0.0, 0.0, style.width, style.height).apply {
        paint = NetworkTheme.current.canvasBackground
        strokePaint = NetworkTheme.current.subnetOutline
    }.also { addChild(it) }
    private val content = PNode().also { addChild(it) }
    private var snapshot: TokenProbabilitySnapshot? = null
    private var firstVisible = 0
    private var dragOffset: Point2D? = null
    var onMoved: (() -> Unit)? = null
    var onMoveFinished: (() -> Unit)? = null
    private val previous = createArrowButton(ArrowDirection.LEFT) { scroll(-style.visibleRows) }.also { addChild(it) }
    private val next = createArrowButton(ArrowDirection.RIGHT) { scroll(style.visibleRows) }.also { addChild(it) }

    init {
        addChild(PText(style.title).apply {
            font = Theme.small
            textPaint = NetworkTheme.current.valueText
            setOffset(PADDING, PADDING)
            pickable = false
        })
        previous.setOffset(style.width - 44.0, style.height - 22.0)
        next.setOffset(style.width - 22.0, style.height - 22.0)
        addInputEventListener(object : PBasicInputEventHandler() {
            override fun mousePressed(event: PInputEvent) {
                dragOffset = event.getPositionRelativeTo(this@TokenProbabilityCardNode.parent)
                event.isHandled = true
            }

            override fun mouseDragged(event: PInputEvent) {
                val start = dragOffset ?: return
                val point = event.getPositionRelativeTo(this@TokenProbabilityCardNode.parent)
                setOffset(offset.x + point.x - start.x, offset.y + point.y - start.y)
                dragOffset = point
                onMoved?.invoke()
                event.isHandled = true
            }

            override fun mouseReleased(event: PInputEvent) {
                if (dragOffset != null) {
                    dragOffset = null
                    onMoveFinished?.invoke()
                }
                event.isHandled = true
            }

            override fun mouseWheelRotated(event: PInputEvent) {
                scroll(event.wheelRotation * style.visibleRows)
                event.isHandled = true
            }
        })
    }

    fun refresh(value: TokenProbabilitySnapshot?) {
        if (value == snapshot) return
        snapshot = value
        val visible = if (value?.showAll == true) visibleCells else rankedVisibleRows
        firstVisible = firstVisible.coerceAtMost(maxOf(0, (value?.entries?.size ?: 0) - visible))
        rebuild()
    }

    fun scroll(deltaRows: Int) {
        val value = snapshot ?: return
        val delta = if (value.showAll) deltaRows * style.columns
            else Integer.signum(deltaRows) * rankedVisibleRows
        val visible = if (value.showAll) visibleCells else rankedVisibleRows
        firstVisible = (firstVisible + delta).coerceIn(0, maxOf(0, value.entries.size - visible))
        rebuild()
    }

    private fun rebuild() {
        content.removeAllChildren()
        val value = snapshot
        val visible = if (value?.showAll == true) visibleCells else rankedVisibleRows
        previous.visible = value != null && firstVisible > 0
        next.visible = value != null && firstVisible + visible < value.entries.size
        value ?: return
        if (value.showAll) grid(value) else ranked(value)
    }

    private fun grid(value: TokenProbabilitySnapshot) {
        val entries = value.entries.drop(firstVisible).take(visibleCells)
        entries.forEachIndexed { index, entry ->
            val col = index % style.columns
            val row = index / style.columns
            addEntry(value, entry, PADDING + (col + 0.5) * cellStep, HEADER_HEIGHT + (row + 0.5) * cellStep, true)
        }
        if (value.entries.size > visibleCells) {
            content.addChild(PText("${firstVisible + 1}-${firstVisible + entries.size} / ${value.entries.size}").apply {
                font = Theme.tiny
                textPaint = NetworkTheme.current.valueText
                setOffset(PADDING, style.height - 15.0)
                pickable = false
            })
        }
    }

    private fun ranked(value: TokenProbabilitySnapshot) {
        val entries = value.entries.drop(firstVisible).take(rankedVisibleRows)
        entries.forEachIndexed { index, entry ->
            addEntry(value, entry, PADDING + RADIUS, HEADER_HEIGHT + index * ROW_STEP + RADIUS, false)
        }
        if (value.entries.size > rankedVisibleRows) {
            content.addChild(PText("${firstVisible + 1}-${firstVisible + entries.size} / ${value.entries.size}").apply {
                font = Theme.tiny
                textPaint = NetworkTheme.current.valueText
                setOffset(PADDING, style.height - 15.0)
                pickable = false
            })
        }
    }

    /** One neuron circle centered at (x, y); grid cells label above, ranked rows label beside. */
    private fun addEntry(value: TokenProbabilitySnapshot, entry: TokenProbabilitySnapshot.Entry, x: Double, y: Double, grid: Boolean) {
        val sampled = entry.tokenId == value.sampledTokenId
        val label = tokenLabel(entry.tokenId).replace("\n", " ").take(if (grid) 11 else 28)
        content.addChild(NeuronCircleNode(scalingFactor).apply {
            customStrokeColor = if (sampled) NetworkTheme.current.sourceHandle else null
            setClamped(sampled)
            drawActivation(entry.probability, -1.0..1.0)
            if (grid) setLabel(label)
            setOffset(x, y)
            pickable = false
        })
        if (!grid) {
            content.addChild(PText(label).apply {
                font = Theme.tiny
                textPaint = NetworkTheme.current.valueText
                setOffset(x + RADIUS + 6.0, y - 6.0)
                pickable = false
            })
        }
    }

    private val cellStep get() = (style.width - 2 * PADDING) / style.columns
    private val visibleCells get() = style.columns * style.visibleRows

    /** Rows the ranked list can show inside the card's fixed height; the rest page. */
    private val rankedVisibleRows get() =
        maxOf(1, ((style.height - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_STEP).toInt())

    companion object {
        private const val PADDING = 10.0
        private const val HEADER_HEIGHT = 32.0
        private const val FOOTER_HEIGHT = 24.0
        private const val RADIUS = NEURON_DIAMETER / 2.0
        private const val ROW_STEP = NEURON_DIAMETER + 8.0
    }
}
