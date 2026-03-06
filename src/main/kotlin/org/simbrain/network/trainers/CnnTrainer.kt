package org.simbrain.network.trainers

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.simbrain.network.core.*
import org.simbrain.network.events.TrainerEvents
import org.simbrain.network.events.TrainingStats
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.network.updaterules.interfaces.DifferentiableUpdateRule
import org.simbrain.util.flatten
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import smile.math.matrix.Matrix
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

/**
 * Configuration for CNN training.
 */
class CnnTrainerConfig : EditableObject {
    var learningRate by GuiEditable(
        initValue = 0.001,
        description = "Learning rate used by Adam updates.",
        order = 10,
    )
    var beta1 by GuiEditable(
        initValue = 0.9,
        description = "Exponential decay rate for first-moment estimates in Adam.",
        order = 20,
    )
    var beta2 by GuiEditable(
        initValue = 0.999,
        description = "Exponential decay rate for second-moment estimates in Adam.",
        order = 30,
    )
    var batchSize by GuiEditable(
        initValue = 32,
        description = "Mini-batch size used for each training iteration.",
        order = 40,
    )
    var lossFunction: CnnLossFunction by GuiEditable(
        initValue = CnnLossFunction.CrossEntropy,
        description = "Loss used to train the output layer.",
        order = 50,
    )
    var weightInitializationStrategy: WeightInitializationStrategy by GuiEditable(
        initValue = He(),
        description = "Dense-layer weight initialization strategy. He is the default for CNN training.",
        showDetails = false,
        order = 60,
    )
    var stoppingCondition by GuiEditable(
        initValue = SupervisedTrainer.StoppingCondition(),
        description = "Optional automatic stopping conditions used while running.",
        showDetails = false,
        order = 70,
    )
    var testConfiguration by GuiEditable(
        initValue = SupervisedTrainer.TestConfiguration().apply { enabled = false },
        description = "Controls periodic evaluation on testing data.",
        showDetails = false,
        order = 80,
    )
    var computeAccuracy by GuiEditable(
        initValue = false,
        description = "Compute classification accuracy when targets are one-hot encoded.",
        order = 90,
    )
    override val name = "CNN Trainer Config"
}

/**
 * Loss functions operating on DoubleArrays.
 */
sealed class CnnLossFunction(val shortName: String, val description: String) : EditableObject {

    override val name = description

    /** Compute scalar loss. */
    abstract fun loss(actual: DoubleArray, target: DoubleArray): Double

    /** Compute output-layer gradient (dL/d(output)). Written into [grad]. */
    abstract fun outputGrad(actual: DoubleArray, target: DoubleArray, grad: DoubleArray)

    object SSE : CnnLossFunction("SSE", "Sum Squared Error") {
        override fun loss(actual: DoubleArray, target: DoubleArray): Double {
            var sum = 0.0
            for (i in actual.indices) {
                val d = actual[i] - target[i]
                sum += d * d
            }
            return sum
        }

        override fun outputGrad(actual: DoubleArray, target: DoubleArray, grad: DoubleArray) {
            for (i in actual.indices) {
                grad[i] = 2.0 * (actual[i] - target[i])
            }
        }

        override fun toString() = shortName
    }

    object CrossEntropy : CnnLossFunction("Cross Entropy", "Cross Entropy") {
        override fun loss(actual: DoubleArray, target: DoubleArray): Double {
            val prediction = if (actual.isProbabilityVector()) actual else softmax(actual)
            var sum = 0.0
            for (i in target.indices) {
                if (target[i] > 0.0) {
                    sum -= target[i] * ln(max(prediction[i], 1e-12))
                }
            }
            return sum
        }

        override fun outputGrad(actual: DoubleArray, target: DoubleArray, grad: DoubleArray) {
            // For softmax + cross-entropy, gradient is (prediction - target).
            val prediction = if (actual.isProbabilityVector()) actual else softmax(actual)
            for (i in grad.indices) {
                grad[i] = prediction[i] - target[i]
            }
        }

        override fun toString() = shortName
    }

    override fun toString() = description
}

internal fun softmax(x: DoubleArray): DoubleArray {
    val result = DoubleArray(x.size)
    var maxVal = Double.NEGATIVE_INFINITY
    for (v in x) if (v > maxVal) maxVal = v
    var sum = 0.0
    for (i in x.indices) {
        result[i] = exp(x[i] - maxVal)
        sum += result[i]
    }
    for (i in result.indices) result[i] /= sum
    return result
}

private fun DoubleArray.isProbabilityVector(tolerance: Double = 1e-8): Boolean {
    var sum = 0.0
    for (v in this) {
        if (v < -tolerance || v > 1.0 + tolerance) return false
        sum += v
    }
    return abs(sum - 1.0) < 1e-4
}

/**
 * Represents a single dense layer extracted from the network for DoubleArray-based training.
 */
private class DenseLayerSnapshot(
    val inputSize: Int,
    val outputSize: Int,
    val sourceWeightMatrix: WeightMatrix
) {
    val targetLayer: NeuronArray = sourceWeightMatrix.target as NeuronArray
    val weights = DoubleArray(outputSize * inputSize)
    val biases = DoubleArray(outputSize)
    val weightGrads = DoubleArray(outputSize * inputSize)
    val biasGrads = DoubleArray(outputSize)
    val activations = DoubleArray(outputSize)
    val inputCache = DoubleArray(inputSize)
    val weightedInput = DoubleArray(outputSize)
    val ruleInput = DoubleArray(outputSize)
    val zeroBiases = DoubleArray(outputSize)
    private val evalLayer = NeuronArray(outputSize).apply { biases.fill(0.0) }

    fun syncFromNetwork() {
        sourceWeightMatrix.weights.flatten().copyInto(weights)
        val biasArr = targetLayer.biasArray
        biasArr.copyInto(biases, endIndex = minOf(biasArr.size, biases.size))
        evalLayer.updateRule = targetLayer.updateRule.copy()
    }

    fun syncToNetwork() {
        sourceWeightMatrix.setWeights(weights)
        val targetLayer = sourceWeightMatrix.target as ArrayLayer
        targetLayer.biases.let { mat ->
            for (i in 0 until minOf(biases.size, mat.nrow())) {
                mat[i, 0] = biases[i]
            }
        }
    }

    fun clearGrads() {
        weightGrads.fill(0.0)
        biasGrads.fill(0.0)
    }

    context(Network)
    fun applyUpdateRule() {
        if (targetLayer.updateRule is SoftmaxRule) {
            for (i in ruleInput.indices) {
                evalLayer.inputs[i, 0] = weightedInput[i]
                evalLayer.biases[i, 0] = biases[i]
            }
        } else {
            for (i in ruleInput.indices) {
                evalLayer.inputs[i, 0] = ruleInput[i]
                evalLayer.biases[i, 0] = 0.0
            }
        }
        evalLayer.update()
        System.arraycopy(evalLayer.activationArray, 0, activations, 0, outputSize)
    }
}

/**
 * CNN trainer that auto-discovers the pipeline from an input [TensorLayer] through
 * Conv/Pool/Flatten/Dense layers to an output [NeuronArray], and trains using
 * backpropagation with Adam optimizer.
 *
 * The pipeline is: Tensor → [ConvolutionConnector → Tensor → PoolingConnector → Tensor]* →
 * FlattenConnector → NeuronArray → [WeightMatrix → NeuronArray]*
 */
class CnnTrainer(
    val network: Network,
    val inputTensorLayer: TensorLayer,
    val outputArray: NeuronArray,
    val config: CnnTrainerConfig = CnnTrainerConfig()
) : CoroutineScope {
    /**
     * CNN/backprop parity contract:
     * 1) Trainer lifecycle mirrors supervised behavior (queued start/train/stop, iteration reset, training stats events).
     * 2) Dataset and validation semantics are aligned in the dialog layer.
     * 3) Initialization follows trainer-style strategy hooks; He remains the CNN default.
     * 4) CNN-specific differences are explicit: tensor stages use custom conv/pool forward-backward kernels.
     */

    val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Default + job

    val events = TrainerEvents()

    var trainingData: TrainingDataset? = null
    var testingData: TrainingDataset? = null

    var isRunning = false

    var lastTrainingError = 0.0

    var lastTrainingAccuracy: Double? = null

    var lastTestingAccuracy: Double? = null

    private var batchAccuracySum = 0.0
    private var batchSampleCount = 0

    private var stoppingConditionReached = false

    var iteration = 0
        set(value) {
            field = value
            if (value == 0) {
                adam.reset()
                events.iterationReset.fire()
            }
        }

    // Discovered pipeline components (ordered from input to output)
    internal val tensorLayerStages = mutableListOf<TensorLayer>()             // tensors after input (conv/pool outputs)
    internal val tensorConnectors = mutableListOf<TensorConnector>() // conv and pool connectors in order
    internal lateinit var flattenConnector: FlattenConnector
    private val denseLayers = mutableListOf<DenseLayerSnapshot>()

    /** Pre-allocated buffer for flattened tensor activations fed into the dense layers. */
    private lateinit var flattenBuffer: DoubleArray

    // Task queue for sequential processing
    private val processorChannel = Channel<Pair<TrainerTask, CompletableDeferred<Unit>>>(capacity = Channel.UNLIMITED)

    init {
        launch(coroutineContext) {
            for (event in processorChannel) {
                val (task, signal) = event
                try {
                    when (task) {
                        TrainerTask.Start -> startTrainingHandler()
                        TrainerTask.Train -> trainOnceHandler()
                        TrainerTask.Stop -> stopTrainingHandler()
                        TrainerTask.Randomize -> {
                            initParameters()
                            adam.reset()
                        }
                    }
                } catch (e: Exception) {
                    System.err.println("CnnTrainer error during $task: ${e.message}")
                    e.printStackTrace()
                    // If training was running and we hit an error, stop gracefully
                    if (isRunning) {
                        isRunning = false
                        events.endTraining.fire()
                    }
                } finally {
                    signal.complete(Unit)
                }
            }
        }
    }

    private suspend fun submitTask(task: TrainerTask): CompletableDeferred<Unit> {
        val signal = CompletableDeferred<Unit>()
        processorChannel.send(task to signal)
        return signal
    }

    suspend fun startTraining() {
        submitTask(TrainerTask.Start).await()
    }

    private suspend fun startTrainingHandler() {
        stoppingConditionReached = false
        config.stoppingCondition.resetEarlyStopping()
        isRunning = true
        events.beginTraining.fire().await()
        submitTask(TrainerTask.Train)
    }

    suspend fun stopTraining() {
        submitTask(TrainerTask.Stop).await()
    }

    private fun stopTrainingHandler() {
        isRunning = false
        events.endTraining.fire()
    }

    private suspend fun trainOnceHandler() {
        iteration++
        batchAccuracySum = 0.0
        batchSampleCount = 0

        val data = trainingData ?: error("Training data not set")
        if (data.size == 0) {
            lastTrainingError = 0.0
            events.errorUpdated.fire(
                TrainingStats(
                    trainingError = 0.0,
                    trainingAccuracy = null,
                    testingAccuracy = null
                )
            ).await()
            if (isRunning) submitTask(TrainerTask.Stop)
            return
        }
        val batchSize = minOf(config.batchSize, data.size)
        val startIdx = (0..(data.size - batchSize)).random()
        val rowRange = startIdx until (startIdx + batchSize)

        val loss = trainBatch(rowRange)
        lastTrainingError = loss
        lastTrainingAccuracy = if (config.computeAccuracy && batchSampleCount > 0) {
            batchAccuracySum / batchSampleCount
        } else {
            null
        }

        val shouldComputeTest = config.testConfiguration.enabled &&
            (testingData?.size ?: 0) > 0 &&
            iteration % config.testConfiguration.testFrequency == 0
        val testError = if (shouldComputeTest) computeTestError() else null
        val testAccuracy = if (shouldComputeTest && config.computeAccuracy) {
            computeTestAccuracy().also { lastTestingAccuracy = it }
        } else {
            null
        }

        events.errorUpdated.fire(
            TrainingStats(
                trainingError = loss,
                testingError = testError,
                trainingAccuracy = lastTrainingAccuracy,
                testingAccuracy = testAccuracy
            )
        ).await()

        if (isRunning) {
            if (config.stoppingCondition.validate(iteration, lastTrainingError, testError)) {
                stoppingConditionReached = true
                submitTask(TrainerTask.Stop)
            } else {
                submitTask(TrainerTask.Train)
            }
        } else {
            submitTask(TrainerTask.Stop)
        }
    }

    sealed class TrainerTask {
        object Start : TrainerTask()
        object Train : TrainerTask()
        object Stop : TrainerTask()
        object Randomize : TrainerTask()
    }

    private val adam = DoubleArrayAdam(
        learningRate = config.learningRate,
        beta1 = config.beta1,
        beta2 = config.beta2
    )

    init {
        discoverPipeline()
    }

    /**
     * Walk the graph from [inputTensorLayer] through outgoing connectors to discover
     * the complete CNN → Flatten → Dense pipeline.
     */
    private fun discoverPipeline() {
        var currentTensor = inputTensorLayer

        // Walk through TensorConnectors (Conv/Pool)
        while (true) {
            val outgoing = currentTensor.outgoingTensorConnectors
            val flattenOut = currentTensor.outgoingFlattenConnectors

            if (flattenOut.isNotEmpty()) {
                // Found the flatten connector
                flattenConnector = flattenOut.first()
                break
            }

            check(outgoing.isNotEmpty()) {
                "Pipeline broken: Tensor '${currentTensor.displayName}' has no outgoing connectors"
            }

            val connector = outgoing.first()
            tensorConnectors.add(connector)
            tensorLayerStages.add(connector.target)
            currentTensor = connector.target
        }

        // Walk through WeightMatrix chain from flattenConnector.target to outputArray
        var currentLayer: Layer = flattenConnector.target
        while (currentLayer != outputArray) {
            val outgoing = currentLayer.outgoingConnectors
                .filterIsInstance<WeightMatrix>()
            check(outgoing.isNotEmpty()) {
                "Pipeline broken: Layer '${currentLayer.displayName}' has no outgoing WeightMatrix"
            }
            val wm = outgoing.first()
            denseLayers.add(DenseLayerSnapshot(
                inputSize = wm.source.size,
                outputSize = wm.target.size,
                sourceWeightMatrix = wm
            ))
            currentLayer = wm.target
        }

        // Sync initial weights from network
        denseLayers.forEach { it.syncFromNetwork() }

        // Pre-allocate flatten buffer
        if (denseLayers.isNotEmpty()) {
            flattenBuffer = DoubleArray(denseLayers[0].inputSize)
        }
    }

    /**
     * Run the forward pass for a single input sample.
     * Sets the input tensor activations and propagates through the entire pipeline.
     *
     * The returned array is an internal buffer — callers that need to retain the result
     * across multiple calls must copy it.
     *
     * @param input flat HWC input array (same size as inputTensor.shape.size)
     * @return output activations (DoubleArray of outputArray size); shared internal buffer
     */
    fun forwardPass(input: DoubleArray): DoubleArray {
        // Set input tensor activations
        System.arraycopy(input, 0, inputTensorLayer.activations, 0, inputTensorLayer.shape.size)

        // Forward through CNN stages
        for (i in tensorConnectors.indices) {
            val connector = tensorConnectors[i]
            val targetTensor = tensorLayerStages[i]

            // Clear target inputs before accumulation
            targetTensor.inputs.fill(0.0)
            connector.propagate()

            // Apply activation function (simulating Tensor.update())
            val af = targetTensor.activationFunction
            for (j in targetTensor.activations.indices) {
                val pre = targetTensor.inputs[j] + targetTensor.biases[j]
                targetTensor.preActivations[j] = pre
                targetTensor.activations[j] = af.apply(pre)
            }
            targetTensor.inputs.fill(0.0)
        }

        // Flatten: copy last tensor activations into pre-allocated buffer
        check(denseLayers.isNotEmpty()) { "No dense layers found in pipeline" }
        val src = if (tensorLayerStages.isNotEmpty()) tensorLayerStages.last().activations else inputTensorLayer.activations
        System.arraycopy(src, 0, flattenBuffer, 0, flattenBuffer.size)

        // Forward through dense layers
        var currentInput = flattenBuffer
        for (layer in denseLayers) {
            System.arraycopy(currentInput, 0, layer.inputCache, 0, currentInput.size)
            DoubleArrayOps.matVecMultiply(
                layer.weights, layer.outputSize, layer.inputSize,
                currentInput, layer.zeroBiases, layer.weightedInput
            )
            if (layer.targetLayer.updateRule is SoftmaxRule) {
                System.arraycopy(layer.weightedInput, 0, layer.ruleInput, 0, layer.outputSize)
            } else {
                for (j in 0 until layer.outputSize) {
                    layer.ruleInput[j] = layer.weightedInput[j] + layer.biases[j]
                }
            }
            with(network) { layer.applyUpdateRule() }
            currentInput = layer.activations
        }

        return denseLayers.last().activations
    }

    /**
     * Run the backward pass after a forward pass, computing all gradients.
     *
     * @param target the target output values
     * @return the scalar loss
     */
    fun backwardPass(target: DoubleArray): Double {
        val output = denseLayers.last().activations
        val loss = config.lossFunction.loss(output, target)

        // Output gradient
        val outputGrad = DoubleArray(output.size)
        config.lossFunction.outputGrad(output, target, outputGrad)

        // Backward through dense layers (reverse order)
        var currentGrad = outputGrad
        for (i in denseLayers.indices.reversed()) {
            val layer = denseLayers[i]
            val localGrad = currentGrad.copyOf()

            (layer.targetLayer.updateRule as? DifferentiableUpdateRule)?.let { rule ->
                val deriv = rule.getDerivative(Matrix.column(layer.ruleInput))
                for (j in localGrad.indices) {
                    localGrad[j] *= deriv[j, 0]
                }
            }

            // Weight and bias gradients
            DoubleArrayOps.outerProductAccumulate(
                localGrad, layer.inputCache,
                layer.weightGrads, layer.outputSize, layer.inputSize
            )
            for (j in localGrad.indices) {
                layer.biasGrads[j] += localGrad[j]
            }

            // Propagate gradient to previous layer (except for first dense layer)
            if (i > 0) {
                val prevGrad = DoubleArray(layer.inputSize)
                DoubleArrayOps.transposeVecMultiply(
                    layer.weights, layer.outputSize, layer.inputSize,
                    localGrad, prevGrad
                )
                currentGrad = prevGrad
            } else {
                // First dense layer: propagate gradient back through flatten connector
                val flatGrad = DoubleArray(layer.inputSize)
                DoubleArrayOps.transposeVecMultiply(
                    layer.weights, layer.outputSize, layer.inputSize,
                    localGrad, flatGrad
                )
                // Flatten backward: copy gradient into last tensor's gradients
                flattenConnector.backward(flatGrad)
            }
        }

        // Backward through CNN stages (reverse order)
        for (i in tensorConnectors.indices.reversed()) {
            val connector = tensorConnectors[i]
            val targetTensor = tensorLayerStages[i]

            // Apply activation derivative to target tensor gradients
            val af = targetTensor.activationFunction
            for (j in targetTensor.gradients.indices) {
                targetTensor.gradients[j] *= af.derivative(targetTensor.preActivations[j])
            }

            // Backward through the connector
            when (connector) {
                is ConvolutionConnector -> connector.backward()
                is PoolingConnector -> connector.backward()
            }
        }

        return loss
    }

    /**
     * Train on a batch of samples, accumulating gradients and applying Adam update.
     *
     * @param rowRange the range of row indices in the training data to use
     * @return average loss over the batch
     */
    fun trainBatch(rowRange: IntRange): Double {
        val data = trainingData ?: error("Training data not set")

        // Clear all gradients
        tensorLayerStages.forEach { it.clearGradients() }
        tensorConnectors.filterIsInstance<ConvolutionConnector>().forEach { it.clearGrads() }
        denseLayers.forEach { it.clearGrads() }

        var totalLoss = 0.0
        var count = 0

        for (row in rowRange) {
            if (row >= data.size) break
            val input = data.inputs[row].toDoubleArray()
            val target = data.targets[row].toDoubleArray()

            // Clear tensor gradients for each sample
            tensorLayerStages.forEach { it.clearGradients() }
            // Clear source tensor gradients for conv backward
            inputTensorLayer.clearGradients()

            val output = forwardPass(input)
            if (config.computeAccuracy) {
                computeClassificationAccuracy(output, target)?.let {
                    batchAccuracySum += it
                    batchSampleCount++
                }
            }
            totalLoss += backwardPass(target)
            count++
        }

        if (count == 0) return 0.0

        val scale = 1.0 / count

        // Keep optimizer hyperparameters in sync with editable config.
        adam.learningRate = config.learningRate
        adam.beta1 = config.beta1
        adam.beta2 = config.beta2

        // Apply Adam update
        adam.step()

        // Update conv kernels and biases
        for ((idx, connector) in tensorConnectors.filterIsInstance<ConvolutionConnector>().withIndex()) {
            // Scale gradients by batch size
            for (j in connector.kernelGrads.indices) connector.kernelGrads[j] *= scale
            for (j in connector.biasGrads.indices) connector.biasGrads[j] *= scale

            adam.update("conv${idx}_kernels", connector.kernels, connector.kernelGrads)
            adam.update("conv${idx}_biases", connector.filterBiases, connector.biasGrads)
            
            connector.events.updated.fire()
        }

        // Update dense layers
        for ((idx, layer) in denseLayers.withIndex()) {
            for (j in layer.weightGrads.indices) layer.weightGrads[j] *= scale
            for (j in layer.biasGrads.indices) layer.biasGrads[j] *= scale

            adam.update("dense${idx}_weights", layer.weights, layer.weightGrads)
            adam.update("dense${idx}_biases", layer.biases, layer.biasGrads)
        }

        // Sync updated weights back to network objects
        denseLayers.forEach { it.syncToNetwork() }

        return totalLoss / count
    }

    /**
     * Run one training iteration using the configured batch size.
     */
    suspend fun trainOnce() {
        submitTask(TrainerTask.Train).await()
    }

    suspend fun randomize() {
        submitTask(TrainerTask.Randomize).await()
    }

    fun initParameters() {
        // Keep convolution initialization CNN-specific (He is standard).
        tensorConnectors.filterIsInstance<ConvolutionConnector>().forEach {
            it.heInitialize()
            it.clearGrads()
            it.events.updated.fire()
        }

        // Dense layers follow the shared initialization strategy pattern.
        denseLayers.forEach { layer ->
            config.weightInitializationStrategy.initializeWeights(layer.sourceWeightMatrix)
            val targetLayer = layer.sourceWeightMatrix.target as ArrayLayer
            targetLayer.biasArray.fill(0.0)
            targetLayer.events.updated.fire()
            layer.syncFromNetwork()
            layer.clearGrads()
        }

        tensorLayerStages.forEach {
            it.biases.fill(0.0)
            it.clearGradients()
            it.events.updated.fire()
        }
        inputTensorLayer.clearGradients()
    }

    private fun computeTestError(): Double {
        val data = testingData ?: return 0.0
        if (data.size == 0) return 0.0
        var sum = 0.0
        for (i in 0 until data.size) {
            val output = forwardPass(data.inputs[i].toDoubleArray())
            sum += config.lossFunction.loss(output, data.targets[i].toDoubleArray())
        }
        return sum / data.size
    }

    private fun computeTestAccuracy(): Double {
        val data = testingData ?: return 0.0
        if (data.size == 0) return 0.0
        var sum = 0.0
        var count = 0
        for (i in 0 until data.size) {
            val output = forwardPass(data.inputs[i].toDoubleArray())
            computeClassificationAccuracy(output, data.targets[i].toDoubleArray())?.let {
                sum += it
                count++
            }
        }
        return if (count == 0) 0.0 else sum / count
    }

    private fun computeClassificationAccuracy(output: DoubleArray, target: DoubleArray): Double? {
        if (!isValidOneHotTarget(target)) return null
        val predictedScores = if (outputArray.updateRule is SoftmaxRule) {
            output
        } else if (config.lossFunction == CnnLossFunction.CrossEntropy) {
            softmax(output)
        } else {
            output
        }
        val predictedClass = predictedScores.indices.maxByOrNull { predictedScores[it] } ?: 0
        val targetClass = target.indices.maxByOrNull { target[it] } ?: 0
        return if (predictedClass == targetClass) 1.0 else 0.0
    }

    private fun isValidOneHotTarget(target: DoubleArray): Boolean {
        if (target.size < 2) return false
        val tolerance = 1e-10
        var oneCount = 0
        var sum = 0.0
        for (v in target) {
            sum += v
            if (abs(v - 1.0) < tolerance) oneCount++
            else if (abs(v) > tolerance) return false
        }
        return oneCount == 1 && abs(sum - 1.0) < tolerance
    }

    /**
     * Refresh dense-layer snapshots from the current network state.
     * Useful when this trainer is used for forward inference while another trainer updated weights.
     */
    fun syncFromNetwork() {
        denseLayers.forEach { it.syncFromNetwork() }
    }
}
