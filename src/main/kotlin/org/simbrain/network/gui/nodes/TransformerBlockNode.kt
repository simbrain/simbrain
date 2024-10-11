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
import org.simbrain.network.core.TransformerBlock
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.alignMenu
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.network.gui.spaceMenu
import org.simbrain.util.*
import org.simbrain.util.piccolo.addBorder
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import smile.math.matrix.Matrix
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.geom.Point2D
import java.util.*
import javax.swing.*


class TransformerBlockNode(networkPanel: NetworkPanel, val transformerBlock: TransformerBlock) :
    ArrayLayerNode(networkPanel, transformerBlock) {

    val selfAttentionImage = PImage().apply {
        mainNode.addChild(this)
    }

    val selfAttentionLabel = mainNode.addLabel("Self Attention")

    val sequenceGroup = PNode().apply {
        mainNode.addChild(this)
    }

    val qSequenceImage = PImage().apply {
        sequenceGroup.addChild(this)
    }

    val qSequenceLabel = sequenceGroup.addLabel("q sequence")

    val kSequenceImage = PImage().apply {
        sequenceGroup.addChild(this)
    }

    val kSequenceLabel = sequenceGroup.addLabel("k sequence")

    val vSequenceImage = PImage().apply {
        sequenceGroup.addChild(this)
    }

    val vSequenceLabel = sequenceGroup.addLabel("v sequence")

    val matrixGroup = PNode().apply {
        mainNode.addChild(this)
    }

    val qMatrixImage = PImage().apply {
        matrixGroup.addChild(this)
    }

    val qMatrixLabel = matrixGroup.addLabel("Q matrix")

    val kMatrixImage = PImage().apply {
        matrixGroup.addChild(this)
    }

    val kMatrixLabel = matrixGroup.addLabel("K matrix")

    val vMatrixImage = PImage().apply {
        matrixGroup.addChild(this)
    }

    val vMatrixLabel = matrixGroup.addLabel("V matrix")

    val feedForwardGroup = PNode().apply {
        mainNode.addChild(this)
    }

    val feedForwardInputImage = PImage().apply {
        feedForwardGroup.addChild(this)
    }

    val feedForwardInputLabel = feedForwardGroup.addLabel("FF Input")

    val feedForwardHiddenImage = PImage().apply {
        feedForwardGroup.addChild(this)
    }

    val feedForwardHiddenLabel = feedForwardGroup.addLabel("FF Hidden")

    val feedForwardOutputImage = PImage().apply {
        feedForwardGroup.addChild(this)
    }

    val feedForwardOutputLabel = feedForwardGroup.addLabel("FF Output")

    val feedForwardW1Image = PImage().apply {
        feedForwardGroup.addChild(this)
    }

    val feedForwardW1Label = feedForwardGroup.addLabel("Input -> Hidden")

    val feedForwardW2Image = PImage().apply {
        feedForwardGroup.addChild(this)
    }

    val feedForwardW2Label = feedForwardGroup.addLabel("Hidden -> Output")

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

        val events = transformerBlock.events

        // TODO: Link to network preferences
        labelBackground.paint = Color.white
        labelBackground.setBounds(labelText.bounds)
        labelBackground.addChild(labelText)
        addChild(labelBackground)
        events.labelChanged.on(Dispatchers.Swing) { o, n -> updateTextLabels() }
        updateTextLabels()

        events.updated.on {
            events.updateGraphics.fire()
        }

        events.updateGraphics.on(Dispatchers.Swing) {
            updateImages()
            updateInfoText()
        }

        updateImages()
        updateBorder()

        // call once to make sure all the actions are registered
        contextMenu

    }

    private fun updateImage(image: PImage, matrix: Matrix, location: Point2D, width: kotlin.Double, height: kotlin.Double, strokeWidth: kotlin.Float = 1f) {
        image.removeAllChildren()
        val img = matrix.flatten().toSimbrainColorImage(matrix.ncol(), matrix.nrow())
        image.image = img
        image.setBounds(
            location.x, location.y,
            width, height
        )
        image.addBorder(strokeWidth = strokeWidth)
        updateTextLabels()
    }

    private fun updateImages() {
        updateImage(selfAttentionImage, transformerBlock.selfAttention, point(0.0, infoText.y + infoText.height + 5.0), 200.0, 200.0)

        matrixGroup.setOffset(
            - 120.0 - 10.0 - 10.0,
            selfAttentionImage.y
        )
        updateImage(qMatrixImage, transformerBlock.Q, point(0.0, 0.0), 60.0, 60.0, strokeWidth = 2f)
        updateImage(kMatrixImage, transformerBlock.K, point(0.0, qMatrixImage.y + 70.0), 60.0, 60.0, strokeWidth = 2f)
        updateImage(vMatrixImage, transformerBlock.V, point(0.0, kMatrixImage.y + 70.0), 60.0, 60.0, strokeWidth = 2f)

        sequenceGroup.setOffset(
            - 60.0 - 10.0,
            selfAttentionImage.y
        )
        updateImage(qSequenceImage, transformerBlock.qStack, point(0.0, 0.0), 60.0, 60.0)
        updateImage(kSequenceImage, transformerBlock.kStack, point(0.0, qSequenceImage.y + 70.0), 60.0, 60.0)
        updateImage(vSequenceImage, transformerBlock.vStack, point(0.0, kSequenceImage.y + 70.0), 60.0, 60.0)

        feedForwardGroup.setOffset(
            selfAttentionImage.x + selfAttentionImage.width + 10.0,
            selfAttentionImage.y
        )
        updateImage(feedForwardW1Image, transformerBlock.W1, point(0.0, 30.0), 60.0, 60.0, strokeWidth = 2f)
        updateImage(feedForwardW2Image, transformerBlock.W2, point(0.0, feedForwardW1Image.y + 70.0), 60.0, 60.0, strokeWidth = 2f)
        updateImage(feedForwardOutputImage, transformerBlock.activations, point(70.0, 0.0), 60.0, 60.0)
        updateImage(feedForwardHiddenImage, transformerBlock.feedForwardHidden, point(70.0, feedForwardOutputImage.y + 70.0), 60.0, 60.0)
        updateImage(feedForwardInputImage, transformerBlock.feedForwardInput,point(70.0, feedForwardHiddenImage.y + 70.0), 60.0, 60.0)

        updateTextLabels()
    }

    private fun computeInfoText() = """
            ${transformerBlock.displayName}
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
        ${transformerBlock.toString().split("\n").joinToString("<br>")}
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

            val applyInputs: Action = networkPanel.networkActions.createTestInputPanelAction(transformerBlock)
            contextMenu.add(applyInputs)
            val addActivationToInput = networkPanel.networkActions.createAddActivationToInputAction(transformerBlock)
            contextMenu.add(addActivationToInput)
            contextMenu.addSeparator()

            // Randomize Action
            val randomizeAction = networkPanel.networkActions.randomizeObjectsAction

            contextMenu.add(randomizeAction)

            contextMenu.addSeparator()
            val editComponents: Action = object : AbstractAction("Edit Components...") {
                override fun actionPerformed(event: ActionEvent) {
                    val dialog = StandardDialog()
                    val arrayData = MatrixDataFrame(transformerBlock.selfAttention)
                    dialog.contentPane = JTabbedPane().apply {
                        add("Self Attention", SimbrainTablePanel(arrayData))
                        add("K", SimbrainTablePanel(MatrixDataFrame(transformerBlock.K)))
                        add("Q", SimbrainTablePanel(MatrixDataFrame(transformerBlock.Q)))
                        add("V", SimbrainTablePanel(MatrixDataFrame(transformerBlock.V)))
                        add("k", SimbrainTablePanel(MatrixDataFrame(transformerBlock.kStack)))
                        add("q", SimbrainTablePanel(MatrixDataFrame(transformerBlock.qStack)))
                        add("v", SimbrainTablePanel(MatrixDataFrame(transformerBlock.vStack)))
                    }
                    dialog.addCommitTask {
                        with(networkPanel.network) {
                            transformerBlock.update()
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
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(transformerBlock)
            contextMenu.add(couplingMenu)
            return contextMenu
        }

    override val propertyDialog: StandardDialog
        get() = transformerBlock.createEditorDialog { updateInfoText() }

    override val model: TransformerBlock
        get() = transformerBlock

    /**
     * Update the text label.
     */
    fun updateTextLabels() {
        if (!transformerBlock.label.isNullOrEmpty()) {
            labelText.font = NeuronNode.NEURON_FONT
            labelText.text = "" + transformerBlock.label
            labelText.setOffset(
                this.selfAttentionImage.x - labelText.width / 2 + this.selfAttentionImage.width / 2,
                this.selfAttentionImage.y - labelText.height - 17
            )
            labelBackground.setBounds(labelText.fullBounds)
        }

        fun PText.centerBelow(image: PImage, padding: kotlin.Double = 0.0) {
            setBounds(
                image.x + image.width / 2 - width / 2,
                image.y + image.height + padding,
                width,
                height
            )
        }

        qSequenceLabel.centerBelow(qSequenceImage)
        kSequenceLabel.centerBelow(kSequenceImage)
        vSequenceLabel.centerBelow(vSequenceImage)

        qMatrixLabel.centerBelow(qMatrixImage)
        kMatrixLabel.centerBelow(kMatrixImage)
        vMatrixLabel.centerBelow(vMatrixImage)

        selfAttentionLabel.centerBelow(selfAttentionImage)

        feedForwardInputLabel.centerBelow(feedForwardInputImage)
        feedForwardHiddenLabel.centerBelow(feedForwardHiddenImage)
        feedForwardOutputLabel.centerBelow(feedForwardOutputImage)
        feedForwardW1Label.centerBelow(feedForwardW1Image)
        feedForwardW2Label.centerBelow(feedForwardW2Image)
    }

    fun PNode.addLabel(text: String): PText {
        val label = PText().apply {
            this.text = text
            font = INFO_FONT
        }
        addChild(label)
        return label
    }
}