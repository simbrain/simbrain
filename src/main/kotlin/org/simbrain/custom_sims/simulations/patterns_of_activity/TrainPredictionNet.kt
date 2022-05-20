package org.simbrain.custom_sims.simulations.patterns_of_activity

import org.simbrain.workspace.updater.UpdateAction

/**
 * Training the prediction sub-network.
 */
class TrainPredictionNet(
    /**
     * Reference to simulation object that has all the main variables used.
     */
    var sim: KuramotoOscillators
) : UpdateAction("Custom Learning Rule") {
    // TODO
    var lastPrediction: DoubleArray
    var learningRate = .1

    /**
     * Construct the updater.
     */
    init {
        lastPrediction = sim.predictionRes.activations
    }
    override suspend fun run() {
        mainUpdateMethod()
    }

    /**
     * Training synapses using delta rule.
     */
    private fun mainUpdateMethod() {
        var i = 0
        var error = 0.0
        var sumError = 0.0
        for (neuron in sim.predictionRes.neuronList) {
            // error = target - actual
            // error = current sensory - last prediction
            error = sim.reservoirNet.neuronList[i].activation - lastPrediction[i]
            sumError += error * error
            // System.out.println(i + ":" + error + ":" + neuron.getId());
            neuron.auxValue = error
            i++
        }

        // Update error neuron
        sim.errorNeuron.forceSetActivation(Math.sqrt(sumError))

        // Update all synapses
        for (synapse in sim.predictionSg.synapses) {
            val newStrength = synapse.strength + learningRate * synapse.source.activation * synapse.target.auxValue
            synapse.strength = newStrength
            // System.out.println(newStrength);
        }
        lastPrediction = sim.predictionRes.activations
    }
}