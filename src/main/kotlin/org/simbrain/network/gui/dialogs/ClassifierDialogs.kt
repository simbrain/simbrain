package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.miginfocom.swing.MigLayout
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.SmileClassifierNode
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.network.trainers.createClassificationDataset
import org.simbrain.util.BiMap
import org.simbrain.util.StandardDialog
import org.simbrain.util.display
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.showWarningDialog
import org.simbrain.util.stats.distributions.TwoValued
import org.simbrain.util.stats.distributions.UniformIntegerDistribution
import org.simbrain.util.table.BasicDataFrame
import javax.swing.*

/**
 * Classifier training dialog.
 */
context(NetworkPanel)
fun ClassifierNetwork.getTrainingDialog(): StandardDialog {
    return StandardDialog().apply {

        title = "Train ${classifier.name} classifier with ${classifier.numClasses} labels"
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

        fun createDataSetPanel(dataSet: ClassificationDataset) = DataSetPanel(
            BasicDataFrame(dataSet.inputs),
            BasicDataFrame(dataSet.targets.map { listOf(it) }).apply {
                cellRandomizer = if (classifier is SVMClassifier) {
                    TwoValued()
                } else {
                    UniformIntegerDistribution(0, dataSet.numClasses - 1)
                }
            },
            applyAction = {
                withContext(Dispatchers.Default) {
                    with(network) {
                        inputDataFrame.getRow<Double>(it).let { activations ->
                            inputNeuronGroup.setActivations(activations.toDoubleArray())
                            update()
                        }
                    }
                }
            }
        )

        val trainingDataSetPanel = createDataSetPanel(classifier.trainingData)
        val testingDataSetPanel = createDataSetPanel(classifier.testingData)

        fun DataSetPanel.exportDataSet() = createClassificationDataset(
            inputs = inputDataFrame.get2DDoubleList().map { it.toMutableList() }.toMutableList(),
            targets = targetDataFrame.getIntColumn(0).toMutableList()
        )

        fun syncDataSet() {
            classifier.trainingData = trainingDataSetPanel.exportDataSet()
            classifier.testingData = testingDataSetPanel.exportDataSet()
        }

        val dataSetTabPane = JTabbedPane().apply {
            addTab("Training Set", trainingDataSetPanel)
            addTab("Testing Set", testingDataSetPanel)
        }

        // Training Button
        val trainButton = JButton("Train").apply {
            addActionListener {
                try {
                    classfierProps.commitChanges()
                    syncDataSet()
                    train()
                } catch(e: Exception) {
                    showWarningDialog(e.message.toString())
                }
            }
        }

        if (classfierProps.parameterWidgetMap.isNotEmpty()) {
            add(classfierProps, "wrap")
            addCommitTask(classfierProps::commitChanges)
            add(JSeparator(), "growx, span, wrap")
        }
        contentPane.add(trainButton)
        contentPane.add(statsLabel, "wrap")
        contentPane.add(dataSetTabPane, "wrap")
    }
}

fun main() {
    val networkComponent = NetworkComponent("net 1")
    val np = NetworkPanel(networkComponent)
    val classifier = with(networkComponent.network) {
        val svm = SVMClassifier(ClassificationDataset(
            inputs = mutableListOf(
                mutableListOf(0.0, 0.0),
                mutableListOf(1.0, 0.0),
                mutableListOf(0.0, 1.0),
                mutableListOf(1.0, 1.0)
            ),
            targets = mutableListOf(-1, 1, 1, -1),
            targetLabelMap = BiMap<Int, String>().apply { putAll(mapOf(-1 to "F", 1 to "T")) }
        ))
        val classifier = ClassifierNetwork(svm)
        addNetworkModel(classifier)
        classifier
    }
    SmileClassifierNode(np, classifier).propertyDialog.display()
}