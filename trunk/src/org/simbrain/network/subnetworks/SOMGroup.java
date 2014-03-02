/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.subnetworks;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neuron_update_rules.LinearRule;

/**
 * <b>SOM</b> implements a Self-Organizing Map network.
 *
 * @author William B. St. Clair
 * @author Jeff Yoshimi
 *
 */
public class SOMGroup extends NeuronGroup {

    /** Default alpha. */
    public static final double DEFAULT_ALPHA = 0.06;

    /** Default initial neighborhood size. */
    public static final double DEFAULT_INIT_NSIZE = 100;

    /** Default batchSize. */
    public static final int DEFAULT_BATCH_SIZE = 100;

    /** The default alphaDecayRate. */
    public static final double DEFAULT_DECAY_RATE = 0.002;

    /** The default neighborhoodDecayAmount. */
    public static final double DEFAULT_NEIGHBORHOOD_DECAY_AMOUNT = .05;

    /** Initial Learning Rate. */
    private double initAlpha = DEFAULT_ALPHA;

    /** Learning rate. */
    private double alpha = DEFAULT_ALPHA;

    /**
     * Current Neighborhood Size. With a circular neighborhood, neighborhoodSize
     * connotes radius.
     */
    private double neighborhoodSize = DEFAULT_INIT_NSIZE;

    /**
     * The initial neighborhoodSize. neighborhoodSize is set back to this
     * whenever network is reset.
     */
    private double initNeighborhoodSize = DEFAULT_INIT_NSIZE;

    /**
     * MinDistance, distance and val are changing variables used in the update
     * method.
     */
    private double winDistance, distance, val;

    /** Number of neurons. */
    private int numNeurons = 16;

    /** The number of epochs run in a given batch. */
    private int batchSize = DEFAULT_BATCH_SIZE;

    /** The rate at which the learning rate decays. */
    private double alphaDecayRate = DEFAULT_DECAY_RATE;

    /** The amount that the neighborhood decrements. */
    private double neighborhoodDecayAmount = DEFAULT_NEIGHBORHOOD_DECAY_AMOUNT;

    /**
     * Default layout for neuron groups. Used to set layout defaults in SOM
     * Creation dialog. Overrides superclass DEFAULT_LAYOUT.
     */
    public static final Layout DEFAULT_LAYOUT = new HexagonalGridLayout(50, 50,
            5);

    /**
     * Constructs an SOM network with specified number of neurons.
     *
     * @param numNeurons size of this network in neurons
     * @param root reference to Network.
     */
    public SOMGroup(final Network root, final int numNeurons) {
        super(root);
        for (int i = 0; i < numNeurons; i++) {
            addNeuron(new Neuron(getParentNetwork(), new LinearRule()));
        }
        setLabel("SOM");
    }

    @Override
    public String getTypeDescription() {
        return "Self Organizing Map";
    }


    /**
     * Randomize all weights coming in to this network. The weights will be
     * between 0 and the upper bound of each synapse.
     */
    public void randomizeIncomingWeights() {
        for (Neuron n : getNeuronList()) {
            for (Synapse s : n.getFanIn()) {
                s.setLowerBound(0);
                s.setStrength(s.getUpperBound() * Math.random());
            }
        }
    }

    /**
     * Pushes the weight values of an SOM neuron onto the input neurons.
     */
    public void recall() {
        winDistance = 0;
        Neuron winner = calculateWinner();
        for (Synapse incoming : winner.getFanIn()) {
            incoming.getSource().setActivation(incoming.getStrength());
        }

    }

    /**
     * Resets SOM Network to initial values.
     */
    public void reset() {
        alpha = initAlpha;
        neighborhoodSize = initNeighborhoodSize;
    }

    /**
     * Update the network. This method has the following structure: If all
     * weights are clamped, return. Determine the winner by finding which of the
     * SOM neurons is closest to the input vector. Update the winning neuron and
     * it's neighborhood. The update algorithm accounts for all possible
     * arrangements of the SOM network. - When the neuron is outside of the
     * neighborhood. - When the neuron is within the the neighborhood. Including
     * the current vector, if the total number of vectors analyzed during the
     * current iteration is equal to the total number of vectors to be analyzed,
     * update the network parameters and count one full iteration. Else the
     * network must be in recallMode. If all neurons are clamped, return. Find
     * the SOM neuron with highest activation. Set the activations of input
     * neurons according to the SOM weights.
     */
    @Override
    public void update() {

        winDistance = Double.POSITIVE_INFINITY;
        // winner = 0;
        double physicalDistance;

        // Determine Winner and update neurons: The SOM Neuron with the lowest
        // distance between  its weight vector and the input neurons's weight
        // vector.
        Neuron winner = calculateWinner();
        for (int i = 0; i < getNeuronList().size(); i++) {
            Neuron n = getNeuronList().get(i);
            if (n == winner) {
                n.setActivation(1);
            } else {
                n.setActivation(0);
            }
        }

        // Update Synapses of the neurons within the radius of the winning
        // neuron.
        for (int i = 0; i < getNeuronList().size(); i++) {
            Neuron neuron = getNeuronList().get(i);
            physicalDistance = findPhysicalDistance(neuron, winner);
            // The center of the neuron is within the update region.
            if (physicalDistance <= neighborhoodSize) {
                for (Synapse incoming : neuron.getFanIn()) {
                    val = incoming.getStrength()
                            + alpha
                            * (incoming.getSource().getActivation() - incoming
                                    .getStrength());
                    incoming.setStrength(val);
                }
            }
        }

        // Update alpha and neighborhood size
        alpha = (alpha - alphaDecayRate * alpha);
        if (neighborhoodSize - neighborhoodDecayAmount > 0) {
            neighborhoodSize -= neighborhoodDecayAmount;
        } else {
            neighborhoodSize = 0;
        }
    }

    /**
     * Find the SOM neuron which is closest to the input vector.
     *
     * @return winner
     */
    private Neuron calculateWinner() {
        Neuron winner = null;
        for (int i = 0; i < getNeuronList().size(); i++) {
            Neuron n = getNeuronList().get(i);
            distance = findDistance(n);
            if (distance < winDistance) {
                winDistance = distance;
                winner = n;
            }
        }
        return winner;
    }

    /**
     * Calculates the Euclidian distance between the SOM neuron's weight vector
     * and the input vector.
     *
     * @param n The SOM neuron one wishes to find the for.
     * @return distance.
     */
    private double findDistance(final Neuron n) {
        double ret = 0;
        for (Synapse incoming : n.getFanIn()) {
            ret += Math.pow(incoming.getStrength()
                    - incoming.getSource().getActivation(), 2);
        }
        return ret;
    }

    /**
     * Finds the physical Euclidian Distance between two neurons.
     *
     * @param neuron1 First neuron.
     * @param neuron2 Second neuron.
     * @return physical distance between two neurons in Simbrain.
     */
    private double findPhysicalDistance(final Neuron neuron1,
            final Neuron neuron2) {
        double ret = Math.sqrt(Math.pow(neuron2.getX() - neuron1.getX(), 2)
                + Math.pow(neuron2.getY() - neuron1.getY(), 2));
        return ret;
    }

    /**
     * get Alpha.
     *
     * @return alpha
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * Get alphaDecayRate.
     *
     * @return alphaDecayRate
     */
    public double getAlphaDecayRate() {
        return alphaDecayRate;
    }

    /**
     * Get the Batch Size.
     *
     * @return batchSize
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Returns the default SOM neuron.
     *
     * @return ret default som neuron
     */
    private Neuron getDefaultSOMNeuron() {
        LinearRule rule = new LinearRule();
        Neuron ret = new Neuron(getParentNetwork(), rule);
        rule.setIncrement(1);
        rule.setLowerBound(0);
        return ret;
    }

    /**
     * get Initial Alpha.
     *
     * @return initAlpha
     */
    public double getInitAlpha() {
        return initAlpha;
    }

    /**
     * Get the initial neighborhoodsize.
     *
     * @return initNeighborhoodSize
     */
    public double getInitNeighborhoodSize() {
        return initNeighborhoodSize;
    }

    /**
     * Get neighborhoodDecayAmount.
     *
     * @return neighborhoodDecayAmount
     */
    public double getNeighborhoodDecayAmount() {
        return neighborhoodDecayAmount;
    }

    /**
     * Get the current neighborhood size.
     *
     * @return neighborhoodSize
     */
    public double getNeighborhoodSize() {
        return neighborhoodSize;
    }

    /**
     * Get the number of neurons.
     *
     * @return numNeurons
     */
    public int getNumNeurons() {
        return numNeurons;
    }

    /**
     * Set alphaDecayRate.
     *
     * @param alphaDecayRate decay rate
     */
    public void setAlphaDecayRate(final double alphaDecayRate) {
        this.alphaDecayRate = alphaDecayRate;
    }

    /**
     * Set the Batch Size.
     *
     * @param batchSize Batch Size
     */
    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Set the initial value for alpha. Resets SOM if new.
     *
     * @param initAlpha initial alpha
     */
    public void setInitAlpha(final double initAlpha) {
        this.initAlpha = initAlpha;
    }

    /**
     * Set the initial neighborhood size.
     *
     * @param initNeighborhoodSize initial neighborhood size Resets SOM if new.
     */
    public void setInitNeighborhoodSize(final double initNeighborhoodSize) {
        this.initNeighborhoodSize = initNeighborhoodSize;
        neighborhoodSize = initNeighborhoodSize;
    }

    /**
     * Set neighborhoodDecayAmount.
     *
     * @param neighborhoodDecayAmount decay amount
     */
    public void setNeighborhoodDecayAmount(final double neighborhoodDecayAmount) {
        this.neighborhoodDecayAmount = neighborhoodDecayAmount;
    }

    /**
     * Set the number of neurons.
     *
     * @param numNeurons number of neurons.
     */
    public void setNumNeurons(final int numNeurons) {
        this.numNeurons = numNeurons;
    }

}
