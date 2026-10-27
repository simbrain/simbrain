/**
 * Shared Tiny MNIST support for the feed-forward and CNN MNIST simulations: loading the cached dataset,
 * building altered copies of the testing set (shifted, rotated, noisy), and the control panel both
 * simulations use for training, trying individual images, and testing generalization.
 *
 * The altered testing sets are computed from the cached CSVs, so no extra data is downloaded.
 */
package org.simbrain.custom_sims.simulations.backprop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import net.miginfocom.swing.MigLayout
import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.*
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.Dimension
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val IMAGE_SIDE = 20

class TinyMnistData(val trainingSet: TrainingDataset, val testingSet: TrainingDataset)

fun loadTinyMnist(): TinyMnistData? {
    fun fetch(name: String) = fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_$name.csv")
    val trainInputs = fetch("train_inputs") ?: return null
    val trainLabels = fetch("train_labels") ?: return null
    val testInputs = fetch("test_inputs") ?: return null
    val testLabels = fetch("test_labels") ?: return null
    fun dataset(inputs: String, labels: String) = TrainingDataset(
        inputs = csvToDouble2DArray(inputs).map { it.toMutableList() }.toMutableList(),
        targets = csvToDouble2DArray(labels).map { it.toMutableList() }.toMutableList(),
        inputSize = IMAGE_SIDE * IMAGE_SIDE,
        targetSize = 10,
    )
    return TinyMnistData(dataset(trainInputs, trainLabels), dataset(testInputs, testLabels))
}

/**
 * A testing set derived from the original one by applying [transform] to every image.
 */
class TestSetVariant(val label: String, val transform: (DoubleArray) -> DoubleArray) {
    fun applyTo(original: TrainingDataset) = TrainingDataset(
        inputs = original.inputs.map { transform(it.toDoubleArray()).toMutableList() }.toMutableList(),
        targets = original.targets.map { it.toMutableList() }.toMutableList(),
        inputSize = original.inputSize,
        targetSize = original.targetSize,
    )

    override fun toString() = label
}

/**
 * Feed-forward networks lose accuracy quickly as digits move away from where they appeared in training.
 * The CNN tolerates small shifts better because of its pooling layers, but its final dense layer is
 * still position-specific, so both fail once digits move a few pixels.
 */
val tinyMnistTestVariants = listOf(
    TestSetVariant("Original") { it },
    TestSetVariant("Shifted 1 pixel right") { shiftImage(it, 1, 0) },
    TestSetVariant("Shifted 2 pixels right") { shiftImage(it, 2, 0) },
    TestSetVariant("Shifted 2 pixels down and right") { shiftImage(it, 2, 2) },
    TestSetVariant("Rotated 20 degrees") { rotateImage(it, 20.0) },
    // Seeded per image so the noisy set is the same each time it is built
    TestSetVariant("Noisy") { addNoise(it, 0.3, java.util.Random(it.contentHashCode().toLong())) },
)

class TrainingSize(val count: Int) {
    override fun toString() = "%,d".format(count)
}

val tinyMnistTrainingSizes = listOf(TrainingSize(10000), TrainingSize(1000), TrainingSize(300))

fun TrainingDataset.firstRows(count: Int) = TrainingDataset(
    inputs = inputs.take(count).map { it.toMutableList() }.toMutableList(),
    targets = targets.take(count).map { it.toMutableList() }.toMutableList(),
    inputSize = inputSize,
    targetSize = targetSize,
)

fun shiftImage(image: DoubleArray, dx: Int, dy: Int) = DoubleArray(image.size).also { shifted ->
    for (y in 0 until IMAGE_SIDE) {
        for (x in 0 until IMAGE_SIDE) {
            val sourceX = x - dx
            val sourceY = y - dy
            if (sourceX in 0 until IMAGE_SIDE && sourceY in 0 until IMAGE_SIDE) {
                shifted[y * IMAGE_SIDE + x] = image[sourceY * IMAGE_SIDE + sourceX]
            }
        }
    }
}

/**
 * Rotate clockwise on screen (rows run top to bottom) about the image center, using nearest-neighbor sampling.
 */
fun rotateImage(image: DoubleArray, degrees: Double) = DoubleArray(image.size).also { rotated ->
    val theta = Math.toRadians(degrees)
    val center = (IMAGE_SIDE - 1) / 2.0
    for (y in 0 until IMAGE_SIDE) {
        for (x in 0 until IMAGE_SIDE) {
            val sourceX = (cos(theta) * (x - center) + sin(theta) * (y - center) + center).roundToInt()
            val sourceY = (-sin(theta) * (x - center) + cos(theta) * (y - center) + center).roundToInt()
            if (sourceX in 0 until IMAGE_SIDE && sourceY in 0 until IMAGE_SIDE) {
                rotated[y * IMAGE_SIDE + x] = image[sourceY * IMAGE_SIDE + sourceX]
            }
        }
    }
}

fun addNoise(image: DoubleArray, standardDeviation: Double, random: java.util.Random) = DoubleArray(image.size) { i ->
    (image[i] + random.nextGaussian() * standardDeviation).coerceIn(0.0, 1.0)
}

/**
 * Fraction of rows whose largest output matches the one-hot target. [onProgress] receives the number of rows
 * evaluated so far.
 */
suspend fun TrainingDataset.accuracy(
    predict: suspend (DoubleArray) -> DoubleArray,
    onProgress: suspend (Int) -> Unit = {},
): Double {
    var correct = 0
    inputs.indices.forEach { i ->
        if (predict(inputs[i].toDoubleArray()).argmax() == targets[i].toDoubleArray().argmax()) {
            correct++
        }
        onProgress(i + 1)
    }
    return correct.toDouble() / inputs.size
}

private fun DoubleArray.argmax() = indices.maxBy { this[it] }

/**
 * What the Tiny MNIST control panel needs from a simulation, so that the feed-forward and CNN versions share
 * one panel.
 */
interface TinyMnistModel {
    var trainingSet: TrainingDataset
    var testingSet: TrainingDataset

    /**
     * Run [image] through the network so the result is visible, and return the output activations.
     */
    suspend fun show(image: DoubleArray): DoubleArray

    /**
     * Run [image] through the network without needing to update the display. Used when measuring accuracy
     * over whole datasets.
     */
    suspend fun predict(image: DoubleArray): DoubleArray = show(image)

    /**
     * Called once before a run of [predict] calls, for any setup that need not be repeated per image.
     */
    suspend fun beginEvaluation() {}

    suspend fun openTrainingDialog()

    /**
     * Add an image world the user can draw in, coupled to the input layer.
     */
    suspend fun addDrawingWorld()
}

/**
 * Control panel for training the network, trying individual images, and testing how well the network
 * generalizes to fewer training examples or altered test images.
 */
fun SimbrainDesktop.createTinyMnistControlPanel(
    data: TinyMnistData,
    model: TinyMnistModel,
) = createControlPanel("Tiny MNIST", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
    addTinyMnistControls(data, model, workspace)
}

private fun ControlPanelKt.addTinyMnistControls(data: TinyMnistData, model: TinyMnistModel, workspace: Workspace) {
    addButton("Train network...") {
        model.openTrainingDialog()
    }.apply {
        toolTipText = "Opens the training dialog. This is a convenience: right-clicking the network and choosing Train opens the same dialog."
    }
    val trainingSizeToolTip = "Train on fewer images to make overfitting visible. Press Init parameters in the training dialog to start fresh."
    addComboBox(
        "Training examples",
        tinyMnistTrainingSizes,
        tinyMnistTrainingSizes.first(),
        labelToolTip = trainingSizeToolTip
    ) { size ->
        model.trainingSet = data.trainingSet.firstRows(size.count)
    }.apply { toolTipText = trainingSizeToolTip }
    val testingSetToolTip = "Test on altered copies of the test images, such as shifted or rotated digits."
    addComboBox(
        "Testing set",
        tinyMnistTestVariants,
        tinyMnistTestVariants.first(),
        labelToolTip = testingSetToolTip
    ) { variant ->
        model.testingSet = variant.applyTo(data.testingSet)
    }.apply { toolTipText = testingSetToolTip }
    addSeparator()
    addButton("Random training image") {
        val dataset = model.trainingSet
        model.show(dataset.inputs[Random.nextInt(dataset.size)].toDoubleArray())
    }.apply {
        toolTipText = "Runs a randomly chosen training image through the network."
    }
    addButton("Random test image") {
        val dataset = model.testingSet
        model.show(dataset.inputs[Random.nextInt(dataset.size)].toDoubleArray())
    }.apply {
        toolTipText = "Runs a randomly chosen test image, one the network was not trained on, through the network."
    }
    addButton("Draw your own digit") {
        model.addDrawingWorld()
    }.apply {
        toolTipText = "Opens an image world coupled to the input layer. Draw a digit, then press Run or Step to see the network's answer."
    }
    addSeparator()
    val progressBar = JProgressBar().apply {
        isStringPainted = true
        string = ""
        // Fixed size so the changing progress text cannot resize the panel
        preferredSize = Dimension(250, preferredSize.height)
    }
    addButton("Test generalization") {
        if (workspace.updater.isRunning) {
            withContext(Dispatchers.Swing) { showWarningDialog("Stop the simulation before testing generalization.") }
            return@addButton
        }
        val testingSets = tinyMnistTestVariants.map { it to it.applyTo(data.testingSet) }
        val trainingSet = model.trainingSet
        val total = trainingSet.size + testingSets.sumOf { it.second.size }
        var done = 0
        suspend fun updateProgress(rowsInCurrentSet: Int) {
            if (rowsInCurrentSet % 50 == 0) {
                withContext(Dispatchers.Swing) { progressBar.value = (100 * (done + rowsInCurrentSet)) / total }
            }
        }
        withContext(Dispatchers.Swing) {
            isEnabled = false
            progressBar.value = 0
            progressBar.string = null
        }
        try {
            model.beginEvaluation()
            val trainingAccuracy = trainingSet.accuracy({ model.predict(it) }) { updateProgress(it) }
            done += trainingSet.size
            val testingAccuracies = testingSets.map { (variant, dataset) ->
                (variant.label to dataset.accuracy({ model.predict(it) }) { updateProgress(it) }).also { done += dataset.size }
            }
            withContext(Dispatchers.Swing) {
                progressBar.value = 100
                progressBar.string = "Done"
                showGeneralizationResults(trainingSet.size, trainingAccuracy, testingAccuracies)
            }
        } finally {
            withContext(Dispatchers.Swing) { isEnabled = true }
        }
    }.apply {
        toolTipText = "Reports accuracy on the training set and on every testing set. Stop the simulation first if it is running."
    }
    addComponent(progressBar)
}

private fun generalizationResultsPanel(trainingSize: Int, trainingAccuracy: Double, testingAccuracies: List<Pair<String, Double>>) =
    JPanel(MigLayout("insets 20 24 12 24, wrap 2", "[left]40[right]", "[]4[]")).apply {
        fun heading(text: String) = JLabel(text).apply { font = font.deriveFont(Font.BOLD, 16f) }
        fun row(label: String, accuracy: Double) {
            add(JLabel(label).apply { font = font.deriveFont(15f) })
            add(JLabel(formatPercent(accuracy)).apply { font = font.deriveFont(Font.BOLD, 15f) })
        }
        add(heading("Training set"), "span, wrap")
        row("${"%,d".format(trainingSize)} training images", trainingAccuracy)
        add(heading("Testing sets"), "span, gaptop 16, wrap")
        testingAccuracies.forEach { (label, accuracy) -> row(label, accuracy) }
    }

private fun showGeneralizationResults(trainingSize: Int, trainingAccuracy: Double, testingAccuracies: List<Pair<String, Double>>) {
    StandardDialog().apply {
        title = "Generalization accuracy"
        contentPane = generalizationResultsPanel(trainingSize, trainingAccuracy, testingAccuracies)
        isModal = false
        setAsDoneDialog()
        makeVisible()
    }
}

private fun formatPercent(value: Double) = "${(value * 1000).roundToInt() / 10.0}%"
