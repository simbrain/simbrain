package org.simbrain.network.gui.dialogs

import net.miginfocom.swing.MigLayout
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.SmileClassifierNode
import org.simbrain.network.smile.ClassificationAlgorithm
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.ResourceManager
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.stats.distributions.TwoValued
import org.simbrain.util.table.*
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator

/**
 * SVN Training dialog.
 */
fun ClassificationAlgorithm.getTrainingDialog(): StandardDialog {
    return StandardDialog().apply {

        contentPane = JPanel()
        val statsLabel = JLabel("Score:")
        layout = MigLayout("fillx")
        // layout = MigLayout("fillx, debug")

        // Data Panels
        val inputs = SimbrainDataViewer(
            createFromDoubleArray(this@getTrainingDialog.trainingData.featureVectors), false).apply {
            addAction(table.importCsv)
            addAction(table.randomizeAction)
            preferredSize = Dimension(300, 300)
            addClosingTask {
                this@getTrainingDialog.trainingData.featureVectors = this.model.get2DDoubleArray()
            }
        }

        val targets = SimbrainDataViewer(createFromColumn(this@getTrainingDialog.trainingData.targets), false).apply {
            addAction(table.importCsv)
            addAction(table.randomizeColumnAction)
            table.model.columns[0].columnRandomizer = TwoValued(-1.0,1.0)
            preferredSize = Dimension(200, 300)
            addClosingTask {
                this@getTrainingDialog.trainingData.targets = this.model.getIntColumn(0)
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
                    inputs.table.model.deleteRow(inputs.table.rowCount-1)
                    targets.table.model.deleteRow(targets.table.rowCount-1)
                }
            })
        }

        // Training Button
        val trainButton = JButton("Train").apply {
            addActionListener {
                // TODO: Make a separate commit action and then just call svm.train. See deepnet
                // TODO: Generalize to more than one column?
                this@getTrainingDialog.fit(inputs.table.model.get2DDoubleArray(),
                    targets.table.model.getIntColumn(0))
                    statsLabel.text = "Stats: " + this@getTrainingDialog.stats
            }
        }

        // Add all components
        val classfierGeneralProps = AnnotatedPropertyEditor(this@getTrainingDialog)
        contentPane.add(classfierGeneralProps, "wrap")
        addClosingTask(classfierGeneralProps::commitChanges)
        contentPane.add(JSeparator(), "growx, span, wrap")
        val classfierProps = AnnotatedPropertyEditor(this@getTrainingDialog)
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
        contentPane.add(JPanel().apply{
            add(JLabel("Add / Remove rows:"))
            add(addRemoveRows)
        })
    }
}

fun main() {
    val networkComponent = NetworkComponent("net 1")
    val np = NetworkPanel(networkComponent)
    val classifier = with(networkComponent.network) {
        val svm = SVMClassifier(2, 2)
        val classifier = SmileClassifier(this, svm)
        svm.trainingData.featureVectors = arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(1.0, 1.0)
        )
        svm.trainingData.targets = intArrayOf(-1,1,1,-1)
        addNetworkModel(classifier)
        classifier
    }
    SmileClassifierNode(np, classifier).propertyDialog.run { makeVisible() }
}