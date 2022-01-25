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

import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PText
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.actions.edit.CopyAction
import org.simbrain.network.gui.actions.edit.CutAction
import org.simbrain.network.gui.actions.edit.DeleteAction
import org.simbrain.network.gui.actions.edit.PasteAction
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.util.*
import org.simbrain.util.piccolo.addBox
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.table.NumericTable
import org.simbrain.util.table.SimbrainJTable
import org.simbrain.util.table.SimbrainJTableScrollPanel
import smile.math.matrix.Matrix
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.*
import kotlin.math.sqrt

/**
 * The current pnode representation for all [Layer] objects. May be broken out into subtypes for different
 * subclasses of Layer.
 */
class NeuronArrayNode(networkPanel: NetworkPanel, val neuronArray: NeuronArray):
    ArrayLayerNode(networkPanel, neuronArray) {

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
     * Image to show activationImage.
     */
    private val activationImage = PImage().apply {
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
        events.onUpdated {
            updateActivationImage()
            updateInfoText()
        }
        updateActivationImage()
        activationImage.offset(0.0, infoText.offset.y + infoText.height + 5)
        activationImage.addBox()
        updateBorder()
    }

    private fun updateActivationImage() {
        if (gridMode) {
            // "Grid" case
            val activations = neuronArray.outputs.col(0)
            val len = sqrt(activations.size.toDouble()).toInt()
            val img = activations.toSimbrainColorImage(len, len)
            activationImage.image = img
            activationImage.setBounds(
                0.0, 0.0,
                infoText.width, infoText.width
            )
        } else {
            // "Flat" case
            val activations = neuronArray.outputs.col(0)
            val img = activations.toSimbrainColorImage(activations.size, 1)
            activationImage.image = img
            activationImage.setBounds(
                0.0, 0.0,
                infoText.width, flatPixelArrayHeight.toDouble()
            )
        }
    }

    private fun computeInfoText() = """
            ${neuronArray.label}    nodes: ${neuronArray.size()}
            mean activation: ${neuronArray.activations.col(0).average().format(4)}
            """.trimIndent()
            // if (neuronArray.getPrototypeRule() instanceof SpikingNeuronUpdateRule) {
            //     // TODO: Use this to place a yellow grid over pixels for spiking components
            //     System.out.println(
            //             Arrays.toString(((DataHolder.SpikingDataHolder)
            //                     neuronArray.getDataHolder()).spikes));
            // }

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
                val dialog = arrayDialog
                dialog.isVisible = true
            }
        }
        contextMenu.add(editArray)
        contextMenu.add(DeleteAction(networkPanel))
        contextMenu.addSeparator()
        contextMenu.add(networkPanel.networkActions.connectSelectedModels)
        contextMenu.addSeparator()


        // Layout style
        // TODO: Add a third "LooseNeuron" mode.  It can also be grid or line.  Only allow it for < 1K or some number
        val switchStyle: Action = networkPanel.createAction(
            name = "Switch style",
            iconPath = "menu_icons/grid.png",
            description = "Change to grid style"
        ) {
            gridMode = !gridMode
        }
        contextMenu.add(switchStyle)

        // Example of how to use radio buttons in case we end up with three designs
        val switchStyleMenu = JMenu("Switch Styles")
        val styles = ButtonGroup()
        val state1 = JRadioButtonMenuItem("Style 1", true)
        styles.add(state1)
        state1.addActionListener { println("State 1") }
        switchStyleMenu.add(state1)
        val state2 = JRadioButtonMenuItem("Style 2")
        styles.add(state2)
        state2.addActionListener { println("State 2") }
        switchStyleMenu.add(state2)
        val state3 = JRadioButtonMenuItem("Style 3")
        state3.addActionListener { println("State 3") }
        styles.add(state3)
        switchStyleMenu.add(state3)
        contextMenu.add(switchStyleMenu)
        contextMenu.addSeparator()

        // Randomize Action
        val randomizeAction: Action = object : AbstractAction("Randomize") {
            init {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Rand.png"))
                putValue(SHORT_DESCRIPTION, "Randomize neuro naarray")
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

    /**
     * Returns the dialog for editing this neuron array.
     */
    private val arrayDialog: StandardDialog
        get() {
            val dialog: StandardDialog = AnnotatedPropertyEditor(neuronArray).dialog
            dialog.pack()
            dialog.setLocationRelativeTo(null)
            dialog.addClosingTask { updateInfoText() }
            return dialog
        }

    override fun getPropertyDialog(): JDialog {
        return arrayDialog
    }

    override fun getModel(): NeuronArray {
        return neuronArray
    }
}