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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PText
import org.simbrain.network.core.ActivationStack
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.randomizeBiases
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.alignMenu
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.network.gui.spaceMenu
import org.simbrain.util.*
import org.simbrain.util.piccolo.addBorder
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import java.awt.Color
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.*

/**
 * The current pnode representation for all [Layer] objects. May be broken out into subtypes for different
 * subclasses of Layer.
 */
class ActivationStackNode(networkPanel: NetworkPanel, val activationStack: ActivationStack) :
    ArrayLayerNode(networkPanel, activationStack) {

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

    /**
     * Text corresponding to neuron's (optional) label.
     */
    private val labelText = PText()

    /**
     * Background for label text, so that background objects don't show up.
     */
    private val labelBackground = PNode()

    override val margin = 10.0

    /**
     * Height of array when in "flat" mode.
     */
    private val flatPixelArrayHeight = 10

    /**
     * Text showing info about the array.
     */
    private val infoText = PText().apply {
        font = INFO_FONT
        text = computeInfoText()
        mainNode.addChild(this)
    }

    /**
     * Create a new neuron array node.
     *
     * @param np Reference to NetworkPanel
     * @param na reference to model neuron array
     */
    init {

        val events = activationStack.events

        // TODO: Link to network preferences
        labelBackground.paint = Color.white
        labelBackground.setBounds(labelText.bounds)
        labelBackground.addChild(labelText)
        addChild(labelBackground)
        events.labelChanged.on(Dispatchers.Swing) { o, n -> updateTextLabel() }
        updateTextLabel()

        events.updated.on {
            events.updateGraphics.fire()
        }

        events.updateGraphics.on(Dispatchers.Swing) {
            updateActivationImage()
            updateInfoText()
        }

        updateActivationImage()
        activationImage.offset(0.0, infoText.offset.y + infoText.height + 5)
        spikeImage.offset(0.0, infoText.offset.y + infoText.height + 5)
        activationImage.addBorder()
        updateBorder()

        // call once to make sure all the actions are registered
        contextMenu

    }

    private fun updateActivationImage() {
        activationImage.removeAllChildren()
        spikeImage.removeAllChildren()
        biasImage.removeAllChildren()
        val activations = activationStack.activations

        val img = activations.flatten().toSimbrainColorImage(activations.ncol(), activations.nrow())
        activationImage.image = img
        activationImage.setBounds(
            0.0, 0.0,
            infoText.width, infoText.width
        )
        activationImage.addBorder()
        updateTextLabel()
    }

    private fun computeInfoText() = """
            ${activationStack.id}    Rows: ${activationStack.activations.nrow()} Cols: ${activationStack.activations.ncol()}
            Mean activation: ${activationStack.activations.flatten().average().format(4)}
            """.trimIndent()

    /**
     * Update status text.
     */
    private fun updateInfoText() {
        infoText.text = computeInfoText()
    }

    override val toolTipText
        get() = """
        <html>
        ${activationStack.toString().split("\n").joinToString("<br>")}
        </html>
    """.trimIndent()

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()

            // Edit Menu
            contextMenu.add(networkPanel.networkActions.cutAction)
            contextMenu.add(networkPanel.networkActions.copyAction)
            contextMenu.add(networkPanel.networkActions.pasteAction)
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

            val applyInputs: Action = networkPanel.networkActions.createTestInputPanelAction(activationStack)
            contextMenu.add(applyInputs)
            val addActivationToInput = networkPanel.networkActions.createAddActivationToInputAction(activationStack)
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
                    val arrayData = MatrixDataFrame(activationStack.activations)
                    dialog.contentPane = SimbrainTablePanel(arrayData)
                    dialog.addCommitTask {
                        with(networkPanel.network) {
                            activationStack.update()
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
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(activationStack)
            contextMenu.add(couplingMenu)
            return contextMenu
        }

    override val propertyDialog: StandardDialog
        get() = activationStack.createEditorDialog { updateInfoText() }

    override val model: ActivationStack
        get() = activationStack

    /**
     * Update the text label.
     */
    fun updateTextLabel() {
        if (!activationStack.label.isNullOrEmpty()) {
            labelText.font = NeuronNode.NEURON_FONT
            labelText.text = "" + activationStack.label
            labelText.setOffset(
                activationImage.x - labelText.width / 2 + activationImage.width / 2,
                activationImage.y - labelText.height - 17
            )
            labelBackground.setBounds(labelText.fullBounds)
        }
    }
}