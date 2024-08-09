/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.nodes

import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.events.LocationEvents
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.createTrainOnPatternAction
import org.simbrain.network.gui.dialogs.getUnsupervisedTrainingPanel
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.util.*
import org.simbrain.util.piccolo.Outline
import javax.swing.*

/**
 * PNode representation of a subnetwork. This class contains an interaction box
 * an [Outline] node (containing neuron groups and synapse groups)
 * as children. The outlinedobjects node draws the boundary around the contained
 * nodes. The interaction box is the point of contact. Layout happens in the
 * overridden layoutchildren method.
 *
 * @author Jeff Yoshimi
 */
open class SubnetworkNode(networkPanel: NetworkPanel, val subnetwork: Subnetwork) : ScreenElement(networkPanel) {

    /**
     * The interaction box for this neuron group.
     */
    private var interactionBox: SubnetworkNodeInteractionBox

    /**
     * The outlined objects (neuron and synapse groups) for this node.
     */
    val outline: Outline = Outline()

    /**
     * The outlined objects
     */
    private val outlinedObjects: MutableSet<ScreenElement> = LinkedHashSet()

    private var infoTextNode: ScreenElement? = null

    public override fun layoutChildren() {
        updateOutline()
        interactionBox.setOffset(
            outline.fullBounds.getX()
                    + Outline.ARC_SIZE / 2,
            outline.fullBounds.getY() - interactionBox.fullBounds.getHeight() + 1
        )
    }

    /**
     * Need to maintain a list of nodes which are outlined
     */
    fun addNode(node: ScreenElement) {
        outlinedObjects.add(node)
        node.model.events.deleted.on(swingDispatcher) {
            outlinedObjects.remove(node)
            outline.resetOutlinedNodes(outlinedObjects)
        }
        (node.model as? LocatableModel)?.events?.locationChanged?.fire()

        updateOutline()
    }

    /**
     * Set a custom interaction box.
     *
     * @param newBox the newBox to set.
     */
    protected fun setInteractionBox(newBox: SubnetworkNodeInteractionBox) {
        this.removeChild(interactionBox)
        this.interactionBox = newBox
        this.addChild(interactionBox)
        updateText()
    }

    /**
     * Update the text in the interaction box.
     */
    fun updateText() {
        interactionBox.setText(subnetwork.displayName)
    }

    override val model: NetworkModel
        get() = subnetwork

    override val isDraggable: Boolean
        get() = true

    override val contextMenu: JPopupMenu?
        get() = defaultContextMenu

    protected val defaultContextMenu: JPopupMenu = JPopupMenu().addDefaultSubnetActions()

    protected fun JPopupMenu.addDefaultSubnetActions(): JPopupMenu = apply {
        add(renameAction)
        add(removeAction)
    }

    protected fun JPopupMenu.applyBasicActions() = apply {
        add(networkPanel.networkActions.cutAction)
        add(networkPanel.networkActions.copyAction)
        add(renameAction)
        add(removeAction)
        addSeparator()

        // Edit Submenu
        // TODO: Add check
        add(networkPanel.createAction(name = "Edit...") {
            subnetwork.createEditorDialog().display()
        })
        addSeparator()
    }

    context(NetworkPanel)
    protected fun JPopupMenu.applyUnsupervisedActions(net: UnsupervisedNetwork) = apply {
        applyBasicActions()
        add(createAction("Train...") {
            getUnsupervisedTrainingPanel(net) {
                net.trainOnCurrentPattern()
            }
        })
        add(with(networkPanel.network) { net.createTrainOnPatternAction() })
        addSeparator()
        add(createAction("Randomize") {
            net.randomize()
        })
    }

    protected fun createEditAction(name: String) = createAction(name = name) {
        propertyDialog?.run {
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    /**
     * Action for editing the group name.
     */
    protected val <T: JComponent> T.renameAction get() = createAction(
        name = "Rename..."
    ) {
        val newName = JOptionPane.showInputDialog("Name:", subnetwork.label)
        subnetwork.label = newName
    }

    protected val <T: JComponent> T.removeAction get() = createAction(
        name = "Remove Network...",
        iconPath = "menu_icons/RedX_small.png",
        description = "Remove this subnetwork...",
        coroutineScope = networkPanel.network
    ) {
        subnetwork.delete()
    }

    /**
     * Create a subnetwork node.
     *
     * @param networkPanel parent panel
     * @param subnet       the layered network
     */
    init {
        interactionBox = SubnetworkNodeInteractionBox(networkPanel)
        interactionBox.setText(subnetwork.displayName)
        addChild(outline)
        addChild(interactionBox)

        val events: LocationEvents = subnetwork.events
        events.deleted.on(swingDispatcher) { removeFromParent() }
        events.labelChanged.on(swingDispatcher) { _, _ -> updateText() }
        events.locationChanged.on(swingDispatcher) { this.layoutChildren() }
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        for (node in outlinedObjects) {
            if (node is NeuronGroupNode) {
                node.offset(dx, dy)
            } else if (node is NeuronArrayNode) {
                node.offset(dx, dy)
            }
        }
        if (infoTextNode != null) {
            infoTextNode!!.offset(dx, dy)
        }
        outline.resetOutlinedNodes(outlinedObjects)
    }

    private fun updateOutline() {
        val nodes = HashSet(outlinedObjects)
        if (infoTextNode != null) {
            nodes.add(infoTextNode!!)
        }
        outline.resetOutlinedNodes(nodes)
    }

    fun setInfoTextNode(infoTextNode: ScreenElement) {
        val offset = subnetwork.location
        val infoTextInitLocation = (infoTextNode.model as LocatableModel).location
        val finalLocation = infoTextInitLocation.plus(offset)
        this.infoTextNode = infoTextNode
        (infoTextNode.model as LocatableModel).setLocation(finalLocation.x, finalLocation.y)
        subnetwork.events.customInfoUpdated.on(swingDispatcher) { this.updateOutline() }
        updateOutline()
    }


    /**
     * Basic interaction box for subnetwork nodes. Ensures a property dialog
     * appears when the box is double-clicked.
     */
    inner class SubnetworkNodeInteractionBox(net: NetworkPanel?) : InteractionBox(net) {

        override val contextMenu: JPopupMenu?
            get() = this@SubnetworkNode.contextMenu

        override val propertyDialog: StandardDialog?
            get() = this@SubnetworkNode.propertyDialog

        override val model: NetworkModel
            get() = this@SubnetworkNode.subnetwork
    }
}
