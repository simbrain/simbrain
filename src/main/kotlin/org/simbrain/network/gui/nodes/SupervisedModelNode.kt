package org.simbrain.network.gui.nodes

import org.piccolo2d.util.PBounds
import org.simbrain.network.core.InfoText
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.events.LocationEvents
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.util.NetworkTheme
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import java.awt.Color
import org.simbrain.util.piccolo.Outline
import org.simbrain.util.showInputDialog
import org.simbrain.util.swingDispatcher
import javax.swing.JComponent
import javax.swing.JPopupMenu

open class SupervisedModelNode(networkPanel: NetworkPanel, val supervisedModel: SupervisedModel) : ScreenElement(networkPanel) {

    protected val interactionBox: SupervisedModelNodeInteractionBox = SupervisedModelNodeInteractionBox(networkPanel)

    protected open val tabFill: Color get() = NetworkTheme.current.tabFillSupervised

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
        infoTextNode?.let { positionInfoTextNode(it) }
    }

    fun setInfoTextNode(infoTextNode: ScreenElement) {
        this.infoTextNode = infoTextNode
        supervisedModel.events.customInfoUpdated.on(swingDispatcher) { this.layoutChildren() }
        layoutChildren()
    }

    private fun positionInfoTextNode(infoNode: ScreenElement) {
        val infoText = supervisedModel.customInfo as? InfoText ?: return
        when (infoText.position) {
            InfoText.Position.BELOW_INTERACTION_BOX -> infoNode.centerFullBoundsOnPoint(
                interactionBox.fullBounds.centerX,
                interactionBox.fullBounds.maxY + infoText.spacing + infoNode.fullBounds.height / 2
            )
            InfoText.Position.ABOVE_INTERACTION_BOX -> infoNode.centerFullBoundsOnPoint(
                interactionBox.fullBounds.centerX,
                interactionBox.fullBounds.minY - infoText.spacing - infoNode.fullBounds.height / 2
            )
            InfoText.Position.BELOW_OUTLINE -> infoNode.centerFullBoundsOnPoint(
                outline.fullBounds.centerX,
                outline.fullBounds.maxY + infoText.spacing + infoNode.fullBounds.height / 2
            )
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
    open fun updateText() {
        interactionBox.setText(supervisedModel.displayName)
    }

    override val model: NetworkModel
        get() = supervisedModel

    override val isDraggable: Boolean
        get() = true

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            add(renameAction)
            add(removeAction)
            addSeparator()
            add(createEditAction("Edit ${supervisedModel.displayName}..."))
            add(createAction("Train...") {
                propertyDialog.run {
                    pack()
                    setLocationRelativeTo(null)
                    isVisible = true
                }
            })
            addSeparator()
            add(createAction("Add Current Data to Training Set") {
                supervisedModel.trainingSet.inputs.add(supervisedModel.inputLayer.activationArray.toMutableList())
                supervisedModel.trainingSet.targets.add(supervisedModel.outputLayer.activationArray.toMutableList())
            })
            add(createAction("Add Current Data to Testing Set") {
                supervisedModel.testingSet.inputs.add(supervisedModel.inputLayer.activationArray.toMutableList())
                supervisedModel.testingSet.targets.add(supervisedModel.outputLayer.activationArray.toMutableList())
            })
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
        val newName = showInputDialog("Name:", supervisedModel.label)
        if (newName != null) {
            supervisedModel.label = newName
        }
    }

    protected open val removeActionName: String get() = "Remove Supervised Model..."

    private val <T: JComponent> T.removeAction get() = createAction(
        name = removeActionName,
        iconPath = "menu_icons/minus.png",
        description = removeActionName,
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
        addChild(outline)
        addChild(interactionBox)

        val events: LocationEvents = supervisedModel.events
        events.labelChanged.on(swingDispatcher) { _, _ -> updateText() }
        events.locationChanged.on(swingDispatcher) { this.layoutChildren() }

        createApplyImmediateLearningAction()
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        outlinedObjects.filter { it.model is LocatableModel }.forEach { it.offset(dx, dy) }
        infoTextNode?.offset(dx, dy)
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

        override val tabFillColor: Color get() = this@SupervisedModelNode.tabFill

        override val contextMenu: JPopupMenu
            get() = this@SupervisedModelNode.contextMenu

        override val propertyDialog: StandardDialog
            get() = this@SupervisedModelNode.propertyDialog

        override val model: NetworkModel
            get() = this@SupervisedModelNode.supervisedModel

    }
}
