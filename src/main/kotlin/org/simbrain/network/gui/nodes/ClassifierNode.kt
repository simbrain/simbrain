package org.simbrain.network.gui.nodes

import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PBounds
import org.simbrain.network.NetworkComponent
import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.DataPanel
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.table.NumericTable
import java.awt.Dialog.ModalityType
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class SmileClassifierNode(val np : NetworkPanel, val classifier : SmileClassifier) : ScreenElement(np) {

    /**
     * Square shape around array node.
     */
    private val borderBox = PPath.createRectangle(0.0, 0.0, 200.0, 100.0).also {
        addChild(it)
        pickable = true
    }

    /**
     * Text showing info about the array.
     */
    private val infoText = PText().also {
        it.setFont(NeuronArrayNode.INFO_FONT)
        addChild(it)
        it.offset(8.0, 8.0)
        // updateInfoText()
    }

    init {
        setBounds(borderBox.bounds)

        classifier.events.apply {
            onDeleted{ removeFromParent() }
            onUpdated{
                // renderArrayToActivationsImage()
                updateInfoText()
            }
            onClampChanged{}
            onLocationChange{}
        }
    }

    /**
     * Image to show activationImage.
     */
    private val activationImage = PImage()

    /**
     * Update status text.
     */
    private fun updateInfoText() {
        infoText.setText(classifier.toString())
        val pb: PBounds = infoText.getBounds()
        borderBox.setBounds(pb.x - 2, pb.y - 2, pb.width + 20, pb.height + 20)
        setBounds(borderBox.getBounds())
    }

    override fun getModel(): NetworkModel {
        return classifier
    }

    override fun isSelectable(): Boolean {
        return true
    }

    override fun isDraggable(): Boolean {
        return true
    }

    override fun getPropertyDialog() = StandardDialog().apply {

        title = "Smile Classifier"
        modalityType = ModalityType.MODELESS // Set to modeless so the dialog can be left open

        val mainPanel = JPanel()
        contentPane = mainPanel
        mainPanel.apply {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

            add(JPanel().apply{
                layout = FlowLayout(FlowLayout.LEFT)
                add(AnnotatedPropertyEditor(classifier))
            })

            // Data Panels
            val dataPanels = JPanel()
            val inputs = DataPanel().apply {
                table.setData(classifier.trainingInputs)
                events.onApply { data -> println(data.contentDeepToString()) }
                addClosingTask { applyData() }
                dataPanels.add(this)
            }
            val targets = DataPanel().apply {
                table.setData(classifier.targets.map { doubleArrayOf(it.toDouble()) }.toTypedArray())
                addClosingTask { applyData() }
                dataPanels.add(this)
            }

            // Training Button
            add(JPanel().apply{
                layout = FlowLayout(FlowLayout.LEFT)
                add(JButton("Train").apply {
                    addActionListener {
                        classifier.train(inputs.table.asDoubleArray(), targets.table.firstColumnAsIntArray())
                    }
                })
            })

            // Add the data panels
            add(dataPanels)
        }

    }

}

fun NumericTable.firstColumnAsIntArray(): IntArray {
    val returnList = IntArray(rowCount)
    for (i in 0 until rowCount) {
        returnList[i] = this.getLogicalValueAt(i, 0).toInt()
    }
    return returnList
}

fun main() {
    val networkComponent = NetworkComponent("net 1")
    val np = NetworkPanel(networkComponent)
    val classifier = with (networkComponent.network) {
        val classifier = SmileClassifier(this, SVMClassifier(), 2, 1)
        addNetworkModel(classifier)
        classifier
    }
    SmileClassifierNode(np, classifier).propertyDialog.run { makeVisible() }
}