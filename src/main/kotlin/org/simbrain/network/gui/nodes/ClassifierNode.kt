package org.simbrain.network.gui.nodes

import org.simbrain.network.NetworkComponent
import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.DataPanel
import org.simbrain.network.matrix.Classifier
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.table.NumericTable
import smile.classification.SVM
import smile.math.kernel.PolynomialKernel
import java.awt.Dialog.ModalityType
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class SmileClassifierNode(val np : NetworkPanel, val classifier : Classifier) : ScreenElement(np) {

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
                        val kernel = PolynomialKernel(2)
                        val result = SVM.fit(inputs.table.asDoubleArray(), targets.table.firstColumnAsIntArray(), kernel, 1000.0,
                            1E-3)
                        println(result)
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
        val classifier = Classifier(this, 2)
        addNetworkModel(classifier)
        classifier
    }
    SmileClassifierNode(np, classifier).propertyDialog.run { makeVisible() }
}