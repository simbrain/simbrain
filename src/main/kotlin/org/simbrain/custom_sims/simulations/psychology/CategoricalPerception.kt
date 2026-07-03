package org.simbrain.custom_sims.simulations.psychology

import kotlinx.coroutines.Dispatchers
import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.network.trainers.*
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.place
import org.simbrain.util.runWithProgressWindow
import org.simbrain.util.showMessageDialog
import kotlin.math.sqrt

// Tunable parameters

/**
 * Size of the dense representation layer — the primary analysis target.
 * Sits between the CNN flatten output and the output layer, receiving gradients
 * from both prototype reconstruction and label prediction.
 */
const val REPR_LAYER_SIZE = 16

/** Epochs for stage 1 (prototype sorting only). */
const val STAGE1_EPOCHS = 500

/** Epochs for stage 2 (add L1 labels). */
const val STAGE2_EPOCHS = 200

/** Samples generated per shape class (circle, ellipse, square, rectangle). */
const val SAMPLES_PER_CLASS = 50

/** Minimum shape size (radius / half-side) for randomly-placed input shapes. */
const val INPUT_MIN_SIZE = 5.0

/** Maximum shape size (radius / half-side) for randomly-placed input shapes. */
const val INPUT_MAX_SIZE = 25.0

/** Height and width of the target prototype grid (7×7 = 49 units). */
const val TARGET_GRID = 7

/** Size of the centered prototype target shape (radius / half-side in target grid pixels). */
const val TARGET_SIZE = 2.0

/** Number of label units appended to each output target (4 L1 + 3 L2). */
const val NUM_LABEL_UNITS = 7

/**
 * Training conditions for the categorical perception experiment.
 *
 * - NO_LABELS: output = prototype image only; all label units are 0
 * - L1_LABELS: output = prototype image + one-hot L1 label (4 units) + 3 zeros
 */
enum class TrainingCondition(val displayName: String) {
    NO_LABELS("Prototype sorting (no labels)"),
    L1_LABELS("Prototype sorting (L1 labels)");

    override fun toString() = displayName
}

/**
 * L1 label indices within the 7-unit label block (positions 0–3 of the label block).
 * Order matches ShapeType.entries: CIRCLE=0, ELLIPSE=1, SQUARE=2, RECTANGLE=3.
 */
private val L1_INDEX = mapOf(
    ShapeType.CIRCLE    to 0,
    ShapeType.ELLIPSE   to 1,
    ShapeType.SQUARE    to 2,
    ShapeType.RECTANGLE to 3
)

/**
 * Builds a label vector of length [NUM_LABEL_UNITS] for a given shape and condition.
 *
 * - NO_LABELS:  all zeros
 * - L1_LABELS:  one-hot at the L1 index for this shape; remaining units = 0
 */
private fun buildLabelVector(shape: ShapeType, condition: TrainingCondition): DoubleArray {
    val labels = DoubleArray(NUM_LABEL_UNITS)
    when (condition) {
        TrainingCondition.NO_LABELS -> { /* all zeros */ }
        TrainingCondition.L1_LABELS -> {
            labels[L1_INDEX[shape]!!] = 1.0
        }
    }
    return labels
}

/**
 * Appends label vectors to an existing [TrainingDataset].
 * The prototype image targets are kept; [NUM_LABEL_UNITS] label units are appended.
 * Dataset order is assumed to be: all CIRCLE samples, then ELLIPSE, SQUARE, RECTANGLE.
 */
private fun TrainingDataset.withLabels(condition: TrainingCondition): TrainingDataset {
    val samplesPerClass = inputs.size / ShapeType.entries.size
    val newTargets = targets.mapIndexed { idx, target ->
        val classIndex = idx / samplesPerClass
        val shape = ShapeType.entries.getOrElse(classIndex) { ShapeType.CIRCLE }
        val labelVec = buildLabelVector(shape, condition)
        (target + labelVec.toList()).toMutableList()
    }.toMutableList()
    return TrainingDataset(
        inputs = inputs.toMutableList(),
        targets = newTargets,
        inputSize = inputSize,
        targetSize = targetSize + NUM_LABEL_UNITS
    )
}

val categoricalPerception = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Categorical Perception")
    val network = networkComponent.network

    // Base Dataset (no labels)
    // Input: 50×50 binary image; Target: 7×7 binary prototype image (no padding)
    val baseDataset = createShapeDataset(
        height = 50,
        width = 50,
        targetHeight = TARGET_GRID,
        targetWidth = TARGET_GRID,
        samplesPerClass = SAMPLES_PER_CLASS,
        minSize = INPUT_MIN_SIZE,
        maxSize = INPUT_MAX_SIZE,
        targetSize = TARGET_SIZE,
        rngSeed = 42L
    )

    var currentCondition = TrainingCondition.NO_LABELS

    fun buildDatasets(condition: TrainingCondition): Pair<TrainingDataset, TrainingDataset> {
        val full = baseDataset.withLabels(condition)
        return splitDataSet(full, splitRatio = 0.8)
    }

    var (trainingSet, testingSet) = buildDatasets(currentCondition)

    val trainSamplesPerClass = trainingSet.inputs.size / ShapeType.entries.size

    // CNN Pipeline
    // Input(50×50×1) → Conv1(3×3, 4 filters, SAME, ReLU) → Pool1(2×2)
    //               → Conv2(3×3, 8 filters, SAME, ReLU) → Pool2(2×2)
    //               → Flatten → ReprLayer(REPR_LAYER_SIZE, sigmoid) ← analysis target
    //               → Output(56)  [49 prototype (7×7) + 7 labels]

    val inputShape = TensorShape(50, 50, 1)

    val inputLayer = TensorLayer(inputShape).apply {
        label = "Input (50×50×1)"
        isClamped = true
        setLocation(-586.0, 160.0)
    }

    val conv1OutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 4)
    val conv1Out = TensorLayer(conv1OutShape).apply {
        label = "Conv1 (${conv1OutShape})"
        activationFunction = TensorActivation.RELU
        setLocation(107.0, -154.0)
    }
    ConvolutionConnector(inputLayer, conv1Out, kernelSize = 3, numFilters = 4, stride = 1, padding = Padding.SAME)

    val pool1OutShape = conv1OutShape.poolOutputShape(2, 2)
    val pool1Out = TensorLayer(pool1OutShape).apply {
        label = "Pool1 (${pool1OutShape})"
        setLocation(918.0, 160.0)
    }
    PoolingConnector(conv1Out, pool1Out, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

    val conv2OutShape = pool1OutShape.convOutputShape(3, 1, Padding.SAME, 8)
    val conv2Out = TensorLayer(conv2OutShape).apply {
        label = "Conv2 (${conv2OutShape})"
        activationFunction = TensorActivation.RELU
        setLocation(918.0, -992.0)
    }
    ConvolutionConnector(pool1Out, conv2Out, kernelSize = 3, numFilters = 8, stride = 1, padding = Padding.SAME)

    val pool2OutShape = conv2OutShape.poolOutputShape(2, 2)
    val pool2Out = TensorLayer(pool2OutShape).apply {
        label = "Pool2 (${pool2OutShape})"
        setLocation(107.0, -992.0)
    }
    PoolingConnector(conv2Out, pool2Out, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

    val flatSize = pool2OutShape.size
    val flatArray = NeuronArray(flatSize).apply {
        label = "Flatten ($flatSize)"
        setLocation(107.0, -552.0)
    }
    FlattenConnector(pool2Out, flatArray)

    // Primary analysis target: dense representation layer receiving gradients from
    // both prototype reconstruction and label prediction.
    val reprLayer = NeuronArray(REPR_LAYER_SIZE).apply {
        label = "Repr ($REPR_LAYER_SIZE, sigmoid)"
        updateRule = SigmoidalRule()
        setLocation(-586.0, -992.0)
    }
    WeightMatrix(flatArray, reprLayer)

    // Output: 49 prototype pixels (7×7) + 7 label units
    val protoSize = TARGET_GRID * TARGET_GRID
    val outputArray = NeuronArray(protoSize + NUM_LABEL_UNITS).apply {
        label = "Output (7×7 + 7 labels)"
        setLocation(-586.0, -358.0)
        gridMode = true
    }
    WeightMatrix(reprLayer, outputArray)

    // Output windows (driven by listener, no weight matrices needed)
    val imageView = NeuronArray(protoSize).apply {
        label = "Image (7×7)"
        gridMode = true
        setLocation(-1228.0, -387.0)
    }
    network.addNetworkModelAsync(imageView, usePlacementManager = false)

    val labelView = NeuronArray(NUM_LABEL_UNITS).apply {
        label = "Labels (7×1)"
        circleMode = true
        setLocation(-1237.0, -152.0)
    }
    network.addNetworkModelAsync(labelView, usePlacementManager = false)

    outputArray.events.updated.on(Dispatchers.Default) {
        val out = outputArray.activationArray
        imageView.activationArray = out.copyOfRange(0, protoSize)
        labelView.activationArray = out.copyOfRange(protoSize, protoSize + NUM_LABEL_UNITS)
    }

    val cnnModel = network.addConvolutionalNeuralNetwork(inputLayer, outputArray) {
        label = "Categorical Perception CNN"
        this.trainingSet = trainingSet
        this.testingSet = testingSet
    }
    cnnModel.trainerConfig.apply {
        learningRate = 0.001
        batchSize = 32
        lossFunction = CnnLossFunction.SSE
        testConfiguration.enabled = true
        testConfiguration.testFrequency = 10
    }

    inputLayer.activations = trainingSet.inputs[0].toDoubleArray()

    // Analysis functions

    fun euclidean(a: DoubleArray, b: DoubleArray): Double {
        var sum = 0.0
        for (i in a.indices) { val d = a[i] - b[i]; sum += d * d }
        return sqrt(sum)
    }

    fun collectReprActivations(): Map<ShapeType, List<DoubleArray>> {
        val result = mutableMapOf<ShapeType, MutableList<DoubleArray>>()
        ShapeType.entries.forEach { result[it] = mutableListOf() }

        trainingSet.inputs.forEachIndexed { idx, input ->
            val classIndex = idx / trainSamplesPerClass
            val shapeType = ShapeType.entries.getOrNull(classIndex) ?: return@forEachIndexed
            inputLayer.activations = input.toDoubleArray()
            network.update()
            result[shapeType]!!.add(reprLayer.activationArray.copyOf())
        }
        return result
    }

    fun withinClassDistance(activations: List<DoubleArray>): Double {
        if (activations.size < 2) return 0.0
        var total = 0.0
        var count = 0
        for (i in activations.indices) {
            for (j in i + 1 until activations.size) {
                total += euclidean(activations[i], activations[j])
                count++
            }
        }
        return if (count > 0) total / count else 0.0
    }

    fun betweenClassDistance(a: List<DoubleArray>, b: List<DoubleArray>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        var total = 0.0
        for (va in a) for (vb in b) total += euclidean(va, vb)
        return total / (a.size * b.size)
    }

    fun runAnalysis(): String {
        val activations = collectReprActivations()
        val sb = StringBuilder()
        sb.appendLine("=== Categoricity Analysis ===")
        sb.appendLine("Condition: ${currentCondition.displayName}")
        sb.appendLine("Repr layer size: $REPR_LAYER_SIZE  |  Samples/class: $trainSamplesPerClass")
        sb.appendLine()

        val types = ShapeType.entries
        val withinDists = types.associateWith { withinClassDistance(activations[it]!!) }

        val betweenDists = mutableMapOf<Pair<ShapeType, ShapeType>, Double>()
        for (i in types.indices) {
            for (j in i + 1 until types.size) {
                val d = betweenClassDistance(activations[types[i]]!!, activations[types[j]]!!)
                betweenDists[types[i] to types[j]] = d
                betweenDists[types[j] to types[i]] = d
            }
        }

        sb.appendLine("Within-class distances (lower = more compact):")
        types.forEach { t ->
            sb.appendLine("  %-12s  %.4f".format(t.name, withinDists[t]))
        }
        sb.appendLine()

        sb.appendLine("Between-class distances (higher = more separated):")
        for (i in types.indices) {
            for (j in i + 1 until types.size) {
                val key = types[i] to types[j]
                sb.appendLine("  %-12s ↔ %-12s  %.4f".format(types[i].name, types[j].name, betweenDists[key]))
            }
        }
        sb.appendLine()

        sb.appendLine("Categoricity ratio (within / mean-between):")
        sb.appendLine("  (< 1.0 = good categorical separation)")
        types.forEach { t ->
            val others = types.filter { it != t }
            val meanBetween = others.map { betweenDists[t to it]!! }.average()
            val ratio = if (meanBetween > 0) withinDists[t]!! / meanBetween else Double.NaN
            sb.appendLine("  %-12s  %.4f".format(t.name, ratio))
        }

        return sb.toString()
    }

    // Staged training helper
    // Stage 1: prototype sorting only (no labels). Stage 2: add labels.
    // The key CP result is the *change* in repr-layer geometry between stages.

    val stagedTrainer = CnnTrainer(network, inputLayer, outputArray, cnnModel.trainerConfig)

    fun runStagedTraining(labelCondition: TrainingCondition) {
        val totalEpochs = STAGE1_EPOCHS + STAGE2_EPOCHS
        runWithProgressWindow(totalEpochs, "Staged Training") { epoch ->
            if (epoch == 0) {
                val (train, _) = buildDatasets(TrainingCondition.NO_LABELS)
                trainingSet = train
                cnnModel.trainingSet = train
                stagedTrainer.trainingData = train
                currentCondition = TrainingCondition.NO_LABELS
            } else if (epoch == STAGE1_EPOCHS) {
                val (train, _) = buildDatasets(labelCondition)
                trainingSet = train
                cnnModel.trainingSet = train
                stagedTrainer.trainingData = train
                currentCondition = labelCondition
            }
            val batchEnd = minOf(cnnModel.trainerConfig.batchSize, trainingSet.size)
            stagedTrainer.trainBatch(0 until batchEnd)
        }
    }

    // Control panel

    withGui {
        val controlPanel = createControlPanel("Analysis", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addLabel("Training Condition:")
            addComboBox("", TrainingCondition.entries, currentCondition) { selectedCondition ->
                currentCondition = selectedCondition
                val (newTrain, newTest) = buildDatasets(selectedCondition)
                trainingSet = newTrain
                testingSet = newTest
                cnnModel.trainingSet = newTrain
                cnnModel.testingSet = newTest
                inputLayer.activations = newTrain.inputs[0].toDoubleArray()
            }

            addSeparator()

            addButton("Run Staged Training") {
                runStagedTraining(TrainingCondition.L1_LABELS)
            }

            addSeparator()

            addButton("Analyze Repr Layer") {
                val result = runAnalysis()
                showMessageDialog(result, "Repr Layer Analysis")
            }
        }.awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 940, 677)
    }

    addSidebarInfo("""
        # Categorical Perception

        A CNN trained to map shapes at arbitrary positions and sizes to a canonical
        centered prototype. The key question is whether adding category labels during
        training warps the network's internal representations in the way described by
        Cangelosi, Greco & Harnad (1999): within-category compression and
        between-category separation (categorical perception effects).

        # Simulation Details

        The `Repr` layer is the analysis target. It receives gradients from both prototype reconstruction and label
        prediction, making it the layer most likely to show CP warping effects.

        ## Training Data

        - **Input**: circle, ellipse, square, or rectangle at a random position,
          size uniformly drawn from [$INPUT_MIN_SIZE, $INPUT_MAX_SIZE]
        - **Target**: same shape centered on a ${TARGET_GRID}×${TARGET_GRID} grid at fixed size $TARGET_SIZE,
          followed by $NUM_LABEL_UNITS label units (see Training Conditions below)
        - **Samples**: $SAMPLES_PER_CLASS per class × 4 classes = ${SAMPLES_PER_CLASS * 4} total
          (80% train / 20% test)

        ## Training Conditions

        ### Prototype sorting (no labels)
        All label units are 0. Baseline with no linguistic influence.

        ### Prototype sorting (L1 labels)
        One-hot basic-level label appended: `[CIRCLE, ELLIPSE, SQUARE, RECTANGLE, 0, 0, 0]`

        # What to Do

        ## Option A - Staged training
        Click `Run Staged Training` in the control panel. This replicates the
        paper's sequential procedure:

        1. `Stage 1` ($STAGE1_EPOCHS epochs): prototype sorting only - repr layer
           geometry forms around shape similarity alone
        2. Click `Analyze Repr Layer` to record baseline within/between distances
        3. `Stage 2` ($STAGE2_EPOCHS more epochs): label condition added - repr layer
           is reshaped by the additional categorization pressure
        4. Click `Analyze Repr Layer` again and compare to baseline

        The CP effect is the *change*: within-class distances should decrease and
        between-class distances should increase after the label stage.

        ## Option B - Manual staged training
        1. Set condition to `Prototype sorting (no labels)` in the dropdown
        2. Right-click the `CNN` outline and select `Train...`, then run until loss stabilizes
        3. Click `Analyze Repr Layer`; this is your baseline
        4. Switch condition to `L1` or `L2` in the dropdown (weights are preserved)
        5. Train again for a smaller number of epochs
        6. Click `Analyze Repr Layer` and compare to baseline

        ## Analysis Metrics

        `Analyze Repr Layer` runs all training inputs through the network,
        collects the $REPR_LAYER_SIZE-unit repr-layer activations, and computes:

        - **Within-class distance**: mean pairwise Euclidean distance within each
          shape class - lower means more compact clusters
        - **Between-class distance**: mean pairwise distance across shape classes -
          higher means better separation
        - **Categoricity ratio**: within / mean(between) - below 1.0 indicates
          good categorical separation

        The CP hypothesis predicts that label training will lower within-class
        distances and raise between-class distances relative to the no-label baseline.

        # References

        Cangelosi, A., Greco, A., & Harnad, S. (1999). [_From robotic toil to symbolic theft: Grounding transfer from entry-level to higher-level categories_](https://pearl.plymouth.ac.uk/cgi/viewcontent.cgi?article=2719&context=secam-research). _Connection Science_, _12_(2), 143–162.

        # Credits

        Tony Liantao Shan

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

    """.trimIndent())
}
