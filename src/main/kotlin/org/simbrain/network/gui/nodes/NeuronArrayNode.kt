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
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.actions.edit.CopyAction
import org.simbrain.network.gui.actions.edit.CutAction
import org.simbrain.network.gui.actions.edit.DeleteAction
import org.simbrain.network.gui.actions.edit.PasteAction
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.*
import org.simbrain.util.piccolo.addBorder
import org.simbrain.util.table.NumericTable
import org.simbrain.util.table.SimbrainJTable
import org.simbrain.util.table.SimbrainJTableScrollPanel
import smile.math.matrix.Matrix
import java.awt.Color
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.*
import kotlin.math.sqrt

/**
 * The current pnode representation for all [Layer] objects. May be broken out into subtypes for different
 * subclasses of Layer.
 */
class NeuronArrayNode(networkPanel: NetworkPanel, val neuronArray: NeuronArray) :
    ArrayLayerNode(networkPanel, neuronArray) {

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


    /**
     * Text corresponding to neuron's (optional) label.
     */
    private val labelText = PText()

    /**
     * Background for label text, so that background objects don't show up.
     */
    private val labelBackground = PNode()

    /**
     * If true, show the image array as a grid; if false show it as a horizontal line.
     */
    private var gridMode = false
        set(value) {
            field = value
            updateActivationImage()
            updateBorder()
        }

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

        val events = neuronArray.events

        events.gridModeChanged.on {
            gridMode = neuronArray.isGridMode
        }
        gridMode = neuronArray.isGridMode

        // TODO: Link to network preferences
        labelBackground.paint = Color.white
        labelBackground.setBounds(labelText.bounds)
        labelBackground.addChild(labelText)
        addChild(labelBackground)
        events.labelChanged.on(Dispatchers.Swing) { o, n -> updateTextLabel() }
        updateTextLabel()

        events.updated.on(Dispatchers.Swing) {
            updateActivationImage()
            updateInfoText()
        }
        events.updateRuleChanged.on(Dispatchers.Swing) {
            if (!neuronArray.updateRule.isSpikingRule) {
                mainNode.removeChild(spikeImage)
            }
        }
        updateActivationImage()
        activationImage.offset(0.0, infoText.offset.y + infoText.height + 5)
        spikeImage.offset(0.0, infoText.offset.y + infoText.height + 5)
        activationImage.addBorder()
        updateBorder()

    }

    private fun updateActivationImage() {
        activationImage.removeAllChildren()
        val activations = neuronArray.outputs.col(0)
        if (gridMode) {
            // "Grid" case
            val len = sqrt(activations.size.toDouble()).toInt()
            val img = activations.toSimbrainColorImage(len, len)
            activationImage.image = img
            activationImage.setBounds(
                0.0, 0.0,
                infoText.width, infoText.width
            )
            if (neuronArray.updateRule.isSpikingRule) {
                val spikes = (neuronArray.dataHolder as SpikingMatrixData).spikes
                spikeImage.image = spikes.toOverlay(len, len, NeuronNode.spikingColor)
                spikeImage.setBounds(
                    0.0, 0.0,
                    infoText.width, infoText.width
                )
            }
        } else {
            // "Flat" case
            val img = activations.toSimbrainColorImage(activations.size, 1)
            activationImage.image = img
            activationImage.setBounds(
                0.0, 0.0,
                infoText.width, flatPixelArrayHeight.toDouble()
            )
            activationImage.addBorder()
            if (neuronArray.updateRule.isSpikingRule) {
                val spikes = (neuronArray.dataHolder as SpikingMatrixData).spikes
                spikeImage.image = spikes.toOverlay(activations.size, 1, NeuronNode.spikingColor)
                spikeImage.setBounds(
                    0.0, 0.0,
                    infoText.width, flatPixelArrayHeight.toDouble()
                )
            }
        }
        updateTextLabel()
    }

    private fun computeInfoText() = """
            ${neuronArray.id}    nodes: ${neuronArray.size()}
            mean activation: ${neuronArray.activations.col(0).average().format(4)}
            """.trimIndent()

    /**
     * Update status text.
     */
    private fun updateInfoText() {
        infoText.text = computeInfoText()
    }

    override fun getToolTipText() = neuronArray.toString()

    override fun getContextMenu(): JPopupMenu {
        val contextMenu = JPopupMenu()

        // Edit Menu
        contextMenu.add(CutAction(networkPanel))
        contextMenu.add(CopyAction(networkPanel))
        contextMenu.add(PasteAction(networkPanel))
        contextMenu.addSeparator()
        val editArray: Action = object : AbstractAction("Edit...") {
            override fun actionPerformed(event: ActionEvent) {
                propertyDialog.display()
            }
        }
        contextMenu.add(editArray)
        contextMenu.add(DeleteAction(networkPanel))
        contextMenu.addSeparator()
        contextMenu.add(networkPanel.networkActions.connectSelectedModels)
        contextMenu.addSeparator()

        // Choose style
        val switchStyle: Action = networkPanel.createAction(
            name = "Toggle line / grid",
            iconPath = "menu_icons/grid.png",
            description = "Toggle line / grid style"
        ) {
            neuronArray.isGridMode = !neuronArray.isGridMode
        }
        contextMenu.add(switchStyle)
        contextMenu.addSeparator()

        // Randomize Action
        val randomizeAction: Action = object : AbstractAction("Randomize") {
            init {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Rand.png"))
                putValue(SHORT_DESCRIPTION, "Randomize neuron array")
            }

            override fun actionPerformed(event: ActionEvent) {
                neuronArray.randomize()
            }
        }
        contextMenu.add(randomizeAction)
        contextMenu.addSeparator()
        val editComponents: Action = object : AbstractAction("Edit Components...") {
            override fun actionPerformed(event: ActionEvent) {
                val dialog = StandardDialog()
                val arrayData = NumericTable(neuronArray.outputs.col(0))
                dialog.contentPane = SimbrainJTableScrollPanel(
                    SimbrainJTable.createTable(arrayData)
                )
                dialog.addClosingTask {
                    neuronArray.addInputs(Matrix(arrayData.vectorCurrentRow))
                    neuronArray.update()
                }
                dialog.pack()
                dialog.setLocationRelativeTo(null)
                dialog.isVisible = true
            }
        }
        contextMenu.add(editComponents)

        // Coupling menu
        contextMenu.addSeparator()
        val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(neuronArray)
        contextMenu.add(couplingMenu)
        return contextMenu
    }

    override fun getPropertyDialog(): JDialog {
        return neuronArray.createDialog { updateInfoText()}
    }

    override fun getModel(): NeuronArray {
        return neuronArray
    }

    /**
     * Update the text label.
     */
    fun updateTextLabel() {
        if (!neuronArray.label.isNullOrEmpty()) {
            labelText.font = NeuronNode.NEURON_FONT
            labelText.text = "" + neuronArray.label
            labelText.setOffset(
                activationImage.x - labelText.width / 2 + activationImage.width / 2,
                activationImage.y - labelText.height - 17
            )
            labelBackground.setBounds(labelText.fullBounds)
        }
    }
}