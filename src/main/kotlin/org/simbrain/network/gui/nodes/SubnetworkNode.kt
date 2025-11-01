package org.simbrain.network.gui.nodes

import org.piccolo2d.util.PBounds
import org.simbrain.network.core.InfoText
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.events.LocationEvents
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.createTrainOnPatternAction
import org.simbrain.network.gui.dialogs.createTrainOnceOnPatternAction
import org.simbrain.network.gui.dialogs.getUnsupervisedTrainingPanel
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.util.*
import org.simbrain.util.piccolo.Outline
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPopupMenu

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
        interactionBox.centerFullBoundsOnPoint(
            outline.fullBounds.centerX,
            outline.fullBounds.getY() - interactionBox.fullBounds.getHeight() / 2 + 0.5
        )
        
        // Automatically position InfoText relative to interaction box
        infoTextNode?.let { infoNode ->
            positionInfoTextNode(infoNode)
        }
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
        add(networkPanel.createAction(name = "Edit ${subnetwork.displayName}...") {
            subnetwork.createEditorDialog().display()
        })
        addSeparator()
    }

    context(NetworkPanel)
    protected fun JPopupMenu.applyUnsupervisedActions(net: UnsupervisedNetwork) = apply {
        applyBasicActions()
        add(createAction("Add Current Pattern to Training Data") {
            net.trainingData.add(net.inputLayer.activationArray.toMutableList())
        })
        addSeparator()
        add(createAction("Train...") {
            getUnsupervisedTrainingPanel(net) {
                net.trainOnCurrentPattern()
            }.display()
        })
        add(with(networkPanel.network) { net.createTrainOnPatternAction() })
        add(net.createTrainOnceOnPatternAction())
        addSeparator()
        add(
            createAction(
                iconPath = "menu_icons/Rand.png",
                name = "Randomize",
                keyboardShortcut = 'R'
            ) {
                net.randomize(null)
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
        name = "Remove network...",
        iconPath = "menu_icons/minus.png",
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
        infoTextNode?.offset(dx, dy)
        outline.resetOutlinedNodes(outlinedObjects)
    }

    private fun updateOutline() {
        val nodes = HashSet(outlinedObjects)
        outline.resetOutlinedNodes(nodes)
    }

    fun setInfoTextNode(infoTextNode: ScreenElement) {
        this.infoTextNode = infoTextNode
        subnetwork.events.customInfoUpdated.on(swingDispatcher) { this.layoutChildren() }
        layoutChildren() // Trigger initial positioning
    }

    /**
     * Automatically position InfoText based on its configuration
     */
    private fun positionInfoTextNode(infoNode: ScreenElement) {
        // Get InfoText configuration from the model
        val infoText = subnetwork.customInfo as? InfoText ?: return
        
        when (infoText.position) {
            InfoText.Position.BELOW_INTERACTION_BOX -> {
                infoNode.centerFullBoundsOnPoint(
                    interactionBox.fullBounds.centerX,
                    interactionBox.fullBounds.maxY + infoText.spacing + infoNode.fullBounds.height / 2
                )
            }
            InfoText.Position.ABOVE_INTERACTION_BOX -> {
                infoNode.centerFullBoundsOnPoint(
                    interactionBox.fullBounds.centerX,
                    interactionBox.fullBounds.minY - infoText.spacing - infoNode.fullBounds.height / 2
                )
            }
            InfoText.Position.BELOW_OUTLINE -> {
                infoNode.centerFullBoundsOnPoint(
                    outline.fullBounds.centerX,
                    outline.fullBounds.maxY + infoText.spacing + infoNode.fullBounds.height / 2
                )
            }
        }
    }


    override fun isIntersecting(bound: PBounds?): Boolean {
        return interactionBox.isIntersecting(bound)
    }


    /**
     * Basic interaction box for subnetwork nodes. Ensures a property dialog
     * appears when the box is double-clicked.
     */
    inner class SubnetworkNodeInteractionBox(net: NetworkPanel) : InteractionBox(net) {

        override val contextMenu: JPopupMenu?
            get() = this@SubnetworkNode.contextMenu

        override val propertyDialog: StandardDialog?
            get() = this@SubnetworkNode.propertyDialog

        override val model: NetworkModel
            get() = this@SubnetworkNode.subnetwork

    }
}
