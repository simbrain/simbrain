package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.nodes.PImage
import org.simbrain.network.core.ActivationSequence
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.randomizeBiases
import org.simbrain.network.gui.*
import org.simbrain.util.*
import org.simbrain.util.piccolo.addBorder
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JMenu
import javax.swing.JPopupMenu

/**
 * PNode representation for [ActivationSequence]
 */
class ActivationSequenceNode(networkPanel: NetworkPanel, val activationSequence: ActivationSequence) :
    ArrayLayerNode(networkPanel, activationSequence) {

    /**
     * Main pixel image for activations.
     */
    protected val activationImage = PImage().apply {
        mainNode.addChild(this)
    }

    /**
     * Image with spikes and transparent background overlaid on the activation image for spiking neuron arrays.
     */
    private val spikeImage = PImage().apply {
        mainNode.addChild(this)
    }

    protected val biasImage = PImage()

    val imageSize = 100.0

    override val margin = 10.0

    /**
     * Create a new neuron array node.
     *
     * @param np Reference to NetworkPanel
     * @param na reference to model neuron array
     */
    init {

        val events = activationSequence.events



        events.updated.on {
            events.updateGraphics.fire()
        }

        events.updateGraphics.on(Dispatchers.Swing) {
            updateActivationImage()
        }

        updateActivationImage()
        activationImage.offset(0.0, 5.0)
        spikeImage.offset(0.0, 5.0)
        activationImage.addBorder()
        updateBorder()

        // call once to make sure all the actions are registered
        contextMenu

    }

    private fun updateActivationImage() {
        activationImage.removeAllChildren()
        spikeImage.removeAllChildren()
        biasImage.removeAllChildren()
        val activations = activationSequence.activations

        val img = activations.flatten().toSimbrainColorImage(activations.ncol(), activations.nrow())
        activationImage.image = img
        activationImage.setBounds(
            0.0, 0.0,
            imageSize, imageSize
        )
        activationImage.addBorder()
    }

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()

            // Edit Menu
            contextMenu.add(networkPanel.networkActions.cutAction)
            contextMenu.add(networkPanel.networkActions.copyAction)
            contextMenu.add(networkPanel.networkActions.pasteAction)
            contextMenu.add(networkPanel.networkActions.duplicateAction)
            contextMenu.addSeparator()
            val editArray: Action = object : AbstractAction("Edit...") {
                override fun actionPerformed(event: ActionEvent) {
                    propertyDialog?.display()
                }
            }
            contextMenu.add(editArray)
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()
            contextMenu.add(networkPanel.networkActions.connectSelectedModels)
            contextMenu.addSeparator()

            val applyInputs: Action = networkPanel.networkActions.createTestInputPanelAction(activationSequence)
            contextMenu.add(applyInputs)
            val addActivationToInput = networkPanel.networkActions.createAddActivationToInputAction(activationSequence)
            contextMenu.add(addActivationToInput)
            contextMenu.addSeparator()

            // Randomize Action
            val randomizeAction = networkPanel.networkActions.randomizeObjectsAction

            contextMenu.add(randomizeAction)
            val randomizeBiasesAction = networkPanel.createAction(
                name = "Randomize Biases",
                description = "Randomize the biases of this neuron array",
                iconPath = "menu_icons/Rand.png"
            ) {
                with(network) {
                    networkPanel.selectionManager
                        .filterSelectedModels<NeuronArray>()
                        .forEach { it.randomizeBiases() }
                }
            }
            contextMenu.add(randomizeBiasesAction)
            contextMenu.addSeparator()
            val editComponents: Action = object : AbstractAction("Edit Components...") {
                override fun actionPerformed(event: ActionEvent) {
                    val dialog = StandardDialog()
                    val arrayData = MatrixDataFrame(activationSequence.activations)
                    dialog.contentPane = SimbrainTablePanel(arrayData)
                    dialog.addCommitTask {
                        with(networkPanel.network) {
                            activationSequence.update()
                        }
                    }
                    dialog.pack()
                    dialog.setLocationRelativeTo(null)
                    dialog.isVisible = true
                }
            }
            contextMenu.add(editComponents)

            contextMenu.addSeparator()
            contextMenu.add(networkPanel.alignMenu)
            contextMenu.add(networkPanel.spaceMenu)

            // Coupling menu
            contextMenu.addSeparator()
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(activationSequence)
            contextMenu.add(couplingMenu)
            return contextMenu
        }

    private fun createEditDialog(editingObjects: List<ActivationSequenceNode>): StandardDialog? = editingObjects.let { aqnList ->

        if (aqnList.isEmpty()) return null

        aqnList.map { it.model }.createEditorDialog()
    }


    override fun createEditDialog(): StandardDialog? = createEditDialog(networkPanel.filterSelectedNodeByClass<ActivationSequenceNode>())

    override val propertyDialog get() = createEditDialog(listOf(this))

    override val model: ActivationSequence
        get() = activationSequence


}