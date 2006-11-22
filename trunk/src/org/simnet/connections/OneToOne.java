package org.simnet.connections;

import java.util.ArrayList;
import java.util.Iterator;

import org.simbrain.network.nodes.NeuronNode;
import org.simnet.interfaces.Network;
import org.simnet.synapses.ClampedSynapse;

public class OneToOne extends ConnectNeurons {
        
    public OneToOne(Network network, ArrayList neurons, ArrayList neurons2) {
        super(network, neurons, neurons2);
    }

    public void connectNeurons() {
        Iterator targets = targetNeurons.iterator();
        for (Iterator sources = sourceNeurons.iterator(); sources.hasNext(); ) {
            NeuronNode source = (NeuronNode) sources.next();
            if (targets.hasNext()) {
                NeuronNode target = (NeuronNode) targets.next();
                network.addWeight(new ClampedSynapse(source.getNeuron(), target.getNeuron()));                    
            }
        }
    }
}
