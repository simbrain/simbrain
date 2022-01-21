package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.matrix.ArrayLayer
import org.simbrain.util.*
import java.awt.BasicStroke
import java.awt.Font
import java.awt.RenderingHints

abstract class ArrayLayerNode(networkPanel: NetworkPanel, val layer: ArrayLayer):
    ScreenElement(networkPanel) {

    protected val CLAMPED_STROKE = BasicStroke(2f)

    protected val INFO_FONT = Font("Arial", Font.PLAIN, 8)

    /**
     * Margin around main box in pixels. Override to specify further.
     */
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

    /**
     * All children should be added to this so that bound computations are correct.
     */
    val mainNode = PNode().also {
        addChild(it)
    }

    /**
     * Box drawn around the [mainNode] together with the [margin].
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
        val (x1,y1) = bounds // top left of full bounds in local coordinates
        this.globalTranslation = layer.location - point(layer.width / 2, layer.height / 2) - point(x1, y1)
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    private fun pushViewPositionToModel() {
        val centerLocation = globalFullBounds.center2D
        layer.location = centerLocation
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

    /**
     * Forces sharp rendering.
     */
    override fun paint(paintContext: PPaintContext) {
        paintContext.graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
        super.paint(paintContext)
    }
}