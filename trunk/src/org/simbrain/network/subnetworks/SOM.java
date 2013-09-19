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

import java.io.File;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.neuron_update_rules.LinearRule;

/**
 * <b>SOM</b> implements a Self-Organizing Map network.
 * 
 * @author William B. St. Clair
 * 
 *         TODO: Move all "training" functions over to trainer
 */
public class SOM extends NeuronGroup {

    /** Default alpha. */
    public static final double DEFAULT_ALPHA = 0.06;

    /** Default initial neighborhood size. */
    public static final double DEFAULT_INIT_NSIZE = 100;

    /** Default numInputVectors. */
    public static final int DEFAULT_INPUT_VECTORS = 4;

    /** Default Update Interval. */
    public static final int DEFAULT_UPDATE_INTERVAL = 50;

    /** Standard update. */
    public static final int STANDARD = 0;

    /** WTA update. */
    public static final int WTA = 1;

    /** Update method. */
    public static final int updateMethod = WTA;

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

    /** The total epochs the SOM has iterated through since last reset. */
    private int epochs = 0;

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

    /** Number of input vectors. */
    private int numInputVectors = DEFAULT_INPUT_VECTORS;

    /** Number of neurons. */
    private int numNeurons = 16;

    /** Number of vectors seen by the SOM since last full iteration. */
    private int vectorNumber = 0;

    /** Input training file for persistence. */
    private File trainingINFile = null;

    /** Input portion of training corpus. */
    private double[][] trainingInputs;

    /** The number of epochs run in a given batch. */
    private int batchSize = DEFAULT_BATCH_SIZE;

    /** The rate at which the learning rate decays. */
    private double alphaDecayRate = DEFAULT_DECAY_RATE;

    /** The amount that the neighborhood decrements. */
    private double neighborhoodDecayAmount = DEFAULT_NEIGHBORHOOD_DECAY_AMOUNT;

    /**
     * Constructs an SOM network with specified number of neurons.
     * 
     * @param numNeurons
     *            size of this network in neurons
     * @param root
     *            reference to Network.
     */
    public SOM(final Network root, final int numNeurons) {
	super(root);
	for (int i = 0; i < numNeurons; i++) {
	    addNeuron(new Neuron(getParentNetwork(), new LinearRule()));
	}
	setLabel("SOM");
    }

    /**
     * Iterates the network based on training inputs. Does not respect superior
     * networks.
     * 
     * TODO: Integrate this code with train and update.
     */
    public void iterate() {
	for (vectorNumber = 0; vectorNumber <= numInputVectors - 1; vectorNumber++) {

	    winDistance = Double.POSITIVE_INFINITY;
	    int counter;
	    double physicalDistance;
	    Neuron winner = null;

	    // Determine Winner: The SOM Neuron with the lowest distance between
	    // it's weight vector and the input neurons's weight vector.
	    for (int i = 0; i < getNeuronList().size(); i++) {
		Neuron n = getNeuronList().get(i);
		distance = 0;
		counter = 0;
		for (Synapse incoming : n.getFanIn()) {
		    distance += Math.pow(incoming.getStrength()
			    - trainingInputs[vectorNumber][counter], 2);
		    counter++;
		}
		if (distance < winDistance) {
		    winDistance = distance;
		    winner = n;
		}
	    }

	    // Update Weights of the neurons within the radius of the winning
	    // neuron.
	    for (int i = 0; i < getNeuronList().size(); i++) {
		Neuron neuron = getNeuronList().get(i);
		physicalDistance = findPhysicalDistance(neuron, winner);

		// The center of the neuron is within the update region.
		if (physicalDistance <= neighborhoodSize) {
		    counter = 0;
		    for (Synapse incoming : neuron.getFanIn()) {
			val = incoming.getStrength()
				+ alpha
				* (trainingInputs[vectorNumber][counter] - incoming
					.getStrength());
			incoming.setStrength(val);
			counter++;
		    }
		}
	    }
	} // end this training vector

	alpha = (alpha - alphaDecayRate * alpha);
	if (neighborhoodSize - neighborhoodDecayAmount > 0) {
	    neighborhoodSize -= neighborhoodDecayAmount;
	} else {
	    neighborhoodSize = 0;
	}
	epochs++;
    }

    /**
     * Randomize all weights coming in to this network. The weights will be
     * between 0 and the upper bound of each synapse.
     */
    public void randomizeIncomingWeights() {
	for (Neuron n : getNeuronList()) {
	    for (Synapse s : n.getFanIn()) {
		s.setStrength(s.getUpperBound() * Math.random());
	    }
	}
    }

    /**
     * Pushes the weight values of an SOM neuron onto the input neurons.
     */
    public void recall() {
	winDistance = 0;
	Neuron winner = null;
	for (int i = 0; i < getNeuronList().size(); i++) {
	    Neuron n = getNeuronList().get(i);
	    if (n.getActivation() > winDistance) {
		winDistance = n.getActivation();
		winner = n;
	    }
	}
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
	vectorNumber = 0;
	epochs = 0;
    }

    /**
     * Trains the network in batches based on trainingInputs. Does not respect
     * superior networks.
     * 
     * TODO: Integrate this code with train and update
     */
    public void train() {
	int epochNumber;
	for (epochNumber = 0; epochNumber < batchSize; epochNumber++) {
	    for (vectorNumber = 0; vectorNumber <= numInputVectors - 1; vectorNumber++) {

		winDistance = Double.POSITIVE_INFINITY;
		Neuron winner = null;
		int counter;
		double physicalDistance;

		// Determine Winner: The SOM Neuron with the lowest distance
		// between
		// it's weight vector and the input neurons's weight vector.
		for (int i = 0; i < getNeuronList().size(); i++) {
		    Neuron n = getNeuronList().get(i);
		    distance = 0;
		    counter = 0;
		    for (Synapse incoming : n.getFanIn()) {
			distance += Math.pow(incoming.getStrength()
				- trainingInputs[vectorNumber][counter], 2);
			counter++;
		    }
		    if (distance < winDistance) {
			winDistance = distance;
			winner = n;
		    }
		}

		// Update Weights of the neurons within the radius of the
		// winning neuron.
		for (Neuron neuron : getNeuronList()) {
		    physicalDistance = findPhysicalDistance(neuron, winner);

		    // The center of the neuron is within the update region.
		    if (physicalDistance <= neighborhoodSize) {
			counter = 0;
			for (Synapse incoming : neuron.getFanIn()) {
			    val = incoming.getStrength()
				    + alpha
				    * (trainingInputs[vectorNumber][counter] - incoming
					    .getStrength());
			    incoming.setStrength(val);
			    counter++;
			}
		    }
		}
	    } // end this training vector

	    alpha = (alpha - alphaDecayRate * alpha);
	    if (neighborhoodSize - neighborhoodDecayAmount > 0) {
		neighborhoodSize -= neighborhoodDecayAmount;
	    } else {
		neighborhoodSize = 0;
	    }
	    epochs++;
	} // end epoch
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

	// Determine Winner: The SOM Neuron with the lowest distance between
	// its weight vector and the input neurons's weight vector.
	Neuron winner = calculateWinner();

	// Neuron update
	if (!getParentNetwork().getClampNeurons()) {
	    if (updateMethod == STANDARD) {
		super.update();
	    } else {
		for (int i = 0; i < getNeuronList().size(); i++) {
		    Neuron n = getNeuronList().get(i);
		    if (n == winner) {
			n.setActivation(1);
		    } else {
			n.setActivation(0);
		    }
		}
	    }
	}

	// Update Synapses of the neurons within the radius of the winning
	// neuron.
	if (!getParentNetwork().getClampWeights()) {
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
	    // TODO: Now reducing decay rate as
	    // percentage. Document and make
	    // others consistent with this.
	    if (neighborhoodSize - neighborhoodDecayAmount > 0) {
		neighborhoodSize -= neighborhoodDecayAmount;
	    } else {
		neighborhoodSize = 0;
	    }
	}
    }

    /**
     * Find the SOM neuron which is closest to the input vector. TODO: Reuse
     * this more.
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
     * @param n
     *            The SOM neuron one wishes to find the for.
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
     * @param neuron1
     *            First neuron.
     * @param neuron2
     *            Second neuron.
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
	Neuron ret = new Neuron(getParentNetwork(), new LinearRule());
	ret.setIncrement(1);
	ret.setLowerBound(0);
	return ret;
    }

    /**
     * get Epochs.
     * 
     * @return epochs
     */
    public int getEpochs() {
	return epochs;
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
     * Get the current neighborhoodsize.
     * 
     * @return neighborhoodSize
     */
    public double getNeighborhoodSize() {
	return neighborhoodSize;
    }

    /**
     * Get the total number of input vectors.
     * 
     * @return numInputVectors
     */
    public int getNumInputVectors() {
	return numInputVectors;
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
     * Get the input training File.
     * 
     * @return trainingINFile
     */
    public File getTrainingINFile() {
	return trainingINFile;
    }

    /**
     * Get the training inputs.
     * 
     * @return trainingInputs
     */
    public double[][] getTrainingInputs() {
	return trainingInputs;
    }

    /**
     * Get the current vector number.
     * 
     * @return vectorNumber
     */
    public int getVectorNumber() {
	return vectorNumber;
    }

    /**
     * Set alphaDecayRate.
     * 
     * @param alphaDecayRate
     *            decay rate
     */
    public void setAlphaDecayRate(final double alphaDecayRate) {
	this.alphaDecayRate = alphaDecayRate;
    }

    /**
     * Set the Batch Size.
     * 
     * @param batchSize
     *            Batch Size
     */
    public void setBatchSize(final int batchSize) {
	this.batchSize = batchSize;
    }

    /**
     * Set Epochs.
     * 
     * @param epochs
     *            epochs
     */
    public void setEpochs(final int epochs) {
	this.epochs = epochs;
    }

    /**
     * Set the initial value for alpha. Resets SOM if new.
     * 
     * @param initAlpha
     *            initial alpha
     */
    public void setInitAlpha(final double initAlpha) {
	this.initAlpha = initAlpha;
    }

    /**
     * Set the initial neighborhoodsize.
     * 
     * @param initNeighborhoodSize
     *            initial neighborhood size Resets SOM if new.
     */
    public void setInitNeighborhoodSize(final double initNeighborhoodSize) {
	this.initNeighborhoodSize = initNeighborhoodSize;
	// TODO: make it possibly to directly set n-hood size
	neighborhoodSize = initNeighborhoodSize;
    }

    /**
     * Set neighborhoodDecayAmount.
     * 
     * @param neighborhoodDecayAmount
     *            decay amount
     */
    public void setNeighborhoodDecayAmount(final double neighborhoodDecayAmount) {
	this.neighborhoodDecayAmount = neighborhoodDecayAmount;
    }

    /**
     * Set the total number of input vectors. Resets SOM if new.
     * 
     * @param numInputVectors
     *            total input vectors
     */
    public void setNumInputVectors(final int numInputVectors) {
	this.numInputVectors = numInputVectors;
    }

    /**
     * Set the number of neurons.
     * 
     * @param numNeurons
     *            number of neurons.
     */
    public void setNumNeurons(final int numNeurons) {
	this.numNeurons = numNeurons;
    }

    /**
     * Set the training input File.
     * 
     * @param trainingINFile
     *            input file
     */
    public void setTrainingINFile(final File trainingINFile) {
	this.trainingINFile = trainingINFile;
    }

    /**
     * Set the training inputs.
     * 
     * @param trainingInputs
     *            inputs
     */
    public void setTrainingInputs(final double[][] trainingInputs) {
	this.trainingInputs = trainingInputs;
    }
}
