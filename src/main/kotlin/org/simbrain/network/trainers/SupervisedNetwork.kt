package org.simbrain.network.trainers

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.propertyeditor.EditableObject

/**
 * Interface for networks that can be trained using standard supervised learning methods.
 */
interface SupervisedNetwork {

    val trainerConfig: SupervisedTrainerConfig

    var trainingSet: TrainingDataset

    var testingSet: TrainingDataset

    val inputLayer: Layer

    val outputLayer: Layer

    val layers: LinkedHashSet<Layer>

    context(Network) fun forwardPass()

    fun initWeights()

    fun initBiases()

    fun possibleLossFunctions() = when(outputLayer.updateRule) {
        is SoftmaxRule -> listOf(BackpropLossFunction.CrossEntropy::class.java)
        else -> listOf(BackpropLossFunction.SSE::class.java, BackpropLossFunction.MSE::class.java, BackpropLossFunction.RMSE::class.java)
    }

    /**
     * Sets up automatic loss function update when the output layer's update rule changes.
     * This ensures that the loss function is always compatible with the output layer's activation function.
     * For example, CrossEntropy is used with Softmax, while SSE/MSE/RMSE are used with other activation functions.
     */
    fun setupLossFunctionAutoUpdate() {
        // Check initial state and update if needed
        updateLossFunctionIfNeeded()
        
        // Listen to output layer update events for future changes
        outputLayer.events.updated.on {
            updateLossFunctionIfNeeded()
        }
    }

    /**
     * Checks if the current loss function is compatible with the output layer's update rule.
     * If not, automatically switches to the first valid loss function from possibleLossFunctions().
     */
    fun updateLossFunctionIfNeeded() {
        // Check if current loss function can be used with the output layer
        if (!trainerConfig.lossFunction.canUse(outputLayer)) {
            // Get the first valid loss function class and convert to instance
            val validLossFunctionClasses = possibleLossFunctions()
            if (validLossFunctionClasses.isNotEmpty()) {
                val newLossFunction = classToLossFunctionInstance(validLossFunctionClasses[0])
                if (newLossFunction != null) {
                    trainerConfig.lossFunction = newLossFunction
                }
            }
        }
    }

    /**
     * Converts a loss function class to its singleton instance.
     */
    fun classToLossFunctionInstance(clazz: Class<out EditableObject>): BackpropLossFunction? {
        return when (clazz) {
            BackpropLossFunction.SSE::class.java -> BackpropLossFunction.SSE
            BackpropLossFunction.MSE::class.java -> BackpropLossFunction.MSE
            BackpropLossFunction.RMSE::class.java -> BackpropLossFunction.RMSE
            BackpropLossFunction.CrossEntropy::class.java -> BackpropLossFunction.CrossEntropy
            else -> null
        }
    }

}