package org.simbrain.custom_sims.simulations.psychology

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.*
import org.simbrain.network.trainers.*
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.place
import org.simbrain.util.showMessageDialog
import kotlin.math.sqrt

// ── Tunable parameters ────────────────────────────────────────────────────────

/** Number of units in the bottleneck layer. Central to the categoricity analysis. */
const val BOTTLENECK_SIZE = 5

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

// ─────────────────────────────────────────────────────────────────────────────

/**
 * The three training conditions for the categorical perception experiment.
 *
 * - NO_LABELS: output = prototype image only; all 7 label units are 0
 * - L1_LABELS: output = prototype image + one-hot L1 label (4 units) + 3 zeros
 * - L2_LABELS: output = prototype image + one-hot L1 label (4 units) + one-hot L2 label (3 units)
 */
enum class TrainingCondition(val displayName: String) {
    NO_LABELS("Prototype sorting (no labels)"),
    L1_LABELS("Prototype sorting (L1 labels)"),
    L2_LABELS("Prototype sorting (L2 labels)");

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
 * L2 label indices within the 7-unit label block (positions 4–6 of the label block).
 * Superordinate categories:
 *   index 4 = "round"   (CIRCLE, ELLIPSE)
 *   index 5 = "angular" (SQUARE, RECTANGLE)
 *   index 6 = unused (always 0)
 */
private val L2_INDEX = mapOf(
    ShapeType.CIRCLE    to 4,
    ShapeType.ELLIPSE   to 4,
    ShapeType.SQUARE    to 5,
    ShapeType.RECTANGLE to 5
)

/**
 * Builds a label vector of length [NUM_LABEL_UNITS] for a given shape and condition.
 *
 * - NO_LABELS:  all zeros
 * - L1_LABELS:  one-hot at the L1 index for this shape; L2 units = 0
 * - L2_LABELS:  one-hot at the L1 index + one-hot at the L2 index
 */
private fun buildLabelVector(shape: ShapeType, condition: TrainingCondition): DoubleArray {
    val labels = DoubleArray(NUM_LABEL_UNITS)
    when (condition) {
        TrainingCondition.NO_LABELS -> { /* all zeros */ }
        TrainingCondition.L1_LABELS -> {
            labels[L1_INDEX[shape]!!] = 1.0
        }
        TrainingCondition.L2_LABELS -> {
            labels[L1_INDEX[shape]!!] = 1.0
            labels[L2_INDEX[shape]!!] = 1.0
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

    // --- Base Dataset (no labels) ---
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

    // --- CNN Pipeline ---
    // Input(50×50×1) → Conv1(3×3, 8 filters, SAME, ReLU) → Pool1(2×2)
    //               → Conv2(3×3, 16 filters, SAME, ReLU) → Pool2(2×2)
    //               → Flatten → Bottleneck(BOTTLENECK_SIZE, sigmoid) → Output(2507)
    //                                                                         ↑ 49 prototype (7×7) + 7 labels

    val inputShape = TensorShape(50, 50, 1)

    val inputLayer = TensorLayer(inputShape).apply {
        label = "Input (50×50×1)"
        isClamped = true
        setLocation(-586.0, 160.0)
    }

    val conv1OutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 8)
    val conv1Out = TensorLayer(conv1OutShape).apply {
        label = "Conv1 (${conv1OutShape})"
        activationFunction = TensorActivation.RELU
        setLocation(107.0, -154.0)
    }
    ConvolutionConnector(inputLayer, conv1Out, kernelSize = 3, numFilters = 8, stride = 1, padding = Padding.SAME)

    val pool1OutShape = conv1OutShape.poolOutputShape(2, 2)
    val pool1Out = TensorLayer(pool1OutShape).apply {
        label = "Pool1 (${pool1OutShape})"
        setLocation(918.0, 160.0)
    }
    PoolingConnector(conv1Out, pool1Out, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

    val conv2OutShape = pool1OutShape.convOutputShape(3, 1, Padding.SAME, 16)
    val conv2Out = TensorLayer(conv2OutShape).apply {
        label = "Conv2 (${conv2OutShape})"
        activationFunction = TensorActivation.RELU
        setLocation(918.0, -992.0)
    }
    ConvolutionConnector(pool1Out, conv2Out, kernelSize = 3, numFilters = 16, stride = 1, padding = Padding.SAME)

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

    val bottleneck = NeuronArray(BOTTLENECK_SIZE).apply {
        label = "Bottleneck ($BOTTLENECK_SIZE)"
        updateRule = SigmoidalRule()
        setLocation(-586.0, -992.0)
    }
    WeightMatrix(flatArray, bottleneck)

    // Output: 49 prototype pixels (7×7) + 7 label units
    val protoSize = TARGET_GRID * TARGET_GRID
    val outputArray = NeuronArray(protoSize + NUM_LABEL_UNITS).apply {
        label = "Output (7×7 + 7 labels)"
        setLocation(-586.0, -358.0)
        gridMode = true
    }
    WeightMatrix(bottleneck, outputArray)

    // --- Output windows (driven by listener, no weight matrices needed) ---
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

    outputArray.events.updated.on {
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

    // --- Analysis functions ---

    fun euclidean(a: DoubleArray, b: DoubleArray): Double {
        var sum = 0.0
        for (i in a.indices) { val d = a[i] - b[i]; sum += d * d }
        return sqrt(sum)
    }

    fun collectBottleneckActivations(): Map<ShapeType, List<DoubleArray>> {
        val result = mutableMapOf<ShapeType, MutableList<DoubleArray>>()
        ShapeType.entries.forEach { result[it] = mutableListOf() }

        trainingSet.inputs.forEachIndexed { idx, input ->
            val classIndex = idx / trainSamplesPerClass
            val shapeType = ShapeType.entries.getOrNull(classIndex) ?: return@forEachIndexed
            inputLayer.activations = input.toDoubleArray()
            network.update()
            result[shapeType]!!.add(bottleneck.activationArray.copyOf())
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
        val activations = collectBottleneckActivations()
        val sb = StringBuilder()
        sb.appendLine("=== Categoricity Analysis ===")
        sb.appendLine("Condition: ${currentCondition.displayName}")
        sb.appendLine("Bottleneck size: $BOTTLENECK_SIZE  |  Samples/class: $trainSamplesPerClass")
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

    // --- Control panel ---

    withGui {
        place(networkComponent, 300, 0, 940, 677)

        createControlPanel("Analysis", 0, 0) {
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

            addButton("Analyze Bottleneck") {
                val result = runAnalysis()
                showMessageDialog(result, "Bottleneck Analysis")
            }
        }
    }

    addSidebarInfo("""
        # Categorical Perception

        A CNN trained to map shapes at arbitrary positions and sizes to a canonical
        centered prototype — modeling the perceptual compression underlying categorical
        perception. The network can be trained under three conditions that differ in
        what linguistic label information is included in the output targets.

        ## Architecture

        ```
        Input  (50×50×1)
          ↓  Conv1  3×3, 8 filters, SAME, ReLU  → ${conv1OutShape}
          ↓  Pool1  2×2 max                      → ${pool1OutShape}
          ↓  Conv2  3×3, 16 filters, SAME, ReLU → ${conv2OutShape}
          ↓  Pool2  2×2 max                      → ${pool2OutShape}
          ↓  Flatten                             → $flatSize
          ↓  Bottleneck  $BOTTLENECK_SIZE units, sigmoid   ← analysis target
          ↓  Output      ${TARGET_GRID * TARGET_GRID + NUM_LABEL_UNITS} units (${TARGET_GRID}×${TARGET_GRID} prototype pixels + 7 labels)
        ```

        ## Training Data

        - **Input**: circle, ellipse, square, or rectangle at a random position,
          size uniformly drawn from [$INPUT_MIN_SIZE, $INPUT_MAX_SIZE]
        - **Target**: same shape type, centered on a ${TARGET_GRID}×${TARGET_GRID} grid at fixed size $TARGET_SIZE,
          followed by $NUM_LABEL_UNITS label units (see Training Conditions below)
        - **Samples**: $SAMPLES_PER_CLASS per class × 4 classes = ${SAMPLES_PER_CLASS * 4} total
          (80% train / 20% test)

        ## Training Conditions

        Select a condition from the dropdown in the control panel before training.
        Each condition changes what the network must learn to produce in the 7 label
        units appended to the 2500-pixel prototype output.

        ### Prototype sorting (no labels)
        All 7 label units are set to 0. The network only learns to produce the
        centered prototype image. This is the baseline condition with no linguistic
        influence.

        ### Prototype sorting (L1 labels)
        A one-hot L1 (basic-level) label is appended. The 7 label units are:

        ```
        [CIRCLE, ELLIPSE, SQUARE, RECTANGLE, 0, 0, 0]
        ```

        For example, a circle input has label units `[1, 0, 0, 0, 0, 0, 0]`.
        The network must learn both the prototype image and which of the four
        basic-level shape categories the input belongs to.

        ### Prototype sorting (L2 labels)
        Both L1 and L2 (superordinate) labels are appended. The 7 label units are:

        ```
        [CIRCLE, ELLIPSE, SQUARE, RECTANGLE, round, angular, 0]
        ```

        L2 superordinate categories:
        - **round** (unit 5): active for CIRCLE and ELLIPSE
        - **angular** (unit 6): active for SQUARE and RECTANGLE

        For example, a circle input has label units `[1, 0, 0, 0, 1, 0, 0]`.
        The network must learn the prototype image, the basic-level category,
        and the superordinate category simultaneously.

        This structure mirrors the notebook
        `CP_network_PT_2500.ipynb` (Shan & Yoshimi), where the output tensor
        is `[prototype (2500) | L1 labels (4) | L2 labels (3)]`.

        ## What to Do

        1. Select a **Training Condition** from the dropdown in the control panel
        2. Right-click the **Categorical Perception CNN** outline → **Train...**
        3. Click **Run** and watch the SSE loss decrease
        4. Click **Analyze Bottleneck** to measure categorical structure in the
           bottleneck layer
        5. Repeat steps 1–4 for each condition to compare how label training
           affects the geometry of the bottleneck representations

        ## Analysis Metrics

        The **Analyze Bottleneck** button runs all training inputs through the network,
        collects the $BOTTLENECK_SIZE-unit bottleneck activations, and computes:

        - **Within-class distance**: mean pairwise Euclidean distance among activations
          of the same shape type — lower means more compact clusters
        - **Between-class distance**: mean pairwise distance across shape types —
          higher means better separation
        - **Categoricity ratio**: within / mean(between) — values below 1.0 indicate
          good categorical separation

        The key hypothesis is that label training (especially L2) will produce more
        categorical bottleneck representations — lower within-class distances and
        higher between-class distances — compared to the no-label baseline.
    """.trimIndent())
}
