package org.simnet.networks;

import java.util.Iterator;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.neurons.LinearNeuron;

/**
 * <b>Competitive</b> implements a Competitive network.
 *
 * @author Jeff Yoshimi
 */
public class Competitive extends Network {

    /** Learning rate. */
    private double epsilon = .5;

    /**
     * Default constructor used by Castor.
     */
    public Competitive() {
    }

    /**
     * Constructs a competitive network with specified number of neurons.
     *
     * @param numNeurons size of this network in neurons.
     */
    public Competitive(final int numNeurons) {
        super();
        for (int i = 0; i < numNeurons; i++) {
            this.addNeuron(new LinearNeuron());
        }
    }

    /**
     * Update the network.
     */
    public void update() {

        // Winner Take All for Neurons
        updateAllNeurons();
        double max = 0;
        int winner = 0;
        Neuron win = null;

        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = (Neuron) neuronList.get(i);
            if (n.getActivation() > max) {
                max = n.getActivation();
                winner = i;
            }
        }

        double val;
        for (int i = 0; i < neuronList.size(); i++) {
            if (i == winner) {
                win = ((Neuron) neuronList.get(i));
                win.setActivation(1);
                for (Iterator j = win.getFanIn().iterator(); j.hasNext();) {
                    Synapse incoming = (Synapse) j.next();
                    val = incoming.getStrength()
                        + epsilon * (incoming.getSource().getActivation() - incoming.getStrength());
                    incoming.setStrength(val);
                }
            } else {
                ((Neuron) neuronList.get(i)).setActivation(0);
            }
        }
    }

    /**
     * Returns epsilon.
     *
     * @return Returns the epsilon value.
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * Sets epsilon.
     *
     * @param epsilon The new epsilon value.
     */
    public void setEpsilon(final double epsilon) {
        this.epsilon = epsilon;
    }

}
