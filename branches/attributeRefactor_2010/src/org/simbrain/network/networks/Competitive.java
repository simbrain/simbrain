package org.simbrain.network.networks;

import java.util.Iterator;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neurons.LinearNeuron;

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

    /** Normalize inputs boolean. */
    private boolean normalizeInputs = true;

    /** Use leaky learning boolean. */
    private boolean useLeakyLearning = false;

    /** Leaky epsilon value. */
    private double leakyEpsilon = epsilon / 4;

    /** Max, value and activation values. */
    private double max, val, activation;

    /** Winner value. */
    private int winner;

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
     * @param root reference to RootNetwork.
     */
    public Competitive(final RootNetwork root, final int numNeurons, final Layout layout) {
        super();
        this.setRootNetwork(root);
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
        max = 0;
        winner = 0;

        // Determine Winner
        for (int i = 0; i < getNeuronList().size(); i++) {
            Neuron n = (Neuron) getNeuronList().get(i);
            n.getAverageInput();
            if (n.getActivation() > max) {
                max = n.getActivation();
                winner = i;
            }
        }

        // Update weights on winning neuron
        for (int i = 0; i < getNeuronList().size(); i++) {
            Neuron neuron = ((Neuron) getNeuronList().get(i));
            // Don't update weights if no incoming lines have greater than zero activation
            if (neuron.getNumberOfActiveInputs(0) == 0) {
                return;
            }
            if (i == winner) {
                if (!getRootNetwork().getClampNeurons()) {
                    neuron.setActivation(winValue);
                }
                if (!getRootNetwork().getClampWeights()) {
                    // Apply learning rule
                    for (Synapse incoming : neuron.getFanIn()) {
                      activation = incoming.getSource().getActivation();

                      if (normalizeInputs) {
                          activation /= neuron.getTotalInput();
                      }

                      val =  incoming.getStrength() + epsilon * (activation - incoming.getStrength());
                      incoming.setStrength(val);
                }
              }
            } else {
                if (!getRootNetwork().getClampNeurons()) {
                    neuron.setActivation(loseValue);
                }
                if ((useLeakyLearning) & (!getRootNetwork().getClampWeights())) {
                    for (Synapse incoming : neuron.getFanIn()) {
                      activation = incoming.getSource().getActivation();
                      if (normalizeInputs) {
                          activation /= neuron.getTotalInput();
                      }
                      val = incoming.getStrength() + leakyEpsilon * (activation - incoming.getStrength());
                      incoming.setStrength(val);
                    }
                }
            }
        }
        //normalizeIncomingWeights();
    }

    /**
     * Normalize  weights coming in to this network, separtely for each neuron.
     */
    public void normalizeIncomingWeights() {

        for (Iterator i = getNeuronList().iterator(); i.hasNext(); ) {
            Neuron n = (Neuron) i.next();
            double normFactor = n.getSummedIncomingWeights();
            for (Synapse s : n.getFanIn()) {
                s.setStrength(s.getStrength() / normFactor);
            }
        }
    }

    /**
     * Normalize all weights coming in to this network.
     */
    public void normalizeAllIncomingWeights() {

        double normFactor = getSummedIncomingWeights();
        for (Iterator i = getNeuronList().iterator(); i.hasNext(); ) {
            Neuron n = (Neuron) i.next();
            for (Synapse s : n.getFanIn()) {
                s.setStrength(s.getStrength() / normFactor);
            }
        }
    }

    /**
     * Randomize all weights coming in to this network.
     */
    public void randomizeIncomingWeights() {

        for (Iterator i = getNeuronList().iterator(); i.hasNext(); ) {
            Neuron n = (Neuron) i.next();
            for (Synapse s : n.getFanIn()) {
                s.randomize();
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
        for (Iterator i = getNeuronList().iterator(); i.hasNext(); ) {
            Neuron n = (Neuron) i.next();
            ret += n.getSummedIncomingWeights();
        }
        return ret;
    }

    /**
     * Randomize and normalize weights.
     */
    public void randomize() {
        randomizeIncomingWeights();
        normalizeIncomingWeights();
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

    /**
     * Return leaky epsilon value.
     *
     * @return Leaky epsilon value
     */
    public double getLeakyEpsilon() {
        return leakyEpsilon;
    }

    /**
     * Sets the leaky epsilon value.
     *
     * @param leakyEpsilon Leaky epsilon value to set
     */
    public void setLeakyEpsilon(final double leakyEpsilon) {
        this.leakyEpsilon = leakyEpsilon;
    }

    /**
     * Return the normalize inputs value.
     *
     * @return the normailize inputs value
     */
    public boolean getNormalizeInputs() {
        return normalizeInputs;
    }

    /**
     * Sets the normalize inputs value.
     *
     * @param normalizeInputs Normalize inputs value to set
     */
    public void setNormalizeInputs(final boolean normalizeInputs) {
        this.normalizeInputs = normalizeInputs;
    }

    /**
     * Return the leaky learning value.
     *
     * @return the leaky learning value
     */
    public boolean getUseLeakyLearning() {
        return useLeakyLearning;
    }

    /**
     * Sets the leaky learning value.
     *
     * @param useLeakyLearning The leaky learning value to set
     */
    public void setUseLeakyLearning(final boolean useLeakyLearning) {
        this.useLeakyLearning = useLeakyLearning;
    }

    /** @Override. */
    public Network duplicate() {
        Competitive net = new Competitive();
        net = (Competitive) super.duplicate(net);
        return net;
    }

}
