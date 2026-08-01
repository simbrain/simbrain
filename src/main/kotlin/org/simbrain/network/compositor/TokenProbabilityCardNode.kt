package org.simbrain.network.compositor

import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.network.gui.ArrowDirection
import org.simbrain.network.gui.createArrowButton
import org.simbrain.util.NetworkTheme
import org.simbrain.util.Theme
import org.simbrain.util.toSimbrainColor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Point2D

data class TokenProbabilityCardStyle(
    val title: String = "next-token probabilities",
    val width: Double = 230.0,
    val height: Double = 210.0,
    val columns: Int = 5,
    val visibleRows: Int = 4,
)

/** Compact canvas card for a copied next-token distribution. */
class TokenProbabilityCardNode(
    private val tokenLabel: (Int) -> String,
    private val style: TokenProbabilityCardStyle = TokenProbabilityCardStyle(),
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
                dragOffset = null
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
        firstVisible = firstVisible.coerceAtMost(maxOf(0, (value?.entries?.size ?: 0) - visibleCells))
        rebuild()
    }

    fun scroll(deltaRows: Int) {
        val value = snapshot ?: return
        if (!value.showAll) return
        firstVisible = (firstVisible + deltaRows * style.columns)
            .coerceIn(0, maxOf(0, value.entries.size - visibleCells))
        rebuild()
    }

    private fun rebuild() {
        content.removeAllChildren()
        val value = snapshot
        previous.visible = value?.showAll == true && firstVisible > 0
        next.visible = value?.showAll == true && firstVisible + visibleCells < value.entries.size
        value ?: return
        if (value.showAll) grid(value) else ranked(value)
    }

    private fun grid(value: TokenProbabilitySnapshot) {
        val entries = value.entries.drop(firstVisible).take(visibleCells)
        entries.forEachIndexed { index, entry ->
            val col = index % style.columns
            val row = index / style.columns
            addEntry(value, entry, PADDING + col * cellStep, HEADER_HEIGHT + row * cellStep, cellSize, true)
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
        value.entries.forEachIndexed { index, entry ->
            addEntry(value, entry, PADDING, HEADER_HEIGHT + index * ROW_STEP, ROW_WIDTH, false)
        }
    }

    private fun addEntry(value: TokenProbabilitySnapshot, entry: TokenProbabilitySnapshot.Entry, x: Double, y: Double, size: Double, grid: Boolean) {
        val palette = NetworkTheme.current
        content.addChild(PPath.createRectangle(x, y, if (grid) size else 12.0, if (grid) size else 12.0).apply {
            paint = Color(entry.probability.toFloat().toSimbrainColor(palette.coolNode, palette.neutralMidpoint, palette.hotNode))
            strokePaint = if (entry.tokenId == value.sampledTokenId) palette.sourceHandle else palette.imageBorder
            stroke = BasicStroke(if (entry.tokenId == value.sampledTokenId) 2f else 1f)
        })
        val label = tokenLabel(entry.tokenId).replace("\n", " ").take(if (grid) 11 else 28)
        content.addChild(PText(if (grid) label else "$label  ${"%.1f".format(entry.probability * 100)}%").apply {
            font = Theme.tiny
            textPaint = palette.valueText
            setOffset(if (grid) x - 2.0 else x + 18.0, if (grid) y + size + 1.0 else y - 1.0)
            pickable = false
        })
    }

    private val cellStep get() = (style.width - 2 * PADDING) / style.columns
    private val cellSize get() = minOf(34.0, cellStep - 8.0)
    private val visibleCells get() = style.columns * style.visibleRows

    companion object {
        private const val PADDING = 10.0
        private const val HEADER_HEIGHT = 32.0
        private const val ROW_STEP = 20.0
        private const val ROW_WIDTH = 200.0
    }
}
