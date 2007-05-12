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

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.neurons.LinearNeuron;
import org.simnet.util.ConnectNets;

import edu.wlu.cs.levy.SNARLI.BPLayer;


/**
 * <b>Elman</b> networks are simple recurrent networks based on the work of Jeff Elman.
 */
public class Elman extends Backprop {

    /** Copy of hidden units. */
    private BPLayer copy;

    /** Number of input neurons. */
    private int nInput = 3;

    /** Number of output neurons. */
    private int nHidden = 1;

    /**
     * Default constructor.
     */
    public Elman() {
        super();
    }

    /**
     * Build network and initialize nodes and weights
     * to appropriate values.
     */
    public void defaultInit() {
        buildInitialNetwork();

        for (int i = 0; i < getFlatSynapseList().size(); i++) {
            ((Synapse) getFlatSynapseList().get(i)).setUpperBound(10);
            ((Synapse) getFlatSynapseList().get(i)).setLowerBound(-10);
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
        super.buildInitialNetwork();
        StandardNetwork copyLayer = new StandardNetwork();
        this.setMu(0);

        // Use Linear neurons for the copy layer
        for (int i = 0; i < super.getNHidden(); i++) {
            copyLayer.addNeuron(new LinearNeuron());
        }
        addNetwork(copyLayer);
        ConnectNets.oneWayOneOne(this, this.getNetwork(1), copyLayer);
        ConnectNets.oneWayFull(this, copyLayer, this.getNetwork(1));
        copyLayer.setDelays(1);
        buildSnarliNetwork();
    }

    /**
     * Create the Snarli network.  The Simbrain network is used .
     * to set all weights and biases.
     */
    public void buildSnarliNetwork() {
        super.buildSnarliNetwork();
        copy = new BPLayer(getNetwork(3).getNeuronCount());

        //Make 1-1 delayed connections
        try {
            copy.delay(this.getHid());
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
        //Connect the copy layer fully to the hidden layer
        this.getHid().connect(copy);
        this.getHid().setWeights(copy, ConnectNets.getWeights(getNetwork(3), getNetwork(1)));
    }

    /**
     * Train Elman network.
     */
    public void train() {
        attachInputsAndOutputs();
        batchTrain();
        updateSimbrainNetwork();
    }

    /**
     * Iterate Elman network learning.
     */
    public void iterate() {
        attachInputsAndOutputs();
        batchIterate();
        updateSimbrainNetwork();
    }

    /**
     * Batch train for one iteration.
     */
    public void batchIterate() {
        this.getOut().getRMSError();
        this.getOut().online(1, getEta(), 0, getErrorInterval());
    }

    /**
     * Update Simbrain network to match SNARLI network after it is trained.
     */
    public void updateSimbrainNetwork() {
        super.updateSimbrainNetwork();
        ConnectNets.setConnections(getNetwork(3), getNetwork(1), getHid().getWeights(copy));
    }


    /**
     * Randomize Elman network (all but weights from hidden to copy).
     */
    public void randomize() {
        super.randomize();
        if (copy == null) {
            buildSnarliNetwork();
        }
        copy.randomize();
        ConnectNets.setConnections(getNetwork(3), getNetwork(1), getHid().getWeights(copy));
    }

    /**
     * Override update function to update input and copy layer first.
     */
    public void update() {
        getNetwork(0).update();
        getNetwork(3).update();
        getNetwork(1).update();
        getNetwork(2).update();
        checkAllBounds();
    }

    /**
     * @return Number of input neurons.
     */
    public int getNInput() {
        return nInput;
    }

    /**
     * @return Number of hidden neurons.
     */
    public int getNHidden() {
        return nHidden;
    }

    /** @Override. */
    public Network duplicate() {
        Elman net = new Elman();
        net = (Elman) super.duplicate(net);
        return net;
    }
}
