package org.simbrain.network.gui.nodes

import org.piccolo2d.util.PBounds
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.events.LocationEvents
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.piccolo.Outline
import org.simbrain.util.swingDispatcher
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPopupMenu

class SupervisedModelNode(networkPanel: NetworkPanel, val supervisedModel: SupervisedModel) : ScreenElement(networkPanel) {

    private var interactionBox: SupervisedModelNodeInteractionBox = SupervisedModelNodeInteractionBox(networkPanel)

    /**
     * The outlined objects (neuron and synapse groups) for this node.
     */
    val outline: Outline = Outline()

    /**
     * The outlined objects
     */
    private val outlinedObjects: MutableSet<ScreenElement> = LinkedHashSet()

    override val toolTipText: String
        get() = "Double click here to train..."

    public override fun layoutChildren() {
        updateOutline()
        interactionBox.centerFullBoundsOnPoint(
            outline.fullBounds.centerX,
            outline.fullBounds.getY() - interactionBox.fullBounds.getHeight() / 2 + 0.5
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
        node.model.events
        (node.model as? LocatableModel)?.let { locatableModel ->
            locatableModel.events.locationChanged.on(swingDispatcher) {
                updateOutline()
            }
            locatableModel.events.locationChanged.fire()
        }

        updateOutline()
    }

    /**
     * Update the text in the interaction box.
     */
    fun updateText() {
        interactionBox.setText(supervisedModel.displayName)
    }

    override val model: NetworkModel
        get() = supervisedModel

    override val isDraggable: Boolean
        get() = true

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            add(createEditAction("Edit / Train..."))
            add(renameAction)
            add(removeAction)
            addSeparator()
            add(createApplyImmediateLearningAction())
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) { supervisedModel.getSupervisedTrainingDialog() }

    private fun createEditAction(name: String) = createAction(name = name) {
        propertyDialog.run {
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    private val <T: JComponent> T.renameAction get() = createAction(
        name = "Rename..."
    ) {
        val newName = JOptionPane.showInputDialog("Name:", supervisedModel.label)
        supervisedModel.label = newName
    }

    private val <T: JComponent> T.removeAction get() = createAction(
        name = "Remove Supervised Model...",
        iconPath = "menu_icons/minus.png",
        description = "Remove this supervised model...",
        coroutineScope = networkPanel.network
    ) {
        supervisedModel.deleteBlocking()
    }

    private fun createApplyImmediateLearningAction() = networkPanel.createAction(
        name = "Apply Immediate Learning",
        description = "Train this model on the values currently in the input and target layers",
        keyboardShortcut = 'L'
    ) {
        networkPanel.selectionManager.filterSelectedModels<SupervisedModel>().forEach { sm ->
            with(networkPanel.network) { sm.applyImmediateLearning() }
        }
    }

    init {
        interactionBox.setText(supervisedModel.displayName)
        interactionBox.paint = Color(209, 255, 204)
        addChild(outline)
        addChild(interactionBox)

        val events: LocationEvents = supervisedModel.events
        events.labelChanged.on(swingDispatcher) { _, _ -> updateText() }
        events.locationChanged.on(swingDispatcher) { this.layoutChildren() }

        createApplyImmediateLearningAction()
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        outlinedObjects.filter { it.model is LocatableModel }.forEach { it.offset(dx, dy) }
        outline.resetOutlinedNodes(outlinedObjects)
    }

    private fun updateOutline() {
        val nodes = HashSet(outlinedObjects)
        outline.resetOutlinedNodes(nodes)
    }

    override fun isIntersecting(bound: PBounds?): Boolean {
        return interactionBox.isIntersecting(bound)
    }

    /**
     * Basic interaction box for supervisedModel nodes. Ensures a property dialog
     * appears when the box is double-clicked.
     */
    inner class SupervisedModelNodeInteractionBox(net: NetworkPanel) : InteractionBox(net) {

        override val contextMenu: JPopupMenu
            get() = this@SupervisedModelNode.contextMenu

        override val propertyDialog: StandardDialog
            get() = this@SupervisedModelNode.propertyDialog

        override val model: NetworkModel
            get() = this@SupervisedModelNode.supervisedModel

        override val toolTipText: String
            get() = "Double click to train..."

    }
}
