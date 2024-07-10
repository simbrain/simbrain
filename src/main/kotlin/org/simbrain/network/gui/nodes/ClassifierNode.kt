package org.simbrain.network.gui.nodes

import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PText
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getTrainingDialog
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.util.*
import org.simbrain.util.piccolo.*
import org.simbrain.workspace.couplings.getProducer
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
            image = smileClassifier.inputActivations.toDoubleArray().toSimbrainColorImage(smileClassifier.inputSize(), 1)
            val (x, y, w, h) = bounds
            setBounds(x, y, 100.0, 20.0)
        }
        outputImage.apply {
            image = smileClassifier.outputs.toDoubleArray().toSimbrainColorImage(smileClassifier.outputSize(), 1)
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

    override val model: NetworkModel
        get() = smileClassifier

    override val toolTipText: String
        get() = """ 
            <html>
            Output: (${Utils.doubleArrayToString(smileClassifier.outputs.toDoubleArray(), 2)})<br>
            Input: (${Utils.doubleArrayToString(smileClassifier.inputs.toDoubleArray(), 2)})
            </html>
            """.trimIndent()

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            add(JMenuItem("Set Properties / Train ...").apply {
                addActionListener {
                    propertyDialog?.display()
                }
            })
            addSeparator()
            add(
                SimbrainDesktop.actionManager.createCoupledProjectionPlotAction(
                    smileClassifier.getProducer(SmileClassifier::getInputActivationArray),
                    objectName = "${smileClassifier.id ?: "Smile Classifier"} Inputs"
                )
            )
            add(
                SimbrainDesktop.actionManager.createCoupledProjectionPlotAction(
                    smileClassifier.getProducer(SmileClassifier::outputActivations),
                    objectName = "${smileClassifier.id ?: "Smile Classifier"} Outputs"
                )
            )
        }

    override val propertyDialog: StandardDialog
        get() = smileClassifier.getTrainingDialog()

}

