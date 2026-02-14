package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.TensorConnector
import org.simbrain.network.gui.ArrowDirection
import org.simbrain.network.gui.ImageBox
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createArrowButton
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.bezierArrow
import java.awt.Font
import java.awt.RenderingHints
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

    private val arrow: BezierArrow

    init {
        pickable = true

        arrow = bezierArrow {
            color = java.awt.Color.DARK_GRAY

            padding {
                tail = 0.0
                head = 5.0 + arrowSize
            }

            lateralOffset { 0.5 }

            onUpdated { curve ->
                val pt = curve?.p(0.5) ?: line(connector.source.location, connector.target.location).p(0.5)
                val px = pt.x
                val py = pt.y
                if (connector is ConvolutionConnector) {
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
            renderKernelImage()
        }

        // Wire up events
        val connectorEvents = connector.events
        connectorEvents.updated.on { connectorEvents.updateGraphics.fire() }
        connectorEvents.updateGraphics.on(Dispatchers.Swing) {
            if (connector is ConvolutionConnector) renderKernelImage()
            updateDetailLabel()
        }
        connectorEvents.labelChanged.on(Dispatchers.Swing) { _, _ ->
            interactionBox.setText(connector.displayName)
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

    private fun renderKernelImage() {
        val conv = connector as? ConvolutionConnector ?: return
        val kSize = conv.kernelSize
        val inC = conv.source.shape.channels
        val kernelArea = kSize * kSize
        val filterOffset = currentFilter * inC * kernelArea + currentInputChannel * kernelArea
        val slice = DoubleArray(kernelArea)
        for (i in 0 until kernelArea) {
            slice[i] = conv.kernels[filterOffset + i]
        }
        imageBox.image = slice.toSimbrainColorImage(kSize, kSize)
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
                contextMenu.add(networkPanel.createAction(name = "Next Filter") { nextFilter() })
                contextMenu.add(networkPanel.createAction(name = "Previous Filter") { previousFilter() })
                contextMenu.add(networkPanel.createAction(name = "Next Input Channel") { nextInputChannel() })
                contextMenu.add(networkPanel.createAction(name = "Previous Input Channel") { previousInputChannel() })
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
