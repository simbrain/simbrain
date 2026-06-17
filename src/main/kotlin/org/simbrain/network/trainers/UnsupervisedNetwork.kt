package org.simbrain.network.trainers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.events.TrainerEvents
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution

interface UnsupervisedNetwork: EditableObject {

    var trainingData: MutableList<MutableList<Double>>

    var testingData: MutableList<MutableList<Double>>

    val inputLayer: Layer

    val trainer: UnsupervisedTrainer

    context(Network)
    fun trainOnInputData()

    context(Network)
    fun trainOnCurrentPattern()

    fun randomize(randomizer: ProbabilityDistribution?)

}

class UnsupervisedTrainer: EditableObject {

    var iteration = 0

    var isRunning = false

    var maxIterations by GuiEditable(
        initValue = 1000
    )

    @UserParameter(
        label = "Learning Rate",
        description = "Step size for unsupervised learning updates. Controls how quickly weights adapt to input patterns"
    )
    var learningRate = .01

    @Transient
    val events = TrainerEvents()

    context(Network)
    suspend fun startTraining(network: UnsupervisedNetwork) {
        if (iteration >= maxIterations) {
            events.iterationReset.fire()
        }
        isRunning = true
        events.beginTraining.fireAsync()
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

    fun copyFrom(other: UnsupervisedTrainer) {
        this.maxIterations = other.maxIterations
        this.learningRate = other.learningRate
    }

    override val name: String
        get() = "Unsupervised Trainer"
}