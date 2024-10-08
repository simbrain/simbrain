package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.ArrayLayer
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NeuronArray
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

    fun rotateNode() {
        (layer as? NeuronArray)?.let { neuronArray ->
            val centerLocation = point(layer.width / 2, layer.height / 2) - point(x, y)
            val currentRadian = mainNode.rotation
            if (neuronArray.verticalLayout && mainNode.rotation != -Math.PI / 2) {
                val targetRadian = -Math.PI / 2
                mainNode.rotateAboutPoint(targetRadian - currentRadian, centerLocation)
            }
            if (!neuronArray.verticalLayout && mainNode.rotation != 0.0) {
                val targetRadian = 0.0
                mainNode.rotateAboutPoint(targetRadian - currentRadian, centerLocation)
            }
        }
        pushBoundsToModel()
    }

    init {
        layer.events.apply {
            clampChanged.on(dispatcher = Dispatchers.Swing) { updateBorder() }
            locationChanged.on(dispatcher = Dispatchers.Swing) { pullViewPositionFromModel() }
        }
        (layer as? NeuronArray)?.events?.visualPropertiesChanged?.on(dispatcher = Dispatchers.Swing) { rotateNode() }
        rotateNode()

        pickable = true

        pullViewPositionFromModel()
    }

    private fun pullViewPositionFromModel() {
        // Top left of bounds in local coordinates
        // Note that we cannot use fullbounds here because they include the node handle
        val (x,y) = bounds
        // Convert model's center location to top-left location, then subtract the padding
        this.globalTranslation = layer.location - point(layer.width / 2, layer.height / 2) - point(x, y)
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        (model as LocatableModel).location += point(dx, dy)
        pullViewPositionFromModel()
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