package org.simbrain.network.trainers

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.simbrain.network.core.*
import org.simbrain.network.events.TrainerEvents
import org.simbrain.network.events.TrainingStats
import org.simbrain.network.trainers.SupervisedTrainer.*
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.toColumnVector
import smile.math.matrix.Matrix
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.reflect.KFunction


/**
 * Editable config object for supervised trainer.
 */
open class SupervisedTrainerConfig(lossFunctionProvider: KFunction<List<Class<out EditableObject>>>? = null): CopyableObject {

    var lossFunction:BackpropLossFunction by GuiEditable(
        initValue = BackpropLossFunction.SSE,
        description = "Loss function used to measure prediction error. SSE/MSE for regression; cross Entropy for classification",
        typeMapProvider = lossFunctionProvider
    )

    var optimizer: Optimizer by GuiEditable(
        initValue = AdamOptimizer(),
        description = "Optimization algorithm for updating network weights.",
        showDetails = false,
        order = 20
    )

    @UserParameter(
        label = "Update type", 
        description = "Training update method. Stochastic updates after each random example; Sequential Online processes all examples in order; Epoch processes entire dataset; Batch processes fixed-size random batches",
        showDetails = false, 
        order = 30
    )
    open var updateType: UpdateMethod = UpdateMethod.Epoch()

    var weightInitializationStrategy: WeightInitializationStrategy by GuiEditable(
        initValue = Xavier(),
        description = "Strategy for initializing network weights. Xavier for general use, He for ReLU networks, LeCun for SELU networks",
        showDetails = false,
        order = 40
    )

    var stoppingCondition by GuiEditable(
        initValue = StoppingCondition(),
        description = "Conditions for automatically stopping training. Can stop after max iterations, error threshold, or early stopping",
        showDetails = false,
        order = 50
    )

    var testConfiguration by GuiEditable(
        initValue = TestConfiguration(),
        description = "Settings for testing the network on validation data during training to monitor generalization performance",
        showDetails = false,
        order = 60
    )

    @UserParameter(
        label = "Compute accuracy",
        description = "Calculate and display classification accuracy for networks with one-hot encoded targets",
        order = 70
    )
    var computeAccuracy by GuiEditable(
        initValue = false,
        description = "Calculate and display classification accuracy for networks with one-hot encoded targets",
        order = 70
    )

    override val name = "Optimizer properties"

    var learningRate by optimizer::learningRate

    fun <T: SupervisedTrainerConfig> copyCurrentInto(toCopy: T) = toCopy.also {
        it.lossFunction = lossFunction
        it.optimizer = optimizer.copy()
        it.updateType = updateType.copy()
        it.weightInitializationStrategy = weightInitializationStrategy.copy()
        it.stoppingCondition = stoppingCondition.copy()
        it.testConfiguration = testConfiguration.copy()
        it.computeAccuracy = computeAccuracy
    }

    override fun copy() = copyCurrentInto(SupervisedTrainerConfig())
}

/**
 * Manage iteration-based training algorithms for [SupervisedNetwork], which in turn has a [SupervisedTrainerConfig].
 */
open class SupervisedTrainer(val network: Network, val supervisedNetwork: SupervisedNetwork) : CoroutineScope {

    val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Default + job

    val config = supervisedNetwork.trainerConfig

    var iteration = 0
        set(value) {
            field = value
            if (value == 0) {
                events.iterationReset.fire()
                config.optimizer.reset()
            }
        }

    /**
     * Used when reopening the trainer controls so user knows where things left off
     */
    var lastTrainingError = 0.0

    /**
     * Last training accuracy (only available for softmax networks)
     */
    var lastTrainingAccuracy: Double? = null

    /**
     * Last testing accuracy (only available for softmax networks)
     */
    var lastTestingAccuracy: Double? = null

    /**
     * Accumulator for batch accuracy calculation (avoids recomputing forward passes)
     */
    private var batchAccuracySum = 0.0
    private var batchSampleCount = 0

    /**
     * RMS of the optimizer's per-parameter update from the most recent training iteration.
     * Computed across all weights, synapse-group weights, and biases that were updated.
     */
    var lastEffectiveStepSize: Double? = null
        private set

    private var stepSquaredSum = 0.0
    private var stepParamCount = 0

    private fun recordOptimizerUpdate(update: Matrix) {
        for (i in 0 until update.nrow()) {
            for (j in 0 until update.ncol()) {
                val v = update[i, j]
                stepSquaredSum += v * v
            }
        }
        stepParamCount += update.nrow() * update.ncol()
    }

    var isRunning = false

    private var stoppingConditionReached = false

    val events = TrainerEvents()

    // Task queue
    val processorChannel = Channel<Pair<TrainerTask, CompletableDeferred<Unit>>>(capacity = Channel.UNLIMITED)

    init {
        // Wait for incoming tasks and ensures each one is completed before the next one begins
        launch(coroutineContext) {
            for (event in processorChannel) {
                val (task, signal) = event
                when (task) {
                    TrainerTask.Start -> startTrainingHandler()
                    TrainerTask.Train -> trainOnceHandler()
                    TrainerTask.Stop -> stopTrainingHandler()
                    TrainerTask.Randomize -> {
                        supervisedNetwork.initWeights()
                        supervisedNetwork.initBiases()
                        supervisedNetwork.trainerConfig.optimizer.reset()
                    }
                }
                signal.complete(Unit)
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
        // Clear stopping condition flag to allow continued training
        stoppingConditionReached = false

        // Reset early stopping state when training starts
        config.stoppingCondition.resetEarlyStopping()
        isRunning = true
        events.beginTraining.fire()
        submitTask(TrainerTask.Train)
    }

    suspend fun stopTraining() {
        submitTask(TrainerTask.Stop).await()
    }

    private suspend fun stopTrainingHandler() {
        isRunning = false
        events.endTraining.fire()
    }

    suspend fun trainOnce() {
        submitTask(TrainerTask.Train).await()
    }

    private suspend fun trainOnceHandler() {
        iteration++

        // Reset batch accuracy accumulators
        batchAccuracySum = 0.0
        batchSampleCount = 0
        stepSquaredSum = 0.0
        stepParamCount = 0
        
        with(config.updateType) {
            lastTrainingError = when (this) {
                is UpdateMethod.Stochastic -> trainRow(Random.nextInt(supervisedNetwork.trainingSet.size))
                is UpdateMethod.Epoch -> trainBatch(0 until supervisedNetwork.trainingSet.size)
                is UpdateMethod.Batch -> {
                    val startIndex = Random.nextInt(0, supervisedNetwork.trainingSet.size - batchSize + 1)
                    val endIndex = startIndex + batchSize
                    trainBatch(startIndex until  endIndex)
                }
                is UpdateMethod.SequentialOnline -> {
                    // Process all examples in order, updating weights after each one
                    var totalError = 0.0
                    for (i in 0 until supervisedNetwork.trainingSet.size) {
                        totalError += trainBatch(i until i + 1)
                    }
                    totalError / supervisedNetwork.trainingSet.size
                }
            }
        }
        
        // Use accumulated accuracy from batch training (much faster!)
        lastTrainingAccuracy = if (config.computeAccuracy && batchSampleCount > 0) {
            batchAccuracySum / batchSampleCount
        } else {
            null
        }

        lastEffectiveStepSize = if (stepParamCount > 0) sqrt(stepSquaredSum / stepParamCount) else null
        
        val shouldComputeTest = config.testConfiguration.enabled && supervisedNetwork.testingSet.size > 0 && 
                                iteration % config.testConfiguration.testFrequency == 0
        
        val testError = if (shouldComputeTest) {
            computeTestError()
        } else {
            null
        }
        
        // Compute test accuracy when test error is computed and accuracy is enabled
        val testAccuracy = if (shouldComputeTest && config.computeAccuracy) {
            lastTestingAccuracy = computeTestAccuracy()
            lastTestingAccuracy
        } else {
            null
        }
        
        val trainingStats = TrainingStats(
            trainingError = lastTrainingError,
            testingError = testError,
            trainingAccuracy = lastTrainingAccuracy,
            testingAccuracy = testAccuracy,
            effectiveStepSize = lastEffectiveStepSize
        )
        events.errorUpdated.fire(trainingStats)
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

    suspend fun randomize() {
        submitTask(TrainerTask.Randomize).await()
    }

    protected open fun trainRow(rowNum: Int): Double {
        return trainBatch(rowNum until rowNum + 1)
    }

    /**
     * @return the mean error for the batch
     */
    open fun trainBatch(rowRange: IntRange, probe: StructuredProbe? = null): Double {
        val weightAccumulator: HashMap<WeightMatrix, Matrix> = HashMap()
        val synapseGroupAccumulator: HashMap<SynapseGroup, Matrix> = HashMap()
        val biasesAccumulator: HashMap<Layer, Matrix> = HashMap()
        val rawMatrixAccumulator: HashMap<Matrix, Matrix> = HashMap()

        val probeContext = probe?.createMapProbe("trainBatch")

        val error = with(supervisedNetwork) {
            rowRange.sumOf { rowNum ->
                val rowProbeContext = probeContext?.createMapProbe("trainRow-$rowNum")
                inputLayer.setActivations(trainingSet.inputs[rowNum].toDoubleArray())
                
                // Handle target reshaping for ActivationSequence outputs
                val targetVec = if (outputLayer is ActivationSequence) {
                    // Reshape flattened target back to sequence format
                    val flatTarget = trainingSet.targets[rowNum].toDoubleArray()
                    val sequenceLayer = outputLayer as ActivationSequence
                    val sequenceSize = sequenceLayer.sequenceSize
                    val vocabSize = sequenceLayer.size
                    
                    // Reshape from (1, sequenceSize * vocabSize) to (sequenceSize, vocabSize)
                    val reshapedTarget = Matrix(sequenceSize, vocabSize)
                    for (seqPos in 0 until sequenceSize) {
                        for (vocabPos in 0 until vocabSize) {
                            val flatIndex = seqPos * vocabSize + vocabPos
                            reshapedTarget[seqPos, vocabPos] = flatTarget[flatIndex]
                        }
                    }
                    reshapedTarget
                } else {
                    trainingSet.targets[rowNum].toDoubleArray().toColumnVector()
                }
                
                with(network) {
                    forwardPass()
                    rowProbeContext?.write("forwardPassOutputActivations", outputLayer.activations.clone())
                    
                    // Accumulate accuracy during training (reuse forward pass results)
                    if (config.computeAccuracy) {
                        val accuracy = classificationAccuracy(outputLayer.activations, targetVec)
                        if (accuracy != null) {
                            batchAccuracySum += accuracy
                            batchSampleCount++
                        }
                    }
                    
                    supervisedNetwork.layers.accumulateBackprop(
                        listOf(inputLayer),
                        targetVec,
                        outputLayer,
                        weightAccumulator,
                        synapseGroupAccumulator,
                        biasesAccumulator,
                        rawMatrixAccumulator,
                        lossFunction = config.lossFunction,
                        probe = rowProbeContext
                    )
                }
            }
        }

        // accumulateBackprop sums per-row deltas. Average them here so the optimizer sees a
        // per-sample-equivalent gradient, regardless of batch size. Adam-family optimizers are
        // scale-invariant (the constant cancels in m̂/√v̂), but non-normalizing optimizers like
        // BasicOptimizer need this averaging to stay stable as batch size changes. CnnTrainer already
        // does the same scaling.
        val batchScale = 1.0 / rowRange.count().coerceAtLeast(1)

        applyAccumulatedDeltas(
            weightAccumulator,
            synapseGroupAccumulator,
            biasesAccumulator,
            rawMatrixAccumulator,
            batchScale,
            probeContext
        )

        return error / rowRange.count()
    }

    /**
     * Hand the accumulated deltas to the optimizer and write them into the network's parameters. One
     * call is one optimizer step, so a trainer that updates more than once per iteration, such as
     * [BPTTTrainer] with its one step per unrolled window, calls this repeatedly.
     */
    protected fun applyAccumulatedDeltas(
        weightAccumulator: HashMap<WeightMatrix, Matrix>,
        synapseGroupAccumulator: HashMap<SynapseGroup, Matrix>,
        biasesAccumulator: HashMap<Layer, Matrix>,
        rawMatrixAccumulator: HashMap<Matrix, Matrix>,
        batchScale: Double,
        probeContext: StructuredProbe.MapProbe? = null
    ) {
        val weightAccumulatorContext = probeContext?.createMapProbe("weightAccumulators")

        weightAccumulatorContext?.writeAll(weightAccumulator) { wm, delta ->
            wm.displayName to delta
        }

        weightAccumulator.forEach { (wm, delta) ->
            val weightsDelta = config.optimizer.computeDelta(wm.weights, delta.clone().mul(batchScale))
            weightAccumulatorContext?.write("delta_${wm.displayName}") { weightsDelta.clone() }
            recordOptimizerUpdate(weightsDelta)

            wm.weights.add(weightsDelta)
            weightAccumulatorContext?.write("weights_${wm.displayName}") { wm.weights.clone() }

            wm.events.updated.fire()
        }

        weightAccumulatorContext?.writeAll(synapseGroupAccumulator) { sg, delta ->
            sg.displayName to delta
        }

        synapseGroupAccumulator.forEach { (sg, delta) ->
            val weightMatrix = sg.getWeightMatrix()
            val delta = config.optimizer.computeDelta(weightMatrix, delta.clone().mul(batchScale))
            weightAccumulatorContext?.write("delta_${sg.displayName}") { delta.clone() }
            recordOptimizerUpdate(delta)

            sg.setWeightMatrix(weightMatrix.add(delta))
            weightAccumulatorContext?.write("weights_${sg.displayName}") { sg.getWeightMatrix().clone() }

            sg.events.updated.fire()
        }


        probeContext?.createMapProbe("biasesAccumulator")?.writeAll(biasesAccumulator) { na, delta ->
            na.displayName to delta
        }

        val computeDeltaContext = probeContext?.createMapProbe("computeDelta")

        // A clamped layer's activations are forced rather than computed, so its biases cannot affect
        // anything the network produces. Applying deltas to them would let them drift on every batch,
        // and under time-unrolled training that drift compounds once per timestep. Deltas are still
        // accumulated above so that every Layer implementation stays comparable via probes.
        biasesAccumulator.filterKeys { !it.isClamped }.forEach { (na, delta) ->
            val delta = config.optimizer.computeDelta(na.biases, delta.clone().mul(batchScale))
            computeDeltaContext?.write(na.displayName, delta)
            recordOptimizerUpdate(delta)
            na.biases = na.biases.add(delta)
            na.events.updated.fire()
        }

        probeContext?.createMapProbe("updatedBiases")?.writeAll(biasesAccumulator) { na, _ -> na.displayName to na.biases.clone() }

        rawMatrixAccumulator.forEach { (matrix, delta) ->
            val update = config.optimizer.computeDelta(matrix, delta.clone().mul(batchScale))
            recordOptimizerUpdate(update)
            matrix.add(update)
        }

        probeContext?.write("rawMatrixAccumulator", rawMatrixAccumulator)
    }

    /**
     * Compute the error on the testing set
     */
    open suspend fun computeTestError(): Double {
        return supervisedNetwork.testingSet.sumOf { (input, target) ->
            // Handle input reshaping for ActivationSequence inputs
            if (supervisedNetwork.inputLayer is ActivationSequence) {
                supervisedNetwork.inputLayer.setActivations(input.toDoubleArray())
            } else {
                supervisedNetwork.inputLayer.activations = input.toDoubleArray().toColumnVector()
            }
            with(network) { supervisedNetwork.forwardPass() }
            val output = supervisedNetwork.outputLayer.activations
            
            // Handle target reshaping for ActivationSequence outputs
            val reshapedTarget = if (supervisedNetwork.outputLayer is ActivationSequence) {
                // Reshape flattened target back to sequence format
                val sequenceLayer = supervisedNetwork.outputLayer as ActivationSequence
                val sequenceSize = sequenceLayer.sequenceSize
                val vocabSize = sequenceLayer.size
                
                // Reshape from (sequenceSize * vocabSize, 1) to (sequenceSize, vocabSize)
                val reshapedTarget = Matrix(sequenceSize, vocabSize)
                for (seqPos in 0 until sequenceSize) {
                    for (vocabPos in 0 until vocabSize) {
                        val flatIndex = seqPos * vocabSize + vocabPos
                        reshapedTarget[seqPos, vocabPos] = target[flatIndex]
                    }
                }
                reshapedTarget
            } else {
                target.toDoubleArray().toColumnVector()
            }
            
            config.lossFunction.scalarLoss(output, reshapedTarget)
        } / supervisedNetwork.testingSet.size
    }

    /**
     * Compute the accuracy on the testing set
     */
    open suspend fun computeTestAccuracy(): Double {
        return supervisedNetwork.testingSet.mapNotNull { (input, target) ->
            // Handle input reshaping for ActivationSequence inputs
            if (supervisedNetwork.inputLayer is ActivationSequence) {
                supervisedNetwork.inputLayer.setActivations(input.toDoubleArray())
            } else {
                supervisedNetwork.inputLayer.activations = input.toDoubleArray().toColumnVector()
            }
            with(network) { supervisedNetwork.forwardPass() }
            val output = supervisedNetwork.outputLayer.activations
            
            // Handle target reshaping for ActivationSequence outputs
            val reshapedTarget = if (supervisedNetwork.outputLayer is ActivationSequence) {
                // Reshape flattened target back to sequence format
                val sequenceLayer = supervisedNetwork.outputLayer as ActivationSequence
                val sequenceSize = sequenceLayer.sequenceSize
                val vocabSize = sequenceLayer.size
                
                // Reshape from (sequenceSize * vocabSize, 1) to (sequenceSize, vocabSize)
                val reshapedTarget = Matrix(sequenceSize, vocabSize)
                for (seqPos in 0 until sequenceSize) {
                    for (vocabPos in 0 until vocabSize) {
                        val flatIndex = seqPos * vocabSize + vocabPos
                        reshapedTarget[seqPos, vocabPos] = target[flatIndex]
                    }
                }
                reshapedTarget
            } else {
                target.toDoubleArray().toColumnVector()
            }
            
            classificationAccuracy(output, reshapedTarget)
        }.let { validAccuracies ->
            if (validAccuracies.isEmpty()) 0.0 else validAccuracies.sum() / validAccuracies.size
        }
    }




    sealed class UpdateMethod: CopyableObject {
        class Stochastic : UpdateMethod() {
            override fun copy() = this
            override fun toString() = "Stochastic"
        }

        class Epoch : UpdateMethod() {
            override fun copy() = this
            override fun toString() = "Epoch"
        }

        class Batch(@UserParameter(
            label = "Batch size", 
            description = "Number of training examples processed together in each update. Larger batches are more stable but use more memory",
            minimumValue = 1.0, 
            order = 1
        ) var batchSize: Int = 5) : UpdateMethod() {
            override fun copy() = Batch(batchSize)
            override fun toString() = "Batch"
        }

        class SequentialOnline : UpdateMethod() {
            override fun copy() = this
            override fun toString() = "Sequential Online"
        }

        override fun getTypeList(): List<Class<out CopyableObject>>? {
            return listOf(
                Stochastic::class.java,
                Epoch::class.java,
                Batch::class.java,
                SequentialOnline::class.java
            )
        }

        /**
         * Given the temporal nature of the rule, SequentialOnline should be used with SRN
         */
        fun srnTypeList() = listOf(SequentialOnline::class.java)

        /**
         * BPTT reads a whole epoch as one time-ordered sequence and then splits it into unrolled
         * windows itself, applying an update per window, so it takes the full row range at once.
         */
        fun bpttTypeList() = listOf(Epoch::class.java)

        abstract override fun copy(): UpdateMethod
    }

    class StoppingCondition: CopyableObject {
        var maxIterations by GuiEditable(
            initValue = 10_000,
            description = "Maximum number of training iterations before stopping automatically",
            order = 1
        )
        var useErrorThreshold by GuiEditable(
            initValue = false,
            description = "Stop training when error falls below specified threshold",
            order = 2
        )
        var errorThreshold by GuiEditable(
            0.1,
            description = "Training stops when error falls below this value",
            order = 3,
            conditionallyEnabledBy = StoppingCondition::useErrorThreshold
        )
        
        var useEarlyStopping by GuiEditable(
            initValue = false,
            order = 4,
            description = "Stop training when test error stops improving (requires test data)"
        )
        var earlyStoppingPatience by GuiEditable(
            initValue = 5,
            order = 5,
            description = "Number of test evaluations to wait for improvement before stopping",
            conditionallyEnabledBy = StoppingCondition::useEarlyStopping
        )
        var earlyStoppingMinDelta by GuiEditable(
            initValue = 0.0,
            order = 6,
            description = "Minimum improvement required to reset patience counter",
            conditionallyEnabledBy = StoppingCondition::useEarlyStopping
        )
        
        // Internal state for early stopping (not serialized)
        @Transient
        private var bestTestError = Double.MAX_VALUE
        @Transient
        private var patienceCounter = 0

        override fun copy(): StoppingCondition {
            return StoppingCondition().also {
                it.maxIterations = maxIterations
                it.useErrorThreshold = useErrorThreshold
                it.errorThreshold = errorThreshold
                it.useEarlyStopping = useEarlyStopping
                it.earlyStoppingPatience = earlyStoppingPatience
                it.earlyStoppingMinDelta = earlyStoppingMinDelta
                // Don't copy transient state - let it reset
            }
        }

        fun validate(iterations: Int, trainingError: Double, testError: Double? = null): Boolean {
            // Check standard stopping conditions
            val standardStop = iterations >= maxIterations || (useErrorThreshold && trainingError < errorThreshold)
            
            // Check early stopping if enabled and test error is available
            val earlyStop = if (useEarlyStopping && testError != null) {
                checkEarlyStopping(testError)
            } else {
                false
            }
            
            return standardStop || earlyStop
        }
        
        private fun checkEarlyStopping(testError: Double): Boolean {
            val improvement = bestTestError - testError
            
            if (improvement > earlyStoppingMinDelta) {
                // Improvement found - reset patience and update best error
                bestTestError = testError
                patienceCounter = 0
                return false
            } else {
                // No improvement - increment patience
                patienceCounter++
                return patienceCounter >= earlyStoppingPatience
            }
        }
        
        /**
         * Reset early stopping state (called when training starts)
         */
        fun resetEarlyStopping() {
            bestTestError = Double.MAX_VALUE
            patienceCounter = 0
        }
        
        /**
         * Get current early stopping status for display
         */
        fun getEarlyStoppingStatus(): String? {
            return if (useEarlyStopping) {
                "Best test error: ${if (bestTestError == Double.MAX_VALUE) "N/A" else "%.6f".format(bestTestError)}, Patience: $patienceCounter/$earlyStoppingPatience"
            } else {
                null
            }
        }
    }

    class TestConfiguration: CopyableObject {
        var enabled by GuiEditable(
            initValue = true,
            description = "Enable testing on validation data during training to monitor generalization performance",
            order = 1
        )

        var testFrequency by GuiEditable(
            initValue = 10,
            description = "How often to test on validation data (every N training iterations)",
            order = 2,
            conditionallyEnabledBy = TestConfiguration::enabled
        )

        override fun copy(): TestConfiguration {
            return TestConfiguration().also {
                it.enabled = enabled
                it.testFrequency = testFrequency
            }
        }
    }

    sealed class TrainerTask {
        object Start : TrainerTask()
        object Train : TrainerTask()
        object Stop : TrainerTask()
        object Randomize : TrainerTask()
    }

}

class SRNTrainerConfig(lossFunctionProvider: KFunction<List<Class<out EditableObject>>>? = null): SupervisedTrainerConfig(lossFunctionProvider) {

    init {
        testConfiguration.enabled = false
    }

    override var updateType: UpdateMethod by GuiEditable(
        initValue = UpdateMethod.SequentialOnline(),
        description = "Training update method. SRN networks require Sequential Online mode to properly handle temporal sequences by processing examples in order",
        typeMapProvider = UpdateMethod::srnTypeList,
        order = 3
    )

    override fun copy(): SRNTrainerConfig {
        return copyCurrentInto(SRNTrainerConfig()).also {
            it.updateType = updateType
        }
    }
}
