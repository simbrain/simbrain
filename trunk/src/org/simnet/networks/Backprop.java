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
package org.simnet.networks;

import org.simnet.connections.AllToAll;
import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.RootNetwork;
import org.simnet.interfaces.Synapse;
import org.simnet.layouts.Layout;
import org.simnet.neurons.LinearNeuron;
import org.simnet.neurons.SigmoidalNeuron;
import org.simnet.synapses.ClampedSynapse;

/**
 * <b>Backprop</b> implements a standard three layer backpropagation network.
 * This will either be replaced or supplemented by a native simnet implementation.
 */
public class Backprop extends Network {

    /** Number of input units. */
    private int nInputs = 3;

    /** Number of hidden units. */
    private int nHidden = 4;

    /** Number of output units. */
    private int nOutputs = 3;

    /** Number of epochs. */
    private int epochs = 1000;

    /** Flag indicating whether the network should be trained or not. */
    private boolean train = true;

    /** Current error. */
    private double error;

    /** Learning rate. */
    private double eta = .5;

    /** Bias learning rate. */
    private double biasEta = 0;

    /** Momentum. */
    private double mu = .9;

    /** Number of iterations since last weight update. */
    private int lastUpdateIter = 0;

    /** Simbrain representation of input layer. */
    private StandardNetwork inputLayer;

    /** Simmbrain representation of hidden layer. */
    private StandardNetwork hiddenLayer;

    /** Simbrain representation of output layer. */
    private StandardNetwork outputLayer;

    /** last weight change for momentum. */
    double [][] last_delW_out = null;
    double [][] last_delW_hid = null;
    double [] last_delB_out = null;
    double [] last_delB_hid = null;

    /**
     * Default constructor.
     */
    public Backprop() {
        super();
    }

    /**
     * Construct a backprop network with a specified number of input, hidden, and output layers.
     *
     * @param inputs Number of neurons to be inputs
     * @param hidden Number of neurons to be hidden
     * @param outputs Number of neurons to be outputs
     * @param layout Defines how the neurons are layed out
     * @param root reference to RootNetwork.
     */
    public Backprop(final RootNetwork root, final int inputs, final int hidden,
            final int outputs, final Layout layout) {
        super();
        this.setRootNetwork(root);
        this.setParentNetwork(root);
        nInputs = inputs;
        nHidden = hidden;
        nOutputs = outputs;
        createNeurons();
        initVariables();
        layout.layoutNeurons(this);
        makeConnections();
    }

    /**
     * Initiate variables.
     *
     */
    private void initVariables() {

        nInputs = inputLayer.getNeuronCount();
        nHidden = hiddenLayer.getNeuronCount();
        nOutputs = outputLayer.getNeuronCount();

        last_delW_hid = new double[nInputs][nHidden];
        last_delW_out = new double[nHidden][nOutputs];
        last_delB_hid = new double[nHidden];
        last_delB_out = new double[nOutputs];

        for (int i = 0; i < nInputs; i++) {
            for (int j = 0; j < nHidden; j++) {
            last_delW_hid[i][j] = 0;
            }
        }
        for (int i = 0; i < nHidden; i++) {
            for (int j = 0; j < nOutputs; j++) {
            last_delW_out[i][j] = 0;
            }
        }
        for (int i = 0; i < nHidden; i++) {
            last_delB_hid[i] = 0;
        }

        for (int i = 0; i < nOutputs; i++) {
            last_delB_out[i] = 0;
        }

    }

    /**
     * Intialize post unmarshalling.
     */
    protected void postUnmarshallingInit() {
        super.postUnmarshallingInit();
    }

    /**
     * Create neurons.
     */
    protected void createNeurons() {
        inputLayer = new StandardNetwork(this.getRootNetwork());
        hiddenLayer = new StandardNetwork(this.getRootNetwork());
        outputLayer = new StandardNetwork(this.getRootNetwork());
        inputLayer.setParentNetwork(this);
        hiddenLayer.setParentNetwork(this);
        outputLayer.setParentNetwork(this);
        for (int i = 0; i < nInputs; i++) {
            // Using a LinearNeuron so that it could read the activations from
            // the worlds (like the data world)
            inputLayer.addNeuron(new LinearNeuron());
        }

        for (int i = 0; i < nHidden; i++) {
            hiddenLayer.addNeuron(getDefaultNeuron());
        }

        for (int i = 0; i < nOutputs; i++) {
            outputLayer.addNeuron(getDefaultNeuron());
        }

        addNetworkReference(inputLayer);
        addNetworkReference(hiddenLayer);
        addNetworkReference(outputLayer);

    }

    /**
     * Create connections.
     */
    private void makeConnections() {
        AllToAll connector = new AllToAll(this, inputLayer.getFlatNeuronList(), hiddenLayer.getFlatNeuronList());
        connector.connectNeurons();
        AllToAll connector2 = new AllToAll(this, hiddenLayer.getFlatNeuronList(), outputLayer.getFlatNeuronList());
        connector2.connectNeurons();

        for (int i = 0; i < getFlatSynapseList().size(); i++) {
            ((Synapse) getFlatSynapseList().get(i)).setUpperBound(4);
            ((Synapse) getFlatSynapseList().get(i)).setLowerBound(-4);
            ((ClampedSynapse) getFlatSynapseList().get(i)).setClipped(true);
        }

        for (int i = 0; i < getFlatNeuronList().size(); i++) {
            ((Neuron) getFlatNeuronList().get(i)).setUpperBound(1);
            ((Neuron) getFlatNeuronList().get(i)).setLowerBound(0);
            ((Neuron) getFlatNeuronList().get(i)).setIncrement(1);
        }

    }

    /**
     * Return the default neuron, with settings, for backprop nets.
     *
     * @return the neuron, with appropriate settings, that should be used in
     *         building a backprop net.
     */
    protected SigmoidalNeuron getDefaultNeuron() {
        SigmoidalNeuron ret = new SigmoidalNeuron();
        ret.setLowerBound(0);
        ret.setUpperBound(1);
        return ret;
    }

    /**
     * The core update function of the neural network. Calls the current update function on each neuron, decays all the
     * neurons, and checks their bounds.
     */
    public void update() {

        // update the input layer activation
        for (int i = 0; i < inputLayer.getNeuronCount(); i++) {
            inputLayer.getNeuron(i).update();
            inputLayer.getNeuron(i).setActivation(inputLayer.getNeuron(i).getBuffer());
        }
        // update the hidden layer activation
        for (int i = 0; i < hiddenLayer.getNeuronCount(); i++) {
            hiddenLayer.getNeuron(i).update();
            hiddenLayer.getNeuron(i).setActivation(hiddenLayer.getNeuron(i).getBuffer());
        }
        // update the output layer activation
        for (int i = 0; i < outputLayer.getNeuronCount(); i++) {
            outputLayer.getNeuron(i).update();
            outputLayer.getNeuron(i).setActivation(outputLayer.getNeuron(i).getBuffer());
        }

    // update the weights
    if (this.train) {
        updateWeights();
    }
    }

    /**
     * update the weights.
     *
     */
    private void updateWeights() {

    if (last_delW_hid == null) {
        initVariables();
    }

    double [] delta_out = new double[nOutputs];
    double [] delta_hidden = new double[nHidden];
    double delW;

    // compute delta for the output layer
    for (int i = 0; i < nOutputs; i++) {
        delta_out[i] = (outputLayer.getNeuron(i).getTargetValue() - outputLayer.getNeuron(i).getActivation())
        * (1 - outputLayer.getNeuron(i).getActivation()) * outputLayer.getNeuron(i).getActivation();
    }
    // compute delta for the hidden layer
    for (int h = 0; h < nHidden; h++) {
        delta_hidden[h] = 0;
        for (int o = 0; o < nOutputs; o++) {
            delta_hidden[h] += delta_out[o] * this.getSynapse(this.hiddenLayer.getNeuron(h),
                    this.outputLayer.getNeuron(o)).getStrength();
        }
        delta_hidden[h] *= this.hiddenLayer.getNeuron(h).getActivation()
        * (1 - this.hiddenLayer.getNeuron(h).getActivation());
    }

    // update the weights from the hidden layer to the output layer
    for (int h = 0; h < nHidden; h++) {
        for (int o = 0; o < nOutputs; o++) {
        delW = this.eta * delta_out[o] * hiddenLayer.getNeuron(h).getActivation() + this.mu * last_delW_out[h][o];
        last_delW_out[h][o] = delW;
        this.getSynapse(this.hiddenLayer.getNeuron(h), this.outputLayer.getNeuron(o)).setStrength(
            this.getSynapse(this.hiddenLayer.getNeuron(h), this.outputLayer.getNeuron(o)).getStrength() +
            delW);
        }
    }
    // update the output layer bias weights
    for (int o = 0; o < nOutputs; o++) {
        delW = this.biasEta * delta_out[o] + this.mu * last_delB_out[o];
        last_delB_out[o] = delW;
        ((SigmoidalNeuron) this.outputLayer.getNeuron(o)).setBias(((SigmoidalNeuron)
                this.outputLayer.getNeuron(o)).getBias() + delW);
    }

    // update the weights from the input layer to the hidden layer
    for (int i = 0; i < nInputs; i++) {
        for (int h = 0; h < nHidden; h++) {
        delW = this.eta * delta_hidden[h] * inputLayer.getNeuron(i).getActivation() + this.mu * last_delW_hid[i][h];
        last_delW_hid[i][h] = delW;
        this.getSynapse(this.inputLayer.getNeuron(i), this.hiddenLayer.getNeuron(h)).setStrength(
            this.getSynapse(this.inputLayer.getNeuron(i), this.hiddenLayer.getNeuron(h)).getStrength() +
            delW);
        }
    }
    // update the hidden layer bias weights
    for (int h = 0; h < nHidden; h++) {
        delW = this.biasEta * delta_hidden[h] + this.mu * last_delB_hid[h];
        last_delB_hid[h] = delW;
        ((SigmoidalNeuron) this.hiddenLayer.getNeuron(h)).setBias(((SigmoidalNeuron)
                this.hiddenLayer.getNeuron(h)).getBias() + delW);
    }
    }

    /**
     * Randomize the network.
     */
    public void randomize() {
        if (this.getNetworkList().size() == 0) {
            return;
        }

        for (int i = 0; i < getFlatSynapseList().size(); i++) {
            ((Synapse) getFlatSynapseList().get(i)).randomize();
        }
    }

    /**
     * @return Returns the error.
     */
    public double getError() {
        return error;
    }

    /**
     * @param error The error to set.
     */
    public void setError(final double error) {
        this.error = error;
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
     * @return Returns the mu.
     */
    public double getMu() {
        return mu;
    }

    /**
     * @param mu The mu to set.
     */
    public void setMu(final double mu) {
        this.mu = mu;
    }

    /**
     * @return Returns the n_hidden.
     */
    public int getNHidden() {
        return nHidden;
    }

    /**
     * @param nHidden The nHidden to set.
     */
    public void setNHidden(final int nHidden) {
        this.nHidden = nHidden;
    }

    /**
     * @return Returns the n_inputs.
     */
    public int getNInputs() {
        return nInputs;
    }

    /**
     * @param nInputs The nInputs to set.
     */
    public void setNInputs(final int nInputs) {
        this.nInputs = nInputs;
    }

    /**
     * @return Returns the n_outputs.
     */
    public int getNOutputs() {
        return nOutputs;
    }

    /**
     * @param nOutputs The nOutputs to set.
     */
    public void setNOutputs(final int nOutputs) {
        this.nOutputs = nOutputs;
    }

    /**
     * Returns bias values.
     * @param net Network
     * @return Neuron biases
     */
    public double[] getBiases(final StandardNetwork net) {
        double[] ret = new double[net.getNeuronCount()];

        for (int i = 0; i < net.getNeuronCount(); i++) {
            ret[i] = ((SigmoidalNeuron) net.getNeuron(i)).getBias();
        }

        return ret;
    }

    /**
     * Set bias values for all neurons in this network.
     *
     * @param biases Array of new bias values
     * @param net Network to get biases
     */
    public void setBiases(final StandardNetwork net, final double[] biases) {
        if (biases.length != net.getNeuronCount()) {
            System.out.println("Invalid argument to setBiases");

            return;
        }

        for (int i = 0; i < net.getNeuronCount(); i++) {
            ((SigmoidalNeuron) net.getNeuron(i)).setBias(biases[i]);
        }
    }

    /** @Override. */
    public Network duplicate() {
        Backprop bp = new Backprop();
        bp = (Backprop) super.duplicate(bp);
        bp.setNInputs(nInputs);
        bp.setNHidden(nHidden);
        bp.setNOutputs(nOutputs);
        bp.setEta(this.getEta());
        bp.setBiasEta(this.getBiasEta());
        bp.setMu(this.getMu());
        return bp;
    }

    /** @return train true if the network weights should be updated, false otherwise */
    public boolean getTrain() {
        return train;
    }

    /** @param train to set */
    public void setTrain(final boolean train) {
        this.train = train;
    }


    /** @return biasEta bias learning rate */
    public double getBiasEta() {
        return biasEta;
    }

    /** @param biasEta bias learning rate to set */
    public void setBiasEta(final double biasEta) {
        this.biasEta = biasEta;
    }

    /**
     * @return the hiddenLayer
     */
    public StandardNetwork getHiddenLayer() {
        return hiddenLayer;
    }

    /**
     * @param hiddenLayer the hiddenLayer to set
     */
    public void setHiddenLayer(final StandardNetwork hiddenLayer) {
        this.hiddenLayer = hiddenLayer;
    }

    /**
     * @return the inputLayer
     */
    public StandardNetwork getInputLayer() {
        return inputLayer;
    }

    /**
     * @param inputLayer the inputLayer to set
     */
    public void setInputLayer(final StandardNetwork inputLayer) {
        this.inputLayer = inputLayer;
    }

    /**
     * @return the outputLayer
     */
    public StandardNetwork getOutputLayer() {
        return outputLayer;
    }

    /**
     * @param outputLayer the outputLayer to set
     */
    public void setOutputLayer(final StandardNetwork outputLayer) {
        this.outputLayer = outputLayer;
    }

}
