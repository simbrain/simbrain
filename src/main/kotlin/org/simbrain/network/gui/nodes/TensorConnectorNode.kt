package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.TensorConnector
import org.simbrain.network.gui.ArrowDirection
import org.simbrain.network.gui.ImageBox
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createArrowButton
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.bezierArrow
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JPopupMenu

/**
 * GUI node for a [TensorConnector]. Draws an arrow between source and target TensorNodes.
 * For [ConvolutionConnector], also shows a kernel heatmap with filter/channel navigation.
 */
class TensorConnectorNode(networkPanel: NetworkPanel, val connector: TensorConnector) :
    ScreenElement(networkPanel) {

    val sourceNode by lazy { networkPanel.getNode(connector.source) }
    val targetNode by lazy { networkPanel.getNode(connector.target) }

    private val imgSize = 60
    val imageBox = ImageBox(imgSize, imgSize, 1f)

    val interactionBox = TensorConnectorInteractionBox(networkPanel)

    /** Detail label for filter/channel info (ConvolutionConnector only). */
    private val detailLabel = PText("").apply {
        font = Font("Arial", Font.PLAIN, 9)
    }

    /** For ConvolutionConnector: current filter and input channel being viewed. */
    private var currentFilter = 0
    private var currentInputChannel = 0

    // Navigation buttons (only created for ConvolutionConnector)
    private val filterPrevButton: PNode? = (connector as? ConvolutionConnector)?.let {
        createArrowButton(ArrowDirection.UP) { previousFilter() }
    }
    private val filterNextButton: PNode? = (connector as? ConvolutionConnector)?.let {
        createArrowButton(ArrowDirection.DOWN) { nextFilter() }
    }
    private val channelPrevButton: PNode? = (connector as? ConvolutionConnector)?.let {
        createArrowButton(ArrowDirection.LEFT) { previousInputChannel() }
    }
    private val channelNextButton: PNode? = (connector as? ConvolutionConnector)?.let {
        createArrowButton(ArrowDirection.RIGHT) { nextInputChannel() }
    }

    /** Container for kernel grid display. */
    val kernelGridGroup = PNode()

    // --- Pre-allocated resources for ConvolutionConnector visualization ---

    /** Pre-allocated buffer for extracting a single kernel slice. */
    private val kernelSlice: DoubleArray? = (connector as? ConvolutionConnector)?.let {
        DoubleArray(it.kernelSize * it.kernelSize)
    }

    /** Pre-allocated BufferedImage for single-kernel view. */
    private val singleKernelImage: BufferedImage? = (connector as? ConvolutionConnector)?.let {
        BufferedImage(it.kernelSize, it.kernelSize, BufferedImage.TYPE_INT_RGB)
    }

    /** Pre-allocated grid cell BufferedImages: [filter][channel]. */
    private val gridCellImages: Array<Array<BufferedImage>>? = (connector as? ConvolutionConnector)?.let { conv ->
        Array(conv.numFilters) { Array(conv.source.shape.channels) {
            BufferedImage(conv.kernelSize, conv.kernelSize, BufferedImage.TYPE_INT_RGB)
        }}
    }

    /** Pre-allocated grid cell PImage nodes: [filter][channel]. */
    private val gridCellPImages: Array<Array<PImage>>? = (connector as? ConvolutionConnector)?.let { conv ->
        val kSize = conv.kernelSize
        val numFilters = conv.numFilters
        val inputChannels = conv.source.shape.channels
        val cellSize = (120.0 / maxOf(numFilters, inputChannels)).coerceIn(8.0, 20.0)
        val gap = 2.0
        val labelOffset = 14.0

        // Add column labels ("C1", "C2", ...) along top
        for (c in 0 until inputChannels) {
            val label = PText("C${c + 1}").apply {
                font = Font("Arial", Font.PLAIN, 7)
            }
            label.setOffset(
                labelOffset + c * (cellSize + gap) + (cellSize - label.width) / 2,
                0.0
            )
            kernelGridGroup.addChild(label)
        }

        // Add row labels ("F1", "F2", ...) along left
        for (f in 0 until numFilters) {
            val label = PText("F${f + 1}").apply {
                font = Font("Arial", Font.PLAIN, 7)
            }
            label.setOffset(
                0.0,
                labelOffset + f * (cellSize + gap) + (cellSize - label.height) / 2
            )
            kernelGridGroup.addChild(label)
        }

        // Create PImage and border nodes for each cell
        Array(numFilters) { f ->
            Array(inputChannels) { c ->
                val x = labelOffset + c * (cellSize + gap)
                val y = labelOffset + f * (cellSize + gap)
                val pImage = PImage(gridCellImages!![f][c])
                pImage.setBounds(x, y, cellSize, cellSize)
                kernelGridGroup.addChild(pImage)

                // Thin border
                val border = PPath.createRectangle(x, y, cellSize, cellSize)
                border.paint = null
                border.strokePaint = Color.GRAY
                border.stroke = BasicStroke(0.5f)
                kernelGridGroup.addChild(border)

                pImage
            }
        }
    }

    private val arrow: BezierArrow

    init {
        pickable = true

        arrow = bezierArrow {
            color = NetworkPreferences.connectorArrowColor
            padding {
                tail = 0.0
                head = 5.0 + arrowSize
            }

            lateralOffset { 0.5 }

            onUpdated { curve ->
                val pt = curve?.p(0.5) ?: line(connector.source.location, connector.target.location).p(0.5)
                val px = pt.x
                val py = pt.y

                val conv = connector as? ConvolutionConnector
                if (conv != null && conv.kernelGridMode) {
                    // Grid mode: center kernelGridGroup on midpoint
                    kernelGridGroup.centerFullBoundsOnPoint(px, py)
                    val gridHalfH = kernelGridGroup.fullBounds.height / 2.0
                    interactionBox.centerFullBoundsOnPoint(
                        px, py - gridHalfH - interactionBox.fullBounds.height / 2.0 - 3.0
                    )
                } else if (connector is ConvolutionConnector) {
                    imageBox.centerFullBoundsOnPoint(px, py)

                    val hi = imgSize.toDouble() / 2.0
                    val gap = 3.0
                    val bh = filterPrevButton!!.fullBounds.height
                    val bw = channelPrevButton!!.fullBounds.width

                    filterPrevButton.centerFullBoundsOnPoint(px, py - hi - gap - bh / 2)
                    filterNextButton!!.centerFullBoundsOnPoint(px, py + hi + gap + bh / 2)
                    channelPrevButton.centerFullBoundsOnPoint(px - hi - gap - bw / 2, py)
                    channelNextButton!!.centerFullBoundsOnPoint(px + hi + gap + bw / 2, py)

                    val bottomEdge = py + hi + gap + bh
                    detailLabel.centerFullBoundsOnPoint(px, bottomEdge + 6.0)
                    interactionBox.centerFullBoundsOnPoint(
                        px, py - hi - gap - bh - interactionBox.fullBounds.height / 2.0 - 1.0
                    )
                } else {
                    interactionBox.centerFullBoundsOnPoint(px, py)
                }
            }
        }

        addChild(arrow)
        addChild(interactionBox)

        if (connector is ConvolutionConnector) {
            addChild(filterPrevButton)
            addChild(filterNextButton)
            addChild(channelPrevButton)
            addChild(channelNextButton)
            addChild(imageBox)
            addChild(detailLabel)
            addChild(kernelGridGroup)
            renderKernelImage()
            syncKernelDisplayMode()
            // Set kernelGridGroup's own bounds so NodeHandle selection handles work
            kernelGridGroup.getUnionOfChildrenBounds(null)?.let { cb ->
                kernelGridGroup.setBounds(cb)
            }
        }

        // Wire up events
        val connectorEvents = connector.events
        connectorEvents.updated.on { connectorEvents.updateGraphics.fire() }
        connectorEvents.updateGraphics.on(Dispatchers.Swing) {
            if (connector is ConvolutionConnector) {
                if (connector.kernelGridMode) {
                    renderKernelGrid()
                } else {
                    renderKernelImage()
                }
            }
            updateDetailLabel()
        }
        connectorEvents.labelChanged.on(Dispatchers.Swing) { _, _ ->
            interactionBox.setText(connector.displayName)
        }
        connectorEvents.visualPropertiesChanged.on(Dispatchers.Swing) {
            syncKernelDisplayMode()
        }

        connector.source.events.locationChanged.on(Dispatchers.Swing) {
            arrow.invalidateFullBounds()
        }
        connector.target.events.locationChanged.on(Dispatchers.Swing) {
            arrow.invalidateFullBounds()
        }

        interactionBox.setText(connector.displayName)
        arrow.invalidateFullBounds()
        updateDetailLabel()
    }

    /** Switch visibility between single-kernel UI and grid UI. */
    private fun syncKernelDisplayMode() {
        val conv = connector as? ConvolutionConnector ?: return
        if (conv.kernelGridMode) {
            imageBox.visible = false
            filterPrevButton?.visible = false
            filterNextButton?.visible = false
            channelPrevButton?.visible = false
            channelNextButton?.visible = false
            detailLabel.visible = false
            kernelGridGroup.visible = true
            renderKernelGrid()
        } else {
            imageBox.visible = true
            filterPrevButton?.visible = true
            filterNextButton?.visible = true
            channelPrevButton?.visible = true
            channelNextButton?.visible = true
            detailLabel.visible = true
            kernelGridGroup.visible = false
            renderKernelImage()
            updateDetailLabel()
        }
        arrow.invalidateFullBounds()
    }

    private fun renderKernelImage() {
        val conv = connector as? ConvolutionConnector ?: return
        val slice = kernelSlice ?: return
        val img = singleKernelImage ?: return
        val kSize = conv.kernelSize
        val inC = conv.source.shape.channels
        val kernelArea = kSize * kSize
        val filterOffset = currentFilter * inC * kernelArea + currentInputChannel * kernelArea
        for (i in 0 until kernelArea) {
            slice[i] = conv.kernels[filterOffset + i]
        }
        slice.writeSimbrainColorImage(img)
        imageBox.image = img
    }

    private fun renderKernelGrid() {
        val conv = connector as? ConvolutionConnector ?: return
        val slice = kernelSlice ?: return
        val cellImages = gridCellImages ?: return
        val cellPImages = gridCellPImages ?: return

        val inputChannels = conv.source.shape.channels
        val kSize = conv.kernelSize
        val kernelArea = kSize * kSize

        for (f in 0 until conv.numFilters) {
            for (c in 0 until inputChannels) {
                val filterOffset = f * inputChannels * kernelArea + c * kernelArea
                for (i in 0 until kernelArea) {
                    slice[i] = conv.kernels[filterOffset + i]
                }
                slice.writeSimbrainColorImage(cellImages[f][c])
                cellPImages[f][c].invalidatePaint()
            }
        }
    }

    private fun updateDetailLabel() {
        if (connector is ConvolutionConnector) {
            val c = connector
            detailLabel.text = "F${currentFilter + 1}/${c.numFilters} Ch${currentInputChannel + 1}/${c.source.shape.channels}"
        }
    }

    fun nextFilter() {
        val conv = connector as? ConvolutionConnector ?: return
        currentFilter = (currentFilter + 1) % conv.numFilters
        renderKernelImage()
        updateDetailLabel()
    }

    fun previousFilter() {
        val conv = connector as? ConvolutionConnector ?: return
        currentFilter = (currentFilter - 1 + conv.numFilters) % conv.numFilters
        renderKernelImage()
        updateDetailLabel()
    }

    fun nextInputChannel() {
        val conv = connector as? ConvolutionConnector ?: return
        currentInputChannel = (currentInputChannel + 1) % conv.source.shape.channels
        renderKernelImage()
        updateDetailLabel()
    }

    fun previousInputChannel() {
        val conv = connector as? ConvolutionConnector ?: return
        currentInputChannel = (currentInputChannel - 1 + conv.source.shape.channels) % conv.source.shape.channels
        renderKernelImage()
        updateDetailLabel()
    }

    fun updateArrowColorFromPreferences() {
        arrow.updateColorFromPreferences()
        layoutChildren()
    }

    override fun layoutChildren() {
        val srcBounds = sourceNode?.globalBounds ?: return
        val tgtBounds = targetNode?.globalBounds ?: return
        arrow.layout(srcBounds.outlines, tgtBounds.outlines, false)
    }

    override fun paint(paintContext: PPaintContext) {
        paintContext.graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
        super.paint(paintContext)
    }

    override val isDraggable: Boolean = false

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()
            contextMenu.add(networkPanel.createAction(name = "Edit...") {
                propertyDialog?.display()
            })

            if (connector is ConvolutionConnector) {
                contextMenu.addSeparator()

                // Kernel grid toggle
                contextMenu.add(networkPanel.createAction(
                    name = if (connector.kernelGridMode) "Show Single Kernel View" else "Show Kernel Grid"
                ) {
                    connector.kernelGridMode = !connector.kernelGridMode
                })

                if (!connector.kernelGridMode) {
                    contextMenu.add(networkPanel.createAction(name = "Next Filter") { nextFilter() })
                    contextMenu.add(networkPanel.createAction(name = "Previous Filter") { previousFilter() })
                    contextMenu.add(networkPanel.createAction(name = "Next Input Channel") { nextInputChannel() })
                    contextMenu.add(networkPanel.createAction(name = "Previous Input Channel") { previousInputChannel() })
                }

                contextMenu.addSeparator()
                contextMenu.add(networkPanel.networkActions.randomizeObjectsAction)
            }

            return contextMenu
        }

    override fun createEditDialog(): StandardDialog? {
        return connector.createEditorDialog()
    }

    override val propertyDialog: StandardDialog? get() = createEditDialog()

    override val model: TensorConnector get() = connector

    override fun isIntersecting(bound: PBounds?): Boolean {
        if (bound == null) return false
        return imageBox.globalBounds.intersects(bound) ||
                arrow.globalBounds.intersects(bound) ||
                interactionBox.globalBounds.intersects(bound) ||
                detailLabel.globalBounds.intersects(bound) ||
                kernelGridGroup.globalBounds.intersects(bound) ||
                (filterPrevButton?.globalBounds?.intersects(bound) == true) ||
                (filterNextButton?.globalBounds?.intersects(bound) == true) ||
                (channelPrevButton?.globalBounds?.intersects(bound) == true) ||
                (channelNextButton?.globalBounds?.intersects(bound) == true)
    }

    inner class TensorConnectorInteractionBox(net: NetworkPanel) : InteractionBox(net) {
        override val contextMenu: JPopupMenu
            get() = this@TensorConnectorNode.contextMenu
        override fun createEditDialog(): StandardDialog? =
            this@TensorConnectorNode.createEditDialog()
        override val propertyDialog: StandardDialog?
            get() = this@TensorConnectorNode.createEditDialog()
        override val isDraggable: Boolean get() = false
        override val model: TensorConnector
            get() = this@TensorConnectorNode.connector
    }
}
