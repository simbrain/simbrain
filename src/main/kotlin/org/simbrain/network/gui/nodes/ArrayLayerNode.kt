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
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import java.awt.BasicStroke
import java.awt.Font
import java.awt.RenderingHints
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

abstract class ArrayLayerNode(networkPanel: NetworkPanel, val layer: ArrayLayer):
    ScreenElement(networkPanel) {

    private val interactionBox: ArrayLayerInteractionBox = ArrayLayerInteractionBox(networkPanel)

    protected val CLAMPED_STROKE = BasicStroke(2f)

    protected val INFO_FONT: Font get() = Theme.tiny

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
    var borderBox = createBorder()
        set(value) {
            removeChild(field)
            addChild(value)
            value.lowerToBottom()
            setBounds(value.bounds)
            pushBoundsToModel()
            field = value
        }

    fun rotateNode() {
        // Clear any existing rotation (e.g. from legacy serialized state)
        if (mainNode.rotation != 0.0) {
            val centerLocation = mainNode.bounds.center2D
            mainNode.rotateAboutPoint(-mainNode.rotation, centerLocation)
        }
        updateBorder()
    }

    init {
        layer.events.apply {
            clampChanged.on(dispatcher = Dispatchers.Swing) { updateBorder() }
            locationChanged.on(dispatcher = Dispatchers.Swing) { 
                pullViewPositionFromModel()
                layoutChildren()
            }
            labelChanged.on(dispatcher = Dispatchers.Swing) { _, _ ->
                interactionBox.setText(layer.displayName)
            }
        }
        (layer as? NeuronArray)?.events?.visualPropertiesChanged?.on(dispatcher = Dispatchers.Swing) { rotateNode() }
        rotateNode()

        addChild(interactionBox)
        interactionBox.setText(layer.displayName)

        pickable = true

        pullViewPositionFromModel()
        
        // Position interaction box initially
        layoutChildren()
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
        newBorder.paint = NetworkPreferences.backgroundColor
        newBorder.strokePaint = NetworkTheme.current.nodeOutline
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

    public override fun layoutChildren() {
        interactionBox.centerFullBoundsOnPoint(
            borderBox.fullBounds.centerX,
            borderBox.fullBounds.getY() - interactionBox.fullBounds.getHeight() / 2 + 0.5
        )
    }

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

    override fun refreshTheme() {
        borderBox.paint = NetworkPreferences.backgroundColor
        borderBox.strokePaint = NetworkTheme.current.nodeOutline
    }

    /**
     * Basic interaction box for array layer nodes. Ensures a property dialog
     * appears when the box is double-clicked.
     */
    inner class ArrayLayerInteractionBox(net: NetworkPanel) : InteractionBox(net) {

        override val contextMenu: JPopupMenu?
            get() = this@ArrayLayerNode.contextMenu

        override val propertyDialog: StandardDialog?
            get() = this@ArrayLayerNode.propertyDialog

        override val model: ArrayLayer
            get() = this@ArrayLayerNode.layer

    }

}