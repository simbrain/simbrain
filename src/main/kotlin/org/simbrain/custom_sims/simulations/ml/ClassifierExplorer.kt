/**
 * Pieces shared by the machine learning simulations: the [ClassifierChoice] list and [trainClassifier] factory, the
 * [ClassifierExplorer] control panel sections for choosing and comparing classifiers and loading training or test
 * examples onto a network's inputs, and [placeExplorerWindows] for the common window layout.
 *
 * The simulations own their data and network. The explorer reads them through [ClassifierExplorer.network],
 * [ClassifierExplorer.dataset], and [ClassifierExplorer.classNames], which a simulation may replace, for example when
 * the model comparison simulation switches datasets.
 */
package org.simbrain.custom_sims.simulations.ml

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.bottomEdgeWithGap
import org.simbrain.custom_sims.rightEdgeWithGap
import org.simbrain.network.NetworkComponent
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.smile.SimbrainClassifier
import org.simbrain.network.smile.classifiers.KNNClassifier
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import org.simbrain.network.smile.classifiers.RandomForestClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.util.ControlPanelKt
import org.simbrain.util.place
import org.simbrain.util.showResults
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel

enum class ClassifierChoice(private val description: String) {
    LogisticRegression("Logistic regression"),
    KNN("K nearest neighbors"),
    SVM("Support vector machine"),
    RandomForest("Random forest");

    override fun toString() = description
}

/**
 * Create a classifier of the given kind on [data] and fit it to its training split. The defaults are the settings
 * the comparison simulations use.
 *
 * @param k neighbors that vote, for K nearest neighbors
 * @param kernelWidth Gaussian kernel width, for the support vector machine
 * @param maxDepth and [numTrees] for the random forest
 */
fun trainClassifier(
    choice: ClassifierChoice,
    data: ClassificationDataset,
    k: Int = 5,
    kernelWidth: Double = 3.0,
    maxDepth: Int = 20,
    numTrees: Int = 100
): SimbrainClassifier {
    val model = when (choice) {
        ClassifierChoice.LogisticRegression -> LogisticRegClassifier(data)
        ClassifierChoice.KNN -> KNNClassifier(data).also { it.k = k }
        ClassifierChoice.SVM -> SVMClassifier(data).also { it.kernelWidth = kernelWidth }
        ClassifierChoice.RandomForest -> RandomForestClassifier(data).also {
            it.maxDepth = maxDepth
            it.numTrees = numTrees
        }
    }
    model.fitTrainingData()
    return model
}

private fun percent(fraction: Double) = "%.1f%%".format(100 * fraction)

/**
 * @param exampleNoun what one row of the data is called in the controls, such as "flower" or "row"
 * @param describeData heading for the comparison table, such as the dataset name
 */
class ClassifierExplorer(
    private val workspace: Workspace,
    var network: ClassifierNetwork,
    var dataset: ClassificationDataset,
    var classNames: List<String>,
    var classifierChoice: ClassifierChoice,
    private val exampleNoun: String,
    private val describeData: () -> String
) {

    /**
     * Held while the classifier, network, or loaded example changes, so overlapping clicks apply in order.
     */
    val lock = Mutex()

    private var useTestSet = true

    private var exampleIndex = -1

    private var trainingAccuracy: JLabel? = null
    private var testAccuracy: JLabel? = null
    private lateinit var status: JLabel
    private var classChooser: JComboBox<String>? = null

    private val examples get() = with(network.classifier) { if (useTestSet) testingData else trainingData }

    private val setName get() = if (useTestSet) "Test" else "Training"

    private val emptyStatus = "<html>---<br>&nbsp;<br>&nbsp;</html>"

    /**
     * Train the chosen classifier on [dataset], put it in the network, and refresh the accuracies and the loaded
     * example. Call with [lock] held.
     */
    suspend fun retrainLocked() {
        network.classifier = trainClassifier(classifierChoice, dataset)
        showAccuracy()
        if (exampleIndex >= 0) showExample()
    }

    suspend fun retrain() = lock.withLock { retrainLocked() }

    /**
     * Refresh the controls after the simulation replaced [network], [dataset], or [classNames]. Call with [lock]
     * held.
     */
    suspend fun resetLocked() {
        exampleIndex = -1
        withContext(Dispatchers.Swing) {
            status.text = emptyStatus
            classChooser?.model = DefaultComboBoxModel(classNames.toTypedArray())
        }
        showAccuracy()
    }

    /**
     * Refresh the accuracies and the loaded example after the simulation changed the classifier itself, clearing
     * the example if [newData] replaced the examples. Call with [lock] held.
     */
    suspend fun classifierChangedLocked(newData: Boolean) {
        showAccuracy()
        if (newData) {
            exampleIndex = -1
            withContext(Dispatchers.Swing) { status.text = emptyStatus }
        } else if (exampleIndex >= 0) {
            showExample()
        }
    }

    suspend fun showAccuracy() {
        val classifier = network.classifier
        withContext(Dispatchers.Swing) {
            trainingAccuracy?.text = percent(classifier.trainingAccuracy)
            testAccuracy?.text = percent(classifier.testingAccuracy)
        }
    }

    private suspend fun showExample() {
        if (exampleIndex !in examples.inputs.indices) return
        val input = examples.inputs[exampleIndex]
        network.inputNeuronGroup.setActivations(input.toDoubleArray())
        workspace.iterateSuspend()
        val actual = classNames[examples.targets[exampleIndex]]
        val predicted = classNames.getOrNull(network.classifier.predict(input.toDoubleArray()))
        val result = if (actual == predicted) "correct" else "wrong"
        setStatus("$setName $exampleNoun ${exampleIndex + 1} of ${examples.inputs.size} ($result)<br>" +
                "True class: $actual<br>Predicted: $predicted")
    }

    /**
     * Load a random example of [classIndex], or of any class if it is null.
     */
    private suspend fun showRandomExample(classIndex: Int?) {
        val matching = examples.targets.indices.filter { classIndex == null || examples.targets[it] == classIndex }
        if (matching.isEmpty()) {
            val ofClass = classIndex?.let { "<br>of class ${classNames[it]}" } ?: ""
            setStatus("No ${setName.lowercase()} ${exampleNoun}s$ofClass")
            return
        }
        exampleIndex = matching.random()
        showExample()
    }

    private suspend fun step(forward: Boolean) {
        val count = examples.inputs.size
        if (count == 0) return
        exampleIndex = when {
            forward -> (exampleIndex + 1) % count
            exampleIndex <= 0 -> count - 1
            else -> exampleIndex - 1
        }
        showExample()
    }

    private suspend fun setStatus(html: String) = withContext(Dispatchers.Swing) {
        status.text = "<html>$html</html>"
    }

    /**
     * Classifier choice, accuracies, and a button that compares all four classifiers in a table.
     */
    fun addModelControls(panel: ControlPanelKt) = with(panel) {
        addComboBox(
            "Classifier",
            ClassifierChoice.entries,
            classifierChoice,
            toolTip = "Kind of model to train on the data."
        ) {
            classifierChoice = it
            launch { retrain() }
        }
        addAccuracyControls(panel)
        addButton("Compare all models") { showComparison() }.apply {
            toolTipText = "Train all four models on the current data and show their accuracy."
        }
    }

    /**
     * Training and test accuracy of the classifier in the network.
     */
    fun addAccuracyControls(panel: ControlPanelKt) = with(panel) {
        trainingAccuracy = addLabelledText(
            "Training accuracy", "---",
            toolTip = "Percent of the training ${exampleNoun}s the model classifies correctly."
        )
        testAccuracy = addLabelledText(
            "Test accuracy", "---",
            toolTip = "Percent of the held-out test ${exampleNoun}s the model classifies correctly."
        )
    }

    /**
     * How the example controls choose an example's class.
     */
    enum class ExampleStyle {
        /**
         * One button per class, with a list choosing between training and test examples. Suits a few short class
         * names.
         */
        ClassButtons,

        /**
         * A list of classes, with separate buttons for a test or a training example. Suits many classes or long
         * class names.
         */
        ClassList,

        /**
         * Buttons for a test or a training example of any class. Suits data where the class is easy to see, such as
         * points in a plot.
         */
        AnyClass
    }

    /**
     * Buttons that load a random example or step through the examples in order, and a status line with the true and
     * predicted class. Stepping goes through whichever set, training or test, was used last.
     */
    fun addExampleControls(panel: ControlPanelKt, style: ExampleStyle) = with(panel) {
        if (style == ExampleStyle.ClassButtons) {
            addComboBox(
                "${exampleNoun.replaceFirstChar { it.uppercase() }}s",
                listOf("Test ${exampleNoun}s", "Training ${exampleNoun}s"),
                "Test ${exampleNoun}s",
                toolTip = "Whether the buttons below load training ${exampleNoun}s or test ${exampleNoun}s."
            ) { choice ->
                launch {
                    lock.withLock {
                        useTestSet = choice.startsWith("Test")
                        exampleIndex = -1
                        withContext(Dispatchers.Swing) { status.text = emptyStatus }
                    }
                }
            }
            addRow("Random") {
                classNames.forEachIndexed { classIndex, name ->
                    addButton(name) { lock.withLock { showRandomExample(classIndex) } }.apply {
                        toolTipText = "Load a randomly chosen $name $exampleNoun onto the inputs."
                    }
                }
            }
        } else {
            if (style == ExampleStyle.ClassList) classChooser = addComboBox(
                "Class",
                classNames,
                toolTip = "Class of the $exampleNoun that the buttons below load."
            )
            addRow("Random") {
                listOf(true to "Test $exampleNoun", false to "Training $exampleNoun").forEach { (test, label) ->
                    addButton(label) {
                        lock.withLock {
                            useTestSet = test
                            showRandomExample(classChooser?.selectedIndex)
                        }
                    }.apply {
                        val ofClass = if (style == ExampleStyle.ClassList) " of the selected class" else ""
                        toolTipText = "Load a randomly chosen ${label.lowercase()}$ofClass onto the inputs."
                    }
                }
            }
        }
        addRow("Step through") {
            addButton("Previous") { lock.withLock { step(forward = false) } }.apply {
                toolTipText = "Load the previous $exampleNoun in the current set onto the inputs."
            }
            addButton("Next") { lock.withLock { step(forward = true) } }.apply {
                toolTipText = "Load the next $exampleNoun in the current set onto the inputs."
            }
        }
        status = addLabelledText(
            "Status", emptyStatus,
            toolTip = "The loaded $exampleNoun's true class and the model's prediction."
        ).apply {
            // Fixed size so longer status text cannot resize the panel. The layout honors minimum sizes, and an HTML
            // label's minimum grows with its text, so pin all three.
            val size = Dimension(280, preferredSize.height)
            preferredSize = size
            minimumSize = size
            maximumSize = size
        }
    }

    private fun showComparison() {
        val classifiers = ClassifierChoice.entries.associateWith { trainClassifier(it, dataset) }
        // Every classifier splits the data the same way, so any of them gives the training and test rows
        val (trainingRows, testRows) = classifiers.values.first().let { it.trainingData to it.testingData }
        val mostCommon = trainingRows.targets.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        fun ClassificationDataset.fractionMostCommon() =
            if (targets.isEmpty()) Double.NaN else targets.count { it == mostCommon }.toDouble() / targets.size
        showResults("Model Comparison") {
            section(describeData(), listOf("Training", "Test")) {
                classifiers.forEach { (choice, classifier) ->
                    row(choice.toString(), percent(classifier.trainingAccuracy), percent(classifier.testingAccuracy))
                }
                row(
                    "Baseline (most common class)",
                    percent(trainingRows.fractionMostCommon()),
                    percent(testRows.fractionMostCommon())
                )
            }
            note("${dataset.inputs.size} ${exampleNoun}s, ${dataset.inputs.firstOrNull()?.size ?: 0} features, " +
                    "${classNames.size} classes.")
        }
    }
}

/**
 * Place the plot under the control panel and the network to the right of both, as tall as the two together.
 *
 * @return the plot's bounds, for placing a replacement plot in the same spot
 */
suspend fun SimbrainDesktop.placeExplorerWindows(
    controlPanel: ControlPanelKt,
    plot: ProjectionComponent,
    networkComponent: NetworkComponent
): Rectangle {
    val plotY = controlPanel.bottomEdgeWithGap()
    val plotHeight = (desktopPane.height - plotY - SIM_WINDOW_GAP).coerceIn(320, 420)
    val plotBounds = Rectangle(SIM_WINDOW_GAP, plotY, maxOf(controlPanel.width, 420), plotHeight)
    place(plot, plotBounds.x, plotBounds.y, plotBounds.width, plotBounds.height)
    val networkX = controlPanel.rightEdgeWithGap()
    val networkWidth = (desktopPane.width - networkX - SIM_WINDOW_GAP).coerceIn(560, 760)
    place(networkComponent, networkX, SIM_WINDOW_GAP, networkWidth, plotY + plotHeight - SIM_WINDOW_GAP)
    return plotBounds
}
