/** Displays a tensor-to-array bridge and shares property editing between its arrow and interaction box. */
package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.util.PBounds
import org.simbrain.network.core.FlattenConnector
import org.simbrain.network.gui.LaidOutConnector
import org.simbrain.network.gui.Obstacles
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

    override fun layoutArrow() {
        val srcBodyBounds = sourceNode?.globalBounds ?: return
        val tgtBodyBounds = targetNode?.globalBounds ?: return
        val srcTopBounds = sourceNode?.footprint ?: return
        val tgtTopBounds = targetNode?.footprint ?: return
        shaft = arrow.layout(
            directionalOutlines(srcTopBounds, srcBodyBounds),
            directionalOutlines(tgtTopBounds, tgtBodyBounds),
            false
        )
    }

    /** Place the label box on the arrow's shaft, clear of [obstacles] where there is room. */
    override fun placeOnArrow(obstacles: Obstacles): Rectangle2D {
        val label = interactionBox.asShaftDrawing()
        val curve = shaft
        val center = if (curve != null) {
            val ownLayers = listOfNotNull(sourceNode?.footprint, targetNode?.footprint)
            ShaftPlacer.place(curve, label, obstacles.layers, obstacles.drawings, ownLayers)
                .let { point(it.centerX, it.centerY) }
        } else {
            line(connector.source.location, connector.target.location).p(0.5)
        }
        interactionBox.centerOn(center.x, center.y)
        return label.footprintAt(center.x, center.y, 0.0)
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
