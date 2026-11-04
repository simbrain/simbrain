package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.miginfocom.swing.MigLayout
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.SmileClassifierNode
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.smile.classifiers.RandomForestClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.network.trainers.createClassificationDataset
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.stats.distributions.UniformIntegerDistribution
import org.simbrain.util.table.BasicDataFrame
import java.awt.Dimension
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
        val trainingStatsLabel = JLabel("---")
        val testingStatsLabel = JLabel("---")
        layout = MigLayout("fill, ins 8")
        fun updateStatsLabel() {
            trainingStatsLabel.text = classifier.trainingStats
            testingStatsLabel.text = classifier.testingStats
        }
        updateStatsLabel()
        events.updated.onUi(viewScope) {
            updateStatsLabel()
        }

        fun createDataSetPanel(dataSet: ClassificationDataset) = DataSetPanel(
            BasicDataFrame(dataSet.inputs),
            BasicDataFrame(dataSet.targets.map { listOf(it) }).apply {
                cellRandomizer = UniformIntegerDistribution(0, dataSet.numClasses - 1)
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

        val trainingPanel = JPanel(MigLayout("fill")).apply {
            add(trainingStatsLabel, "wrap")
            add(trainingDataSetPanel, "grow, push")
        }

        val testPanel = JPanel(MigLayout("fill")).apply {
            add(testingStatsLabel, "wrap")
            add(testingDataSetPanel, "grow, push")
        }

        val dataSetTabPane = JTabbedPane().apply {
            addTab("Training data", trainingPanel)
            addTab("Testing data", testPanel)
        }

        // Training Button
        val trainButton = JButton("Train").apply {
            addActionListener {
                try {
                    classfierProps.commitChanges()
                    syncDataSet()
                    UnsavedChanges.mark()
                    train()
                } catch(e: Exception) {
                    showMessage(e.message.toString(), MessageKind.WARNING)
                }
            }
        }

        if (classfierProps.parameterWidgetMap.isNotEmpty()) {
            add(classfierProps, "wrap")
            addCommitTask(classfierProps::commitChanges)
            add(JSeparator(), "growx, span, wrap")
        }
        contentPane.add(trainButton,  "wrap")
        contentPane.add(dataSetTabPane, "grow, push, wrap")
    }
}

/**
 * Dialog that shows one tree of a trained random forest at a time as nested if/then/else rules, with arrow buttons to
 * step through the trees.
 *
 * @param featureNames names for the inputs, in order
 * @param classNames names for the class labels, indexed by label
 */
fun RandomForestClassifier.createTreeViewer(featureNames: List<String>, classNames: List<String>): StandardDialog {
    var treeIndex = 0
    var showCounts = false
    val rulesPane = JEditorPane("text/html", "").apply {
        isEditable = false
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        font = Theme.font(14)
        border = BorderFactory.createEmptyBorder(8, 12, 8, 12)
    }
    val counter = JLabel()
    fun showTree() {
        val rule = tree(treeIndex)
        rulesPane.text = if (rule == null) "" else treeRulesHtml(rule, featureNames, classNames, showCounts)
        rulesPane.caretPosition = 0
        counter.text = "Tree ${treeIndex + 1} of $trainedTreeCount"
    }
    val previous = JButton("◀").apply {
        toolTipText = "Show the previous tree."
        addActionListener {
            treeIndex = Math.floorMod(treeIndex - 1, trainedTreeCount)
            showTree()
        }
    }
    val next = JButton("▶").apply {
        toolTipText = "Show the next tree."
        addActionListener {
            treeIndex = (treeIndex + 1) % trainedTreeCount
            showTree()
        }
    }
    val countsBox = JCheckBox("Show leaf counts").apply {
        toolTipText = "<html>After each prediction, show how many of the training points that reach it have that class.<br>" +
                "Each tree trains on its own random resample of the training points, so the counts differ between trees.</html>"
        addActionListener {
            showCounts = isSelected
            showTree()
        }
    }
    showTree()
    val explanation = JLabel(
        "<html><body style='width: 380px'>The forest predicts by a vote of all $trainedTreeCount trees, each " +
                "with max depth $trainedMaxDepth. Each tree was trained on a different random resample of the training points, so " +
                "the trees ask different questions.</body></html>"
    )
    return StandardDialog().apply {
        title = "Random Forest Trees"
        contentPane = JPanel(MigLayout("fill, ins 12", "[grow]")).apply {
            add(explanation, "wrap")
            add(JPanel(MigLayout("ins 0", "[][][]")).apply {
                add(previous)
                add(counter, "gapx 8 8")
                add(next)
            }, "center, wrap")
            add(JScrollPane(rulesPane).apply { preferredSize = Dimension(440, 360) }, "grow, push, wrap")
            add(countsBox)
        }
        setAsDoneDialog()
    }
}

/**
 * HTML for [rule] as nested if/then/else blocks. A branch that ends in a prediction stays on the line of its test.
 */
private fun treeRulesHtml(
    rule: RandomForestClassifier.TreeRule,
    featureNames: List<String>,
    classNames: List<String>,
    showCounts: Boolean
): String {
    val muted = Theme.mutedText.htmlHex
    fun feature(index: Int) = (featureNames.getOrNull(index) ?: "input ${index + 1}").escapeHtml()
    fun number(value: Double) = "%.2f".format(value).replace("-", "−")
    fun leaf(leaf: RandomForestClassifier.TreeRule.Leaf) = buildString {
        append((classNames.getOrNull(leaf.label) ?: leaf.label.toString()).escapeHtml())
        if (showCounts) append(" <span style='color: $muted'>(${leaf.count} of ${leaf.total})</span>")
    }
    fun block(content: String) = "<div style='margin-left: 24px'>$content</div>"
    fun render(rule: RandomForestClassifier.TreeRule): String = when (rule) {
        is RandomForestClassifier.TreeRule.Leaf -> "<div><b>always</b> ${leaf(rule)}</div>"
        is RandomForestClassifier.TreeRule.Split -> buildString {
            val test = "<b>if</b> ${feature(rule.feature)} ≤ ${number(rule.threshold)} <b>then</b>"
            val ifTrue = rule.ifTrue
            if (ifTrue is RandomForestClassifier.TreeRule.Leaf) {
                append("<div>$test ${leaf(ifTrue)}</div>")
            } else {
                append("<div>$test</div>").append(block(render(ifTrue)))
            }
            val ifFalse = rule.ifFalse
            if (ifFalse is RandomForestClassifier.TreeRule.Leaf) {
                append("<div><b>else</b> ${leaf(ifFalse)}</div>")
            } else {
                append("<div><b>else</b></div>").append(block(render(ifFalse)))
            }
        }
    }
    return "<html><body>${render(rule)}</body></html>"
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
            targets = mutableListOf(-1, 1, 1, -1)
        ))
        val classifier = ClassifierNetwork(svm)
        addNetworkModelAsync(classifier)
        classifier
    }
    SmileClassifierNode(np, classifier).propertyDialog.display()
}