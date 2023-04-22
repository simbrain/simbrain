package org.simbrain.network.gui.nodes

import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PText
import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getTrainingDialog
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.util.StandardDialog
import org.simbrain.util.Utils
import org.simbrain.util.piccolo.*
import org.simbrain.util.toSimbrainColorImage
import org.simbrain.workspace.gui.SimbrainDesktop
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class SmileClassifierNode(networkPanel: NetworkPanel, private val smileClassifier: SmileClassifier):
    ArrayLayerNode(networkPanel, smileClassifier) {

    private val infoText = PText().apply {
        font = INFO_FONT
        text = computeInfoText()
        mainNode.addChild(this)
    }

    private val outputImage = PImage().apply {
        mainNode.addChild(this)
    }

    private val inputImage = PImage().apply {
        mainNode.addChild(this)
    }

    init {
        val events = smileClassifier.events
        events.updated.on(wait = true) {
            updateActivationImages()
            updateInfoText()
            updateBorder()
        }

        // Set up components from top to bottom
        updateInfoText()
        updateActivationImages()

        val outLabel = PText("Out:")
        outLabel.font = INFO_FONT
        outLabel.offset(0.0,  infoText.offset.y + infoText.height + 7)
        addChild(outLabel)
        outputImage.offset(20.0,  infoText.offset.y + infoText.height + 5)
        outputImage.addBorder()

        val inLabel = PText("In:")
        inLabel.font = INFO_FONT
        inLabel.offset(0.0,  outputImage.offset.y + outputImage.height + 7)
        addChild(inLabel)
        inputImage.offset(20.0, outputImage.offset.y + outputImage.height + 5)
        inputImage.addBorder()
        updateBorder()
    }

    private fun updateActivationImages() {
        // TODO: Magic Numbers
        inputImage.apply {
            image =   smileClassifier.inputs.col(0).toSimbrainColorImage(smileClassifier.inputSize(), 1)
            val (x, y, w, h) = bounds
            setBounds(x, y, 100.0, 20.0)
        }
        outputImage.apply {
            image =   smileClassifier.outputs.col(0).toSimbrainColorImage(smileClassifier.outputSize(), 1)
            val (x, y, w, h) = bounds
            setBounds(x, y, 100.0, 20.0)
        }
    }

    private fun updateInfoText() {
        infoText.text = computeInfoText()
    }

    /**
     * Update status text.
     */
    private fun computeInfoText() = "Winning class: ${smileClassifier.winningLabel}"

    override fun getModel(): NetworkModel {
        return smileClassifier
    }

    override fun getToolTipText(): String {
        return  """ 
                <html>
                Output: (${Utils.doubleArrayToString(smileClassifier.outputs.col(0), 2)})<br>
                Input: (${Utils.doubleArrayToString(smileClassifier.inputs.col(0), 2)})
                </html>
                """.trimIndent()
    }

    override fun getContextMenu(): JPopupMenu? {
        return JPopupMenu().apply {
            add(JMenuItem("Set Properties / Train ...").apply { addActionListener {
                propertyDialog.run { makeVisible() }
            } })
            addSeparator()
            with(SimbrainDesktop.workspace.couplingManager) {
                add(SimbrainDesktop.actionManager.createCoupledProjectionPlotAction(
                        smileClassifier.getProducer("getInputActivations"),
                        name = "Input Projection"
                    )
                )
                add(SimbrainDesktop.actionManager.createCoupledProjectionPlotAction(
                        smileClassifier.getProducer("getOutputActivations"),
                        name = "Output Projection"
                    )
                )
            }
        }
    }

    override fun getPropertyDialog(): StandardDialog {
        return smileClassifier.getTrainingDialog()
    }

}

