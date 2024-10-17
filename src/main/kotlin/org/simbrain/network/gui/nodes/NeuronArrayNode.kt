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
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.randomizeBiases
import org.simbrain.network.events.NeuronArrayEvents
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.alignMenu
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.network.gui.nodes.NeuronNode.Companion.DIAMETER
import org.simbrain.network.gui.nodes.NeuronNode.Companion.TEXT_VISIBILITY_THRESHOLD
import org.simbrain.network.gui.spaceMenu
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.*
import org.simbrain.util.piccolo.addBorder
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import org.simbrain.workspace.couplings.getConsumer
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.BasicStroke
import java.awt.Color
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.*
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The current pnode representation for all [Layer] objects. May be broken out into subtypes for different
 * subclasses of Layer.
 */
class NeuronArrayNode(networkPanel: NetworkPanel, val neuronArray: NeuronArray) :
    ArrayLayerNode(networkPanel, neuronArray) {

    val imageNodeGroup = PNode().apply {
        if (!neuronArray.circleMode) {
            mainNode.addChild(this)
        }
    }

    val neuronCircleGroup = PNode().apply {
        if (neuronArray.circleMode) {
            mainNode.addChild(this)
        }
    }

    val neuronCircles by lazy {
        neuronArray.activationArray.map { activation ->
            NeuronCircleNode().also { circle ->
                circle.activation = activation
            }
        }.onEach {
            neuronCircleGroup.addChild(it)
        }
    }

    private fun layoutNeuronCircles() {
        if (!neuronArray.circleMode) return
        val ncol = if (neuronArray.gridMode) {
            ceil(sqrt(neuronArray.size.toDouble())).toInt()
        } else {
            neuronArray.size
        }
        val offset = 50.0
        neuronCircles.forEachIndexed { i, circle ->
            val row = i / ncol
            val col = i % ncol
            circle.setOffset(
                col * offset,
                row * offset
            )
        }
    }

    /**
     * Main pixel image for activations.
     */
    protected val activationImage = PImage().apply {
        imageNodeGroup.addChild(this)
    }

    /**
     * Image with spikes and transparent background overlaid on the activation image for spiking neuron arrays.
     */
    private val spikeImage = PImage().apply {
        imageNodeGroup.addChild(this)
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
            layoutNeuronCircles()
        }

    private var showBias = false
        set(value) {
            if (value != field) {
                if (value) {
                    imageNodeGroup.addChild(biasImage)
                } else {
                    imageNodeGroup.removeChild(biasImage)
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
        val events = neuronArray.events as NeuronArrayEvents

        events.visualPropertiesChanged.on(Dispatchers.Swing) {
            gridMode = neuronArray.gridMode
            showBias = neuronArray.isShowBias
            networkPanel.network.events.zoomToFitPage.fire()
        }
        gridMode = neuronArray.gridMode
        showBias = neuronArray.isShowBias

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
        events.updateRuleChanged.on(Dispatchers.Swing) {
            if (!neuronArray.updateRule.isSpikingRule) {
                mainNode.removeChild(spikeImage)
            }
        }
        events.clampChanged.on(Dispatchers.Swing) {
            updateActivationImage()
        }
        updateActivationImage()
        neuronCircleGroup.setOffset(DIAMETER / 2.0, DIAMETER / 2.0 + 20.0 + infoText.height)
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
        val activations = neuronArray.activations.toDoubleArray()

        fun renderGridImages() {
            val len = ceil(sqrt(activations.size.toDouble())).toInt()
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
            if (showBias) {
                biasImage.image = neuronArray.biases.toDoubleArray().toSimbrainColorImage(len, len)
                biasImage.setBounds(
                    0.0, infoText.width + infoText.height + margin,
                    infoText.width, infoText.width
                )
                biasImage.addBorder()
            }
        }

        fun renderFlatImages() {
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
            if (showBias) {
                biasImage.image = neuronArray.biases.toDoubleArray().toSimbrainColorImage(activations.size, 1)
                biasImage.setBounds(
                    0.0, flatPixelArrayHeight.toDouble() + infoText.height + margin,
                    infoText.width, flatPixelArrayHeight.toDouble()
                )
                biasImage.addBorder()
            }
        }

        fun renderNeuronCircles() {
            neuronCircles.forEachIndexed { i, circle ->
                circle.activation = activations[i]
                circle.setClamped(neuronArray.isClamped)
                circle.setLabel(neuronArray.labelArray[i])
            }
        }

        if (neuronArray.circleMode) {
            if (mainNode.indexOfChild(neuronCircleGroup) == -1) {
                mainNode.addChild(neuronCircleGroup)
            }
            mainNode.removeChild(imageNodeGroup)
            renderNeuronCircles()
        } else {
            if (mainNode.indexOfChild(imageNodeGroup) == -1) {
                mainNode.addChild(imageNodeGroup)
            }
            mainNode.removeChild(neuronCircleGroup)
            if (gridMode) {
                renderGridImages()
            } else {
                renderFlatImages()
            }
        }
        updateTextLabel()
    }

    private fun computeInfoText() = """
            ${neuronArray.id}    Nodes: ${neuronArray.size} ${if (neuronArray.targetValues != null) "T" else ""}
            Mean activation: ${neuronArray.activations.toDoubleArray().average().format(4)}
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
        ${neuronArray.toString().split("\n").joinToString("<br>")}
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

            // Choose style
            val switchStyle: Action = networkPanel.createAction(
                name = "Toggle line / grid",
                iconPath = "menu_icons/grid.png",
                keyboardShortcut = 'G',
                description = "Toggle line / grid style"
            ) {
                networkPanel.selectionManager
                    .filterSelectedModels<NeuronArray>()
                    .forEach { it.gridMode = !it.gridMode }
            }
            contextMenu.add(switchStyle)

            val switchOrientation: Action = networkPanel.createAction(
                name = "Switch to ${if (neuronArray.verticalLayout) "horizontal" else "vertical"} layout",
                description = "Toggle horizontal / vertical layout"
            ) {
                networkPanel.selectionManager
                    .filterSelectedModels<NeuronArray>()
                    .forEach { it.verticalLayout = !it.verticalLayout }
            }
            contextMenu.add(switchOrientation)

            val toggleCircleMode: Action = networkPanel.createAction(
                name = "Toggle circle mode",
                description = "Toggle activation rendering mode between circle and image",
            ) {
                networkPanel.selectionManager
                    .filterSelectedModels<NeuronArray>()
                    .forEach { it.circleMode = !it.circleMode }
            }
            contextMenu.add(toggleCircleMode)

            val toggleShowBias: Action = networkPanel.createAction(
                name = "Toggle bias visibility",
                keyboardShortcut = 'B',
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

            val applyInputs: Action = networkPanel.networkActions.createTestInputPanelAction(neuronArray)
            contextMenu.add(applyInputs)
            val addActivationToInput = networkPanel.networkActions.createAddActivationToInputAction(neuronArray)
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
                    val arrayData = MatrixDataFrame(neuronArray.activations)
                    dialog.contentPane = SimbrainTablePanel(arrayData)
                    dialog.addCommitTask {
                        with(networkPanel.network) {
                            neuronArray.update()
                        }
                    }
                    dialog.pack()
                    dialog.setLocationRelativeTo(null)
                    dialog.isVisible = true
                }
            }
            contextMenu.add(editComponents)

            // Projection Plot Action
            contextMenu.addSeparator()
            contextMenu.add(
                actionManager.createCoupledPlotMenu(
                    neuronArray.getProducer(NeuronArray::activationArray),
                    objectName = "${neuronArray.id ?: "Neuron Array"} Activations",
                    menuTitle = "Plot Activation"
                )
            )
            contextMenu.add(
                actionManager.createCoupledPlotMenu(
                    neuronArray.getProducer(NeuronArray::biasArray),
                    objectName = "${neuronArray.id ?: "Neuron Array"} Biases",
                    menuTitle = "Plot Biases"
                )
            )
            contextMenu.add(actionManager.createImageInput(
                neuronArray.getConsumer(NeuronArray::addInputsMismatched),
                neuronArray.size,
                menuTitle = "Add coupled image world",
                postActionBlock = { neuronArray.gridMode = true }
            ))
            contextMenu.add(
                actionManager.createCoupledDataWorldAction(
                    name = "Record Activations",
                    neuronArray.getProducer(NeuronArray::activationArray),
                    sourceName = "${neuronArray.id ?: "Neuron Array"} Activations",
                    neuronArray.size
                )
            )
            contextMenu.add(
                actionManager.createCoupledDataWorldAction(
                    name = "Record Biases",
                    neuronArray.getProducer(NeuronArray::biasArray),
                    sourceName = "${neuronArray.id ?: "Neuron Array"} Biases",
                    neuronArray.size
                )
            )

            contextMenu.addSeparator()
            contextMenu.add(networkPanel.alignMenu)
            contextMenu.add(networkPanel.spaceMenu)

            // Coupling menu
            contextMenu.addSeparator()
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(neuronArray)
            contextMenu.add(couplingMenu)
            return contextMenu
        }

    override val propertyDialog: StandardDialog
        get() = neuronArray.createEditorDialog { updateInfoText() }

    override val model: NeuronArray
        get() = neuronArray

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

    inner class NeuronCircleNode: PPath.Double() {
        val circle = createEllipse(
            (0 - DIAMETER / 2).toFloat(),
            (0 - DIAMETER / 2).toFloat(),
            DIAMETER.toFloat(),
            DIAMETER.toFloat()
        ).also { addChild(it) }

        val activationText = PText().also {
            it.font = NeuronNode.NEURON_FONT_BOLD
            addChild(it)
        }

        val labelText = PText().also {
            it.font = NeuronNode.NEURON_FONT
            addChild(it)
        }

        private var isTextVisible = networkPanel.scalingFactor > TEXT_VISIBILITY_THRESHOLD
            set(value) {
                if (field != value) {
                    field = value
                    if (value) {
                        drawActivation()
                        addChild(activationText)
                        addChild(labelText)
                    } else {
                        removeChild(activationText)
                        removeChild(labelText)
                    }
                }
            }

        var activation: kotlin.Double = 0.0
            set(value) {
                field = value
                drawActivation()
            }

        private fun drawActivation() {
            circle.paint = activation.toSimbrainColor(neuronArray.updateRule.let {
                it.graphicalLowerBound..it.graphicalUpperBound
            }).toColor()

            if (isTextVisible) {
                activationText.text = if (activation > -0.95 && activation < 0.95) {
                    activation.format(1).replace("0.", ".").replace(Regex("^-?.0$"), "0")
                } else {
                    activation.format(0)
                }

                val targetWidth = min(DIAMETER.toDouble() * 0.8, activationText.width)
                activationText.scale = targetWidth / activationText.width
                activationText.centerBoundsOnPoint(circle.bounds.x + circle.bounds.width / 2, circle.bounds.y + circle.bounds.height / 2)
            }
        }

        fun setLabel(label: String) {
            labelText.text = label
            labelText.centerBoundsOnPoint(circle.bounds.x + circle.bounds.width / 2, circle.bounds.y - 8.0)
        }

        fun setClamped(clamped: Boolean) {
            if (clamped) {
                circle.stroke = BasicStroke(2.0f)
            } else {
                circle.stroke = BasicStroke(1.0f)
            }
        }

        override fun paint(paintContext: PPaintContext?) {
            isTextVisible = networkPanel.scalingFactor > TEXT_VISIBILITY_THRESHOLD
            super.paint(paintContext)
        }

    }
}