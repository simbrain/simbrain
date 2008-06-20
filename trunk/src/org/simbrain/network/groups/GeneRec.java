package org.simbrain.network.groups;

import java.util.ArrayList;
import java.util.Hashtable;

import org.simbrain.network.interfaces.BiasedNeuron;
import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;

/**
 * Will implement the Leabra / GeneRec algorith.
 */
public class GeneRec extends Group {

    /** Learning rate. */
    private double epsilon = .05;

    /** How many times to iterate plus and minus phases. */
    private int numUpdates = 30;

    /** For matching plusPhases to MinusPhases. */
    private Hashtable<Neuron, Double> plusToMinusMapping = new Hashtable<Neuron, Double>();

    /** @see Group. */
    public GeneRec(final RootNetwork net, final ArrayList<Object> items) {
        super(net);
        this.addObjectReferences(items);
        referenceNetwork.setUpdateMethod(getParent().getUpdateMethod());
    }

    /**
     * Randomize all weights and neuron biases, if any.
     * Does not use subnet randomization functions.
     */
    public void randomize() {
        for (Neuron neuron : getFlatNeuronList()) {
            if (neuron instanceof BiasedNeuron) {
                ((BiasedNeuron) neuron).setBias(neuron.getRandomValue());
            }
        }
        for (Synapse synapse : getFlatSynapseList()) {
            synapse.randomizeSymmetric();
        }
    }

   /** @Override. */
    public void update() {

        if (!this.isOn()) {
            return;
        }

        //Compute minus phase
        for (Neuron neuron : getFlatNeuronList()) {
            if (neuron.isInput()) {
                neuron.setActivation(neuron.getInputValue());
                neuron.setClamped(true);
            }
        }
        for (int i = 0; i < numUpdates; i++) {
            referenceNetwork.update();
        }

        // Store minus phase activations
        for (Neuron neuron : this.getFlatNeuronList()) {
            plusToMinusMapping.put(neuron, neuron.getActivation());
        }

        // Compute plus phase
        for (Neuron neuron : getFlatNeuronList()) {
            neuron.setActivation(neuron.getTargetValue());
            neuron.setClamped(true);
        }
        for (int i = 0; i < numUpdates; i++) {
            referenceNetwork.update();
        }

        // Update synapses
        for (Synapse synapse : this.getFlatSynapseList()) {
            double plusPhaseSource = synapse.getSource().getActivation();
            double plusPhaseTarget = synapse.getTarget().getActivation();
            double minusPhaseSource = (plusToMinusMapping.get(synapse.getSource()));
            double minusPhaseTarget = (plusToMinusMapping.get(synapse.getTarget()));
            // See CECN, p. 165, eq (5.39)
            double delta = epsilon * (plusPhaseSource * plusPhaseTarget - minusPhaseSource * minusPhaseTarget);
            //System.out.println("Delta weight: " + delta);
            synapse.setStrength(synapse.clip(synapse.getStrength() + delta));
        }

        // Update biases
        for (Neuron neuron : this.getFlatNeuronList()) {
            double plusPhase = neuron.getActivation();
            double minusPhase = plusToMinusMapping.get(neuron);
            double delta = epsilon * (minusPhase - plusPhase);
            //System.out.println("Delta bias: " + delta);
            if (neuron instanceof BiasedNeuron) {
                ((BiasedNeuron) neuron).setBias(neuron.clip(((BiasedNeuron) neuron).getBias() + delta));
            }
        }

        // Reset neuron activations to minusphase
        for (Neuron neuron : getFlatNeuronList()) {
            neuron.setClamped(false);
            neuron.setActivation(plusToMinusMapping.get(neuron));
        }

    }

    /** @Override. */
    public Network duplicate() {
        // TODO Auto-generated method stub
        return null;
    }


}
