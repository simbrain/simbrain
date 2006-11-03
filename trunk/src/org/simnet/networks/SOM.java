package org.simnet.networks;

import java.io.File;
import java.util.Iterator;

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

    /** Default initial neighborhood size. */
    private static final double DEFAULT_INIT_NSIZE = 100;

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
    private double initNeighborhoodSize = DEFAULT_INIT_NSIZE;

    /** MinDistance, distance and val are changing variables used in the update method. */
    private double winDistance, distance, val;

    /** Default numInputVectors. */
    private static final int DEFAULT_INPUT_VECTORS = 4;

    /** Number of input vectors. */
    private int numInputVectors = DEFAULT_INPUT_VECTORS;

    /** Number of neurons. */
    private int numNeurons = 2;

    /** Number of vectors seen by the SOM since last full iteration. */
    private int vectorNumber = 0;

    /** Winner index. */
    private int winner;

    /** Input training file for persistance. */
    private File trainingINFile = null;

    /** Standard update. */
    private static final int STANDARD = 0;

    /** WTA update. */
    private static final int WTA = 1;

    /** Update method. */
    private static final int updateMethod = WTA;

    /** Update interval. */
    private static final int updateInterval = 50;

    /** Input portion of training corpus. */
    private double[][] trainingInputs;

    /** Default batchSize. */
    private static final int DEFAULT_BATCH_SIZE = 100;

    /** The number of epochs run in a given batch. */
    private int batchSize = DEFAULT_BATCH_SIZE;

    /** The rate at which the learning rate decays. */
    private double alphaDecayRate = DEFAULT_DECAY_RATE;

    /** The default alphaDecayRate. */
    private static final double DEFAULT_DECAY_RATE = 0.05;

    /** The amount that the neighborhood decrements. */
    private int neighborhoodDecayAmount = DEFAULT_NEIGHBORHOOD_DECAY_AMOUNT;

    /** The default neighborhoodDecayAmount. */
    private static final int DEFAULT_NEIGHBORHOOD_DECAY_AMOUNT = 5;

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
            this.addNeuron(getDefaultSOMNeuron());
        }
        layout.layoutNeurons(this);
    }

    /**
     * Returns the default SOM neuron.
     * @return ret default som neuron
     */
    private Neuron getDefaultSOMNeuron() {
        LinearNeuron ret = new LinearNeuron();
        ret.setIncrement(1);
        ret.setLowerBound(0);
        return ret;
    }

    /**
     * Update the network.
     * This method has the following structure:
     * If the network is not in recall mode, update SOM network.
     *  If all weights are clamped, return.
     *  Determine the winner by finding which of the SOM neurons is closest to the input vector.
     *  Update the winning neuron and it's neighborhood. The update algorithm accounts for all
     *  possible arrangements of the SOM network.
     *             - When the neuron is outside of the neighborhood.
     *             - When the neuron is within the the neighborhood.
     *  Including the current vector, if the total number of vectors analyzed during the current
     *  iteration is equal to the total number of vectors to be analyzed, update the network
     *  parameters and count one full iteration.
     * Else the network must be in recallMode.
     *  If all neurons are clamped, return.
     *  Find the SOM neuron with heighest activation.
     *  Set the activations of input neurons according to the SOM weights.
     */
    public void update() {


            winDistance = Double.MAX_VALUE;
            winner = 0;
            double physicalDistance;

            // Determine Winner: The SOM Neuron with the lowest distance between
            // it's weight vector and the input neurons's weight vector.
            for (int i = 0; i < getNeuronList().size(); i++) {
                Neuron n = (Neuron) getNeuronList().get(i);
                distance = findDistance(n);
                if (distance < winDistance) {
                    winDistance = distance;
                    winner = i;
                }
            }
            Neuron winningNeuron = (Neuron) getNeuronList().get(winner);

            // Neuron update
            if (!getClampNeurons()) {
                if (updateMethod == STANDARD) {
                    this.updateAllNeurons();
                } else {
                    for (int i = 0; i < getNeuronList().size(); i++) {
                        Neuron n = (Neuron) getNeuronList().get(i);
                        if (n == winningNeuron) {
                            n.setActivation(1);
                        } else {
                            n.setActivation(0);
                        }
                    }
                }
            }

            // Synapse update
            if (!getClampWeights()) {
                // Update Weights of the neurons within the radius of the winning neuron.
                for (int i = 0; i < getNeuronList().size(); i++) {
                    Neuron neuron = ((Neuron) getNeuronList().get(i));
                    physicalDistance = findPhysicalDistance(neuron, winningNeuron);

                    // The center of the neuron is within the update region.
                    if (physicalDistance <= neighborhoodSize) {
                        for (Iterator l = neuron.getFanIn().iterator(); l.hasNext(); ) {
                            Synapse incoming = (Synapse) l.next();
                            val = incoming.getStrength() + alpha * (incoming.getSource().getActivation()
                                    - incoming.getStrength());
                            incoming.setStrength(val);
                        }
                    }
                }

                // Whenvere updateInterval time-steps pass, update learning rate, etc.
                if (this.getTime() % updateInterval == 0) {
                    alpha *= alphaDecayRate;
                    //Update neighborhoodSize.
                    if (neighborhoodSize - neighborhoodDecayAmount > 0) {
                        neighborhoodSize -= neighborhoodDecayAmount;
                    } else {
                        neighborhoodSize = 0;
                    }
                }
            }
    }

    /**
     * Trains the network in batches based on trainingInputs.
     * Does not respect superior networks.
     */
    public void train() {
        int epochNumber;
        for (epochNumber = 0; epochNumber < batchSize; epochNumber++) {
            for (vectorNumber = 0; vectorNumber <= numInputVectors - 1; vectorNumber++) {

                winDistance = Double.MAX_VALUE;
                winner = 0;
                int counter;
                double physicalDistance;

                // Determine Winner: The SOM Neuron with the lowest distance between
                // it's weight vector and the input neurons's weight vector.
                for (int i = 0; i < getNeuronList().size(); i++) {
                    Neuron n = (Neuron) getNeuronList().get(i);
                    distance = 0;
                    counter = 0;
                    for (Iterator k = n.getFanIn().iterator(); k.hasNext(); ) {
                        Synapse incoming = (Synapse) k.next();
                        distance +=  Math.pow(incoming.getStrength() - trainingInputs[vectorNumber][counter], 2);
                        counter++;
                    }
                    if (distance < winDistance) {
                        winDistance = distance;
                        winner = i;
                    }
                }
                Neuron winningNeuron = (Neuron) getNeuronList().get(winner);

                // Update Weights of the neurons within the radius of the winning neuron.
                for (int i = 0; i < getNeuronList().size(); i++) {
                    Neuron neuron = ((Neuron) getNeuronList().get(i));
                    physicalDistance = findPhysicalDistance(neuron, winningNeuron);

                    // The center of the neuron is within the update region.
                    if (physicalDistance <= neighborhoodSize) {
                        counter = 0;
                        for (Iterator l = neuron.getFanIn().iterator(); l.hasNext(); ) {
                            Synapse incoming = (Synapse) l.next();
                            val = incoming.getStrength() + alpha * (trainingInputs[vectorNumber][counter]
                                    - incoming.getStrength());
                            incoming.setStrength(val);
                            counter++;
                        }
                    }
                }
            } // end this training vector

            alpha *= alphaDecayRate;
            if (neighborhoodSize - neighborhoodDecayAmount > 0) {
                neighborhoodSize -= neighborhoodDecayAmount;
            } else {
                neighborhoodSize = 0;
            }
            epochs++;
        } // end epoch
    }


    /**
     * Iterates the network based on training inputs.
     * Does not respect superior networks.
     */
    public void iterate() {
        for (vectorNumber = 0; vectorNumber <= numInputVectors - 1; vectorNumber++) {

            winDistance = Double.MAX_VALUE;
            winner = 0;
            int counter;
            double physicalDistance;

            // Determine Winner: The SOM Neuron with the lowest distance between
            // it's weight vector and the input neurons's weight vector.
            for (int i = 0; i < getNeuronList().size(); i++) {
                Neuron n = (Neuron) getNeuronList().get(i);
                distance = 0;
                counter = 0;
                for (Iterator k = n.getFanIn().iterator(); k.hasNext(); ) {
                    Synapse incoming = (Synapse) k.next();
                    distance +=  Math.pow(incoming.getStrength() - trainingInputs[vectorNumber][counter], 2);
                    counter++;
                }
                if (distance < winDistance) {
                    winDistance = distance;
                    winner = i;
                }
            }
            Neuron winningNeuron = (Neuron) getNeuronList().get(winner);

            // Update Weights of the neurons within the radius of the winning neuron.
            for (int i = 0; i < getNeuronList().size(); i++) {
                Neuron neuron = ((Neuron) getNeuronList().get(i));
                physicalDistance = findPhysicalDistance(neuron, winningNeuron);

                // The center of the neuron is within the update region.
                if (physicalDistance <= neighborhoodSize) {
                    counter = 0;
                    for (Iterator l = neuron.getFanIn().iterator(); l.hasNext(); ) {
                        Synapse incoming = (Synapse) l.next();
                        val = incoming.getStrength() + alpha * (trainingInputs[vectorNumber][counter]
                                - incoming.getStrength());
                        incoming.setStrength(val);
                        counter++;
                    }
                }
            }
        } // end this training vector

        alpha *= alphaDecayRate;
        if (neighborhoodSize - neighborhoodDecayAmount > 0) {
            neighborhoodSize -= neighborhoodDecayAmount;
        } else {
            neighborhoodSize = 0;
        }
        epochs++;
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
    }

    /**
     * Pushes the weight values of an SOM neuron onto the input neurons.
     */
    public void recall() {
        winDistance = 0;
        for (int i = 0; i < getNeuronList().size(); i++) {
            Neuron n = (Neuron) getNeuronList().get(i);
            if (n.getActivation() > winDistance) {
                winDistance = n.getActivation();
                winner = i;
            }
        }
        Neuron winningNeuron = (Neuron) getNeuronList().get(winner);
        for (Iterator l = winningNeuron.getFanIn().iterator(); l.hasNext(); ) {
            Synapse incoming = (Synapse) l.next();
            incoming.getSource().setActivation(incoming.getStrength());
        }

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
            this.initAlpha = initAlpha;
    }
    /**
     * Set the initial neighborhoodsize.
     * @param initNeighborhoodSize initial neighborhood size
     * Resets SOM if new.
     */
    public void setInitNeighborhoodSize(final double initNeighborhoodSize) {
            this.initNeighborhoodSize = initNeighborhoodSize;
    }

    /**
     * Set the total number of input vectors.
     * Resets SOM if new.
     * @param numInputVectors total input vectors
     */
    public void setNumInputVectors(final int numInputVectors) {
            this.numInputVectors = numInputVectors;
    }

    /**
     * Get the input training File.
     * @return trainingINFile
     */
    public File getTrainingINFile() {
        return trainingINFile;
    }

    /**
     * Set the training input File.
     * @param trainingINFile input file
     */
    public void setTrainingINFile(final File trainingINFile) {
         this.trainingINFile = trainingINFile;
    }

    /**
     * Get the training inputs.
     * @return trainingInputs
     */
    public double[][] getTrainingInputs() {
        return trainingInputs;
    }

    /**
     * Set the training inputs.
     * @param trainingInputs inputs
     */
    public void setTrainingInputs(final double[][] trainingInputs) {
        this.trainingInputs = trainingInputs;
        }

    /**
     * Set Epochs.
     * @param epochs epochs
     */
    public void setEpochs(final int epochs) {
        this.epochs = epochs;
    }

    /**
     * Get the Batch Size.
     * @return batchSize
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Set the Batch Size.
     * @param batchSize Batch Size
     */
    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Get alphaDecayRate.
     * @return alphaDecayRate
     */
    public double getAlphaDecayRate() {
        return alphaDecayRate;
    }

    /**
     * Get neighborhoodDecayAmount.
     * @return neighborhoodDecayAmount
     */
    public int getNeighborhoodDecayAmount() {
        return neighborhoodDecayAmount;
    }

    /**
     * Set alphaDecayRate.
     * @param alphaDecayRate decay rate
     */
    public void setAlphaDecayRate(final double alphaDecayRate) {
        this.alphaDecayRate = alphaDecayRate;
    }

    /**
     * Set neighborhoodDecayAmount.
     * @param neighborhoodDecayAmount decay amount
     */
    public void setNeighborhoodDecayAmount(final int neighborhoodDecayAmount) {
        this.neighborhoodDecayAmount = neighborhoodDecayAmount;
    }
}
