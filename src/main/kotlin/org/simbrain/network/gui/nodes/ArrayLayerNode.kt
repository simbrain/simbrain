package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.matrix.ArrayLayer
import org.simbrain.util.*
import java.awt.BasicStroke
import java.awt.Font

abstract class ArrayLayerNode(val layer: ArrayLayer, networkPanel: NetworkPanel): ScreenElement(networkPanel) {

    protected val CLAMPED_STROKE = BasicStroke(2f)

    protected val INFO_FONT = Font("Arial", Font.PLAIN, 8)

    open protected val margin = 10.0

    init {
        layer.events.apply {
            onDeleted { removeFromParent() }
            onClampChanged { updateBorder() }
            onLocationChange { pullViewPositionFromModel() }
        }
        pickable = true

        pullViewPositionFromModel()
    }

    val mainNode = PNode().also {
        addChild(it)
    }

    /**
     * Square shape around array node.
     */
    private var borderBox = createBorder()
        set(value) {
            removeChild(field)
            addChild(value)
            value.lowerToBottom()
            setBounds(value.bounds)
            pushBoundsToModel()
            field = value
        }

    private fun pullViewPositionFromModel() {
        this.globalTranslation = layer.location - point(width / 2, height / 2) + point(margin, margin)
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    private fun pushViewPositionToModel() {
        layer.location = globalTranslation + point(width / 2, height / 2) - point(margin, margin)
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        pushViewPositionToModel()
        super.offset(dx, dy)
    }

    private fun createBorder(): PPath {
        val newBound = mainNode.fullBounds.addPadding(margin)
        val (x, y, w, h) = newBound
        val newBorder = PPath.createRectangle(x, y, w, h)
        newBorder.stroke = if (layer.isClamped) {
            CLAMPED_STROKE
        } else {
            DEFAULT_STROKE
        }
        return newBorder
    }

    fun updateBorder() {
        borderBox = createBorder()
    }

    private fun pushBoundsToModel() {
        layer.width = bounds.width
        layer.height = bounds.height
    }

    override fun isSelectable() = true

    override fun acceptsSourceHandle() = true

    override fun isDraggable() = true
}