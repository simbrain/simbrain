package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.Tensor
import org.simbrain.network.gui.*
import org.simbrain.util.*
import org.simbrain.util.piccolo.addBorder
import java.awt.BasicStroke
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JMenu
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

/**
 * GUI node for a [Tensor]. Shows stacked channel images with navigation buttons.
 */
class TensorNode(networkPanel: NetworkPanel, val tensor: Tensor) : ScreenElement(networkPanel) {

    private val interactionBox = TensorInteractionBox(networkPanel)

    private val mainNode = PNode().also { addChild(it) }

    private val imageSize = 100.0
    private val channelOffset = 3.0

    /** Currently visible front channel (index). */
    private var currentChannel = 0

    /** When true and channels==3, show an RGB composite. */
    var rgbComposite = false

    private var cachedImage: BufferedImage? = null

    /** Return a reusable BufferedImage, only allocating when the dimensions change. */
    private fun getOrCreateImage(w: Int, h: Int): BufferedImage {
        val existing = cachedImage
        if (existing != null && existing.width == w && existing.height == h) return existing
        return BufferedImage(w, h, BufferedImage.TYPE_INT_RGB).also { cachedImage = it }
    }

    private val activationImage = PImage().apply { mainNode.addChild(this) }
    private val channelLabel = PText("").apply {
        font = Font("Arial", Font.PLAIN, 10)
        mainNode.addChild(this)
    }

    private val prevChannelButton = createArrowButton(ArrowDirection.LEFT) { previousChannel() }
        .also { mainNode.addChild(it) }
    private val nextChannelButton = createArrowButton(ArrowDirection.RIGHT) { nextChannel() }
        .also { mainNode.addChild(it) }

    private val margin = 10.0

    var borderBox = createBorder()
        set(value) {
            removeChild(field)
            addChild(value)
            value.lowerToBottom()
            setBounds(value.bounds)
            pushBoundsToModel()
            field = value
        }

    init {
        val tensorEvents = tensor.events
        tensorEvents.clampChanged.on(dispatcher = Dispatchers.Swing) { updateBorder() }
        tensorEvents.locationChanged.on(dispatcher = Dispatchers.Swing) {
            pullViewPositionFromModel()
            layoutChildren()
        }
        tensorEvents.labelChanged.on(dispatcher = Dispatchers.Swing) { _, _ ->
            interactionBox.setText(tensor.displayName)
        }
        tensorEvents.updated.on {
            tensorEvents.updateGraphics.fire()
        }
        tensorEvents.updateGraphics.on(Dispatchers.Swing) {
            updateActivationImage()
        }

        addChild(interactionBox)
        interactionBox.setText(tensor.displayName)

        pickable = true
        pullViewPositionFromModel()
        updateActivationImage()
        updateBorder()
        layoutChildren()
    }

    private fun updateActivationImage() {
        if (rgbComposite && tensor.shape.channels == 3) {
            renderRGBComposite()
        } else {
            renderSingleChannel()
        }
        updateChannelLabel()
    }

    private fun renderSingleChannel() {
        activationImage.removeAllChildren()
        val ch = currentChannel.coerceIn(0, tensor.shape.channels - 1)
        val channelData = tensor.getChannel(ch)
        val img = getOrCreateImage(tensor.shape.width, tensor.shape.height)
        channelData.writeSimbrainColorImage(img)
        activationImage.image = img
        activationImage.setBounds(0.0, 0.0, imageSize, imageSize)
        activationImage.addBorder()
    }

    private fun renderRGBComposite() {
        activationImage.removeAllChildren()
        val w = tensor.shape.width
        val h = tensor.shape.height
        val img = getOrCreateImage(w, h)
        val pixels = (img.raster.dataBuffer as java.awt.image.DataBufferInt).data
        val rCh = tensor.getChannel(0)
        val gCh = tensor.getChannel(1)
        val bCh = tensor.getChannel(2)
        for (i in pixels.indices) {
            val r = (rCh[i].coerceIn(0.0, 1.0) * 255).toInt()
            val g = (gCh[i].coerceIn(0.0, 1.0) * 255).toInt()
            val b = (bCh[i].coerceIn(0.0, 1.0) * 255).toInt()
            pixels[i] = (r shl 16) or (g shl 8) or b
        }
        activationImage.image = img
        activationImage.setBounds(0.0, 0.0, imageSize, imageSize)
        activationImage.addBorder()
    }

    private fun updateChannelLabel() {
        channelLabel.text = if (rgbComposite && tensor.shape.channels == 3) {
            "RGB Composite"
        } else {
            "Ch ${currentChannel + 1}/${tensor.shape.channels}"
        }

        val navY = imageSize + 4
        channelLabel.setOffset(
            (imageSize - channelLabel.width) / 2,
            navY
        )

        val showArrows = tensor.shape.channels > 1 && !(rgbComposite && tensor.shape.channels == 3)
        prevChannelButton.visible = showArrows
        nextChannelButton.visible = showArrows
        if (showArrows) {
            val arrowY = navY + (channelLabel.height - prevChannelButton.fullBounds.height) / 2
            prevChannelButton.setOffset(0.0, arrowY)
            nextChannelButton.setOffset(imageSize - nextChannelButton.fullBounds.width, arrowY)
        }
    }

    fun nextChannel() {
        if (!rgbComposite) {
            currentChannel = (currentChannel + 1) % tensor.shape.channels
            updateActivationImage()
            updateBorder()
        }
    }

    fun previousChannel() {
        if (!rgbComposite) {
            currentChannel = (currentChannel - 1 + tensor.shape.channels) % tensor.shape.channels
            updateActivationImage()
            updateBorder()
        }
    }

    // --- Position management ---

    private fun pullViewPositionFromModel() {
        val (x, y) = bounds
        this.globalTranslation = tensor.location -
                point(tensor.renderWidth / 2, tensor.renderHeight / 2) - point(x, y)
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        (model as LocatableModel).location += point(dx, dy)
        pullViewPositionFromModel()
    }

    private fun createBorder(): PPath {
        val newBound = mainNode.fullBounds.addPadding(margin)
        val (x, y, w, h) = newBound
        val newBorder = createRectangle(x, y, w, h)
        newBorder.stroke = if (tensor.isClamped) BasicStroke(2f) else DEFAULT_STROKE
        return newBorder
    }

    fun updateBorder() {
        SwingUtilities.invokeLater {
            borderBox = createBorder()
        }
    }

    private fun pushBoundsToModel() {
        tensor.renderWidth = bounds.width
        tensor.renderHeight = bounds.height
    }

    override fun acceptsSourceHandle() = true

    override val isDraggable = true

    public override fun layoutChildren() {
        interactionBox.centerFullBoundsOnPoint(
            borderBox.fullBounds.centerX,
            borderBox.fullBounds.y - interactionBox.fullBounds.height / 2 + 0.5
        )
    }

    override fun paint(paintContext: PPaintContext) {
        paintContext.graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
        super.paint(paintContext)
    }

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()
            contextMenu.add(networkPanel.networkActions.cutAction)
            contextMenu.add(networkPanel.networkActions.copyAction)
            contextMenu.add(networkPanel.networkActions.pasteAction)
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()

            // Edit
            contextMenu.add(networkPanel.createAction(name = "Edit Tensor...") {
                propertyDialog?.display()
            })
            contextMenu.addSeparator()

            // Add layers (Flow B)
            contextMenu.add(networkPanel.createAction(name = "Add Conv Layer...") {
                networkPanel.showAddConvLayerDialog(tensor)
            })
            contextMenu.add(networkPanel.createAction(name = "Add Pool Layer...") {
                networkPanel.showAddPoolLayerDialog(tensor)
            })
            contextMenu.add(networkPanel.createAction(name = "Add Flatten Layer") {
                networkPanel.addFlattenLayer(tensor)
            })
            contextMenu.addSeparator()

            // Channel navigation
            if (tensor.shape.channels > 1) {
                contextMenu.add(networkPanel.createAction(name = "Next Channel") { nextChannel() })
                contextMenu.add(networkPanel.createAction(name = "Previous Channel") { previousChannel() })
                if (tensor.shape.channels == 3) {
                    contextMenu.add(networkPanel.createAction(
                        name = if (rgbComposite) "Show Single Channel" else "Show RGB Composite"
                    ) {
                        rgbComposite = !rgbComposite
                        updateActivationImage()
                        updateBorder()
                    })
                }
                contextMenu.addSeparator()
            }

            // Randomize
            contextMenu.add(networkPanel.networkActions.randomizeObjectsAction)
            contextMenu.addSeparator()

            // Coupling menu
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(tensor)
            contextMenu.add(couplingMenu)

            return contextMenu
        }

    override fun createEditDialog(): StandardDialog? {
        return tensor.createEditorDialog()
    }

    override val propertyDialog: StandardDialog?
        get() = createEditDialog()

    override val model: Tensor get() = tensor

    inner class TensorInteractionBox(net: NetworkPanel) : InteractionBox(net) {
        override val contextMenu: JPopupMenu?
            get() = this@TensorNode.contextMenu
        override val propertyDialog: StandardDialog?
            get() = this@TensorNode.propertyDialog
        override val model: Tensor
            get() = this@TensorNode.tensor
    }
}
