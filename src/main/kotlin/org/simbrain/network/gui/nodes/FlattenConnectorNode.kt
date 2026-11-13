/** Displays a tensor-to-array bridge and shares property editing between its arrow and interaction box. */
package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.util.PBounds
import org.simbrain.network.core.FlattenConnector
import org.simbrain.network.gui.LaidOutConnector
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createSelectionEditDialog
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.bezierArrow
import org.simbrain.util.piccolo.centerOn
import java.awt.geom.CubicCurve2D
import java.awt.geom.Rectangle2D
import javax.swing.JPopupMenu

/**
 * GUI node for a [FlattenConnector]. Draws a bezier arrow from the source [TensorNode]
 * to the target NeuronArrayNode, with an [InteractionBox] showing the connector label.
 */
class FlattenConnectorNode(networkPanel: NetworkPanel, val connector: FlattenConnector) :
    ScreenElement(networkPanel), LaidOutConnector {

    val sourceNode by lazy { networkPanel.getNode(connector.source) }
    val targetNode by lazy { networkPanel.getNode(connector.target) }

    val interactionBox = FlattenConnectorInteractionBox(networkPanel)

    override val connectionDrawing = ConnectionDrawing(this)

    private val arrow: BezierArrow

    /** The shaft of the arrow as last laid out, or null for layers no arrow can be drawn between. */
    private var shaft: CubicCurve2D? = null

    init {
        pickable = true

        arrow = bezierArrow {
            color = NetworkPreferences.connectorArrowColor

            padding {
                tail = 0.0
                head = 5.0 + arrowSize
            }

            lateralOffset { 0.5 }
        }

        connectionDrawing.addChild(arrow)
        addChild(interactionBox)

        val connectorEvents = connector.events
        connectorEvents.labelChanged.on(Dispatchers.Swing) { _, _ ->
            interactionBox.setText(connector.displayName)
            requestLayout()
        }.untilDisposed()

        connector.source.events.locationChanged.on(Dispatchers.Swing) {
            requestLayout()
        }.untilDisposed()
        connector.target.events.locationChanged.on(Dispatchers.Swing) {
            requestLayout()
        }.untilDisposed()

        interactionBox.setText(connector.displayName)
        layoutWithView()
    }

    fun updateArrowColorFromPreferences() {
        arrow.updateColorFromPreferences()
    }

    /** The space taken up by the two layers the connector joins. */
    private fun ownLayers(): List<Rectangle2D> = listOfNotNull(sourceNode?.footprint, targetNode?.footprint)

    /** The space the two layers took up when the arrow was last drawn. */
    private var drawnBetween: List<Rectangle2D>? = null

    /** The space taken up by the layers in [layers] that the connector does not join. */
    private fun otherLayers(layers: List<Rectangle2D>): List<Rectangle2D> {
        val own = ownLayers()
        return layers.filter { layer -> own.none { it == layer } }
    }

    override fun layoutArrow(layers: List<Rectangle2D>) {
        drawnBetween = ownLayers()
        val srcBodyBounds = sourceNode?.globalBounds ?: return
        val tgtBodyBounds = targetNode?.globalBounds ?: return
        val srcTopBounds = sourceNode?.footprint ?: return
        val tgtTopBounds = targetNode?.footprint ?: return
        shaft = arrow.layout(
            directionalOutlines(srcTopBounds, srcBodyBounds),
            directionalOutlines(tgtTopBounds, tgtBodyBounds),
            false,
            otherLayers(layers)
        )
    }

    /** One of the two layers changed since the arrow was drawn, or another layer is or was in the arrow's way. */
    override fun arrowAffectedBy(layers: List<Rectangle2D>) =
        ownLayers() != drawnBetween || arrow.affectedBy(otherLayers(layers))

    /** The label box, to be placed on the arrow's shaft. */
    override fun drawingToPlace(): ShaftItem? {
        val curve = shaft ?: return null
        return ShaftItem(
            curve, interactionBox.asShaftDrawing(), ownLayers = listOfNotNull(sourceNode?.footprint, targetNode?.footprint)
        )
    }

    override fun placeDrawing(placement: ShaftPlacement?): Rectangle2D {
        val center = placement?.let { point(it.centerX, it.centerY) }
            ?: line(connector.source.location, connector.target.location).p(0.5)
        interactionBox.centerOn(center.x, center.y)
        return interactionBox.asShaftDrawing().footprintAt(center.x, center.y, 0.0)
    }

    override val isDraggable: Boolean = false

    /** The label box. */
    override fun measureFootprint() = footprintOf(interactionBox.globalBounds)

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()
            contextMenu.add(networkPanel.createAction(name = "Edit...") {
                propertyDialog?.display()
            })
            return contextMenu
        }

    override fun createEditDialog(): StandardDialog? {
        return networkPanel.createSelectionEditDialog(connector)
    }

    override val propertyDialog: StandardDialog? get() = createEditDialog()

    override val model: FlattenConnector get() = connector

    override fun isIntersecting(bound: PBounds?): Boolean {
        if (bound == null) return false
        return arrow.globalBounds.intersects(bound) ||
                interactionBox.globalBounds.intersects(bound)
    }

    inner class FlattenConnectorInteractionBox(net: NetworkPanel) : InteractionBox(net) {
        override val contextMenu: JPopupMenu
            get() = this@FlattenConnectorNode.contextMenu
        override fun createEditDialog(): StandardDialog? =
            this@FlattenConnectorNode.createEditDialog()
        override val propertyDialog: StandardDialog?
            get() = this@FlattenConnectorNode.createEditDialog()
        override val isDraggable: Boolean get() = false
        override val model: FlattenConnector
            get() = this@FlattenConnectorNode.connector
    }
}
