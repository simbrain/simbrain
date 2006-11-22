package org.simnet.connections;

import java.util.ArrayList;

import org.simnet.interfaces.Network;

public abstract class ConnectNeurons {

    Network network;
    ArrayList sourceNeurons;
    ArrayList targetNeurons;
    
    
    public ConnectNeurons(Network network, ArrayList neurons, ArrayList neurons2) {
        this.network = network;
        sourceNeurons = neurons;
        targetNeurons = neurons2;
    }
    
    
    public abstract void connectNeurons();

}
