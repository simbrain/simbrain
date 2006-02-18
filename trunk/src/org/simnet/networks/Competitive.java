package org.simnet.networks;

import java.util.Iterator;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.layouts.Layout;
import org.simnet.neurons.LinearNeuron;

/**
 * <b>Competitive</b> implements a Competitive network.
 *
 * @author Jeff Yoshimi
 */
public class Competitive extends Network {

    /** Learning rate. */
    private double epsilon = .1;

    /** Winner value. */
    private double winValue = 1;

    /** loser value. */
    private double loseValue = 0;

    /** Number of neurons. */
    private int numNeurons = 3;

    /**
     * Default constructor used by Castor.
     */
    public Competitive() {
    }

    /**
     * Constructs a competitive network with specified number of neurons.
     *
     * @param numNeurons size of this network in neurons
     * @param layout Defines how neurons are to be layed out
     */
    public Competitive(final int numNeurons, final Layout layout) {
        super();
        for (int i = 0; i < numNeurons; i++) {
            this.addNeuron(new LinearNeuron());
        }
        layout.layoutNeurons(this);
    }

    /**
     * Update the network.
     */
    public void update() {

        updateAllNeurons();
        double max = 0;
        int winner = 0;
        Neuron win = null;

        // Determine Winner
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = (Neuron) neuronList.get(i);
            if (n.getActivation() > max) {
                max = n.getActivation();
                winner = i;
            }
        }

        // Update weights on winning neuron
        double val;
        for (int i = 0; i < neuronList.size(); i++) {
            if (i == winner) {
                win = ((Neuron) neuronList.get(i));

                // Don't update weights if no incoming lines have greater than zero activation
                if (win.getNumberOfActiveInputs(0) == 0) {
                    return;
                }
                win.setActivation(winValue);

                // Update weights
                for (Iterator j = win.getFanIn().iterator(); j.hasNext();) {
                    Synapse incoming = (Synapse) j.next();
                    val = incoming.getStrength()
                        + epsilon * (incoming.getSource().getActivation() - incoming.getStrength())
                        / win.getSummedIncomingWeights();
                    incoming.setStrength(val);
                }
            } else {
                ((Neuron) neuronList.get(i)).setActivation(loseValue);
            }
        }
        //normalizeIncomingWeights();
    }

    /**
     * Normalize all weights coming in to this network.
     */
    private void normalizeIncomingWeights() {

        double normFactor = getSummedIncomingWeights();
        for (Iterator i = neuronList.iterator(); i.hasNext();) {
            Neuron n = (Neuron) i.next();
            for (Iterator j = n.getFanIn().iterator(); j.hasNext();) {
                Synapse s = (Synapse) j.next();
                s.setStrength(s.getStrength() / normFactor);
            }
        }
    }

    /**
     * Returns the sum of all incoming weights to this network.
     *
     * @return the sum of all incoming weights to this network.
     */
    private double getSummedIncomingWeights() {
        double ret = 0;
        for (Iterator i = neuronList.iterator(); i.hasNext();) {
            Neuron n = (Neuron) i.next();
            ret += n.getSummedIncomingWeights();
        }
        return ret;
    }

    /**
     * Return the epsilon.
     *
     * @return the epsilon value.
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

    /**
     * Return the loser value.
     *
     * @return the loser Value
     */
    public final double getLoseValue() {
        return loseValue;
    }

    /**
     * Sets the loser value.
     *
     * @param loseValue The new loser value
     */
    public final void setLoseValue(final double loseValue) {
        this.loseValue = loseValue;
    }

    /**
     * Return the winner value.
     *
     * @return the winner value
     */
    public final double getWinValue() {
        return winValue;
    }

    /**
     * Sets the winner value.
     *
     * @param winValue The new winner value
     */
    public final void setWinValue(final double winValue) {
        this.winValue = winValue;
    }

    /**
     * @return The initial number of neurons.
     */
    public int getNumNeurons() {
        return numNeurons;
    }

}
