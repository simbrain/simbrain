package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.util.PBounds
import org.simbrain.network.core.FlattenConnector
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.bezierArrow
import javax.swing.JPopupMenu

/**
 * GUI node for a [FlattenConnector]. Draws a bezier arrow from the source [TensorNode]
 * to the target NeuronArrayNode, with an [InteractionBox] showing the connector label.
 */
class FlattenConnectorNode(networkPanel: NetworkPanel, val connector: FlattenConnector) :
    ScreenElement(networkPanel) {

    val sourceNode by lazy { networkPanel.getNode(connector.source) }
    val targetNode by lazy { networkPanel.getNode(connector.target) }

    val interactionBox = FlattenConnectorInteractionBox(networkPanel)

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
                interactionBox.centerFullBoundsOnPoint(pt.x, pt.y)
            }
        }

        addChild(arrow)
        addChild(interactionBox)

        val connectorEvents = connector.events
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
    }

    override fun layoutChildren() {
        val srcBounds = sourceNode?.globalBounds ?: return
        val tgtBounds = targetNode?.globalBounds ?: return
        arrow.layout(srcBounds.outlines, tgtBounds.outlines, false)
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
            return contextMenu
        }

    override fun createEditDialog(): StandardDialog? {
        return connector.createEditorDialog()
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
