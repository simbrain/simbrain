package org.simbrain.network.gui.dialogs

import net.miginfocom.swing.MigLayout
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.SmileClassifierNode
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.ResourceManager
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.showWarningDialog
import org.simbrain.util.table.*
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator

/**
 * Classifier training dialog.
 */
fun SmileClassifier.getTrainingDialog(): StandardDialog {
    return StandardDialog().apply {

        title = "Train ${classifier.name} classifier with ${classifier.outputSize} labels"
        contentPane = JPanel()
        // layout = MigLayout("fillx, debug")

        // Classifier properties
        val classfierProps = AnnotatedPropertyEditor(classifier)

        // Manage stats label
        val statsLabel = JLabel("---")
        layout = MigLayout("fillx")
        fun updateStatsLabel() {
            statsLabel.text = classifier.stats
        }
        updateStatsLabel()
        events.updated.on {
            updateStatsLabel()
        }

        // Data Panels
        val inputs = SimbrainDataViewer(
            createFromDoubleArray(classifier.trainingData.featureVectors), false
        ).apply {
            addAction(table.importCsv)
            addAction(table.randomizeAction)
            preferredSize = Dimension(300, 300)
            addClosingTask {
                classifier.trainingData.featureVectors = this.model.get2DDoubleArray()
            }
        }

        val targets = SimbrainDataViewer(createFromColumn(classifier.trainingData.targetLabels), false).apply {
            addAction(table.importCsv)
            addAction(table.randomizeColumnAction)
            preferredSize = Dimension(200, 300)
            addClosingTask {
                classifier.trainingData.targetLabels = this.model.getStringColumn(0)
            }
        }

        val addRemoveRows = JPanel().apply {
            // Add row
            add(JButton().apply {
                icon = ResourceManager.getImageIcon("menu_icons/AddTableRow.png")
                toolTipText = "Insert row at bottom of input and target tables"
                addActionListener {
                    inputs.table.insertRow()
                    targets.table.insertRow()
                }
            })
            add(JButton().apply {
                icon = ResourceManager.getImageIcon("menu_icons/DeleteRowTable.png")
                toolTipText = "Delete last row of input and target tables"
                addActionListener {
                    inputs.table.model.deleteRow(inputs.table.rowCount - 1)
                    targets.table.model.deleteRow(targets.table.rowCount - 1)
                }
            })
        }

        fun applyDataAndTrain() {
            try {
                classfierProps.commitChanges()
                classifier.trainingData.featureVectors = inputs.model.get2DDoubleArray()
                classifier.trainingData.targetLabels = targets.model.getStringColumn(0)
                train()
            } catch(e: Exception) {
                showWarningDialog(e.message.toString())
            }
        }

        // Training Button
        val trainButton = JButton("Train").apply {
            addActionListener {
                applyDataAndTrain()
            }
        }

        if (!classfierProps.widgets.isEmpty()) {
            add(classfierProps, "wrap")
            addClosingTask(classfierProps::commitChanges)
            add(JSeparator(), "growx, span, wrap")
        }
        contentPane.add(trainButton)
        contentPane.add(statsLabel, "wrap")
        contentPane.add(JSeparator(), "span, growx, wrap")
        contentPane.add(JLabel("Inputs"))
        contentPane.add(JLabel("Target Labels"))
        contentPane.add(JSeparator(), "span, growx, wrap")
        contentPane.add(inputs)
        contentPane.add(targets, "wrap")
        contentPane.add(JPanel().apply {
            add(JLabel("Add / Remove rows:"))
            add(addRemoveRows)
        })
    }
}

fun main() {
    val networkComponent = NetworkComponent("net 1")
    val np = NetworkPanel(networkComponent)
    val classifier = with(networkComponent.network) {
        val svm = SVMClassifier(2)
        val classifier = SmileClassifier(this, svm)
        svm.trainingData.featureVectors = arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(1.0, 1.0)
        )
        svm.trainingData.targetLabels = arrayOf("F", "T", "T", "F")
        addNetworkModel(classifier)
        classifier
    }
    SmileClassifierNode(np, classifier).propertyDialog.run { makeVisible() }
}