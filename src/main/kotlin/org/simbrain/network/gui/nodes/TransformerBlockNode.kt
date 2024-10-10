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

    /**
     * Main pixel image for activations.
     */
    protected val selfAttentionImage = PImage().apply {
        mainNode.addChild(this)
    }

    val feedForwardInputImage = PImage().apply {
        mainNode.addChild(this)
    }

    val feedForwardHiddenImage = PImage().apply {
        mainNode.addChild(this)
    }

    val feedForwardOutputImage = PImage().apply {
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
        events.labelChanged.on(Dispatchers.Swing) { o, n -> updateTextLabel() }
        updateTextLabel()

        events.updated.on {
            events.updateGraphics.fire()
        }

        events.updateGraphics.on(Dispatchers.Swing) {
            updateImages()
            updateInfoText()
        }

        updateImages()
        this.selfAttentionImage.offset(0.0, infoText.offset.y + infoText.height + 5)
        this.selfAttentionImage.addBorder()
        updateBorder()

        // call once to make sure all the actions are registered
        contextMenu

    }

    private fun updateFeedForwardImage(image: PImage, matrix: Matrix, location: Point2D) {
        image.removeAllChildren()
        val img = matrix.flatten().toSimbrainColorImage(matrix.ncol(), matrix.nrow())
        image.image = img
        image.setBounds(
            location.x, location.y,
            60.0, 60.0
        )
        image.addBorder()
        updateTextLabel()
    }

    private fun updateImages() {
        this.selfAttentionImage.removeAllChildren()
        val matrix = transformerBlock.selfAttention

        val img = matrix.flatten().toSimbrainColorImage(matrix.ncol(), matrix.nrow())
        this.selfAttentionImage.image = img
        this.selfAttentionImage.setBounds(
            0.0, 0.0,
            200.0, 200.0
        )
        this.selfAttentionImage.addBorder()

        updateFeedForwardImage(feedForwardInputImage, transformerBlock.feedForwardInput, point(210.0, infoText.y + infoText.height + 5.0))
        updateFeedForwardImage(feedForwardHiddenImage, transformerBlock.feedForwardHidden, point(210.0, feedForwardInputImage.y + 70.0))
        updateFeedForwardImage(feedForwardOutputImage, transformerBlock.activations, point(210.0, feedForwardHiddenImage.y + 70.0))

        updateTextLabel()
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
    fun updateTextLabel() {
        if (!transformerBlock.label.isNullOrEmpty()) {
            labelText.font = NeuronNode.NEURON_FONT
            labelText.text = "" + transformerBlock.label
            labelText.setOffset(
                this.selfAttentionImage.x - labelText.width / 2 + this.selfAttentionImage.width / 2,
                this.selfAttentionImage.y - labelText.height - 17
            )
            labelBackground.setBounds(labelText.fullBounds)
        }
    }
}