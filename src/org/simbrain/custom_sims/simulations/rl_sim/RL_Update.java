package org.simbrain.custom_sims.simulations.rl_sim;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A custom updater for use in applying TD Learning and other custom update
 * features (e.g. only activating one vehicle network at a time based on the
 * output of a feed-forward net).
 * <p>
 * For background on TD Learning see.
 * http://www.scholarpedia.org/article/Temporal_difference_learning
 */
//CHECKSTYLE:OFF
public class RL_Update implements NetworkUpdateAction {

    /**
     * Reference to RL_Sim object that has all the main variables used.
     */
    RL_Sim_Main sim;

    /**
     * Reference to main neurons used in td learning.
     */
    Neuron reward, value, tdError;

    /**
     * This variable is a hack needed because the reward neuron's lastactivation
     * value is not being updated properly in this simulation now.
     * <p>
     * Todo: Remove after fixing the issue. The issue is probably based on
     * coupling update.
     */
    double lastReward;

    /**
     * Current winning output neuron.
     */
    Neuron winner;

    /**
     * For training the prediction network.
     */
    double[] lastPredictionLeft;
    double[] lastPredictionRight;
    double learningRate = .1;

    // TODO: The machinery to handle iterations between weight updates is
    // fishy... but works for now

    /* Iterations to leave vehicle on between weight updates. */
    private final int iterationsBetweenWeightUpdates = 1;

    // Variables to help with the above
    private double previousReward;
    double[] previousInput;
    int counter = 0;

    // Helper which associates neurons with integer indices of the array that
    // tracks past states
    Map<Neuron, Integer> neuronIndices = new HashMap();

    /**
     * Construct the updater.
     */
    public RL_Update(RL_Sim_Main sim) {
        super();
        this.sim = sim;
        reward = sim.reward;
        value = sim.value;
        tdError = sim.tdError;
        initMap();
        lastPredictionLeft = sim.predictionLeft.getActivations();
        lastPredictionRight = sim.predictionRight.getActivations();
    }

    @Override
    public String getDescription() {
        return "Custom TD Rule";
    }

    @Override
    public String getLongDescription() {
        return "Custom TD Rule";
    }

    /**
     * Custom update of the network, including application of TD Rules.
     */
    @Override
    public void invoke() {

        // Update input nodes
        sim.leftInputs.update();
        sim.rightInputs.update();

        // Update prediction nodes
        sim.predictionLeft.update();
        sim.predictionRight.update();

        // Reward node
        Network.updateNeurons(Collections.singletonList(sim.reward));

        // Train prediction nodes
        trainPredictionNodes();

        // Value node
        Network.updateNeurons(Collections.singletonList(sim.value));


        // Outputs and vehicles
        if (winner != null) {
            updateVehicleNet(winner);
        }

        // Apply Actor-critic stuff. Update reward "critic" synapses and actor
        // synapses. Only perform these updates after the braitenberg vehicles
        // have run for "iterationsBetweenWeightUpdates"
        if (counter++ % iterationsBetweenWeightUpdates == 0) {

            // Find the winning output neuron
            sim.wtaNet.update();
            winner = sim.wtaNet.getWinner();

            // Update the reward neuron and the change in reward
            Network.updateNeurons(Collections.singletonList(sim.reward));
            updateDeltaReward();

            updateTDError();

            updateCritic();

            updateActor();

            // Record the "before" state of the system.
            previousReward = sim.reward.getActivation();
            System.arraycopy(sim.leftInputs.getActivations(), 0, previousInput, 0, sim.leftInputs.getActivations().length);
            System.arraycopy(sim.rightInputs.getActivations(), 0, previousInput, sim.leftInputs.getActivations().length, sim.rightInputs.getActivations().length);
        }
    }

    /**
     * Train the prediction nodes to predict the next input states.
     */
    private void trainPredictionNodes() {

        setErrors(sim.leftInputs, sim.predictionLeft, lastPredictionLeft);
        setErrors(sim.rightInputs, sim.predictionRight, lastPredictionRight);

        trainDeltaRule(sim.rightToWta);
        trainDeltaRule(sim.leftToWta);

        trainDeltaRule(sim.outputToLeftPrediction);
        trainDeltaRule(sim.rightInputToRightPrediction);
        trainDeltaRule(sim.outputToRightPrediction);

        lastPredictionLeft = sim.predictionLeft.getActivations();
        lastPredictionRight = sim.predictionRight.getActivations();
    }

    /**
     * Set errors on neuron groups.
     */
    void setErrors(NeuronGroup inputs, NeuronGroup predictions, double[] lastPrediction) {
        int i = 0;
        double error = 0;
        sim.preditionError = 0;
        for (Neuron neuron : predictions.getNeuronList()) {
            error = inputs.getNeuronList().get(i).getActivation() - lastPrediction[i];
            sim.preditionError += error * error;
            neuron.setAuxValue(error);
            i++;
        }
        sim.preditionError = Math.sqrt(sim.preditionError);
    }

    /**
     * Train the synapses in a synapse group
     */
    void trainDeltaRule(SynapseGroup group) {
        for (Synapse synapse : group.getAllSynapses()) {
            double newStrength = synapse.getStrength() + learningRate * synapse.getSource().getActivation() * synapse.getTarget().getAuxValue();
            synapse.setStrength(newStrength);
        }
    }

    /**
     * Train the synapses directly
=     */
    void trainDeltaRule(List<Synapse> synapses) {
        for (Synapse synapse : synapses) {
            double newStrength = synapse.getStrength() + learningRate * synapse.getSource().getActivation() * synapse.getTarget().getAuxValue();
            synapse.setStrength(newStrength);
        }
    }

    /**
     * TD Error. Used to drive all learning in the network.
     */
    void updateTDError() {
        double val = sim.deltaReward.getActivation() + sim.gamma * value.getActivation() - value.getLastActivation();
        tdError.forceSetActivation(sim.deltaReward.getActivation() + sim.gamma * value.getActivation() - value.getLastActivation());
    }

    /**
     * Update the vehicle whose name corresponds to the winning output.
     *
     * @param winner
     */
    void updateVehicleNet(Neuron winner) {
        for (NeuronGroup vehicle : sim.vehicles) {
            if (vehicle.getLabel().equalsIgnoreCase(winner.getLabel())) {
                vehicle.update();
            } else {
                vehicle.clearActivations();
            }
        }
    }

    /**
     * Update value synapses. Learn the value function. The "critic".
     */
    void updateCritic() {
        for (Synapse synapse : value.getFanIn()) {
            Neuron sourceNeuron = (Neuron) synapse.getSource();
            double newStrength = synapse.getStrength() + sim.alpha * tdError.getActivation() * sourceNeuron.getLastActivation();
            synapse.setStrength(newStrength);
        }
    }

    /**
     * Update all "actor" neurons. (Roughly) If the last input > output
     * connection led to reward, reinforce that connection.
     */
    void updateActor() {
        for (Neuron neuron : sim.wtaNet.getNeuronList()) {
            // Just update the last winner
            if (neuron.getLastActivation() > 0) {
                for (Synapse synapse : neuron.getFanIn()) {
                    double previousActivation = getPreviousNeuronValue(synapse.getSource());
                    double newStrength = synapse.getStrength() + sim.alpha * tdError.getActivation() * previousActivation;
                    // synapse.setStrength(synapse.clip(newStrength));
                    synapse.setStrength(newStrength);
                }
            }
        }
    }

    /**
     * Returns the "before" state of the given neuron.
     */
    private double getPreviousNeuronValue(Neuron neuron) {
        // System.out.println(previousInput[neuronIndices.get(neuron)]);
        return previousInput[neuronIndices.get(neuron)];
    }

    /**
     * Initialize the map from neurons to indices.
     */
    void initMap() {
        int index = 0;
        for (Neuron neuron : sim.leftInputs.getNeuronList()) {
            neuronIndices.put(neuron, index++);
        }
        for (Neuron neuron : sim.rightInputs.getNeuronList()) {
            neuronIndices.put(neuron, index++);
        }
        previousInput = new double[index];
    }

    /**
     * Update the delta-reward neuron, by taking the difference between the
     * reward neuron's last state and its current state.
     * <p>
     * TODO: Rename needed around here? This is now the "reward" used by the TD
     * algorithm, which is different from the reward signal coming directory
     * from the environment.
     */
    private void updateDeltaReward() {
        double diff = reward.getActivation() - previousReward;
        sim.deltaReward.forceSetActivation(diff);
    }

}
