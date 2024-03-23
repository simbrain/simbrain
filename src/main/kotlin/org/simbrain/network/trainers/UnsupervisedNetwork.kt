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
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.events.TrainerEvents
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix

interface UnsupervisedNetwork: EditableObject {

    var inputData: Matrix

    val inputLayer: NeuronArray

    val trainer: UnsupervisedTrainer

    context(Network)
    fun trainOnInputData()

    context(Network)
    fun trainOnCurrentPattern()

    fun randomize(randomizer: ProbabilityDistribution? = null)

}

class UnsupervisedTrainer: EditableObject {

    var iteration = 0

    var isRunning = false

    var maxIterations by GuiEditable(
        initValue = 1000
    )


    @UserParameter("Learning Rate")
    var learningRate = .01

    @Transient
    val events = TrainerEvents()

    context(Network)
    suspend fun startTraining(network: UnsupervisedNetwork) {
        if (iteration >= maxIterations) {
            events.iterationReset.fire()
        }
        isRunning = true
        events.beginTraining.fireAndForget()
        withContext(Dispatchers.Default) {
            while (isRunning) {
                trainOnce(network)
                if (iteration >= maxIterations) {
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
    suspend fun trainOnce(network: UnsupervisedNetwork) {
        iteration++
        withContext(Dispatchers.Default) {
            network.trainOnInputData()
            events.progressUpdated.fire("Iteration" to iteration)
        }
    }
}