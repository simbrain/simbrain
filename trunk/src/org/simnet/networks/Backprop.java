/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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

import org.simnet.interfaces.ComplexNetwork;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.layouts.Layout;
import org.simnet.neurons.LinearNeuron;
import org.simnet.neurons.SigmoidalNeuron;
import org.simnet.util.ConnectNets;

import edu.wlu.cs.levy.SNARLI.BPLayer;

/**
 * <b>Backprop</b> implements a standard three layer backpropagation network.
 */
public class Backprop extends ComplexNetwork {

    /** number of input units. */
    private int nInputs = 3;
    /** number of hidden units. */
    private int nHidden = 4;
    /** number of output units. */
    private int nOutputs = 3;
    /** Number of epochs. */
    private int epochs = 1000;
    /** Current error. */
    private double error;
    /** Learning rate. */
    private double eta = .5;
    /** Momentum. */
    private double mu = .1;
    /** How often to update error. */
    private int errorInterval = 100;
    /** Input portion of training corpus. */
    private double[][] trainingInputs;
    /** Output portion of training corpus. */
    private double[][] trainingOutputs;
    /** Input Layer of SNARLI network. */
    private BPLayer inp;
    /** Output Layer of SNARLI network. */
    private BPLayer hid;
    /** Hidden Layer of SNARLI network. */
    private BPLayer out;

    /**
     * Default constructor.
     *
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
     */
    public Backprop(final int inputs, final int hidden, final int outputs, final Layout layout) {
        super();
        nInputs = inputs;
        nHidden = hidden;
        nOutputs = outputs;
        defaultInit();
        layout.layoutNeurons(this);
    }

    /**
     * Build network and initialize nodes and weights
     * to appropriate values.
     */
    public void defaultInit() {
        buildInitialNetwork();
        buildSnarliNetwork();

        for (int i = 0; i < getFlatSynapseList().size(); i++) {
            ((Synapse) getFlatSynapseList().get(i)).setUpperBound(100);
            ((Synapse) getFlatSynapseList().get(i)).setLowerBound(-100);
        }

        for (int i = 0; i < getFlatNeuronList().size(); i++) {
            ((Neuron) getFlatNeuronList().get(i)).setUpperBound(1);
            ((Neuron) getFlatNeuronList().get(i)).setLowerBound(-1);
            ((Neuron) getFlatNeuronList().get(i)).setIncrement(1);
        }
    }

    /**
     *  Build the default network.
     */
    protected void buildInitialNetwork() {
        StandardNetwork inputLayer = new StandardNetwork();
        StandardNetwork hiddenLayer = new StandardNetwork();
        StandardNetwork outputLayer = new StandardNetwork();

        for (int i = 0; i < nInputs; i++) {
            inputLayer.addNeuron(new LinearNeuron());
        }

        for (int i = 0; i < nHidden; i++) {
            hiddenLayer.addNeuron(getDefaultNeuron());
        }

        for (int i = 0; i < nOutputs; i++) {
            outputLayer.addNeuron(getDefaultNeuron());
        }

        addNetwork(inputLayer);
        addNetwork(hiddenLayer);
        addNetwork(outputLayer);

        ConnectNets.oneWayFull(this, inputLayer, hiddenLayer);
        ConnectNets.oneWayFull(this, hiddenLayer, outputLayer);
    }

    /**
     * Create the Snarli network.
     */
    public void buildSnarliNetwork() {
        inp = new BPLayer(getNetwork(0).getNeuronCount());
        hid = new BPLayer(getNetwork(1).getNeuronCount());
        out = new BPLayer(getNetwork(2).getNeuronCount());
        hid.connect(inp);
        out.connect(hid);
        hid.setWeights(inp, ConnectNets.getWeights(getNetwork(0), getNetwork(1)));
        hid.setBias(getBiases((StandardNetwork) getNetwork(1)));
        out.setWeights(hid, ConnectNets.getWeights(getNetwork(1), getNetwork(2)));
        out.setBias(getBiases((StandardNetwork) getNetwork(2)));
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
        time++;
        updateAllNetworks();
        // Perhaps this should move explicitly from input to output layers?
        checkAllBounds();
    }

    /**
     * Train the network.
     */
    public void train() {
        //buildSnarliNetwork();
        attachInputsAndOutputs();
        batchTrain();
        updateSimbrainNetwork();
    }

    /**
     * Iterate network training.
     */
    public void iterate() {
        //buildSnarliNetwork();
        attachInputsAndOutputs();
        batchIterate();
        updateSimbrainNetwork();
    }


    /**
     * Attach training files to SNARLI network.
     */
    public void attachInputsAndOutputs() {

        if ((trainingInputs == null) || (trainingOutputs == null)) {
            return;
        }

        inp.attach(trainingInputs);
        out.attach(trainingOutputs);
    }

    /**
     * Update connections and biases of simbrain network.
     */
    public void updateSimbrainNetwork() {
        ConnectNets.setConnections(getNetwork(0), hid.getWeights(inp));
        ConnectNets.setConnections(getNetwork(1), out.getWeights(hid));
        setBiases((StandardNetwork) getNetwork(1), hid.getBias());
        setBiases((StandardNetwork) getNetwork(2), out.getBias());
    }

    /**
     * Forwards to Snarli <code>batchTrain()</code> method.
     */
    public void batchTrain() {
        out.batch(epochs, eta, mu, errorInterval);
    }

    /**
     * Batch train for one iteration.
     */
    public void batchIterate() {
        out.getRMSError();
        out.batch(1, eta, mu, errorInterval);
    }

    /**
     * Randomize the network.
     */
    public void randomize() {
        if ((hid == null) || (out == null)) {
            buildSnarliNetwork();
        }

        if (this.getNetworkList().size() == 0) {
            return;
        }

        hid.randomize();
        out.randomize();
        ConnectNets.setConnections(getNetwork(0), hid.getWeights(inp));
        ConnectNets.setConnections(getNetwork(1), out.getWeights(hid));
        setBiases((StandardNetwork) getNetwork(1), hid.getBias());
        setBiases((StandardNetwork) getNetwork(2), out.getBias());
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
     * @return Returns the error_interval.
     */
    public int getErrorInterval() {
        return errorInterval;
    }

    /**
     * @param errorInterval The errorInterval to set.
     */
    public void setErrorInterval(final int errorInterval) {
        this.errorInterval = errorInterval;
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
     * @return Returns the hid.
     */
    public BPLayer getHid() {
        return hid;
    }

    /**
     * @param hid The hid to set.
     */
    public void setHid(final BPLayer hid) {
        this.hid = hid;
    }

    /**
     * @return Returns the inp.
     */
    public BPLayer getInp() {
        return inp;
    }

    /**
     * @param inp The inp to set.
     */
    public void setInp(final BPLayer inp) {
        this.inp = inp;
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
     * @return Returns the out.
     */
    public BPLayer getOut() {
        return out;
    }

    /**
     * @param out The out to set.
     */
    public void setOut(final BPLayer out) {
        this.out = out;
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
}
