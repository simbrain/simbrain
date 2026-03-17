package org.simbrain.custom_sims.simulations.psychology

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.*
import org.simbrain.network.trainers.CnnLossFunction
import org.simbrain.network.trainers.ShapeType
import org.simbrain.network.trainers.createShapeDataset
import org.simbrain.network.trainers.splitDataSet
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.place
import org.simbrain.util.swingInvokeLater
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

/** Size of the centered prototype target shape. */
const val TARGET_SIZE = 10.0

// ─────────────────────────────────────────────────────────────────────────────

val categoricalPerception = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Categorical Perception")
    val network = networkComponent.network

    // --- Dataset ---
    // Inputs:  50×50 binary image, shape at random position and size [INPUT_MIN_SIZE, INPUT_MAX_SIZE]
    // Targets: 50×50 binary image, same shape type centered at fixed TARGET_SIZE
    val fullDataset = createShapeDataset(
        height = 50,
        width = 50,
        samplesPerClass = SAMPLES_PER_CLASS,
        minSize = INPUT_MIN_SIZE,
        maxSize = INPUT_MAX_SIZE,
        targetSize = TARGET_SIZE,
        rngSeed = 42L
    )
    val (trainingSet, testingSet) = splitDataSet(fullDataset, splitRatio = 0.8)

    // The training data is ordered: all CIRCLE samples, then ELLIPSE, SQUARE, RECTANGLE.
    // We track the index ranges so the analysis can group activations by class.
    val trainSamplesPerClass = trainingSet.inputs.size / ShapeType.entries.size

    // --- CNN Pipeline ---
    // Input(50×50×1) → Conv1(3×3, 8 filters, SAME, ReLU) → Pool1(2×2)
    //               → Conv2(3×3, 16 filters, SAME, ReLU) → Pool2(2×2)
    //               → Flatten → Bottleneck(BOTTLENECK_SIZE, sigmoid) → Output(2500)

    val inputShape = TensorShape(50, 50, 1)

    val inputLayer = TensorLayer(inputShape).apply {
        label = "Input (50×50×1)"
        isClamped = true
        setLocation(-515.9, 282.1)
    }

    val conv1OutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 8)
    val conv1Out = TensorLayer(conv1OutShape).apply {
        label = "Conv1 (${conv1OutShape})"
        activationFunction = TensorActivation.RELU
        setLocation(-84.1, 278.6)
    }
    ConvolutionConnector(inputLayer, conv1Out, kernelSize = 3, numFilters = 8, stride = 1, padding = Padding.SAME)

    val pool1OutShape = conv1OutShape.poolOutputShape(2, 2)
    val pool1Out = TensorLayer(pool1OutShape).apply {
        label = "Pool1 (${pool1OutShape})"
        setLocation(254.7, 276.7)
    }
    PoolingConnector(conv1Out, pool1Out, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

    val conv2OutShape = pool1OutShape.convOutputShape(3, 1, Padding.SAME, 16)
    val conv2Out = TensorLayer(conv2OutShape).apply {
        label = "Conv2 (${conv2OutShape})"
        activationFunction = TensorActivation.RELU
        setLocation(422.0, -214.9)
    }
    ConvolutionConnector(pool1Out, conv2Out, kernelSize = 3, numFilters = 16, stride = 1, padding = Padding.SAME)

    val pool2OutShape = conv2OutShape.poolOutputShape(2, 2)
    val pool2Out = TensorLayer(pool2OutShape).apply {
        label = "Pool2 (${pool2OutShape})"
        setLocation(78.0, -254.4)
    }
    PoolingConnector(conv2Out, pool2Out, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

    val flatSize = pool2OutShape.size
    val flatArray = NeuronArray(flatSize).apply {
        label = "Flatten ($flatSize)"
        setLocation(-164.2, -255.6)
    }
    FlattenConnector(pool2Out, flatArray)

    val bottleneck = NeuronArray(BOTTLENECK_SIZE).apply {
        label = "Bottleneck ($BOTTLENECK_SIZE)"
        updateRule = SigmoidalRule()
        setLocation(-520.8, -280.2)
    }
    WeightMatrix(flatArray, bottleneck)

    val outputArray = NeuronArray(2500).apply {
        label = "Output (50×50)"
        setLocation(-530.0, -6.5)
        gridMode = true
    }
    WeightMatrix(bottleneck, outputArray)

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

    /**
     * Runs all training inputs through the network and collects bottleneck activations,
     * grouped by shape class (in dataset order: CIRCLE, ELLIPSE, SQUARE, RECTANGLE).
     */
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

    /**
     * Computes average pairwise Euclidean distance within a list of activation vectors.
     */
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

    /**
     * Computes average pairwise Euclidean distance between two lists of activation vectors.
     */
    fun betweenClassDistance(a: List<DoubleArray>, b: List<DoubleArray>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        var total = 0.0
        for (va in a) for (vb in b) total += euclidean(va, vb)
        return total / (a.size * b.size)
    }

    /**
     * Runs the full categoricity analysis and returns a formatted results string.
     * For each shape: within-class distance and average between-class distance to all others.
     * Also computes the categoricity ratio: within / mean(between).
     */
    fun runAnalysis(): String {
        val activations = collectBottleneckActivations()
        val sb = StringBuilder()
        sb.appendLine("=== Categoricity Analysis ===")
        sb.appendLine("Bottleneck size: $BOTTLENECK_SIZE  |  Samples/class: $trainSamplesPerClass")
        sb.appendLine()

        val types = ShapeType.entries
        val withinDists = types.associateWith { withinClassDistance(activations[it]!!) }

        // Between-class distances for each pair
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
        place(networkComponent, 300, 0, 700, 900)

        createControlPanel("Analysis", 0, 0) {
            addLabel("<html><b>Categorical Perception</b></html>")
            addSeparator()

            val resultsLabel = addLabel("<html><i>Press Analyze to run</i></html>")

            addButton("Analyze Bottleneck") {
                swingInvokeLater { resultsLabel.text = "<html><i>Running...</i></html>" }
                val result = runAnalysis()
                val html = "<html><pre style='font-size:10px'>" +
                        result.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>") +
                        "</pre></html>"
                swingInvokeLater {
                    resultsLabel.text = html
                    pack()
                }
                println(result)
            }

            addSeparator()
            addLabel("<html><small>Results also printed to console</small></html>")
        }
    }

    addSidebarInfo("""
        # Categorical Perception

        A CNN trained to map shapes at arbitrary positions and sizes to a canonical
        centered prototype — modeling the perceptual compression underlying categorical
        perception.

        ## Architecture

        ```
        Input  (50×50×1)
          ↓  Conv1  3×3, 8 filters, SAME, ReLU  → ${conv1OutShape}
          ↓  Pool1  2×2 max                      → ${pool1OutShape}
          ↓  Conv2  3×3, 16 filters, SAME, ReLU → ${conv2OutShape}
          ↓  Pool2  2×2 max                      → ${pool2OutShape}
          ↓  Flatten                             → $flatSize
          ↓  Bottleneck  $BOTTLENECK_SIZE units, sigmoid   ← analysis target
          ↓  Output      2500 units (50×50)
        ```

        ## Training Data

        - **Input**: circle, ellipse, square, or rectangle at a random position,
          size uniformly drawn from [$INPUT_MIN_SIZE, $INPUT_MAX_SIZE]
        - **Target**: same shape type, centered, fixed size $TARGET_SIZE
        - **Samples**: $SAMPLES_PER_CLASS per class × 4 classes = ${SAMPLES_PER_CLASS * 4} total
          (80% train / 20% test)

        ## What to Do

        1. Right-click the **Categorical Perception CNN** outline → **Train...**
        2. Click **Run** and watch the SSE loss decrease
        3. Click **Analyze Bottleneck** in the control panel before and after training
           to see how categorical structure emerges

        ## Analysis Metrics

        The **Analyze Bottleneck** button runs all training inputs through the network,
        collects the $BOTTLENECK_SIZE-unit bottleneck activations, and computes:

        - **Within-class distance**: mean pairwise Euclidean distance among activations
          of the same shape type — lower means more compact clusters
        - **Between-class distance**: mean pairwise distance across shape types —
          higher means better separation
        - **Categoricity ratio**: within / mean(between) — values below 1.0 indicate
          good categorical separation
    """.trimIndent())
}
