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
import org.simbrain.network.core.randomizeBiases
import org.simbrain.network.events.NeuronArrayEvents2
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.actions.edit.CopyAction
import org.simbrain.network.gui.actions.edit.CutAction
import org.simbrain.network.gui.actions.edit.PasteAction
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.util.BiasedMatrixData
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.*
import org.simbrain.util.piccolo.addBorder
import org.simbrain.util.table.NumericTable
import org.simbrain.util.table.SimbrainJTable
import org.simbrain.util.table.SimbrainJTableScrollPanel
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
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

    protected val biasImage = PImage()

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

    private var showBias = false
        set(value) {
            if (value != field) {
                if (value) {
                    mainNode.addChild(biasImage)
                } else {
                    mainNode.removeChild(biasImage)
                }
            }
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

        // TODO: fixed events type on override
        val events = neuronArray.events as NeuronArrayEvents2

        events.visualPropertiesChanged.on {
            gridMode = neuronArray.isGridMode
            showBias = neuronArray.isShowBias
        }
        gridMode = neuronArray.isGridMode
        showBias = neuronArray.isShowBias

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

        // call once to make sure all the actions are registered
        contextMenu

    }

    private fun updateActivationImage() {
        activationImage.removeAllChildren()
        spikeImage.removeAllChildren()
        biasImage.removeAllChildren()
        val activations = neuronArray.outputs.toDoubleArray()
        if (gridMode) {
            // "Grid" case
            val len = sqrt(activations.size.toDouble()).toInt()
            val img = activations.toSimbrainColorImage(len, len)
            activationImage.image = img
            activationImage.setBounds(
                0.0, 0.0,
                infoText.width, infoText.width
            )
            activationImage.addBorder()
            if (neuronArray.updateRule.isSpikingRule) {
                val spikes = (neuronArray.dataHolder as SpikingMatrixData).spikes
                spikeImage.image = spikes.toOverlay(len, len, NeuronNode.spikingColor)
                spikeImage.setBounds(
                    0.0, 0.0,
                    infoText.width, infoText.width
                )
                spikeImage.addBorder()
            }
            if (showBias && neuronArray.dataHolder is BiasedMatrixData) {
                val biases = (neuronArray.dataHolder as BiasedMatrixData).biases
                biasImage.image = biases.toDoubleArray().toSimbrainColorImage(len, len)
                biasImage.setBounds(
                    0.0, infoText.width + infoText.height + margin,
                    infoText.width, infoText.width
                )
                biasImage.addBorder()
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
                spikeImage.addBorder()
            }
            if (showBias && neuronArray.dataHolder is BiasedMatrixData) {
                val biases = (neuronArray.dataHolder as BiasedMatrixData).biases
                biasImage.image = biases.toDoubleArray().toSimbrainColorImage(activations.size, 1)
                biasImage.setBounds(
                    0.0, flatPixelArrayHeight.toDouble() + infoText.height + margin,
                    infoText.width, flatPixelArrayHeight.toDouble()
                )
                biasImage.addBorder()
            }
        }
        updateTextLabel()
    }

    private fun computeInfoText() = """
            ${neuronArray.id}    Nodes: ${neuronArray.size()} ${if (neuronArray.targetValues != null) "T" else ""}
            Mean activation: ${neuronArray.activations.toDoubleArray().average().format(4)}
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
        contextMenu.add(networkPanel.networkActions.deleteAction)
        contextMenu.addSeparator()
        contextMenu.add(networkPanel.networkActions.connectSelectedModels)
        contextMenu.addSeparator()

        // Choose style
        val switchStyle: Action = networkPanel.createAction(
            name = "Toggle line / grid",
            iconPath = "menu_icons/grid.png",
            description = "Toggle line / grid style"
        ) {
            networkPanel.selectionManager
                .filterSelectedModels<NeuronArray>()
                .forEach { it.isGridMode = !it.isGridMode }
        }
        contextMenu.add(switchStyle)
        val toggleShowBias: Action = networkPanel.createAction(
            name = "Toggle bias visibility",
            description = "Toggle whether biases are visible"
        ) {
            networkPanel.selectionManager
                .filterSelectedModels<NeuronArray>()
                .forEach { it.isShowBias = !it.isShowBias }
        }
        contextMenu.add(toggleShowBias)
        contextMenu.addSeparator()

        val setTargetValues: Action = networkPanel.createAction(
            name = "Set Target",
            description = "Use current activation as target for immediate training",
            keyboardShortcut = CmdOrCtrl + 'T'
        ) {
            neuronArray.targetValues = neuronArray.activations.clone()
        }
        contextMenu.add(setTargetValues)

        val clearTargetValues: Action = networkPanel.createAction(
            name = "Clear Target",
            description = "Clear target values",
            keyboardShortcut = Shift + CmdOrCtrl + 'T'
        ) {
            neuronArray.targetValues = null
        }
        contextMenu.add(clearTargetValues)

        val applyLearning: Action = networkPanel.createAction(
            name = "Apply Learning",
            description = "Train source to target weights using backprop",
            keyboardShortcut = 'L',
            initBlock = {
                fun updateAction() {
                    isEnabled = networkPanel.selectionManager.selectedModels.contains(neuronArray)
                            && neuronArray.targetValues != null
                }
                updateAction()
                networkPanel.selectionManager.events.selection.on { _, _ -> updateAction() }
                networkPanel.selectionManager.events.sourceSelection.on { _, _ -> updateAction() }
            }
        ) {
            networkPanel.applyImmediateLearning()
        }
        contextMenu.add(applyLearning)

        contextMenu.addSeparator()

        // Randomize Action
        val randomizeAction = networkPanel.networkActions.randomizeObjectsAction

        contextMenu.add(randomizeAction)
        if (neuronArray.dataHolder is BiasedMatrixData) {
            val randomizeBiasesAction = networkPanel.createAction(
                name = "Randomize Biases",
                description = "Randomize the biases of this neuron array",
                iconPath = "menu_icons/Rand.png"
            ) {
                networkPanel.selectionManager
                    .filterSelectedModels<NeuronArray>()
                    .forEach { it.randomizeBiases() }
            }
            contextMenu.add(randomizeBiasesAction)
        }
        contextMenu.addSeparator()
        val editComponents: Action = object : AbstractAction("Edit Components...") {
            override fun actionPerformed(event: ActionEvent) {
                val dialog = StandardDialog()
                val arrayData = NumericTable(neuronArray.outputs.toDoubleArray())
                dialog.contentPane = SimbrainJTableScrollPanel(
                    SimbrainJTable.createTable(arrayData)
                )
                dialog.addClosingTask {
                    neuronArray.addInputs(Matrix.column(arrayData.vectorCurrentRow))
                    neuronArray.update()
                }
                dialog.pack()
                dialog.setLocationRelativeTo(null)
                dialog.isVisible = true
            }
        }
        contextMenu.add(editComponents)

        val showActivationHistogram = networkPanel.createAction(
            name = "Show Activation Histogram",
            description = "Show a histogram of the activations of this neuron array",
            iconPath = "menu_icons/BarChart.png"
        ) {
            val updater = neuronArray.activations.showHistogram(title = "Activation Histogram", label = "Activations")
            neuronArray.events.updated.on {
                updater(neuronArray.activations)
            }
        }
        contextMenu.add(showActivationHistogram)

        if (neuronArray.dataHolder is BiasedMatrixData) {
            val showBiasesHistogram = networkPanel.createAction(
                name = "Show Biases Histogram",
                description = "Show a histogram of the biases of this neuron array",
                iconPath = "menu_icons/BarChart.png"
            ) {
                neuronArray.dataHolder.let {
                    if (it is BiasedMatrixData) {
                        val updater = it.biases.showHistogram(title = "Bias Histogram", label = "Biases")
                        neuronArray.events.updated.on {
                            updater(it.biases)
                        }
                    }
                }
            }
            contextMenu.add(showBiasesHistogram)
        }

        // Projection Plot Action
        contextMenu.addSeparator()
        contextMenu.add(actionManager.createCoupledProjectionPlotAction(neuronArray, "Activation Projection Plot"))

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