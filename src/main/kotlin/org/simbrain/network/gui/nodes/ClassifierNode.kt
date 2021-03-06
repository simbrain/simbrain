package org.simbrain.network.gui.nodes

import org.simbrain.network.NetworkComponent
import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.DataPanel
import org.simbrain.network.matrix.Classifier
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
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

        // fun consumeClassifier(classifier: Classifier<DoubleArray>) {
        //     this@SmileClassifierNode.classifier.classifier = classifier
        // }

        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

            add(JPanel().apply{
                layout = FlowLayout(FlowLayout.LEFT)
                add(AnnotatedPropertyEditor(classifier))
            })

            add(JPanel().apply{
                layout = FlowLayout(FlowLayout.LEFT)
                add(JButton("Train").apply {
                    addActionListener {
                        val kernel = PolynomialKernel(2)
                        // SVM.fit(input, target, kernel, 1000.0, 1E-3)
                    }

                })
            })
            add(JPanel().also { dataPanel ->
                JPanel().apply {
                    contentPane = this
                    layout = BoxLayout(this, BoxLayout.LINE_AXIS)
                }

                val inputPanel = DataPanel().apply {
                    table.setData(classifier.trainingInputs)
                    events.onApply { data -> println(data.contentDeepToString()) }
                    addClosingTask { applyData() }
                    dataPanel.add(this)
                }

                val targetPanel = DataPanel().apply {
                    table.setData(classifier.targets.map { doubleArrayOf(it.toDouble()) }.toTypedArray())
                    addClosingTask { applyData() }
                    dataPanel.add(this)
                }

                var input: Array<DoubleArray>? = null
                var target: IntArray? = null

                fun invokeCallback(input: Array<DoubleArray>?, target: IntArray?) {
                    if (input != null && target != null) {
                        val kernel = PolynomialKernel(2)
                        // consumeClassifier(SVM.fit(input, target, kernel, 1000.0, 1E-3))
                    }
                }

                inputPanel.events.onApply { data ->
                    input = data
                    invokeCallback(input, target)
                    classifier.trainingInputs = data
                }

                targetPanel.events.onApply { data ->
                    target = data.map { it[0].toInt() }.toIntArray()
                    invokeCallback(input, target)
                    classifier.targets = target
                }
            }.also { pack() })
        }.also {
            contentPane = it
        }

    }

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