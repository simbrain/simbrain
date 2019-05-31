package org.simbrain.custom_sims.simulations.patterns_of_activity;

import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

/**
 * Training the prediction sub-network.
 */
public class TrainPredictionNet implements NetworkUpdateAction {

    /**
     * Reference to simulation object that has all the main variables used.
     */
    KuramotoOscillators sim;

    // TODO
    double[] lastPrediction;
    double learningRate = .1;

    /**
     * Construct the updater.
     */
    public TrainPredictionNet(KuramotoOscillators sim) {
        super();
        this.sim = sim;
        lastPrediction = sim.predictionRes.getActivations();
    }

    @Override
    public String getDescription() {
        return "Custom Learning Rule";
    }

    @Override
    public String getLongDescription() {
        return "Custom Learning Rule";
    }

    @Override
    public void invoke() {
        mainUpdateMethod();
    }

    /**
     * Training synapses using delta rule.
     */
    private void mainUpdateMethod() {

        int i = 0;
        double error = 0;
        double sumError = 0;
        for (Neuron neuron : sim.predictionRes.getNeuronList()) {
            // error = target - actual
            // error = current sensory - last prediction
            error = sim.reservoirNet.getNeuronList().get(i).getActivation() - lastPrediction[i];
            sumError += error * error;
            // System.out.println(i + ":" + error + ":" + neuron.getId());
            neuron.setAuxValue(error);
            i++;
        }

        // Update error neuron
        sim.errorNeuron.forceSetActivation(Math.sqrt(sumError));

        // Update all synapses
        for (Synapse synapse : sim.predictionSg.getAllSynapses()) {
            double newStrength = synapse.getStrength() + learningRate * synapse.getSource().getActivation() * synapse.getTarget().getAuxValue();
            synapse.setStrength(newStrength);
            // System.out.println(newStrength);
        }

        lastPrediction = sim.predictionRes.getActivations();
    }

}
