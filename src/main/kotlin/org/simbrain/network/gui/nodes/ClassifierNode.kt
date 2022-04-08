package org.simbrain.network.gui.nodes

import net.miginfocom.swing.MigLayout
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PText
import org.simbrain.network.NetworkComponent
import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.ResourceManager
import org.simbrain.util.StandardDialog
import org.simbrain.util.Utils
import org.simbrain.util.piccolo.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.stats.distributions.UniformIntegerDistribution
import org.simbrain.util.table.*
import org.simbrain.util.toSimbrainColorImage
import java.awt.Dialog.ModalityType
import java.awt.Dimension
import javax.swing.*

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
        events.onUpdated {
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
    private fun computeInfoText() = "Winning class: " + smileClassifier.winner

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
        }
    }

    override fun getPropertyDialog(): StandardDialog = StandardDialog().apply {

        // TODO: Generalize training dialog and move

        title = "Smile Classifier"
        modalityType = ModalityType.MODELESS // Set to modeless so the dialog can be left open

        val mainPanel = JPanel()
        val statsLabel = JLabel("Score:")
        contentPane = mainPanel
        mainPanel.apply {

            layout = MigLayout("fillx")
            // layout = MigLayout("debug")

            // Data Panels
            val inputs = SimbrainDataViewer(createFromDoubleArray(smileClassifier.trainingInputs), false).apply {
                addAction(table.importCsv)
                addAction(table.randomizeAction)
                preferredSize = Dimension(300, 300)
                addClosingTask {
                    smileClassifier.trainingInputs = this.model.getRowMajorDoubleArray()
                }
            }

            val targets = SimbrainDataViewer(createFromColumn(smileClassifier.trainingTargets), false).apply {
                addAction(table.importCsv)
                addAction(table.randomizeColumnAction)
                // TODO: Should be 1, -1.
                table.model.columns[0].columnRandomizer = UniformIntegerDistribution(-1,1)
                preferredSize = Dimension(200, 300)
                addClosingTask {
                    smileClassifier.trainingTargets = this.model.getIntColumn(0)
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
                    // TODO: Make a separate commit action and then just call smileClassifier.train. See deepnet
                    // TODO: Generalize to more than one column?
                    smileClassifier.train(inputs.table.model.getColumnMajorArray()
                        , targets.table.model.getIntColumn(0))
                    statsLabel.text = "Stats: " + smileClassifier.classifier.stats
                }
            }

            // Add all components
            val classfierGeneralProps = AnnotatedPropertyEditor(smileClassifier)
            add(classfierGeneralProps, "wrap")
            addClosingTask(classfierGeneralProps::commitChanges)
            add(JSeparator(), "growx, span, wrap")
            val classfierProps = AnnotatedPropertyEditor(smileClassifier.classifier)
            if (!classfierProps.widgets.isEmpty()) {
                add(classfierProps, "wrap")
                addClosingTask(classfierProps::commitChanges)
                add(JSeparator(), "growx, span, wrap")
            }
            add(trainButton)
            add(statsLabel, "wrap")
            add(JSeparator(), "span, growx, wrap")
            add(JLabel("Inputs"))
            add(JLabel("Target Labels"))
            add(JSeparator(), "span, growx, wrap")
            add(inputs)
            add(targets, "wrap")
            add(JPanel().apply{
                add(JLabel("Add / Remove rows:"))
                add(addRemoveRows)
            })
        }

    }

}

fun main() {
    val networkComponent = NetworkComponent("net 1")
    val np = NetworkPanel(networkComponent)
    val classifier = with(networkComponent.network) {
        val classifier = SmileClassifier(this, SVMClassifier(), 2)
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
    SmileClassifierNode(np, classifier).getPropertyDialog().run { makeVisible() }
}