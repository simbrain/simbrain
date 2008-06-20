package org.simbrain.network.connections;

import java.util.ArrayList;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;

/**
 * Subclass this class to create a class which manages the creation of connections
 * between groups of neurons.
 *
 * @author jyoshimi
 */
public abstract class ConnectNeurons {

    /** The network whose neurons are to be connected. */
    protected Network network;
    /** The source group of neurons, generally from which connections will be made. */
    protected ArrayList<Neuron> sourceNeurons;
    /** The garget group of neurons, generally to which connections will be made. */
    protected ArrayList<Neuron> targetNeurons;

    /**
     * Default constructor.
     *
     * @param network network to receive  connections
     * @param neurons source neurons
     * @param neurons2 target neurons
     */
    public ConnectNeurons(final Network network, final ArrayList neurons, final ArrayList neurons2) {
        this.network = network;
        sourceNeurons = neurons;
        targetNeurons = neurons2;
    }

    /**
     * Connect the source to the target neurons using some method.
     */
    public abstract void connectNeurons();

}
