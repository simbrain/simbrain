/**
 * Many of the functions used to implement supervised learning are in this file.
 *
 * A good starting place is [forwardPass]. Note that the general update logic in Simbrain involves first accumulating inputs
 * to each neuron array, which in turn involves updating a "PSR Matrix".
 * For more on this, see https://docs.simbrain.net/docs/network/updateLogic.html
 *
 * The entrypoint for backprop is [accumulateBackprop].
 *
 */
package org.simbrain.network.trainers

import org.simbrain.network.core.*
import org.simbrain.network.gui.nodes.ActivationSequenceProcessor
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import smile.math.matrix.Matrix
import java.util.*
import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random

fun WeightMatrix.computeWeightDeltas(errorSignal: Matrix): Matrix {

    val sourceIsActivationSequenceProcessor = source is ActivationSequenceProcessor
    val targetIsActivationSequenceProcessor = target is ActivationSequenceProcessor

    // source is sequence and target is vector
    if (sourceIsActivationSequenceProcessor && !targetIsActivationSequenceProcessor) {
        // sum of outer products across rows of activation sequence
        return source.activations.rows.map { row ->
            errorSignal.mm(row)
        }.reduce { acc, matrix ->
            acc.add(matrix)
        }
    }

    // source and target are vectors
    if (!sourceIsActivationSequenceProcessor && !targetIsActivationSequenceProcessor) {
        return errorSignal.mm(source.activations.transpose())
    }

    // source and target are sequences
    if (sourceIsActivationSequenceProcessor && targetIsActivationSequenceProcessor) {
        return errorSignal.transpose().mm(source.activations)
    }

    throw IllegalArgumentException("Invalid source and target types: ${source::class.simpleName} and ${target::class.simpleName}")
}

fun SynapseGroup.computeWeightDeltas(errorSignal: Matrix): Matrix {
    return errorSignal.mm(source.activations.transpose())
}

fun WeightMatrix.backpropagateError(errorSignal: Matrix): Matrix {
    // Backpropagate the error signal through the weights to get a new error vector
    //println("Propagating errors through ${source.displayName} [${errorSignal.flatten().joinToString(", ") { it.format(2) }}]")
    return if (target is ActivationSequenceProcessor) {
        // sequence of errors * wm
        errorSignal.mm(weights)
    } else {
        // error vector * wm
        // Prefer this to errorSignal.T.mm(wm).T because that requires an extra transpose
        weights.transpose().mm(errorSignal)
    }
}

fun SynapseGroup.backpropagateError(errorSignal: Matrix): Matrix {
    return getWeightMatrix().transpose().mm(errorSignal)
}

/**
 * Change to bias is error vector times epsilon. Compute this and add it to biases.
 */
fun NeuronArray.updateBiases(error: Matrix, epsilon: Double = .1) {
    this.activations.validateSameShape(error)
    val biasDelta = error.clone().mul(epsilon)
    biases += biasDelta
    events.updated.fire()
}

/**
 * Loss functions for backprop. Note that some of the logic for backprop is encoded in subclasses of this class.
 */
sealed class BackpropLossFunction(
    val shortName: String,
    val description: String
) : EditableObject {

    /**
     * A scalar loss function (e.g., MSE) used primarily in the GUI and as a stopping condition. For example,
     * take the mean of the error values in the [outputError].
     */
    abstract fun scalarLoss(actual: Matrix, target: Matrix): Double

    /**
     * The "vector" of errors at the output layer, which is backpropagated through the network in training.
     */
    abstract fun outputError(actual: Matrix, target: Matrix): Matrix

    /**
     * Called to determine whether a given layer can use this loss function. For example, softmax cannot use SSE or MSE.
     */
    abstract fun canUse(layer: Layer): Boolean

    object SSE : BackpropLossFunction("SSE", "Sum Squared Error") {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual sse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).mul(2.0)
        }

        override fun canUse(layer: Layer) = layer.updateRule !is SoftmaxRule
    }

    object MSE : BackpropLossFunction("MSE", "Mean Squared Error") {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual mse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).mul(2.0).div(actual.size().toDouble())
        }

        override fun canUse(layer: Layer) = layer.updateRule !is SoftmaxRule
    }

    object RMSE : BackpropLossFunction("RMSE", "Root Mean Squared Error") {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual rmse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).div(actual.size() * scalarLoss(actual, target))
        }

        override fun canUse(layer: Layer) = layer.updateRule !is SoftmaxRule

    }

    object CrossEntropy : BackpropLossFunction("Cross Entropy", "Cross Entropy") {
        override fun scalarLoss(actual: Matrix, target: Matrix) = crossEntropy(actual, target)

        /**
         * The error here incorporates the gradient with respect to an assumed softmax output. This is a standard
         * performance optimization (actual outputs - targets)
         *
         * This also explains why [SoftmaxRule] does not implement [org.simbrain.network.updaterules.interfaces.DifferentiableUpdateRule]
         */
        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return actual.clone().sub(target).mul(-1.0) // assume softmax output
        }

        override fun canUse(layer: Layer) = layer.updateRule is SoftmaxRule

        /**
         * Calculate classification accuracy for softmax predictions.
         * Assumes one-hot encoded targets.
         * 
         * @param actual The softmax predictions (probabilities)
         * @param target The one-hot encoded targets
         * @return Accuracy as a value between 0.0 and 1.0
         */
        fun accuracy(actual: Matrix, target: Matrix): Double {
            actual.validateSameShape(target)
            
            // Handle sequence data (multiple rows with multiple columns)
            // For single predictions, we expect a column vector (nrow > 1, ncol = 1)
            if (actual.ncol() > 1) {
                return accuracySequence(actual, target)
            }
            
            // Single prediction case (column vector)
            target.validateColumnVector()
            actual.validateColumnVector()
            
            // Find the predicted class (highest probability)
            val predictedClass = actual.toDoubleArray().indices.maxByOrNull { actual[it, 0] } ?: 0
            
            // Find the target class (should be 1.0 in one-hot encoding)
            val targetClass = target.toDoubleArray().indices.maxByOrNull { target[it, 0] } ?: 0
            
            return if (predictedClass == targetClass) 1.0 else 0.0
        }

        /**
         * Calculate accuracy for sequence data where each row is a separate prediction.
         */
        private fun accuracySequence(actual: Matrix, target: Matrix): Double {
            require(actual.nrow() == target.nrow()) {
                "Sequence length mismatch: predictions has ${actual.nrow()} rows but targets has ${target.nrow()} rows"
            }
            require(actual.ncol() == target.ncol()) {
                "Vocabulary size mismatch: predictions has ${actual.ncol()} columns but targets has ${target.ncol()} columns"
            }

            var correctPredictions = 0
            for (i in 0 until actual.nrow()) {
                // Find predicted class for this position
                val predictedClass = (0 until actual.ncol()).maxByOrNull { actual[i, it] } ?: 0
                
                // Find target class for this position
                val targetClass = (0 until target.ncol()).maxByOrNull { target[i, it] } ?: 0
                
                if (predictedClass == targetClass) {
                    correctPredictions++
                }
            }
            
            return correctPredictions.toDouble() / actual.nrow()
        }
    }

    override fun toString() = description

    fun validateLayer(layer: Layer) {
        if (!canUse(layer)) {
            throw IllegalArgumentException("Layer $layer cannot use loss function $this")
        }
    }
}

/**
 * Computes the update order list for layers in a directed graph starting from the specified `start` layer
 * and ending at the specified `end` layer. This method traverses the incoming connectors of layers in a
 * reverse breadth-first manner to determine the proper order of updates.
 *
 * @param start The starting layer of the update process. The traversal will stop upon reaching this layer.
 * @param end The ending layer for the update process. The traversal begins from this layer and works backward.
 * @return A LinkedHashSet of layers representing the ordered list of layers to be updated.
 *         The order ensures layers are processed starting from the `start` layer to the `end` layer
 *         in accordance with their dependencies.
 */
fun computeOrderedUpdatePath(start: Set<Layer>, end: Layer): LinkedHashSet<Layer> {
    val visited = LinkedHashSet<Layer>()
    val queue = ArrayDeque<Layer>()
    val remainingStarts = start.toMutableSet()
    queue.add(end)
    while (queue.isNotEmpty()) {
        val currentLayer = queue.removeFirst()
        if (currentLayer in visited) {
            continue
        }
        visited.add(currentLayer)

        remainingStarts.remove(currentLayer)
        if (remainingStarts.isEmpty()) break

        for (neighbor in currentLayer.incomingConnectors) {
            if (neighbor.source !in visited) {
                queue.add(neighbor.source)
            }
        }
        (currentLayer as? AbstractNeuronCollection)?.incomingSgs?.forEach { neighbor ->
            if (neighbor.source !in visited) {
                queue.add(neighbor.source)
            }
        }
    }
    if (start.any { it !in visited }) {
        throw IllegalArgumentException("No path found from start (${start.any { it !in visited }}) to end ($end)")
    }
    return LinkedHashSet(visited.reversed())
}

fun LinkedHashSet<Layer>.getAllOutgoingConnectors() = map { it.outgoingConnectors }
    .flatten()
    .filter { it.target in this }
    .toMutableList()

fun LinkedHashSet<Layer>.getAllOutgoingSynapseGroups() = filterIsInstance<AbstractNeuronCollection>()
    .map { it.outgoingSg }
    .flatten()
    .filter { it.target in this }
    .toMutableList()

/**
 * Perform a single forward pass through an ordered set of layers.
 *
 * Assumes the LinkedHashSet has been placed in an appropriate breadth-first order by [computeOrderedUpdatePath], to
 * support skip connections.
 *
 * Multiple inputs can be specified (this is not yet supported in the GUI however)
 *
 * (Optionally) records probes of weights, biases, inputs, and activations at key stages of processing.
 *
 */
context(Network)
fun LinkedHashSet<Layer>.forwardPass(inputValues: List<Matrix>, inputLayers: List<Layer>, probe: StructuredProbe? = null) {

    val probeContext = probe?.createMapProbe("forwardPass")

    if (inputValues.size != inputLayers.size) throw IllegalArgumentException("Must provide same number of input vectors as input layers")
    inputValues.zip(inputLayers).forEach { (a, b) -> a.validateSameShape(b.activations) }

    // Update components but preserve the input accumulation pattern used in training.
    // Normally updates clear inputs (see https://docs.simbrain.net/docs/network/updateLogic.html)
    fun NeuronArray.updateWithoutClearingInputs() {
        if (isClamped) {
            return
        }
        updateRule.apply(this, dataHolder)
        events.updated.fire()
    }
    fun AbstractNeuronCollection.updateWithoutClearingInputs() {
        if (isClamped) {
            return
        }
        neuronList.forEach { neuron ->
            if (neuron.isSpike) {
                neuron.isSpike = false
            }
            if (!neuron.clamped) {
                neuron.updateRule.apply(neuron, neuron.dataHolder)
            }

        }
    }
    fun ArrayLayer.updateWithoutClearingInputs() {
        if (isClamped) {
            return
        }
        
        // Temporarily store inputs before calling update
        val inputsBackup = this.inputs.clone()
        this.update()
        // During training, inputs should be managed by the training loop, not cleared by update()
        // So we restore them here. The training loop will clear them at the appropriate time.
        this.inputs.copyFrom(inputsBackup)
    }

    val allLayers = this
    inputLayers.zip(inputValues).forEach { (layer, value) ->
        layer.activations = value
    }
    probeContext?.createMapProbe("weightsInForwardPass")?.writeAll(
        flatMap {
            it.outgoingConnectors
        }.associate {
            it.displayName to (it as? WeightMatrix)?.weights?.clone()
        }
    )
    probeContext?.createMapProbe("activationsAfterApplyingTrainingInputs")?.writeAll(inputLayers.associate { it.displayName to it.activations.clone() })
    probeContext?.createMapProbe("biasesInForwardPass")?.writeAll(inputLayers.associate { it.displayName to it.biases.clone() })
    val layersContext = probeContext?.createMapProbe("layersInForwardPass")

    allLayers.forEach {
        val layerContext = layersContext?.createMapProbe(it.displayName)
        val inputsBeforeAccumulation = layerContext?.createMapProbe("inputsBeforeAccumulation")
        layerContext?.createMapProbe("inputs")
        val inputsAfterAccumulation = layerContext?.createMapProbe("inputsAfterAccumulation")

        it.clearInputs()
        when (it) {
            is NeuronArray -> {
                inputsBeforeAccumulation?.write(it.displayName, it.inputs.clone())
                it.accumulateInputs()
                inputsAfterAccumulation?.write(it.displayName, it.inputs.clone())
                it.updateWithoutClearingInputs()
            }
            is AbstractNeuronCollection -> {
                inputsBeforeAccumulation?.write(it.displayName, it.inputs.clone())
                if (it.incomingSgs.isNotEmpty()) {
                    it.neuronList.forEach { n -> n.accumulateInputs() }
                } else {
                    it.accumulateInputs()
                }
                inputsAfterAccumulation?.write(it.displayName, it.inputs.clone())
                it.updateWithoutClearingInputs()
            }
            is ArrayLayer -> {
                inputsBeforeAccumulation?.write(it.displayName, it.inputs.clone())
                it.accumulateInputs()
                inputsAfterAccumulation?.write(it.displayName, it.inputs.clone())
                // For ArrayLayer, call update but don't clear inputs since accumulateInputs properly manages them
                it.updateWithoutClearingInputs()
            }
            else -> it.update()
        }
    }
    probeContext?.createMapProbe("afterUpdates")?.writeAll(allLayers.associate { it.displayName to it.activations.clone() })
}

/**
 * Main backprop implementation, which supports skip connections and accumulates weight and bias updates.
 *
 * The data structure is linked because it updates layers in a sequential order and is a set because it only updates each layer once.
 * Assumes LinkedHashSet has been placed in an appropriate "breadth-first" order by [computeOrderedUpdatePath].
 *
 * Parameters are not updated directly. Parameter deltas are accumulated in data structures provided to this function.
 * [SupervisedTrainer.trainBatch] sums these deltas and uses them to update weights, synapses, and biases.
 *
 * Returns a scalar error used in the GUI when training.
 */
context(Network)
fun LinkedHashSet<Layer>.accumulateBackprop(
    inputLayers: List<Layer>,
    targetValues: Matrix,
    outputLayer: Layer,
    weightAccumulator: HashMap<WeightMatrix, Matrix>,
    synapseGroupAccumulator: HashMap<SynapseGroup, Matrix>,
    biasesAccumulator: HashMap<Layer, Matrix>,
    rawMatrixAccumulator: HashMap<Matrix, Matrix>,
    lossFunction: BackpropLossFunction = BackpropLossFunction.SSE,
    probe: StructuredProbe? = null,
): Double {

    val probeContext = probe?.createMapProbe("accumulateBackprop")

    val reversedLayers = reversed()

    targetValues.validateSameShape(outputLayer.activations)
    lossFunction.validateLayer(outputLayer)

    val scalarError = lossFunction.scalarLoss(outputLayer.activations, targetValues)

    probeContext?.write("scalarError", scalarError)

    // printActivationsAndWeights()
    var errorSignal: Matrix = lossFunction.outputError(outputLayer.activations, targetValues)
    var signalSource: Layer = outputLayer

    probeContext?.write("errorSignal") { errorSignal.clone() }
    probeContext?.write("signalSource") { signalSource.displayName }

    val backpropLayersContext = probeContext?.createListProbe("backpropLayers")

    // Map that associates layers with their error signals
    val layerErrorSignals = mutableMapOf<Layer, Matrix>()
    
    // Initialize the output layer's error signal
    layerErrorSignals[outputLayer] = errorSignal

    // Go through layers from output to input
    reversedLayers.forEach { layer ->

        val layerContext = backpropLayersContext?.createMapProbe(layer.displayName)

        // Get the accumulated error signal for this layer
        val currentLayerErrorSignal = layerErrorSignals[layer] ?: errorSignal
        layerContext?.write("currentLayerErrorSignal") { currentLayerErrorSignal.clone() }

        // Process the error signal. Bias update for neuronarray. Full backprop update for transformer block. Etc.
        val processedErrorSignal = layer.processError(currentLayerErrorSignal, signalSource, biasesAccumulator, rawMatrixAccumulator)

        layerContext?.write("processedErrorSignal") { processedErrorSignal.clone() }

        // Process weight matrices feeding into the current layer.
        // For each one we:
        //  1. compute and accumulate a weight delta using source activations
        //  2. "backpropagate" the error signals by accumulating them for the specific source layer
        layer.incomingConnectors.forEach { connector ->
            val connectorContext = layerContext?.createMapProbe(connector.displayName)
            val wm = connector as WeightMatrix
            val weightDeltas = wm.computeWeightDeltas(processedErrorSignal)
            connectorContext?.write("weightDeltas") { weightDeltas.clone() }
            weightAccumulator.getOrPut(wm) {
                Matrix(wm.weights.nrow(), wm.weights.ncol())
            }.add(weightDeltas)
            val currentConnectorErrorSignal = wm.backpropagateError(processedErrorSignal)
            connectorContext?.write("currentConnectorErrorSignal") { currentConnectorErrorSignal.clone() }

            // Accumulate error signal for the specific source layer (supports skip connections)
            layerErrorSignals[wm.source] = layerErrorSignals[wm.source]?.add(currentConnectorErrorSignal) ?: currentConnectorErrorSignal
            connectorContext?.write("accumulatedErrorSignalForSource") { layerErrorSignals[wm.source]?.clone() }
        }
        // Synapse groups are updated in the same way but with different data structures
        (layer as? AbstractNeuronCollection)?.incomingSgs?.forEach { sg ->
            val sgContext = layerContext?.createMapProbe(sg.displayName)
            val weightDeltas = sg.computeWeightDeltas(processedErrorSignal)
            sgContext?.write("weightDeltas") { weightDeltas.clone() }
            synapseGroupAccumulator.getOrPut(sg) {
                Matrix(sg.target.size, sg.source.size)
            }.add(weightDeltas)
            val currentConnectorErrorSignal = sg.backpropagateError(processedErrorSignal)
            sgContext?.write("currentConnectorErrorSignal") { currentConnectorErrorSignal.clone() }

            // Accumulate error signal for the specific source layer (supports skip connections)
            layerErrorSignals[sg.source] = layerErrorSignals[sg.source]?.add(currentConnectorErrorSignal) ?: currentConnectorErrorSignal
            sgContext?.write("accumulatedErrorSignalForSource") { layerErrorSignals[sg.source]?.clone() }
        }
        
        signalSource = layer
    }

    return scalarError
}

/**
 * Returns a list or chain of connectors from input (start) to output (end).
 */
fun getConnectorChain(start: Layer, end: Layer): List<Connector> {

    // special case for recurrent connections from a layer to itself
    if (start === end) {
        return listOf(start.outgoingConnectors.first { it.target === end })
    }

    fun reconstructPath(start: Layer, end: Layer, path: Map<Layer, Connector>): List<Connector> {
        val result = mutableListOf<Connector>()
        var currentLayer: Layer? = end
        while (currentLayer != null && currentLayer != start) {
            result.add(path[currentLayer]!!)
            currentLayer = path[currentLayer]!!.source
        }

        result.reverse()
        return result
    }

    val visited = mutableSetOf<Layer>()
    val queue = ArrayDeque<Layer>()
    val path = mutableMapOf<Layer, Connector>()

    queue.add(start)

    while (queue.isNotEmpty()) {
        val currentLayer = queue.removeFirst()

        if (currentLayer == end) {
            // We've found the end node, so we'll now reconstruct the path.
            return reconstructPath(start, end, path)
        }

        if (currentLayer in visited) {
            continue
        }

        visited.add(currentLayer)
        for (neighbor in currentLayer.outgoingConnectors) {
            if (neighbor.target !in visited) {
                queue.add(neighbor.target)
                path[neighbor.target] = neighbor
            }
        }
    }

    // If there's no path, return an empty list.
    return emptyList()
}

/**
 * Print debugging info for a list of weight matrices.
 */
context(Network)
fun List<WeightMatrix>.printActivationsAndWeights(showWeights: Boolean = false) {
    println(first().source)
    for (wm in this) {
        wm.target.accumulateInputs()
        wm.target.update()
        println(wm)
        if (showWeights) {
            println(wm.weights)
        }
        println(wm.target)
    }

}

fun crossEntropy(predictions: Matrix, targets: Matrix): Double {
    // Handle sequence data (multiple rows)
    if (predictions.nrow() > 1 && targets.nrow() > 1) {
        return crossEntropySequence(predictions, targets)
    }

    // Original column vector case
    targets.validateColumnVector()
    predictions.validateColumnVector()
    var loss = 0.0
    for (i in 0 until targets.nrow()) {
        loss += targets[i, 0] * ln(predictions.get(i, 0).coerceAtLeast(1e-15))
    }
    return -loss
}

/**
 * Compute cross-entropy loss for sequence data where each row is a separate probability distribution.
 * Both predictions and targets should have shape (sequence_length, vocab_size).
 */
fun crossEntropySequence(predictions: Matrix, targets: Matrix): Double {
    require(predictions.nrow() == targets.nrow()) {
        "Sequence length mismatch: predictions has ${predictions.nrow()} rows but targets has ${targets.nrow()} rows"
    }
    require(predictions.ncol() == targets.ncol()) {
        "Vocabulary size mismatch: predictions has ${predictions.ncol()} columns but targets has ${targets.ncol()} columns"
    }

    var totalLoss = 0.0
    for (i in 0 until predictions.nrow()) {
        for (j in 0 until predictions.ncol()) {
            totalLoss += targets[i, j] * ln(predictions[i, j].coerceAtLeast(1e-15))
        }
    }
    return -totalLoss
}

/**
 * Split a dataset into training and testing subsets using MutableList format.
 */
fun splitDataSet(
    inputs: MutableList<MutableList<Double>>,
    targets: MutableList<MutableList<Double>>,
    splitRatio: Double, 
    random: Random = Random(42L)
): Pair<Pair<MutableList<MutableList<Double>>, MutableList<MutableList<Double>>>, Pair<MutableList<MutableList<Double>>, MutableList<MutableList<Double>>>> {
    require(inputs.size == targets.size) { "inputs size (${inputs.size}) must equal targets size (${targets.size})" }
    require(splitRatio in 0.0..1.0) { "splitRatio must be between 0.0 and 1.0" }

    val nrows = inputs.size
    val rowIndices = (0 until nrows).shuffled(random)
    
    val trainRowCount = (nrows * splitRatio).toInt()
    val testRowCount = nrows - trainRowCount

    val trainRowIndices = rowIndices.take(trainRowCount)
    val testRowIndices = rowIndices.takeLast(testRowCount)

    val trainingInputs = trainRowIndices.map { inputs[it].toMutableList() }.toMutableList()
    val trainingTargets = trainRowIndices.map { targets[it].toMutableList() }.toMutableList()
    val testingInputs = testRowIndices.map { inputs[it].toMutableList() }.toMutableList()
    val testingTargets = testRowIndices.map { targets[it].toMutableList() }.toMutableList()

    return (trainingInputs to trainingTargets) to (testingInputs to testingTargets)
}

/**
 * Split a TrainingDataset into training and testing TrainingDatasets.
 * Preserves input and target dimensions, allowing for empty datasets when splitRatio is 0.0 or 1.0.
 */
fun splitDataSet(
    dataset: TrainingDataset,
    splitRatio: Double,
    random: Random = Random(42L)
): Pair<TrainingDataset, TrainingDataset> {
    require(splitRatio in 0.0..1.0) { "splitRatio must be between 0.0 and 1.0" }

    // Generate the same row indices that will be used for data splitting
    val nrows = dataset.inputs.size
    val rowIndices = (0 until nrows).shuffled(random)
    val trainRowCount = (nrows * splitRatio).toInt()
    val trainRowIndices = rowIndices.take(trainRowCount)
    val testRowIndices = rowIndices.drop(trainRowCount)

    // Split the data using the existing function (but we already know the indices)
    val (training, testing) = splitDataSet(dataset.inputs, dataset.targets, splitRatio, random)
    val (trainingInputs, trainingTargets) = training
    val (testingInputs, testingTargets) = testing

    val trainingDataset = TrainingDataset(
        inputs = trainingInputs,
        targets = trainingTargets,
        inputSize = dataset.inputSize,
        targetSize = dataset.targetSize,
        inputRowNames = dataset.inputRowNames?.let { names ->
            trainRowIndices.map { names.getOrNull(it) ?: "Row $it" }
        },
        targetRowNames = dataset.targetRowNames?.let { names ->
            trainRowIndices.map { names.getOrNull(it) ?: "Row $it" }
        },
        inputColumnNames = dataset.inputColumnNames,
        targetColumnNames = dataset.targetColumnNames
    )

    val testingDataset = TrainingDataset(
        inputs = testingInputs,
        targets = testingTargets,
        inputSize = dataset.inputSize,
        targetSize = dataset.targetSize,
        inputRowNames = dataset.inputRowNames?.let { names ->
            testRowIndices.map { names.getOrNull(it) ?: "Row $it" }
        },
        targetRowNames = dataset.targetRowNames?.let { names ->
            testRowIndices.map { names.getOrNull(it) ?: "Row $it" }
        },
        inputColumnNames = dataset.inputColumnNames,
        targetColumnNames = dataset.targetColumnNames
    )

    return trainingDataset to testingDataset
}

/**
 * Split a dataset (inputs only) into training and testing subsets for unsupervised learning.
 */
fun splitDataSet(inputs: Matrix, splitRatio: Double, random: Random = Random(42L)): Pair<Matrix, Matrix> {
    require(splitRatio in 0.0..1.0) { "splitRatio must be between 0.0 and 1.0" }

    val nrows = inputs.nrow()

    val rowIndices = (0 until nrows).shuffled(random)

    val trainRowCount = (nrows * splitRatio).toInt().coerceAtLeast(1)
    val testRowCount = (nrows - trainRowCount).coerceAtLeast(1)

    val trainRowIndices = rowIndices.take(trainRowCount)
    val testRowIndices = rowIndices.takeLast(testRowCount)

    return Pair(
        inputs.rows(*trainRowIndices.toIntArray()),
        inputs.rows(*testRowIndices.toIntArray())
    )
}

/**
 * Split a dataset (inputs only) into training and testing subsets for unsupervised learning using MutableList format.
 */
fun splitDataSet(inputs: MutableList<MutableList<Double>>, splitRatio: Double, random: Random = Random(42L)): Pair<MutableList<MutableList<Double>>, MutableList<MutableList<Double>>> {
    require(splitRatio in 0.0..1.0) { "splitRatio must be between 0.0 and 1.0" }

    val nrows = inputs.size
    val rowIndices = (0 until nrows).shuffled(random)

    val trainRowCount = (nrows * splitRatio).toInt()
    val testRowCount = nrows - trainRowCount

    val trainRowIndices = rowIndices.take(trainRowCount)
    val testRowIndices = rowIndices.takeLast(testRowCount)

    val trainingInputs = trainRowIndices.map { inputs[it].toMutableList() }.toMutableList()
    val testingInputs = testRowIndices.map { inputs[it].toMutableList() }.toMutableList()

    return trainingInputs to testingInputs
}

/**
 * A hierarchical container for structured data capture, supporting both map-like and list-like organization.
 *
 *  Useful for tracing, logging, testing, and debugging complex data flows or computation trees.
 *
 * This abstract base class allows nesting of probe data via two concrete types: [MapProbe] for key-value mappings
 * and [ListProbe] for ordered collections. Each node can contain values or nested probe contexts.
 *
 * When passing a probe to a new context it's best to create a new probe from it (since you can't write to it directly).
 *
 * @property parent Optional parent context, enabling upward traversal or tree-like structures.
 */
sealed class StructuredProbe(var parent: StructuredProbe? = null): Iterable<Pair<Any, Any?>> {

    /**
     * Add key, value pairs to a probe. Basically if you want to give names to things you're probing.
     */
    class MapProbe(parent: StructuredProbe? = null) : StructuredProbe(parent) {
        val data = LinkedHashMap<String, Any?>()

        override fun iterator() = data.entries.map { it.toPair() }.iterator()

        override fun createMapProbe(key: String?): MapProbe {
            require(key != null) { "Key cannot be null for MapContext" }
            return MapProbe(parent = this).also { data[key] = it }
        }
        override fun createListProbe(key: String?): ListProbe {
            require(key != null) { "Key cannot be null for MapContext" }
            return ListProbe(parent = this).also { data[key] = it }
        }

        fun write(key: String, value: Any?) {
            data[key] = value
        }

        fun <K, V> writeAll(map: Map<K, V>, mappers: (K, V) -> Pair<String, Any?> = { k, v -> k.toString() to v }) {
            map.map { mappers(it.key, it.value) }.forEach { (key, value) ->
                write(key, value)
            }
        }

        fun write(key: String, valueProvider: () -> Any?) = write(key, valueProvider())

        override fun toTreeString(indentation: Int): String {
            return data.entries.joinToString("\n") { (key, value) ->
                " ".repeat(indentation) + key + ": \n" + ((value as? StructuredProbe)?.toTreeString(indentation + 2) ?: value.toString().indent(indentation + 2))
            }
        }
    }

    /**
     * Push probe items into a list. Useful mostly with foreach.
     */
    class ListProbe(parent: StructuredProbe? = null) : StructuredProbe(parent) {
        val data = ArrayList<Any?>()
        override fun iterator() = data.mapIndexed { index, value -> index to value }.iterator()
        override fun createMapProbe(key: String?) = MapProbe(parent = this).also { data.add(it) }
        override fun createListProbe(key: String?) = ListProbe(parent = this).also { data.add(it) }

        fun write(value: Any?) {
            data.add(value)
        }

        fun write(valueProvider: () -> Any?) = write(valueProvider())

        override fun toTreeString(indentation: Int): String {
            return data.mapIndexed { index, value ->
                " ".repeat(indentation) + index + ": \n" + ((value as? StructuredProbe)?.toTreeString(indentation + 2) ?: value.toString().indent(indentation + 2))
            }.joinToString("\n")
        }
    }

    abstract fun createMapProbe(key: String? = null): MapProbe

    abstract fun createListProbe(key: String? = null): ListProbe

    abstract fun toTreeString(indentation: Int = 0): String
}

fun diffProbes(a: StructuredProbe, b: StructuredProbe, allowMissing: Boolean = false, diffFunction: (Any, Any) -> Any? = { a, b ->
    when {
        a is Number && b is Number -> (a.toDouble() - b.toDouble()).let { if (abs(it) < 1e-6) true else it }
        a is Matrix && b is Matrix -> a.diff(b).let {
            when (it) {
                is MatrixDiffResult.InTolerance -> true
                is MatrixDiffResult.OutOfTolerance -> it.diff
                is MatrixDiffResult.DimensionsMismatch -> it.reason
            }
        }
        else -> a == b
    }
}): String {
    val result = StringBuilder()
    
    fun StructuredProbe.getPath(pathParts: MutableList<String> = mutableListOf()): List<String> {
        val parentPath = parent?.getPath(pathParts) ?: pathParts
        return parentPath
    }
    
    fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> value
            is Collection<*> -> "[${value.joinToString(", ") { it.toString() }}]"
            is Matrix -> (0..value.nrow() - 1).joinToString("\n") { i ->
                (0..value.ncol() - 1).joinToString(" ") { j ->
                    value[i, j].let {
                        (if (it >= 0) " " else "") + it.format(6)
                    }
                }
            }

            else -> value.toString()
        }.indent()
    }

    fun formatDiff(path: String, a: String, b: String, diff: String) = "$path:\n${a.indent(4)}\n  <->\n${b.indent(4)}\n  diff:\n${diff.indent(4)}\n"

    fun processNode(nodeA: Pair<Any, Any?>, nodeB: Pair<Any, Any?>?, path: List<String>) {
        val (keyA, valueA) = nodeA
        val currentPath = path + keyA.toString()
        
        // Handle leaf nodes
        if (valueA !is StructuredProbe && (nodeB == null || nodeB.second !is StructuredProbe)) {
            val pathStr = "/" + currentPath.joinToString("/")
            
            if (nodeB == null) {
                if (!allowMissing) {
                    result.append("${formatDiff(pathStr, formatValue(valueA), "missing", "missing")}\n")
                }
            } else {
                val (_, valueB) = nodeB
                val diff = diffFunction(valueA ?: "null", valueB ?: "null")
                val isDifferent = when (diff) {
                    is Boolean -> !diff
                    else -> true
                }
                
                if (isDifferent) {
                    val diffStr = when (diff) {
                        is Boolean -> diff.toString()
                        is Number -> diff.toString()
                        is Matrix -> formatValue(diff)
                        else -> "different"
                    }
                    
                    val formattedValueA = formatValue(valueA)
                    val formattedValueB = formatValue(valueB)

                    result.append(formatDiff(pathStr, formattedValueA, formattedValueB, diffStr))
                }
            }
            return
        }
        
        // Handle TrainerProbe nodes
        if (valueA is StructuredProbe && nodeB?.second is StructuredProbe) {
            val probeA = valueA
            val probeB = nodeB.second as StructuredProbe
            
            val aEntries = probeA.toList()
            val bMap = probeB.associate { it.first to it }
            
            // Compare each entry in the first probe
            for (entry in aEntries) {
                val matchingEntry = bMap[entry.first]
                processNode(entry, matchingEntry, currentPath)
            }
            
            // Check for entries that exist only in the second probe if not allowing missing
            if (!allowMissing) {
                val aKeys = aEntries.map { it.first }.toSet()
                val bOnlyEntries = probeB.filter { it.first !in aKeys }
                
                for (entry in bOnlyEntries) {
                    val (entryKey, entryValue) = entry
                    val pathStr = "/" + (currentPath + entryKey.toString()).joinToString("/")
                    result.append(formatDiff(pathStr, "missing", formatValue(entryValue), "missing"))
                }
            }
        }
    }
    
    // Start the recursion with the root nodes
    for (entryA in a) {
        val (keyA, _) = entryA
        val entryB = b.find { (keyB, _) -> keyB == keyA }
        processNode(entryA, entryB, emptyList())
    }
    
    // Check for entries in b that don't exist in a
    if (!allowMissing) {
        val aKeys = a.map { it.first }.toSet()
        val bOnlyEntries = b.filter { it.first !in aKeys }
        
        for (entryB in bOnlyEntries) {
            val (keyB, valueB) = entryB
            val pathStr = "/$keyB"
            result.append(formatDiff(pathStr, "missing", formatValue(valueB), "missing"))
        }
    }
    
    return result.toString().trimEnd()
}
