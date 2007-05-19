/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
package org.simnet.networks;

import java.io.File;

import org.simnet.connections.AllToAll;
import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.RootNetwork;
import org.simnet.interfaces.Synapse;
import org.simnet.layouts.Layout;
import org.simnet.neurons.ClampedNeuron;
import org.simnet.neurons.LinearNeuron;
import org.simnet.util.ConnectNets;


/**
 * <b>LMSNetwork</b> implements a least means squared network.
 */
public class LMSNetwork extends Network {

    /** Input layer of LMSNetwork network. */
    private StandardNetwork inputLayer;

    /** Output layer of LMSNetwork network. */
    private StandardNetwork outputLayer;

    /** Default number of input units. */
    private int defaultInputs = 3;

    /** Default number of output units. */
    private int defaultOutputs = 3;

    /** Number of epochs. */
    private int epochs = 1000;

    /** Current error. */
    private double rmsError;

    /** Learning rate. */
    private double eta = .5;

    /** Input portion of training corpus. */
    private double[][] trainingInputs;

    /** Output portion of training corpus. */
    private double[][] trainingOutputs;

    /** Input training file for persistance. */
    private File trainingINFile = null;

    /** Output training file for persistance. */
    private File trainingOUTFile = null;

    /**
     * Default constructor.
     *
     */
    public LMSNetwork() {
        super();
    }

    /**
     * Construct an LMS network with a specified number of input and output layers.
     *
     * @param nInputs number of input nodes
     * @param nOutputs number of output nodes
     * @param layout layout of the new nodes
     * @param root reference to RootNetwork.
     */
    public LMSNetwork(final RootNetwork root, final int nInputs, final int nOutputs, final Layout layout) {
        super();
        this.setRootNetwork(root);
        this.setParentNetwork(root);
        buildNetwork(nInputs, nOutputs);
        layout.layoutNeurons(this);
        makeConnections();
    }

    /**
     * Build the network.
     *
     * @param nInputs number of input nodes
     * @param nOutputs number of output nodes
     */
    private void buildNetwork(final int nInputs, final int nOutputs) {

        inputLayer = new StandardNetwork(this.getRootNetwork());
        outputLayer = new StandardNetwork(this.getRootNetwork());
        inputLayer.setParentNetwork(this);
        outputLayer.setParentNetwork(this);

        for (int i = 0; i < nInputs; i++) {
            inputLayer.addNeuron(new ClampedNeuron());
        }

        for (int i = 0; i < nOutputs; i++) {
            outputLayer.addNeuron(getDefaultNeuron());
        }

        addNetwork(inputLayer);
        addNetwork(outputLayer);
    }

    /**
     * Connect layers and set weights.
     */
    private void makeConnections() {

        AllToAll connector = new AllToAll(this, inputLayer.getFlatNeuronList(), outputLayer.getFlatNeuronList());
        connector.connectNeurons();

        for (int i = 0; i < getFlatSynapseList().size(); i++) {
            ((Synapse) getFlatSynapseList().get(i)).setUpperBound(10);
            ((Synapse) getFlatSynapseList().get(i)).setLowerBound(-10);
            ((Synapse) getFlatSynapseList().get(i)).randomize();
        }

        for (int i = 0; i < getFlatNeuronList().size(); i++) {
            ((Neuron) getFlatNeuronList().get(i)).setUpperBound(1);
            ((Neuron) getFlatNeuronList().get(i)).setLowerBound(-1);
            ((Neuron) getFlatNeuronList().get(i)).setIncrement(1);
        }

    }

    /**
     * Return the default neuron, with settings, for LMS nets.
     *
     * @return the neuron, with appropriate settings, that should be used in
     *         building a LMS net.
     */
    protected LinearNeuron getDefaultNeuron() {
        LinearNeuron ret = new LinearNeuron();
        ret.setLowerBound(-10);
        ret.setUpperBound(10);
        return ret;
    }

    /**
     * The core update function of the neural network. Calls the current update function on each neuron, decays all the
     * neurons, and checks their bounds.
     */
    public void update() {
        updateAllNetworks();
        // Perhaps this should move explicitly from input to output layers?
        checkAllBounds();
    }

    /**
     * Train the network.
     */
    public void train() {
        for (int i = 0; i < epochs; i++) {
            iterate();
        }
    }

    /**
     * Iterate network training.
     */
    public void iterate() {
        rmsError = 0;
        for (int row = 0; row < trainingInputs.length; row++) {
            for (int i = 0; i < trainingInputs[row].length; i++) {
                inputLayer.getNeuron(i).setActivation(trainingInputs[row][i]);
            }
            outputLayer.update();
            for (int i = 0; i < trainingInputs[row].length; i++) {
                for (int j = 0; j < trainingOutputs[row].length; j++) {
                    Synapse s = inputLayer.getWeight(i, j);
                    double error  = trainingOutputs[row][j] - outputLayer.getNeuron(j).getActivation();
                    rmsError += (error * error);
                    s.setStrength(s.getStrength() + (eta * error *  trainingInputs[row][i]));
                }
            }
        }
        inputLayer.clearActivations();
        outputLayer.clearActivations();
        rmsError = Math.sqrt(rmsError / (trainingInputs[0].length * trainingOutputs[0].length));
    }

    /**
     * @return RMS Error
     */
    public double getRMSError() {
        return rmsError;
    }

    /**
     * @return Returns the epochs.
     */
    public int getEpochs() {
        return epochs;
    }

    /**
     * @param epochs The epochs to set.
     */
    public void setEpochs(final int epochs) {
        this.epochs = epochs;
    }

    /**
     * @return Returns the eta.
     */
    public double getEta() {
        return eta;
    }

    /**
     * @param eta The eta to set.
     */
    public void setEta(final double eta) {
        this.eta = eta;
    }

    /**
     * @return Returns the training_inputs.
     */
    public double[][] getTrainingInputs() {
        return trainingInputs;
    }

    /**
     * @param trainingInputs The trainingInputs to set.
     */
    public void setTrainingInputs(final double[][] trainingInputs) {
        this.trainingInputs = trainingInputs;
    }

    /**
     * @return Returns the trainingOutputs.
     */
    public double[][] getTrainingOutputs() {
        return trainingOutputs;
    }

    /**
     * @param trainingOutputs The trainingOutputs to set.
     */
    public void setTrainingOutputs(final double[][] trainingOutputs) {
        this.trainingOutputs = trainingOutputs;
    }

    /**
     * @return Returns the defaultInputs.
     */
    public int getDefaultInputs() {
        return defaultInputs;
    }

    /**
     * @return Returns the defaultOutputs.
     */
    public int getDefaultOutputs() {
        return defaultOutputs;
    }

    /**
     * @return Returns the input training file.
     */
    public File getTrainingINFile() {
        return trainingINFile;
    }

    /**
     * Sets the input training file.
     *
     * @param trainingINFile File to set input training
     */
    public void setTrainingINFile(final File trainingINFile) {
        this.trainingINFile = trainingINFile;
    }

    /**
     * @return Returns the output training file.
     */
    public File getTrainingOUTFile() {
        return trainingOUTFile;
    }

    /**
     * Sets the output training file.
     *
     * @param trainingOUTFile File to set output training
     */
    public void setTrainingOUTFile(final File trainingOUTFile) {
        this.trainingOUTFile = trainingOUTFile;
    }

    @Override
    public Network duplicate() {
        LMSNetwork net = new LMSNetwork();
        net = (LMSNetwork) super.duplicate(net);
        return net;
    }
}
