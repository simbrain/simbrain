package org.simnet.connections;

import java.util.ArrayList;
import java.util.Iterator;

import org.simbrain.network.nodes.NeuronNode;
import org.simnet.interfaces.Network;
import org.simnet.synapses.ClampedSynapse;

public class Sparse extends ConnectNeurons {
    
    private double connectionProbability = .9; 
    
    public Sparse(Network network, ArrayList neurons, ArrayList neurons2) {
        super(network, neurons, neurons2);
    }

    public void connectNeurons() {
        for (Iterator i = sourceNeurons.iterator(); i.hasNext(); ) {
            NeuronNode source = (NeuronNode) i.next();
            for (Iterator j = targetNeurons.iterator(); j.hasNext(); ) {
                NeuronNode target = (NeuronNode) j.next();
                if (Math.random() > connectionProbability) {
                    network.addWeight(new ClampedSynapse(source.getNeuron(), target.getNeuron()));                    
                }
            }
        }
    }
}
