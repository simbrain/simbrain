package org.simnet.networks;

import java.util.Iterator;
import java.lang.Math;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.layouts.Layout;
import org.simnet.neurons.LinearNeuron;

/**
 * <b>SOM</b> implements a Self-Organizing Map network.
 *
 * @author William B. St. Clair
 */
public class SOM extends Network {


    /** Default alpha. */
    private static final double DEFAULT_ALPHA = 0.6;

   /** Initial Learning Rate. */
    private double initAlpha = DEFAULT_ALPHA;

    /** Learning rate. */
    private double alpha = DEFAULT_ALPHA;

    /** The total epochs the SOM has iterated through since last reset. */
    private int epochs = 0;

    /** Current Neighborhood Size. With a circular neighborhood, neighborhoodSize connotes radius.*/
    private double neighborhoodSize = 0;

    /** The initial neighborhoodSize.
     * neighborhoodSize is set back to this whenever network is reset.
     */
    private double initNeighborhoodSize = 0;

    /** MinDistance, distance and val are changing variables used in the update method. */
    private double winDistance, distance, val;

    /** The radius of a neuron in Simbrain. */
    private static final double neuronRadius = org.simbrain.network.nodes.NeuronNode.getDIAMETER() / 2;

    /** Number of input vectors. */
    private int numInputVectors = 4;

    /** Number of neurons. */
    private int numNeurons = 2;

    /** Number of vectors seen by the SOM since last full iteration. */
    private int vectorNumber = 0;

    /** Winner index. */
    private int winner;

    /** If recallMode is true, the SOM network will update in cluster recall mode.*/
    private boolean recallMode = false;


    /**
     * Default constructor used by Castor.
     */
    public SOM() {
    }

    /**
     * Constructs an SOM network with specified number of neurons.
     *
     * @param numNeurons size of this network in neurons
     * @param layout Defines how neurons are to be layed out
     */
    public SOM(final int numNeurons, final Layout layout) {
        super();
        for (int i = 0; i < numNeurons; i++) {
            this.addNeuron(new LinearNeuron());
        }
        layout.layoutNeurons(this);
    }

    /**
     * Update the network.
     * This method has the following structure:
     * If the network is not in recall mode, update SOM network. {
     *  If all weights are clamped, return.
     *  Determine the winner by finding which of the SOM neurons is closest to the input vector.
     *  Update the winning neuron and it's neighborhood. The update algorithm accounts for all
     *  possible arrangements of the SOM network.
     *             - When the neuron is outside of the neighborhood.
     *             - When the neuron is within the the neighborhood.
     *             - When the neuron is only partially within the neighborhood.
     *  Including the current vector, if the total number of vectors analyzed during the current
     *  iteration is equal to the total number of vectors to be analyzed, update the network
     *  parameters and count one full iteration.
     * }
     * Else the network must be in recallMode. {
     *  If all neurons are clamped, return.
     *  Find the SOM neuron with heighest activation.
     *  Set the activations of input neurons according to the SOM weights.
     */
    public void update() {
        boolean winDistanceUndef = true;

        if (!recallMode) {
            if (getClampWeights()) {
                  return;
           }

            winDistance = 0;
            winner = 0;
            double physicalDistance;

            // Determine Winner: The SOM Neuron with the lowest distance between
            //it's weight vector and the input neurons's weight vecotr.
            for (int i = 0; i < getNeuronList().size(); i++) {
                Neuron n = (Neuron) getNeuronList().get(i);
                distance = findDistance(n);
                if (distance < winDistance || winDistanceUndef) {
                    winDistance = distance;
                    winner = i;
                    winDistanceUndef = false;
                }
            }

            Neuron winningNeuron = (Neuron) getNeuronList().get(winner);

            // Update Weights of the neurons within the radius of the winning neuron.

            for (int i = 0; i < getNeuronList().size(); i++) {
                Neuron neuron = ((Neuron) getNeuronList().get(i));
                physicalDistance = findPhysicalDistance(neuron, winningNeuron);

                if (neuronRadius + neighborhoodSize < physicalDistance) {
//                  No part of the neuron is within the update region.
                    continue;
                }

                else if (physicalDistance < neighborhoodSize || i == winner) {
//                  The center of the neuron is within the update region.
                    for (Iterator l = neuron.getFanIn().iterator(); l.hasNext(); ) {
                        Synapse incoming = (Synapse) l.next();
                        val = incoming.getStrength() + alpha * (incoming.getSource().getActivation()
                                - incoming.getStrength());
                        incoming.setStrength(val);
                    }
                }
                else { // Only part of the neuron, but not it's center, is within the region to update.
                    for (Iterator l = neuron.getFanIn().iterator(); l.hasNext(); ) {
                        Synapse incoming = (Synapse) l.next();
//                      The PartialUpdateCoefficient scales the update by how much of the neuron is in the neighborhood.
                        val = incoming.getStrength() + findPartialUpdateCoefficient(physicalDistance)
                                * alpha * (incoming.getSource().getActivation() - incoming.getStrength());
                        incoming.setStrength(val);
                    }
                }
            }
            vectorNumber++;
            if (vectorNumber == numInputVectors) {
            //If one SOM iteration is complete, update Learning Rate.
                alpha *= 0.5;

                //Update neighborhoodSize.
                if (neighborhoodSize - 12 > 0) {
                    neighborhoodSize -= 12;
                }
                else {
                    neighborhoodSize = 0;
                }
                vectorNumber = 0; //Reset iteration.
                epochs++;
            }
        }
        else { //Recall Mode
            if (getClampNeurons()) {
                return;
            }
            //Determine which SOM vector is to be recalled.
            for (int i = 0; i < getNeuronList().size(); i++) {
                Neuron n = (Neuron) getNeuronList().get(i);
                if (n.getActivation() > winDistance || winDistanceUndef) {
                    winDistance = n.getActivation();
                    winner = i;
                    winDistanceUndef = false;
                }
            }
            Neuron winningNeuron = (Neuron) getNeuronList().get(winner);
            for (Iterator l = winningNeuron.getFanIn().iterator(); l.hasNext(); ) {
                Synapse incoming = (Synapse) l.next();
                incoming.getSource().setActivation(incoming.getStrength());
            }
        }
    }


    /**
     * Calculates the euclidian distance between the SOM neuron's weight vector and the input vector.
     * @param n The SOM neuron one wishes to find the for.
     * @return distance.
     */
    public double findDistance(final Neuron n) {
        double ret = 0;
        for (Iterator k = n.getFanIn().iterator(); k.hasNext(); ) {
            Synapse incoming = (Synapse) k.next();
            ret +=  Math.pow(incoming.getStrength() - incoming.getSource().getActivation(), 2);
         }
        return ret;
    }
    /**
     * Generate the Coefficient to scale the update for current neuron.
     * This is essentially the percentage stating how much of the neuron is within the update radius.
     * Since it's impossible for more than half of a neuron to be within the update radius without
     * it's center being within the update radius, only half of the neuron's total area is used.
     * This is to make the gradient of the coefficient from 0 to 1 more smooth and complete.
     * @param physicalDistance the physical distance between the winning neuron and the neuron in question.
     * @return coefficient
     */
    private double findPartialUpdateCoefficient(final double physicalDistance) {
        double coefficient;
        double areaOfTriangle = (neuronRadius - (neuronRadius + neighborhoodSize - physicalDistance) / 2)
                                * Math.sqrt(Math.pow(neuronRadius, 2) - Math.pow(neuronRadius
                                - (neuronRadius + neighborhoodSize - physicalDistance) / 2, 2));
        double areaOfSector = 1 / 2 * Math.pow(neuronRadius, 2) * Math.asin((Math.sqrt(Math.pow(neuronRadius, 2)
                              - Math.pow(neuronRadius - (neuronRadius + neighborhoodSize - physicalDistance) / 2, 2))
                              / neuronRadius));
        double areaOfRegion = 2 * (areaOfSector - areaOfTriangle);
        coefficient = areaOfRegion / (Math.PI * neuronRadius);
        return coefficient;
    }

    /**
     * Finds the physical euclidian Distance between two neurons.
     * @param neuron1 First neuron.
     * @param neuron2 Second neuron.
     * @return physical distance between two neurons in Simbrain.
     */
    public double findPhysicalDistance(final Neuron neuron1, final Neuron neuron2) {
        double ret = Math.sqrt(Math.pow(neuron2.getX() - neuron1.getX(), 2)
                      + Math.pow(neuron2.getY() - neuron1.getY(), 2));
        return ret;
    }


    /**
     * Randomize all weights coming in to this network.
     * The weights will be between 0 and the upper bound of each synapse.
     */
    public void randomizeIncomingWeights() {
        for (Iterator i = getNeuronList().iterator(); i.hasNext(); ) {
            Neuron n = (Neuron) i.next();
            for (Iterator j = n.getFanIn().iterator(); j.hasNext(); ) {
                Synapse s = (Synapse) j.next();
                s.setStrength(s.getUpperBound() * Math.random());
            }
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
        randomizeIncomingWeights();
    }
    /**
     * get Alpha.
     * @return alpha
     */
    public double getAlpha() {
        return alpha;
    }
    /**
     * get Initial Alpha.
     * @return initAlpha
     */
    public double getInitAlpha() {
        return initAlpha;
    }
    /**
     * get Epochs.
     * @return epochs
     */
    public int getEpochs() {
        return epochs;
    }
    /**
     * Get the initial neighborhoodsize.
     * @return initNeighborhoodSize
     */
    public double getInitNeighborhoodSize() {
        return initNeighborhoodSize;
    }
    /**
     * Get the current neighborhoodsize.
     * @return neighborhoodSize
     */
    public double getNeighborhoodSize() {
        return neighborhoodSize;
    }
    /**
     * Get the total number of input vectors.
     * @return numInputVectors
     */
    public int getNumInputVectors() {
        return numInputVectors;
    }
    /**
     * Is the SOM in recall mode?
     * @return recallMode
     */
    public boolean isRecallMode() {
        return recallMode;
    }
    /**
     * Get the current vector number.
     * @return vectorNumber
     */
    public int getVectorNumber() {
        return vectorNumber;
    }
    /**
     * Get the number of neurons.
     * @return numNeurons
     */
    public int getNumNeurons() {
        return numNeurons;
    }
    /**
     * Set the number of neurons.
     * @param numNeurons number of neurons.
     */
    public void setNumNeurons(final int numNeurons) {
        this.numNeurons = numNeurons;
    }
    /**
     * Set the initial value for alpha.
     * Resets SOM if new.
     * @param initAlpha initial alpha
     */
    public void setInitAlpha(final double initAlpha) {
        if (this.initAlpha != initAlpha) {
            this.initAlpha = initAlpha;
            reset();
        }
    }
    /**
     * Set the initial neighborhoodsize.
     * @param initNeighborhoodSize initial neighborhood size
     * Resets SOM if new.
     */
    public void setInitNeighborhoodSize(final double initNeighborhoodSize) {
        if (this.initNeighborhoodSize != initNeighborhoodSize) {
            this.initNeighborhoodSize = initNeighborhoodSize;
            reset();
        }
    }

    /**
     * Set the total number of input vectors.
     * Resets SOM if new.
     * @param numInputVectors total input vectors
     */
    public void setNumInputVectors(final int numInputVectors) {
        if (this.numInputVectors != numInputVectors) {
            this.numInputVectors = numInputVectors;
            reset();
        }
    }
    /**
     * Set the recall mode boolean.
     * @param recallMode recall mode boolean
     */
    public void setRecallMode(final boolean recallMode) {
        this.recallMode = recallMode;
    }
}
