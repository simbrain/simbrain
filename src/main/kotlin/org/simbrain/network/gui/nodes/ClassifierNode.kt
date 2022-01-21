package org.simbrain.network.gui.nodes

import net.miginfocom.swing.MigLayout
import org.piccolo2d.nodes.PText
import org.simbrain.network.NetworkComponent
import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.ResourceManager
import org.simbrain.util.StandardDialog
import org.simbrain.util.Utils
import org.simbrain.util.math.ProbDistributions.TwoValued
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.table.*
import java.awt.Dialog.ModalityType
import java.awt.Dimension
import javax.swing.*

class SmileClassifierNode(networkPanel: NetworkPanel, private val smileClassifier: SmileClassifier):
    ArrayLayerNode(networkPanel, smileClassifier) {

    /**
     * Text showing info about the classifier.
     */
    private val infoText = PText().apply {
        font = INFO_FONT
        text = computeInfoText()
        mainNode.addChild(this)
    }


    init {
        val events = smileClassifier.events
        events.onUpdated {
            updateInfoText()
            updateBorder()
        }
        updateInfoText()
        updateBorder()
    }

    /**
     * Update status text.
     */
    private fun computeInfoText() = "Output: (" +
                Utils.doubleArrayToString(smileClassifier.outputs.col(0), 2) + ")" +
                "\n\nInput: (" + Utils.doubleArrayToString(smileClassifier.inputs.col(0), 2) + ")"

    private fun updateInfoText() {
        infoText.text = computeInfoText()
    }

    override fun getModel(): NetworkModel {
        return smileClassifier
    }

    override fun getContextMenu(): JPopupMenu? {
        return JPopupMenu().apply {
            add(JMenuItem("Train...").apply { addActionListener {
                getTrainingDialog().run { makeVisible() }
            } })
            add(JMenuItem("Set Properties...").apply { addActionListener {
                propertyDialog.run { makeVisible() }
            } })
        }
    }

    override fun getPropertyDialog(): StandardDialog = AnnotatedPropertyEditor.getDialog(smileClassifier.classifier)

    fun getTrainingDialog() = StandardDialog().apply {

        // TODO: Generalize concept of training dialog and move somewhere else?

        title = "Smile Classifier"
        modalityType = ModalityType.MODELESS // Set to modeless so the dialog can be left open

        val mainPanel = JPanel()
        val statsLabel = JLabel("Score:")
        contentPane = mainPanel
        mainPanel.apply {

            layout = MigLayout("fillx")

            fun SimbrainDataViewer.addCustomActions()  {
                addAction(table.importCsv)
                addAction(table.randomizeColumnAction)
                addAction(table.editColumnAction)
            }

            // Data Panels
            val inputs = SimbrainDataViewer(createFromDoubleArray(smileClassifier.trainingInputs), false).apply {
                addCustomActions()
                addSeparator()
                addAction(table.randomizeAction)
                preferredSize = Dimension(300, 300)
                addClosingTask {
                    smileClassifier.trainingInputs = this.model.getColumnMajorArray()
                }
            }

            val targets = SimbrainDataViewer(createFromColumn(smileClassifier.trainingTargets), false).apply {
                addCustomActions()
                table.model.columns[0].columnRandomizer.probabilityDistribution =
                    TwoValued.TwoValuedBuilder().upper(1).lower(-1).build()
                preferredSize = Dimension(200, 300)
                addClosingTask {
                    smileClassifier.trainingTargets = this.model.getIntColumn(0)
                }
            }

            val addRemoveRows = JToolBar().apply {
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
                    // TODO: Make a separate commit action and then just call smileClassifier.train. See deepnet
                    // TODO: Generalize to more than one column?
                    smileClassifier.train(inputs.table.model.getColumnMajorArray()
                        , targets.table.model.getIntColumn(0))
                    statsLabel.text = "Stats: " + smileClassifier.classifier.stats
                }
            }

            // Add all components
            add(AnnotatedPropertyEditor(smileClassifier), "wrap")
            add(JSeparator(), "growx, span, wrap")
            add(trainButton)
            add(statsLabel, "wrap")
            add(JSeparator(), "span, growx, wrap")
            add(inputs)
            add(targets, "wrap")
            add(addRemoveRows)
        }

    }

}

fun main() {
    val networkComponent = NetworkComponent("net 1")
    val np = NetworkPanel(networkComponent)
    val classifier = with(networkComponent.network) {
        val classifier = SmileClassifier(this, SVMClassifier(), 2, 1, 4)
        classifier.trainingInputs = arrayOf(
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.0, 1.0),
                doubleArrayOf(1.0, 1.0)
            )
        classifier.trainingTargets = intArrayOf(-1,1,1,-1)
        addNetworkModel(classifier)
        classifier
    }
    SmileClassifierNode(np, classifier).getTrainingDialog().run { makeVisible() }
}