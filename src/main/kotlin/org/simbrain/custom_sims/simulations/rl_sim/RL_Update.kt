package org.simbrain.custom_sims.simulations.rl_sim

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.core.updateNeurons
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.workspace.updater.UpdateAction

/**
 * A custom updater for use in applying TD Learning and other custom update
 * features (e.g. only activating one vehicle network at a time based on the
 * output of a feed-forward net).
 *
 *
 * For background on TD Learning see.
 * http://www.scholarpedia.org/article/Temporal_difference_learning
 */
//CHECKSTYLE:OFF
class RL_Update(
    /**
     * Reference to RL_Sim object that has all the main variables used.
     */
    var sim: RL_Sim_Main
) : UpdateAction("Custom TD Rule") {
    /**
     * Reference to main neurons used in td learning.
     */
    var reward: Neuron
    var value: Neuron
    var tdError: Neuron

    /**
     * This variable is a hack needed because the reward neuron's lastactivation
     * value is not being updated properly in this simulation now.
     *
     *
     * Todo: Remove after fixing the issue. The issue is probably based on
     * coupling update.
     */
    var lastReward = 0.0

    /**
     * Current winning output neuron.
     */
    var winner: Neuron? = null

    /**
     * For training the prediction network.
     */
    var lastPredictionLeft: DoubleArray
    var lastPredictionRight: DoubleArray
    var learningRate = .1

    // TODO: The machinery to handle iterations between weight updates is
    // fishy... but works for now
    /* Iterations to leave vehicle on between weight updates. */
    private val iterationsBetweenWeightUpdates = 1

    // Variables to help with the above
    private var previousReward = 0.0
    lateinit var previousInput: DoubleArray
    var counter = 0

    // Helper which associates neurons with integer indices of the array that
    // tracks past states
    var neuronIndices: MutableMap<Neuron?, Int?> = HashMap<Neuron?, Int?>()

    /**
     * Construct the updater.
     */
    init {
        reward = sim.reward
        value = sim.value
        tdError = sim.tdError
        initMap()
        lastPredictionLeft = sim.predictionLeft.activations
        lastPredictionRight = sim.predictionRight.activations
    }

    override val description: String
        get() = "Custom TD Rule"
    override val longDescription: String
        get() = "Custom TD Rule"

    /**
     * Custom update of the network, including application of TD Rules.
     */
    override suspend fun run() {

        // Update input nodes
        sim.leftInputs.update()
        sim.rightInputs.update()

        // Update prediction nodes
        sim.predictionLeft.update()
        sim.predictionRight.update()

        // Reward node
        updateNeurons(listOf(sim.reward))

        // Train prediction nodes
        trainPredictionNodes()

        // Value node
        updateNeurons(listOf(sim.value))


        // Outputs and vehicles
        if (winner != null) {
            updateVehicleNet(winner!!)
        }

        // Apply Actor-critic stuff. Update reward "critic" synapses and actor
        // synapses. Only perform these updates after the braitenberg vehicles
        // have run for "iterationsBetweenWeightUpdates"
        if (counter++ % iterationsBetweenWeightUpdates == 0) {

            // Find the winning output neuron
            sim.wtaNet.update()
            winner = sim.wtaNet.winner

            // Update the reward neuron and the change in reward
            updateNeurons(listOf(sim.reward))
            updateDeltaReward()
            updateTDError()
            updateCritic()
            updateActor()

            // Record the "before" state of the system.
            previousReward = sim.reward.activation
            System.arraycopy(sim.leftInputs.activations, 0, previousInput, 0, sim.leftInputs.activations.size)
            System.arraycopy(
                sim.rightInputs.activations,
                0,
                previousInput,
                sim.leftInputs.activations.size,
                sim.rightInputs.activations.size
            )
        }
    }

    /**
     * Train the prediction nodes to predict the next input states.
     */
    private fun trainPredictionNodes() {
        setErrors(sim.leftInputs, sim.predictionLeft, lastPredictionLeft)
        setErrors(sim.rightInputs, sim.predictionRight, lastPredictionRight)
        trainDeltaRule(sim.rightToWta)
        trainDeltaRule(sim.leftToWta)
        trainDeltaRule(sim.outputToLeftPrediction)
        trainDeltaRule(sim.rightInputToRightPrediction)
        trainDeltaRule(sim.outputToRightPrediction)
        lastPredictionLeft = sim.predictionLeft.activations
        lastPredictionRight = sim.predictionRight.activations
    }

    /**
     * Set errors on neuron groups.
     */
    fun setErrors(inputs: NeuronGroup, predictions: NeuronGroup, lastPrediction: DoubleArray) {
        var i = 0
        var error = 0.0
        sim.preditionError = 0.0
        for (neuron in predictions.neuronList) {
            error = inputs.neuronList[i].activation - lastPrediction[i]
            sim.preditionError += error * error
            neuron.auxValue = error
            i++
        }
        sim.preditionError = Math.sqrt(sim.preditionError)
    }

    /**
     * Train the synapses in a synapse group
     */
    fun trainDeltaRule(group: SynapseGroup2) {
        for (synapse in group.allSynapses) {
            val newStrength = synapse.strength + learningRate * synapse.source.activation * synapse.target.auxValue
            synapse.strength = newStrength
        }
    }

    /**
     * Train the synapses directly
     * =      */
    fun trainDeltaRule(synapses: List<Synapse>) {
        for (synapse in synapses) {
            val newStrength = synapse.strength + learningRate * synapse.source.activation * synapse.target.auxValue
            synapse.strength = newStrength
        }
    }

    /**
     * TD Error. Used to drive all learning in the network.
     */
    fun updateTDError() {
        val `val` = sim.deltaReward.activation + sim.gamma * value.activation - value.lastActivation
        tdError.forceSetActivation(sim.deltaReward.activation + sim.gamma * value.activation - value.lastActivation)
    }

    /**
     * Update the vehicle whose name corresponds to the winning output.
     *
     * @param winner
     */
    fun updateVehicleNet(winner: Neuron) {
        for (vehicle in sim.vehicles) {
            if (vehicle.label.equals(winner.label, ignoreCase = true)) {
                vehicle.update()
            } else {
                vehicle.clear()
            }
        }
    }

    /**
     * Update value synapses. Learn the value function. The "critic".
     */
    fun updateCritic() {
        for (synapse in value.fanIn) {
            val sourceNeuron = synapse.source as Neuron
            val newStrength = synapse.strength + sim.alpha * tdError.activation * sourceNeuron.lastActivation
            synapse.strength = newStrength
        }
    }

    /**
     * Update all "actor" neurons. (Roughly) If the last input > output
     * connection led to reward, reinforce that connection.
     */
    fun updateActor() {
        for (neuron in sim.wtaNet.neuronList) {
            // Just update the last winner
            if (neuron.lastActivation > 0) {
                for (synapse in neuron.fanIn) {
                    val previousActivation = getPreviousNeuronValue(synapse.source)
                    val newStrength = synapse.strength + sim.alpha * tdError.activation * previousActivation
                    // synapse.setStrength(synapse.clip(newStrength));
                    synapse.strength = newStrength
                }
            }
        }
    }

    /**
     * Returns the "before" state of the given neuron.
     */
    private fun getPreviousNeuronValue(neuron: Neuron): Double {
        // System.out.println(previousInput[neuronIndices.get(neuron)]);
        return previousInput[neuronIndices[neuron]!!]
    }

    /**
     * Initialize the map from neurons to indices.
     */
    fun initMap() {
        var index = 0
        for (neuron in sim.leftInputs.neuronList) {
            neuronIndices[neuron] = index++
        }
        for (neuron in sim.rightInputs.neuronList) {
            neuronIndices[neuron] = index++
        }
        previousInput = DoubleArray(index)
    }

    /**
     * Update the delta-reward neuron, by taking the difference between the
     * reward neuron's last state and its current state.
     *
     *
     * TODO: Rename needed around here? This is now the "reward" used by the TD
     * algorithm, which is different from the reward signal coming directory
     * from the environment.
     */
    private fun updateDeltaReward() {
        val diff = reward.activation - previousReward
        sim.deltaReward.forceSetActivation(diff)
    }
}