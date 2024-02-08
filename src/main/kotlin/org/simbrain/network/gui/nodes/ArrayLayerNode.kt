package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.ArrayLayer
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.*
import java.awt.BasicStroke
import java.awt.Font
import java.awt.RenderingHints
import javax.swing.SwingUtilities

abstract class ArrayLayerNode(networkPanel: NetworkPanel, val layer: ArrayLayer):
    ScreenElement(networkPanel) {

    protected val CLAMPED_STROKE = BasicStroke(2f)

    protected val INFO_FONT = Font("Arial", Font.PLAIN, 8)

    /**
     * Margin around main box in pixels. Override to specify further.
     */
    protected open val margin = 10.0

    init {
        layer.events.apply {
            deleted.on { removeFromParent() }
            clampChanged.on { updateBorder() }
            locationChanged.on { pullViewPositionFromModel() }
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
        // Top left of bounds in local coordinates
        // Note that we cannot use fullbounds here because they include the node handle
        val (x,y) = bounds
        // Convert model's center location to top-left location, then subtract the padding
        this.globalTranslation = layer.location - point(layer.width / 2, layer.height / 2) - point(x, y)
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    private fun pushViewPositionToModel() {
        // Networkmodels use the center location
        val centerLocation = borderBox.globalFullBounds.center2D
        layer.location = centerLocation
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        super.offset(dx, dy)
        pushViewPositionToModel()
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
        SwingUtilities.invokeLater {
            borderBox = createBorder()
        }
    }

    private fun pushBoundsToModel() {
        layer.width = bounds.width
        layer.height = bounds.height
    }

    override fun acceptsSourceHandle() = true

    override val isDraggable = true

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