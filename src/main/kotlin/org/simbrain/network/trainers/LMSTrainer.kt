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
import org.simbrain.network.subnetworks.LMSNetwork
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.rowMatrixTransposed
import kotlin.random.Random


// TODO: Pull structures in common with BackpropTrainer.kt (to be written) out as IterableTrainer.kt
class LMSTrainer(val lmsNet: LMSNetwork) : EditableObject {

    @UserParameter(label = "Learning Rate", order = 1)
    val learningRate = .01

    @UserParameter(label = "Update type", order = 1)
    val updateType = UpdateMethod.STOCHASTIC

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

    suspend fun iterate() {
        iteration++
        // TODO: Other update types
        if (updateType == UpdateMethod.STOCHASTIC) {
            trainRow(Random.nextInt(lmsNet.trainingSet.inputs.nrows()))
        }
        events.errorUpdated.fireAndSuspend(error)
    }

    fun trainRow(rowNum: Int) {
        if (rowNum !in 0 until lmsNet.trainingSet.inputs.nrows()) {
            throw IllegalArgumentException("Trying to train invalid row number $rowNum")
        }
        val targets = lmsNet.trainingSet.targets.rowMatrixTransposed(rowNum)
        lmsNet.inputLayer.isClamped = true
        lmsNet.inputLayer.setActivations(lmsNet.trainingSet.inputs.row(rowNum))
        lmsNet.update()
        val outputs = lmsNet.outputLayer.activations
        val rowError = targets.sub(outputs)
        lmsNet.weightMatrix.applyLMS(rowError, learningRate)
        error = rowError.transpose().mm(rowError).sum()
    }

    // TODO: Better name?
    enum class UpdateMethod {
        EPOCH { override fun toString() = "Epoch (whole dataset per iteration)" },
        STOCHASTIC { override fun toString() = "Stochastic (random row per iteration)" },
        SINGLE { override fun toString() = "Single (one row per iteration)" }
    }

    // Randomization / Initialization strategy
    // Stopping Condition. See iterable trainer.
    // Loss Function
    // See API for kotlindl
}