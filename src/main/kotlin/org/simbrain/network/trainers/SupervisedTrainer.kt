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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.events.TrainerEvents
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.rowVectorTransposed
import org.simbrain.util.sse
import smile.math.matrix.Matrix
import kotlin.math.sqrt
import kotlin.random.Random


/**
 * Manage iteration based training algorithms and provides an object that can be edited in the GUI.
 */
abstract class SupervisedTrainer<SN: SupervisedNetwork> : EditableObject {

    @UserParameter(label = "Learning Rate", increment = .01, minimumValue = 0.0, order = 1)
    var learningRate = .01

    @UserParameter(label = "Update type", order = 2)
    open var updateType: UpdateMethod = UpdateMethod.Epoch()

    @UserParameter(
        label = "Aggregation Function",
        description = "How to aggregate error and present it",
        order = 3,
        showDetails = false)
    var aggregationFunction: AggregationFunction = AggregationFunction.Sum()

    var stoppingCondition by GuiEditable(
        initValue = StoppingCondition(),
        order = 4
    )

    var iteration = 0

    /**
     * Used when reopening the trainer controls so user knows where things left off
     */
    var lastError = 0.0

    var isRunning = false

    private var stoppingConditionReached = false

    @Transient val events = TrainerEvents()

    context(Network)
    suspend fun SN.startTraining() {
        if (stoppingConditionReached) {
            stoppingConditionReached = false
            iteration = 0
            events.iterationReset.fire()
        }
        isRunning = true
        events.beginTraining.fire().await()
        withContext(Dispatchers.Default) {
            while (isRunning) {
                trainOnce()
                if (stoppingCondition.validate(iteration, aggregationFunction.aggregatedError.await())) {
                    stoppingConditionReached = true
                    stopTraining()
                }
            }
        }
    }

    suspend fun stopTraining() {
        isRunning = false
        events.endTraining.fire()
    }

    context(Network)
    suspend fun SN.train(iterations: Int) {
        repeat(iterations) {
            trainOnce()
        }
    }

    context(Network, SN)
    suspend fun trainOnce() {
        iteration++
        with(updateType) {
            aggregationFunction.reset()
            when (this) {
                is UpdateMethod.Stochastic -> aggregationFunction.accumulateError(trainRow(Random.nextInt(trainingSet.inputs.nrow())))
                is UpdateMethod.Epoch -> aggregationFunction.accumulateError(
                    trainBatch(0 until trainingSet.size)
                )
                is UpdateMethod.Batch -> {
                    val startIndex = Random.nextInt(0, trainingSet.size - batchSize + 1)
                    val endIndex = startIndex + batchSize
                    aggregationFunction.accumulateError(trainBatch(startIndex until  endIndex))
                }
            }
            aggregationFunction.complete()
        }
        lastError = aggregationFunction.aggregatedError.await()
        events.errorUpdated.fire(aggregationFunction)
    }

    context(Network)
    abstract fun SN.trainRow(rowNum: Int): Double

    context(Network)
    open fun SN.trainBatch(rowRange: IntRange): Double {
        var error = 0.0
        for (i in rowRange) {
            error += trainRow(i)
        }
        return error
    }

    sealed class UpdateMethod: CopyableObject {
        class Stochastic : UpdateMethod() {
            override fun copy() = this
        }

        class Epoch : UpdateMethod() {
            override fun copy() = this
        }

        class Batch(@UserParameter(label = "Batch Size", order = 1) var batchSize: Int = 5) : UpdateMethod() {
            override fun copy() = Batch(batchSize)
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
    }

    /**
     * How to aggregate a trainer's scalar error into what is displayed. Others call this a reduction function.
     */
    sealed class AggregationFunction: CopyableObject {

        protected var runningError = 0.0

        protected var runningCount = 0

        abstract var aggregatedError: Deferred<Double>

        abstract fun accumulateError(error: Double)

        protected fun completeWithResult(result: Double) {
            (aggregatedError as CompletableDeferred<Double>).complete(result)
        }

        fun reset() {
            aggregatedError = CompletableDeferred()
            runningError = 0.0
            runningCount = 0
        }

        abstract fun complete()

        class Mean : AggregationFunction() {
            override fun accumulateError(error: Double) {
                runningError += error
                runningCount++
            }

            @Transient override var aggregatedError: Deferred<Double> = CompletableDeferred()

            override fun copy() = Mean()

            override fun complete() {
                completeWithResult(runningError / runningCount)
            }

            override val name: String = "Mean Squared Error"
        }

        class Sum : AggregationFunction() {
            override fun accumulateError(error: Double) {
                runningError += error
                runningCount++
            }

            @Transient override var aggregatedError: Deferred<Double> = CompletableDeferred()

            override fun copy() = Sum()

            override fun complete() {
                completeWithResult(runningError)
            }

            override val name: String = "Sum Squared Error"
        }

        class RootMean : AggregationFunction() {
            override fun accumulateError(error: Double) {
                runningError += error
                runningCount++
            }

            @Transient override var aggregatedError: Deferred<Double> = CompletableDeferred()

            override fun copy() = RootMean()

            override fun complete() {
                completeWithResult(sqrt(runningError / runningCount))
            }

            override val name: String = "Root Mean Squared Error"
        }

        override fun getTypeList(): List<Class<out CopyableObject>>? {
            return listOf(
                Mean::class.java,
                Sum::class.java,
                RootMean::class.java
            )
        }
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

        override fun copy(): CopyableObject {
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

}

class BackpropTrainer : SupervisedTrainer<BackpropNetwork>() {

    context(Network)
    override fun BackpropNetwork.trainRow(rowNum: Int): Double {
        inputLayer.setActivations(trainingSet.inputs.row(rowNum))
        val targetVec = trainingSet.targets.rowVectorTransposed(rowNum)
        wmList.forwardPass(inputLayer.activations)
        return wmList.applyBackprop(targetVec, epsilon = learningRate, lossFunction = Matrix::sse)
    }

    context(Network)
    override fun BackpropNetwork.trainBatch(rowRange: IntRange): Double {

        val weightAccumulator: HashMap<WeightMatrix, Matrix> = HashMap()
        val biasesAccumulator: HashMap<NeuronArray, Matrix> = HashMap()

        var error = 0.0

        for (i in rowRange) {
            inputLayer.setActivations(trainingSet.inputs.row(i))
            val targetVec = trainingSet.targets.rowVectorTransposed(i)
            wmList.forwardPass(inputLayer.activations)
            error += wmList.accumulateBackprop(targetVec, weightAccumulator, biasesAccumulator, lossFunction = Matrix::sse)
        }

        weightAccumulator.forEach { (wm, delta) ->
            wm.weightMatrix.add(delta.mul(trainer.learningRate))
        }

        biasesAccumulator.forEach { (na, delta) ->
            na.biases.add(delta.mul(trainer.learningRate))
        }

        return error
    }

}

class SRNTrainer : SupervisedTrainer<SRNNetwork>() {

    override var updateType: UpdateMethod by GuiEditable(
        initValue = UpdateMethod.Epoch(),
        typeMapProvider = UpdateMethod::srnTypeList
    )

    context(Network)
    override fun SRNNetwork.trainRow(rowNum: Int): Double {
        val targetVec = trainingSet.targets.rowVectorTransposed(rowNum)
        val inputVec = trainingSet.inputs.rowVectorTransposed(rowNum)

        inputLayer.activations = inputVec
        update()
        return weightMatrixTree.applyBackprop(targetVec, epsilon = learningRate)
    }

}