package org.simbrain.custom_sims.simulations.imageworld

import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.network.trainers.CnnLossFunction
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.trainers.splitDataSet
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.*
import org.simbrain.world.imageworld.filters.ResizeOperation
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.*


private data class CategorySelection(val categories: List<String>, val maxPerCategory: Int)

/**
 * Shows a dialog with a multi-column checkbox grid of [allCategories] and a spinner
 * for the maximum number of images per category.
 * Returns `null` if the user cancels.
 */
private fun showCategorySelectionDialog(
    allCategories: List<String>,
    defaultCategories: List<String>,
    defaultMax: Int
): CategorySelection? {
    val checkboxes = allCategories.map { JCheckBox(it, it in defaultCategories) }
    val spinner = JSpinner(SpinnerNumberModel(defaultMax, 1, 10_000, 10))

    val cols = 4
    val numRows = (allCategories.size + cols - 1) / cols
    val grid = JPanel(GridLayout(numRows, cols, 8, 2))
    checkboxes.forEach { grid.add(it) }
    repeat(numRows * cols - allCategories.size) { grid.add(JLabel()) }

    val maxPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
        add(JLabel("Max images per category:  "))
        add(spinner)
    }

    val panel = JPanel(BorderLayout(0, 10)).apply {
        add(maxPanel, BorderLayout.NORTH)
        add(JScrollPane(grid).apply {
            preferredSize = java.awt.Dimension(cols * 190, 440)
        }, BorderLayout.CENTER)
    }

    while (true) {
        val result = JOptionPane.showConfirmDialog(
            null, panel, "Select Caltech101 Categories",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        )
        if (result != JOptionPane.OK_OPTION) return null

        val selected = checkboxes.filter { it.isSelected }.map { it.text }
        if (selected.size < 2) {
            val confirm = showWarningConfirmDialog("Please select at least 2 categories.")
            if (confirm == JOptionPane.YES_OPTION) {
                continue
            } else {
                return null
            }
        }
        return CategorySelection(
            categories = selected,
            maxPerCategory = spinner.value as Int
        )
    }
}

private const val CALTECH101_URL =
    "https://data.caltech.edu/records/mzrjq-6wc02/files/caltech-101.zip?download=1"
private const val CALTECH101_MD5 = "3138e1922a9193bfa496528edbbc45d0"

/** Categories pre-selected in the checkbox dialog when the full dataset is available. */
private val DEFAULT_CATEGORIES = listOf("airplanes", "crocodile", "Flamingo", "sunflower")

/** Maximum images per category — keeps the dataset balanced and training manageable. */
private const val MAX_PER_CATEGORY = 50

/**
 * Guides the user through obtaining the Caltech101 dataset and selecting categories.
 *
 * 1. If the dataset is not yet cached, shows a download confirmation dialog.
 *    Returns `null` (aborting the sim) if the user declines.
 * 2. Downloads and extracts the dataset (shows a progress bar).
 * 3. Shows a checkbox dialog listing all available categories.
 *    Returns `null` if the user cancels.
 * 4. Returns a sorted map of `categoryName → List<File>` for the selected categories,
 *    capped at [MAX_PER_CATEGORY] images each.
 *
 * Falls back to the bundled 40-image sample if the dataset is unavailable.
 */
private fun loadCategoryImages(): Map<String, List<File>>? {

    // ── Locate or download the dataset ───────────────────────────────────────

    val extractDir = File(
        org.simbrain.util.getSystemCacheDirectory(),
        "caltech-101"
    )
    val alreadyCached = extractDir.exists() && extractDir.listFiles()?.isNotEmpty() == true

    if (!alreadyCached) {
        val confirm = showWarningConfirmDialog(
            "The Caltech101 dataset (~137 MB) will be downloaded from data.caltech.edu\n" +
            "and cached locally. This only happens once."
        )
        if (confirm != JOptionPane.OK_OPTION && confirm != JOptionPane.YES_OPTION) return null
    }

    val root = fetchZipWithCache(CALTECH101_URL, expectedChecksum = CALTECH101_MD5)

    if (root != null) {
        // The Caltech101 zip contains a tar.gz; extract it if not yet done.
        val tarGz = root.walkTopDown().firstOrNull { it.name == "101_ObjectCategories.tar.gz" }
        if (tarGz != null) {
            val alreadyExtracted = File(tarGz.parentFile, "101_ObjectCategories").exists()
            if (!alreadyExtracted) extractTarGz(tarGz, tarGz.parentFile)
        }

        val objCatDir = root.walkTopDown()
            .firstOrNull { it.isDirectory && it.name == "101_ObjectCategories" }

        if (objCatDir != null) {
            val allCategories = (objCatDir.listFiles { f ->
                f.isDirectory && f.name != "BACKGROUND_Google"
            } ?: emptyArray())
                .map { it.name }
                .sorted()

            // Category + max-per-category dialog
            val selection = showCategorySelectionDialog(
                allCategories    = allCategories,
                defaultCategories = DEFAULT_CATEGORIES.filter { it in allCategories },
                defaultMax       = MAX_PER_CATEGORY
            ) ?: return null

            return objCatDir.listFiles { f -> f.isDirectory && f.name in selection.categories }
                ?.sortedBy { it.name }
                ?.associate { dir ->
                    val images = (dir.listFiles { f -> f.extension.lowercase() == "jpg" } ?: emptyArray())
                        .sortedBy { it.name }
                        .take(selection.maxPerCategory)
                    dir.name to images
                }
                ?.filterValues { it.isNotEmpty() }
                ?: emptyMap()
        }
    }

    // Fallback: bundled 40-image sample
    val fallbackFiles = getFilesWithExtension("simulations/images/Caltech101Sample", "jpg")
    val fallbackCategories = fallbackFiles
        .map { it.nameWithoutExtension.takeWhile { c -> !c.isDigit() } }
        .distinct().sorted()

    val selection = showCategorySelectionDialog(
        allCategories     = fallbackCategories,
        defaultCategories = fallbackCategories,
        defaultMax        = MAX_PER_CATEGORY
    ) ?: return null

    return fallbackFiles
        .groupBy { it.nameWithoutExtension.takeWhile { c -> !c.isDigit() } }
        .filterKeys { it in selection.categories }
        .mapValues { (_, files) -> files.sortedBy { it.name }.take(selection.maxPerCategory) }
        .toSortedMap()
}


/**
 * CNN Object Detector: trains a small CNN on Caltech101 RGB images.
 *
 * Pipeline: Input(100×100×3) → Conv1(3×3, 4f, SAME, ReLU) → Pool1(5×5, s=5, MAX)
 *         → Conv2(3×3, 8f, SAME, ReLU) → Pool2(4×4, s=4, MAX)
 *         → Flatten(200) → Dense(N classes)
 *
 * N is determined automatically from user selections in the startup dialog.
 */
val cnnObjectDetector = newSim {

    workspace.clearWorkspace()

    // ── Load dataset (may show confirmation + checkbox dialogs) ───────────────
    val categoryImages = loadCategoryImages() ?: return@newSim
    val categoryNames: List<String> = categoryImages.keys.toList()   // sorted
    val numClasses = categoryNames.size

    val networkComponent = addNetworkComponent("CNN Object Detector")
    val network = networkComponent.network

    val leftX = 0.0; val rightX = 400.0; val topY = 0.0; val stepY = 350.0

    val inputShape = TensorShape(100, 100, 3)
    val inputTensorLayer = TensorLayer(inputShape).apply {
        label = "Input (100×100×3)"; isClamped = true
    }
    inputTensorLayer.setLocation(leftX, topY)

    val conv1OutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 4)
    val conv1Out = TensorLayer(conv1OutShape).apply {
        label = "Conv1 ($conv1OutShape)"; activationFunction = TensorActivation.RELU
    }
    conv1Out.setLocation(leftX, topY + stepY)
    val conv1 = ConvolutionConnector(inputTensorLayer, conv1Out, kernelSize = 3, numFilters = 4, stride = 1, padding = Padding.SAME)

    val pool1OutShape = conv1OutShape.poolOutputShape(5, 5)
    val poolLayer1 = TensorLayer(pool1OutShape).apply { label = "Pool1 ($pool1OutShape)" }
    poolLayer1.setLocation(leftX, topY + stepY * 2)
    val pool1 = PoolingConnector(conv1Out, poolLayer1, poolSize = 5, stride = 5, poolingType = PoolingType.MAX)

    val conv2OutShape = pool1OutShape.convOutputShape(3, 1, Padding.SAME, 8)
    val conv2Out = TensorLayer(conv2OutShape).apply {
        label = "Conv2 ($conv2OutShape)"; activationFunction = TensorActivation.RELU
    }
    conv2Out.setLocation(leftX, topY + stepY * 3)
    val conv2 = ConvolutionConnector(poolLayer1, conv2Out, kernelSize = 3, numFilters = 8, stride = 1, padding = Padding.SAME)

    val pool2OutShape = conv2OutShape.poolOutputShape(4, 4)
    val poolLayer2 = TensorLayer(pool2OutShape).apply { label = "Pool2 ($pool2OutShape)" }
    poolLayer2.setLocation(rightX, topY + stepY * 3)
    val pool2 = PoolingConnector(conv2Out, poolLayer2, poolSize = 4, stride = 4, poolingType = PoolingType.MAX)

    val flatSize = pool2OutShape.size
    val flatArray = NeuronArray(flatSize).apply { label = "Flatten ($flatSize)" }
    flatArray.setLocation(rightX, topY + stepY * 2)
    val flatten = FlattenConnector(poolLayer2, flatArray)

    val outputArray = NeuronArray(numClasses).apply {
        label = "Output ($numClasses classes)"
        updateRule = SoftmaxRule()
        circleMode = true
        labelArray = categoryNames.toTypedArray()
    }
    outputArray.setLocation(rightX, topY)
    WeightMatrix(flatArray, outputArray)

    val component = addImageWorld("Image World")
    placeComponent(component, 591, 0, 360, 300)
    val imageWorld = component.world
    imageWorld.imagePipelineCollection.addPipeline("RGB 100×100") {
        addOperation(ResizeOperation(100, 100))
    }

    val allFiles: List<File> = categoryNames.flatMap { categoryImages.getValue(it) }.shuffled()
    imageWorld.loadImages(allFiles.toTypedArray())
    imageWorld.setCurrentPipeline("RGB 100×100")

    val categoryIndices: Map<String, Int> = categoryNames.withIndex().associate { it.value to it.index }

    val inputs  = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<MutableList<Double>>()

    // Map each file back to its category for one-hot encoding.
    // For Caltech101 the category is the parent directory name;
    // for the bundled fallback it is the filename prefix.
    fun categoryOf(file: File): String =
        file.parentFile?.name?.takeIf { it in categoryIndices }
            ?: file.nameWithoutExtension.takeWhile { !it.isDigit() }

    allFiles.forEachIndexed { idx, imageFile ->
        imageWorld.imageAlbum.setFrame(idx)
        workspace.simpleIterate()

        inputs.add(imageWorld.imagePipelineCollection.currentPipeline.rgbActivations.toMutableList())

        val catIdx = categoryIndices[categoryOf(imageFile)] ?: 0
        targets.add(MutableList(numClasses) { if (it == catIdx) 1.0 else 0.0 })
    }

    val (training, testing) = splitDataSet(inputs, targets, 0.8)
    val (trainingInputs, trainingTargets) = training
    val (testingInputs,  testingTargets)  = testing

    val trainingSet = TrainingDataset(trainingInputs, trainingTargets, inputShape.size, numClasses)
    val testingSet  = TrainingDataset(testingInputs,  testingTargets,  inputShape.size, numClasses)

    val cnnModel = network.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray) {
        label = "Object detection network"
        this.trainingSet = trainingSet
        this.testingSet  = testingSet
    }
    cnnModel.trainerConfig.apply {
        learningRate = 0.001
        batchSize    = 16
        lossFunction = CnnLossFunction.CrossEntropy
        computeAccuracy = true
        testConfiguration.enabled       = true
        testConfiguration.testFrequency = 10
    }

    with(couplingManager) {
        imageWorld.imagePipelineCollection.currentPipeline.let { pipeline ->
            createCoupling(
                pipeline.getProducer(pipeline::rgbActivations),
                inputTensorLayer.getConsumer(inputTensorLayer::setActivations)
            )
        }
    }

    inputTensorLayer.rgbComposite = true

    workspace.iterateSuspend(1)

    place(networkComponent, 0, 0, 600, 800)

    addSidebarInfo(
        """
        # CNN Object Detector

        A convolutional neural network trained on [Caltech101](https://data.caltech.edu/records/mzrjq-6wc02)
        images using full RGB color.

        The dataset is downloaded on first run (~137 MB) and cached locally.
        Subsequent runs load from cache instantly.

        ## Configuration

        On startup a dialog lets you choose which categories to train on and
        how many images per category (default 50). Select at least 2 categories.

        ## CNN Architecture

        - **Input**: 100×100×3 RGB (30,000 values)
        - **Conv1**: 3×3, 4 filters, SAME, ReLU → 100×100×4
        - **Pool1**: 5×5 max pooling, stride 5 → 20×20×4
        - **Conv2**: 3×3, 8 filters, SAME, ReLU → 20×20×8
        - **Pool2**: 4×4 max pooling, stride 4 → 5×5×8 = 200
        - **Flatten**: 200
        - **Dense**: 200 → N classes (softmax + cross-entropy)

        ## How to Use

        1. Right-click the **CNN Photo Album** outline → **Train...**
        2. Click **Run** to train; watch the loss and accuracy improve
        3. After training, close the dialog and click **Run** in the main workspace
        4. Use the left/right arrow buttons in the Image World to cycle through images
        5. Watch the output neurons update in real-time as different images are shown

        ## Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        """.trimIndent()
    )
}
