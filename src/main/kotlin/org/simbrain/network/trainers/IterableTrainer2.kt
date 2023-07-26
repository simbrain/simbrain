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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simbrain.network.events.TrainerEvents2
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.LMSNetwork
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.rowVectorTransposed
import kotlin.random.Random


// Randomization / Initialization strategy
// Stopping Condition. See iterable trainer.
// Loss Function
// See API for kotlindl

abstract class IterableTrainer2(val net: Trainable2): EditableObject {

    @UserParameter(label = "Learning Rate", order = 1)
    var learningRate = .01

    @UserParameter(label = "Update type", order = 1)
    var updateType = UpdateMethod.STOCHASTIC

    var iteration = 0

    var error = 0.0

    var isRunning = false

    val events = TrainerEvents2()

    suspend fun startTraining() {
        isRunning = true
        events.beginTraining.fireAndForget()
        withContext(Dispatchers.Default) {
            while(isRunning) {
                iterate()
            }
        }
    }

    suspend fun stopTraining() {
        isRunning = false
        events.endTraining.fireAndForget()
    }

    suspend fun iterate(iterations: Int) {
        repeat(iterations) {
            iterate()
        }
    }

    suspend fun iterate() {
        iteration++
        if (updateType == UpdateMethod.STOCHASTIC) {
            trainRow(Random.nextInt(net.trainingSet.inputs.nrow()))
        } else if (updateType == UpdateMethod.EPOCH) {
            // TODO: Batch updating
            var totalError = 0.0
            for (i in 0 until net.trainingSet.size) {
                trainRow(i)
                totalError += error
            }
            error = totalError
        }
        events.errorUpdated.fire(error)
    }

    abstract fun trainRow(rowNum: Int)

    open fun applyInputs(rowNum: Int) {
        net.inputLayer.activations = net.trainingSet.inputs.rowVectorTransposed(rowNum)
        net.update()
    }

    abstract fun randomize()

    // TODO: Better name?
    enum class UpdateMethod {
        EPOCH { override fun toString() = "Epoch (whole dataset per iteration)" },
        STOCHASTIC { override fun toString() = "Stochastic (random row per iteration)" },
        SINGLE { override fun toString() = "Single (one row per iteration)" }
    }

}

class LMSTrainer2(val lmsNet: LMSNetwork) : IterableTrainer2(lmsNet) {

    override fun trainRow(rowNum: Int) {
        if (rowNum !in 0 until lmsNet.trainingSet.inputs.nrow()) {
            throw IllegalArgumentException("Trying to train invalid row number $rowNum")
        }
        val targetVec = lmsNet.trainingSet.targets.rowVectorTransposed(rowNum)
        lmsNet.inputLayer.isClamped = true
        lmsNet.inputLayer.setActivations(lmsNet.trainingSet.inputs.row(rowNum))
        lmsNet.update()
        val outputs = lmsNet.outputLayer.activations
        val rowError = targetVec.sub(outputs)
        lmsNet.weightMatrix.applyLMS(rowError, learningRate)
        error = rowError.transpose().mm(rowError).sum()
    }

    override fun randomize() {
        lmsNet.randomize()
    }

}

class BackpropTrainer2(val bp: BackpropNetwork) : IterableTrainer2(bp) {

    override fun trainRow(rowNum: Int) {
        bp.inputLayer.setActivations(bp.trainingSet.inputs.row(rowNum))
        val targetVec = bp.trainingSet.targets.rowVectorTransposed(rowNum)
        error = bp.wmList.applyBackprop(bp.inputLayer.activations, targetVec)
    }

    override fun randomize() {
        bp.randomize()
    }

}

class SRNTrainer(val srn: SRNNetwork) : IterableTrainer2(srn) {

    val weightMatrixTree = WeightMatrixTree(listOf(srn.inputLayer, srn.contextLayer), srn.outputLayer)

    init {
        updateType = UpdateMethod.EPOCH
    }

    override fun trainRow(rowNum: Int) {

        val targetVec = srn.trainingSet.targets.rowVectorTransposed(rowNum)
        val inputVec = srn.trainingSet.inputs.rowVectorTransposed(rowNum)

        srn.inputLayer.activations = inputVec
        srn.update()
        error = weightMatrixTree.applyBackprop(
            listOf(),
            targetVec,
            epsilon = learningRate,
            forwardPass = false
        )
    }

    override fun randomize() {
        srn.randomize()
    }

}