/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.trainers

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.events.TrainerEvents
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.SRNNetwork
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
abstract class SupervisedTrainer<SN: SupervisedNetwork>(val network: Network, val supervisedNetwork: SN) : CoroutineScope {

    private var job = SupervisorJob()

    final override val coroutineContext = Dispatchers.Default + job

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

    protected abstract fun trainRow(rowNum: Int): Double

    /**
     * @return the mean error for the batch
     */
    open fun trainBatch(rowRange: IntRange, probe: TrainerProbe? = null): Double {
        var batchError = 0.0
        for (i in rowRange) {
            batchError += trainRow(i)
        }
        return batchError / rowRange.count()
    }

    /**
     * Compute the error on the testing set
     */
    open suspend fun computeTestError(): Double {
        return supervisedNetwork.testingSet.sumOf { (input, target) ->
            supervisedNetwork.inputLayer.activations = input
            with(network) { supervisedNetwork.forwardPass() }
            val output = supervisedNetwork.outputLayer.activations
            config.lossFunction.scalarLoss(output, target)
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

class BackpropTrainer(network: Network, backpropNetwork: BackpropNetwork) : SupervisedTrainer<BackpropNetwork>(network, backpropNetwork) {

    override fun trainRow(rowNum: Int): Double {
        supervisedNetwork.inputLayer.setActivations(supervisedNetwork.trainingSet.inputs.row(rowNum))
        val targetVec = supervisedNetwork.trainingSet.targets.rowVectorTransposed(rowNum)
        return with(network) {
            supervisedNetwork.wmList.forwardPass(supervisedNetwork.inputLayer.activations)
            supervisedNetwork.wmList.applyBackprop(targetVec, epsilon = config.learningRate, lossFunction = config.lossFunction)
        }
    }

    /**
     * Backprop trains using error accumulation.
     */
    override fun trainBatch(rowRange: IntRange, probe: TrainerProbe?): Double {

        val weightAccumulator: HashMap<WeightMatrix, Matrix> = HashMap()
        val biasesAccumulator: HashMap<NeuronArray, Matrix> = HashMap()

        val trainBatchContext = probe?.newContext("trainBatch")

        var error = 0.0

        for (i in rowRange) {
            supervisedNetwork.inputLayer.setActivations(supervisedNetwork.trainingSet.inputs.row(i))
            val targetVec = supervisedNetwork.trainingSet.targets.rowVectorTransposed(i)
            with(network) {
                supervisedNetwork.wmList.forwardPass(supervisedNetwork.inputLayer.activations)
                error += supervisedNetwork.wmList.accumulateBackprop(targetVec, weightAccumulator, biasesAccumulator, lossFunction = config.lossFunction)
            }

        }

        val weightAccumulatorContext = trainBatchContext?.newContext("weightAccumulators")

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

        trainBatchContext?.newContext("biasesAccumulator")?.writeAll(biasesAccumulator) { na, delta ->
            na.displayName to delta
        }

        val computeDeltaContext = trainBatchContext?.newContext("computeDelta")

        biasesAccumulator.forEach { (na, delta) ->
            val delta = config.optimizer.computeDelta(na.biases, delta)
            computeDeltaContext?.write(na.displayName, delta)
            na.biases.add(delta)
            na.events.updated.fire()
        }

        trainBatchContext?.newContext("updatedBiases")?.writeAll(biasesAccumulator) { na, _ -> na.displayName to na.biases.clone() }

        return error / rowRange.count()
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

class SRNTrainer(network: Network, srnNetwork: SRNNetwork) : SupervisedTrainer<SRNNetwork>(network, srnNetwork) {

    override fun trainRow(rowNum: Int): Double {
        val targetVec = supervisedNetwork.trainingSet.targets.rowVectorTransposed(rowNum)
        val inputVec = supervisedNetwork.trainingSet.inputs.rowVectorTransposed(rowNum)

        supervisedNetwork.inputLayer.activations = inputVec
        network.update() // This sets the context layer so that backprop on the tree can be applied
        return supervisedNetwork.weightMatrixTree.applyBackprop(
            targetVec,
            lossFunction = supervisedNetwork.trainerConfig.lossFunction,
            epsilon = supervisedNetwork.trainerConfig.learningRate
        )
    }

}
