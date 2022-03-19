package org.simbrain.custom_sims.simulations.agent_trails

import org.simbrain.workspace.updater.UpdateAction

/**
 * Training the prediction sub-network.
 */
class TrainPredictionNet(var sim: AgentTrails) : UpdateAction("Custom Learning Rule") {
    // TODO
    var lastPrediction: DoubleArray
    var learningRate = .1

    /**
     * Construct the updater.
     */
    init {
        lastPrediction = sim.predictionNet.activations
    }

    override suspend operator fun invoke() {
        mainUpdateMethod()
    }

    /**
     * Training synapses using delta rule.
     */
    private fun mainUpdateMethod() {
        var i = 0
        var error = 0.0
        var sumError = 0.0
        for (neuron in sim.predictionNet.neuronList) {
            // error = target - actual
            // error = current sensory - last prediction
            error = sim.sensoryNet.neuronList[i].activation - lastPrediction[i]
            sumError += error * error
            // System.out.println(i + ":" + error + ":" + neuron.getId());
            neuron.auxValue = error
            i++
        }

        // Update error neuron
        sim.errorNeuron.forceSetActivation(Math.sqrt(sumError))

        // Update all value synapses
        for (synapse in sim.nc.network.flatSynapseList) {
            val newStrength = synapse.strength + learningRate * synapse.source.activation * synapse.target.auxValue
            synapse.strength = newStrength
            // System.out.println(newStrength);
        }
        lastPrediction = sim.predictionNet.activations
    }
}