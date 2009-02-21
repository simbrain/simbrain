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
    /** The target group of neurons, generally to which connections will be made. */
    protected ArrayList<Neuron> targetNeurons;
    /**
     * Holds "current" connection object.  Used in Gui so that users can set a current type
     * (see parameter-free constructor below)then simply apply it.
     */
    public static ConnectNeurons connectionType = new AllToAll();
    
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
     * This parameter-free constructor is used in the desktop.  User:
     *  - Picks a connection style in the GUI
     *  - Selects source and target neurons
     *  - Invokes connection.
     */
    public ConnectNeurons() {
    }

    /**
     * Apply connection using specified parameters.
     * 
     * @param network
     * @param neurons
     * @param neurons2
     */
    public void connectNeurons(final Network network, final ArrayList neurons, final ArrayList neurons2) {
        this.network = network;
        sourceNeurons = neurons;
        targetNeurons = neurons2;
        connectNeurons();
    }

    /**
     * Connect the source to the target neurons using some method.
     */
    public abstract void connectNeurons();
    
}
