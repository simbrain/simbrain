package org.simbrain.network.trainers

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.simbrain.network.core.*
import org.simbrain.network.events.TrainerEvents
import org.simbrain.network.trainers.SupervisedTrainer.*
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.rowVectorTransposed
import smile.math.matrix.Matrix
import kotlin.random.Random
import kotlin.reflect.KFunction


/**
 * Editable config object for supervised trainer.
 */
open class SupervisedTrainerConfig(lossFunctionProvider: KFunction<List<Class<out EditableObject>>>? = null): CopyableObject {

    var lossFunction:BackpropLossFunction by GuiEditable(
        initValue = BackpropLossFunction.SSE,
        typeMapProvider = lossFunctionProvider
    )

    var optimizer: Optimizer by GuiEditable(
        initValue = AdamOptimizer(),
        showDetails = false,
        order = 20
    )

    @UserParameter(label = "Update type", showDetails = false, order = 30)
    open var updateType: UpdateMethod = UpdateMethod.Epoch()

    var weightInitializationStrategy: WeightInitializationStrategy by GuiEditable(
        initValue = Xavier(),
        showDetails = false,
        order = 40
    )

    var stoppingCondition by GuiEditable(
        initValue = StoppingCondition(),
        showDetails = false,
        order = 50
    )

    var testConfiguration by GuiEditable(
        initValue = TestConfiguration(),
        showDetails = false,
        order = 60
    )

    override val name = "Optimizer Properties"

    var learningRate by optimizer::learningRate

    fun <T: SupervisedTrainerConfig> copyCurrentInto(toCopy: T) = toCopy.also {
        it.lossFunction = lossFunction
        it.optimizer = optimizer.copy()
        it.updateType = updateType.copy()
        it.weightInitializationStrategy = weightInitializationStrategy.copy()
        it.stoppingCondition = stoppingCondition.copy()
        it.testConfiguration = testConfiguration.copy()
    }

    override fun copy() = copyCurrentInto(SupervisedTrainerConfig())
}

/**
 * Manage iteration based training algorithms and provides an object that can be edited in the GUI.
 */
class SupervisedTrainer(val network: Network, val supervisedNetwork: SupervisedNetwork) : CoroutineScope {

    private var job = SupervisorJob()

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
        if (stoppingConditionReached) {
            stoppingConditionReached = false
            iteration = 0
            events.iterationReset.fire()
        }
        isRunning = true
        events.beginTraining.fire().await()
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
        with(config.updateType) {
            lastTrainingError = when (this) {
                is UpdateMethod.Stochastic -> trainRow(Random.nextInt(supervisedNetwork.trainingSet.inputs.nrow()))
                is UpdateMethod.Epoch -> trainBatch(0 until supervisedNetwork.trainingSet.size)
                is UpdateMethod.Batch -> {
                    val startIndex = Random.nextInt(0, supervisedNetwork.trainingSet.size - batchSize + 1)
                    val endIndex = startIndex + batchSize
                    trainBatch(startIndex until  endIndex)
                }
            }
        }
        val testError = if (config.testConfiguration.enabled && iteration % config.testConfiguration.testFrequency == 1) {
            computeTestError()
        } else {
            null
        }
        events.errorUpdated.fire(lastTrainingError to testError).await()
        if (isRunning) {
            if (config.stoppingCondition.validate(iteration, lastTrainingError)) {
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
                inputLayer.setActivations(trainingSet.inputs.row(rowNum))
                
                // Handle target reshaping for ActivationSequence outputs
                val targetVec = if (outputLayer is ActivationSequence) {
                    // Reshape flattened target back to sequence format
                    val flatTarget = trainingSet.targets.row(rowNum)
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
                    trainingSet.targets.rowVectorTransposed(rowNum)
                }
                
                with(network) {
                    forwardPass()
                    rowProbeContext?.write("forwardPassOutputActivations", outputLayer.activations.clone())
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

        val weightAccumulatorContext = probeContext?.createMapProbe("weightAccumulators")

        weightAccumulatorContext?.writeAll(weightAccumulator) { wm, delta ->
            wm.displayName to delta
        }

        weightAccumulator.forEach { (wm, delta) ->
            val weightsDelta = config.optimizer.computeDelta(wm.weights, delta)
            weightAccumulatorContext?.write("delta_${wm.displayName}") { weightsDelta.clone() }

            wm.weights.add(weightsDelta)
            weightAccumulatorContext?.write("weights_${wm.displayName}") { wm.weights.clone() }

            wm.events.updated.fire()
        }

        weightAccumulatorContext?.writeAll(synapseGroupAccumulator) { sg, delta ->
            sg.displayName to delta
        }

        synapseGroupAccumulator.forEach { (sg, delta) ->
            val weightMatrix = sg.getWeightMatrix()
            val delta = config.optimizer.computeDelta(weightMatrix, delta)
            weightAccumulatorContext?.write("delta_${sg.displayName}") { delta.clone() }

            sg.setWeightMatrix(weightMatrix.add(delta))
            weightAccumulatorContext?.write("weights_${sg.displayName}") { sg.getWeightMatrix().clone() }

            sg.events.updated.fire()
        }


        probeContext?.createMapProbe("biasesAccumulator")?.writeAll(biasesAccumulator) { na, delta ->
            na.displayName to delta
        }

        val computeDeltaContext = probeContext?.createMapProbe("computeDelta")

        biasesAccumulator.forEach { (na, delta) ->
            val delta = config.optimizer.computeDelta(na.biases, delta)
            computeDeltaContext?.write(na.displayName, delta)
            na.biases = na.biases.add(delta)
            na.events.updated.fire()
        }

        probeContext?.createMapProbe("updatedBiases")?.writeAll(biasesAccumulator) { na, _ -> na.displayName to na.biases.clone() }

        rawMatrixAccumulator.forEach { (matrix, delta) ->
            matrix.add(config.optimizer.computeDelta(matrix, delta))
        }

        probeContext?.write("rawMatrixAccumulator", rawMatrixAccumulator)

        return error / rowRange.count()
    }

    /**
     * Compute the error on the testing set
     */
    open suspend fun computeTestError(): Double {
        return supervisedNetwork.testingSet.sumOf { (input, target) ->
            supervisedNetwork.inputLayer.activations = input
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
                        reshapedTarget[seqPos, vocabPos] = target[flatIndex, 0]
                    }
                }
                reshapedTarget
            } else {
                target
            }
            
            config.lossFunction.scalarLoss(output, reshapedTarget)
        } / supervisedNetwork.testingSet.size
    }

    sealed class UpdateMethod: CopyableObject {
        class Stochastic : UpdateMethod() {
            override fun copy() = this
            override fun toString() = "Batch = 1"
        }

        class Epoch : UpdateMethod() {
            override fun copy() = this
            override fun toString() = "Epoch"
        }

        class Batch(@UserParameter(label = "Batch Size", minimumValue = 1.0, order = 1) var batchSize: Int = 5) : UpdateMethod() {
            override fun copy() = Batch(batchSize)
            override fun toString() = "Batch = $batchSize"
        }

        override fun getTypeList(): List<Class<out CopyableObject>>? {
            return listOf(
                Stochastic::class.java,
                Epoch::class.java,
                Batch::class.java
            )
        }

        /**
         * Given the temporal nature of the rule, only Epoch should be used with SRN
         */
        fun srnTypeList() = listOf(Epoch::class.java)

        abstract override fun copy(): UpdateMethod
    }

    class StoppingCondition: CopyableObject {
        var maxIterations by GuiEditable(
            initValue = 10_000,
            order = 1
        )
        var useErrorThreshold by GuiEditable(
            initValue = false,
            order = 2
        )
        var errorThreshold by GuiEditable(
            0.1,
            order = 3,
            conditionallyEnabledBy = StoppingCondition::useErrorThreshold
        )

        override fun copy(): StoppingCondition {
            return StoppingCondition().also {
                it.maxIterations = maxIterations
                it.useErrorThreshold = useErrorThreshold
                it.errorThreshold = errorThreshold
            }
        }

        fun validate(iterations: Int, error: Double): Boolean {
            return iterations >= maxIterations || (useErrorThreshold && error < errorThreshold)
        }
    }

    class TestConfiguration: CopyableObject {
        var enabled by GuiEditable(
            initValue = true,
            order = 1
        )

        var testFrequency by GuiEditable(
            initValue = 10,
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
    override var updateType: UpdateMethod by GuiEditable(
        initValue = UpdateMethod.Epoch(),
        typeMapProvider = UpdateMethod::srnTypeList, // Only allow epoch for SRN
        order = 3
    )

    override fun copy(): SRNTrainerConfig {
        return copyCurrentInto(SRNTrainerConfig()).also {
            it.updateType = updateType
        }
    }
}
