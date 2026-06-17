package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.gui.*
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import org.simbrain.util.piccolo.SimbrainImage
import org.simbrain.util.piccolo.addBorder
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JMenu
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

/**
 * GUI node for a [TensorLayer]. Shows stacked channel images with navigation buttons.
 */
class TensorNode(networkPanel: NetworkPanel, val tensorLayer: TensorLayer) : ScreenElement(networkPanel) {

    private val interactionBox = TensorInteractionBox(networkPanel)

    private val mainNode = PNode().also { addChild(it) }

    private val imageSize = 100.0

    /** Pre-allocated main image (tensor shape is fixed at construction). */
    private val mainImage = BufferedImage(tensorLayer.shape.width, tensorLayer.shape.height, BufferedImage.TYPE_INT_RGB)

    /** Pre-allocated buffer for extracting a single channel's data. */
    private val channelBuffer = DoubleArray(tensorLayer.shape.height * tensorLayer.shape.width)

    private val activationImage = SimbrainImage().apply {
        mainNode.addChild(this)
        pickable = true
        addInputEventListener(object : org.piccolo2d.event.PBasicInputEventHandler() {
            override fun mouseMoved(event: org.piccolo2d.event.PInputEvent) {
                val localPt = event.getPositionRelativeTo(this@apply)
                val tensorH = (localPt.y / imageSize * tensorLayer.shape.height).toInt()
                    .coerceIn(0, tensorLayer.shape.height - 1)
                val tensorW = (localPt.x / imageSize * tensorLayer.shape.width).toInt()
                    .coerceIn(0, tensorLayer.shape.width - 1)
                networkPanel.updateReceptiveFieldTrace(tensorLayer, tensorH, tensorW)
            }
            override fun mouseExited(event: org.piccolo2d.event.PInputEvent) {
                networkPanel.clearReceptiveFieldTrace()
            }
        })
    }
    private val channelLabel = PText("").apply {
        font = Theme.label
        mainNode.addChild(this)
    }

    private val prevChannelButton = createArrowButton(ArrowDirection.LEFT) { previousChannel() }
        .also { mainNode.addChild(it) }
    private val nextChannelButton = createArrowButton(ArrowDirection.RIGHT) { nextChannel() }
        .also { mainNode.addChild(it) }

    /** Container for thumbnail strip nodes. */
    private val thumbnailStripNode = PNode().also { mainNode.addChild(it) }

    // Pre-allocated thumbnail resources (fixed count = number of channels)
    private val numChannels = tensorLayer.shape.channels
    private val thumbSize = (imageSize / numChannels).coerceIn(8.0, 20.0)
    private val thumbGap = 1.0
    private val thumbTotalWidth = numChannels * thumbSize + (numChannels - 1) * thumbGap
    private val thumbStartX = (imageSize - thumbTotalWidth) / 2.0
    private val thumbStripY = imageSize + 4.0

    /** Pre-allocated BufferedImages for each thumbnail (written into, never recreated). */
    private val thumbImages = Array(numChannels) {
        BufferedImage(tensorLayer.shape.width, tensorLayer.shape.height, BufferedImage.TYPE_INT_RGB)
    }

    /** Pre-allocated PImage nodes for each thumbnail. */
    private val thumbPImages = Array(numChannels) { c ->
        SimbrainImage(thumbImages[c]).apply {
            val x = thumbStartX + c * (thumbSize + thumbGap)
            setBounds(x, thumbStripY, thumbSize, thumbSize)
            addInputEventListener(object : org.piccolo2d.event.PBasicInputEventHandler() {
                override fun mouseClicked(event: org.piccolo2d.event.PInputEvent) {
                    tensorLayer.currentChannel = c
                }
            })
            thumbnailStripNode.addChild(this)
        }
    }

    /** Pre-allocated border PPath nodes for each thumbnail. */
    private val thumbBorders = Array(numChannels) { c ->
        val x = thumbStartX + c * (thumbSize + thumbGap)
        PPath.createRectangle(x, thumbStripY, thumbSize, thumbSize).apply {
            paint = null
            strokePaint = Color.GRAY
            stroke = BasicStroke(1f)
            thumbnailStripNode.addChild(this)
        }
    }

    /** Trace boxes set by the receptive field tracer. */
    val traceBoxes: MutableList<TraceBox> = mutableListOf()

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
        val tensorEvents = tensorLayer.events
        tensorEvents.clampChanged.on(dispatcher = Dispatchers.Swing) { updateBorder() }
        tensorEvents.locationChanged.on(dispatcher = Dispatchers.Swing) {
            pullViewPositionFromModel()
            layoutChildren()
        }
        tensorEvents.labelChanged.on(dispatcher = Dispatchers.Swing) { _, _ ->
            interactionBox.setText(tensorDisplayText())
        }
        tensorEvents.updated.on {
            tensorEvents.updateGraphics.fire()
        }
        tensorEvents.updateGraphics.on(Dispatchers.Swing) {
            updateActivationImage()
        }
        tensorEvents.visualPropertiesChanged.on(Dispatchers.Swing) {
            updateActivationImage()
            updateBorder()
        }

        addChild(interactionBox)
        interactionBox.setText(tensorDisplayText())

        pickable = true
        pullViewPositionFromModel()
        updateActivationImage()
        updateBorder()
        layoutChildren()
    }

    /**
     * Extract channel [c] into [channelBuffer] without allocation.
     */
    private fun extractChannel(c: Int) {
        for (h in 0 until tensorLayer.shape.height) {
            for (w in 0 until tensorLayer.shape.width) {
                channelBuffer[h * tensorLayer.shape.width + w] = tensorLayer.activations[tensorLayer.shape.index(h, w, c)]
            }
        }
    }

    private fun updateActivationImage() {
        if (tensorLayer.thumbnailStripMode) {
            renderThumbnailStrip()
        } else if (tensorLayer.rgbComposite && tensorLayer.shape.channels == 3) {
            renderRGBComposite()
        } else {
            renderSingleChannel()
        }
        updateChannelLabel()
    }

    private fun renderCurrentChannelToMainImage() {
        val ch = tensorLayer.currentChannel.coerceIn(0, tensorLayer.shape.channels - 1)
        extractChannel(ch)
        channelBuffer.writeSimbrainColorImage(mainImage)
        activationImage.image = mainImage
        activationImage.setBounds(0.0, 0.0, imageSize, imageSize)
        activationImage.addBorder()
        activationImage.visible = true
    }

    /**
     * Build the text shown on the interaction box, appending shape info when it is not already present.
     */
    private fun tensorDisplayText(): String {
        val base = tensorLayer.displayName
        val shapeSummary = tensorLayer.shape.toString()
        return if (base.contains(shapeSummary)) base else "$base ($shapeSummary)"
    }

    private fun renderSingleChannel() {
        activationImage.removeAllChildren()
        thumbnailStripNode.visible = false
        renderCurrentChannelToMainImage()
    }

    private fun renderRGBComposite() {
        activationImage.removeAllChildren()
        thumbnailStripNode.visible = false
        val w = tensorLayer.shape.width
        val h = tensorLayer.shape.height
        val pixels = (mainImage.raster.dataBuffer as java.awt.image.DataBufferInt).data
        for (py in 0 until h) {
            for (px in 0 until w) {
                val idx = py * w + px
                val r = (tensorLayer.activations[tensorLayer.shape.index(py, px, 0)].coerceIn(0.0, 1.0) * 255).toInt()
                val g = (tensorLayer.activations[tensorLayer.shape.index(py, px, 1)].coerceIn(0.0, 1.0) * 255).toInt()
                val b = (tensorLayer.activations[tensorLayer.shape.index(py, px, 2)].coerceIn(0.0, 1.0) * 255).toInt()
                pixels[idx] = (r shl 16) or (g shl 8) or b
            }
        }
        activationImage.image = mainImage
        activationImage.setBounds(0.0, 0.0, imageSize, imageSize)
        activationImage.addBorder()
        activationImage.visible = true
    }

    private fun renderThumbnailStrip() {
        // Render main image for currentChannel
        activationImage.removeAllChildren()
        renderCurrentChannelToMainImage()

        // Update pre-allocated thumbnail pixel data and repaint (no re-assignment needed
        // since writeSimbrainColorImage writes directly into the backing buffer)
        thumbnailStripNode.visible = true
        val selectedCh = tensorLayer.currentChannel.coerceIn(0, tensorLayer.shape.channels - 1)
        for (c in 0 until numChannels) {
            extractChannel(c)
            channelBuffer.writeSimbrainColorImage(thumbImages[c])
            thumbPImages[c].invalidatePaint()

            // Update border: orange for selected, gray for others
            if (c == selectedCh) {
                thumbBorders[c].strokePaint = Color.ORANGE
                thumbBorders[c].stroke = BasicStroke(2f)
            } else {
                thumbBorders[c].strokePaint = Color.GRAY
                thumbBorders[c].stroke = BasicStroke(1f)
            }
        }
    }

    private fun updateChannelLabel() {
        val inStripMode = tensorLayer.thumbnailStripMode

        if (inStripMode) {
            // Hide arrow buttons and channel label text in strip mode
            channelLabel.visible = false
            prevChannelButton.visible = false
            nextChannelButton.visible = false
            return
        }

        channelLabel.visible = true
        channelLabel.text = if (tensorLayer.rgbComposite && tensorLayer.shape.channels == 3) {
            "RGB Composite"
        } else {
            "Ch ${tensorLayer.currentChannel + 1}/${tensorLayer.shape.channels}"
        }

        val navY = imageSize + 4
        channelLabel.setOffset(
            (imageSize - channelLabel.width) / 2,
            navY
        )

        val showArrows = tensorLayer.shape.channels > 1 && !(tensorLayer.rgbComposite && tensorLayer.shape.channels == 3)
        prevChannelButton.visible = showArrows
        nextChannelButton.visible = showArrows
        if (showArrows) {
            val arrowY = navY + (channelLabel.height - prevChannelButton.fullBounds.height) / 2
            prevChannelButton.setOffset(0.0, arrowY)
            nextChannelButton.setOffset(imageSize - nextChannelButton.fullBounds.width, arrowY)
        }
    }

    fun nextChannel() {
        if (!tensorLayer.rgbComposite) {
            tensorLayer.currentChannel = (tensorLayer.currentChannel + 1) % tensorLayer.shape.channels
        }
    }

    fun previousChannel() {
        if (!tensorLayer.rgbComposite) {
            tensorLayer.currentChannel = (tensorLayer.currentChannel - 1 + tensorLayer.shape.channels) % tensorLayer.shape.channels
        }
    }

    // Position management

    private fun pullViewPositionFromModel() {
        val (x, y) = bounds
        this.globalTranslation = tensorLayer.location -
                point(tensorLayer.renderWidth / 2, tensorLayer.renderHeight / 2) - point(x, y)
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        (model as LocatableModel).location += point(dx, dy)
        pullViewPositionFromModel()
    }

    private fun createBorder(): PPath {
        val newBound = mainNode.fullBounds.addPadding(margin)
        val (x, y, w, h) = newBound
        val newBorder = createRectangle(x, y, w, h)
        newBorder.stroke = if (tensorLayer.isClamped) BasicStroke(2f) else DEFAULT_STROKE
        return newBorder
    }

    fun updateBorder() {
        SwingUtilities.invokeLater {
            borderBox = createBorder()
        }
    }

    private fun pushBoundsToModel() {
        tensorLayer.renderWidth = bounds.width
        tensorLayer.renderHeight = bounds.height
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

    override fun paintAfterChildren(paintContext: PPaintContext) {
        super.paintAfterChildren(paintContext)
        val g2 = paintContext.graphics

        // Trace boxes
        if (traceBoxes.isNotEmpty()) {
            val scaleX = imageSize / tensorLayer.shape.width
            val scaleY = imageSize / tensorLayer.shape.height

            for (box in traceBoxes) {
                val px = box.col * scaleX
                val py = box.row * scaleY
                val pw = box.width * scaleX
                val ph = box.height * scaleY

                g2.color = Color(box.color.red, box.color.green, box.color.blue, 60)
                g2.fillRect(px.toInt(), py.toInt(), pw.toInt().coerceAtLeast(1), ph.toInt().coerceAtLeast(1))

                g2.color = box.color
                g2.stroke = BasicStroke(2f)
                g2.drawRect(px.toInt(), py.toInt(), pw.toInt().coerceAtLeast(1), ph.toInt().coerceAtLeast(1))
            }
        }

        // Numeric overlay (re-extract current channel since channelBuffer may have been
        // overwritten by thumbnail strip rendering which iterates all channels)
        if (NetworkPreferences.showNumericOverlays
            && (!tensorLayer.rgbComposite || tensorLayer.shape.channels != 3)) {
            extractChannel(tensorLayer.currentChannel.coerceIn(0, tensorLayer.shape.channels - 1))
            g2.drawNumericOverlay(
                data = channelBuffer,
                rows = tensorLayer.shape.height, cols = tensorLayer.shape.width,
                imageWidth = imageSize, imageHeight = imageSize,
                scalingFactor = networkPanel.scalingFactor,
                decimalPlaces = NetworkPreferences.neuronActivationDecimalPlaces
            )
        }
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
                propertyDialog.display()
            })
            contextMenu.addSeparator()

            // Add layers (Flow B)
            contextMenu.add(networkPanel.createAction(name = "Add Conv Layer...") {
                networkPanel.showAddConvLayerDialog(tensorLayer)
            })
            contextMenu.add(networkPanel.createAction(name = "Add Pool Layer...") {
                networkPanel.showAddPoolLayerDialog(tensorLayer)
            })
            contextMenu.add(networkPanel.createAction(name = "Add Flatten Layer") {
                networkPanel.addFlattenLayer(tensorLayer)
            })
            contextMenu.addSeparator()

            contextMenu.add(networkPanel.networkActions.createConvolutionalNeuralNetworkAction)
            contextMenu.addSeparator()

            // Channel navigation & display modes
            // Thumbnail strip toggle
            contextMenu.add(networkPanel.createAction(
                name = if (tensorLayer.thumbnailStripMode) "Show Single Channel View" else "Show Thumbnail View"
            ) {
                tensorLayer.thumbnailStripMode = !tensorLayer.thumbnailStripMode
            })

            if (tensorLayer.shape.channels == 3) {
                contextMenu.add(
                    networkPanel.createAction(
                        name = if (tensorLayer.rgbComposite) "Hide RGB Composite" else "Show RGB Composite"
                    ) {
                        tensorLayer.rgbComposite = !tensorLayer.rgbComposite
                    })
            }
            contextMenu.addSeparator()

            // Randomize
            contextMenu.add(networkPanel.networkActions.randomizeObjectsAction)
            contextMenu.addSeparator()

            // Clamp
            contextMenu.add(networkPanel.createAction(
                name = if (tensorLayer.isClamped) "Unclamp" else "Clamp"
            ) {
                tensorLayer.toggleClamping()
            })
            contextMenu.addSeparator()

            // Add coupled image world submenu
            val imageWorldMenu = JMenu("Add coupled image world")
            for (c in 0 until tensorLayer.shape.channels) {
                imageWorldMenu.add(actionManager.createTensorChannelImageInput(tensorLayer, c))
            }
            if (tensorLayer.shape.channels == 3) {
                imageWorldMenu.addSeparator()
                imageWorldMenu.add(actionManager.createTensorRgbImageInput(tensorLayer))
            }
            contextMenu.add(imageWorldMenu)
            contextMenu.addSeparator()

            // Coupling menu
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(tensorLayer)
            contextMenu.add(couplingMenu)

            return contextMenu
        }

    override fun createEditDialog(): StandardDialog {
        return tensorLayer.createEditorDialog()
    }

    override val propertyDialog: StandardDialog
        get() = createEditDialog()

    override val model: TensorLayer get() = tensorLayer

    inner class TensorInteractionBox(net: NetworkPanel) : InteractionBox(net) {
        override val contextMenu: JPopupMenu
            get() = this@TensorNode.contextMenu
        override val propertyDialog: StandardDialog
            get() = this@TensorNode.propertyDialog
        override val model: TensorLayer
            get() = this@TensorNode.tensorLayer
    }
}
