package org.simnet.connections;

import java.util.ArrayList;
import java.util.Iterator;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.synapses.ClampedSynapse;

/**
 * Connect neurons sparsely with some probabilities.
 *
 * @author jyoshimi
 *
 */
public class Sparse extends ConnectNeurons {
    
    private double excitatoryProbability = .8; 
    private double inhibitoryProbability = .5; 
    
    /**
     * See super class description.
     *
     * @param network network with neurons to be connected.
     * @param neurons source neurons.
     * @param neurons2 target neurons.
     */
    public Sparse(final Network network, final ArrayList neurons, final ArrayList neurons2) {
        super(network, neurons, neurons2);
    }

    /** @inheritDoc */
    public void connectNeurons() {
        for (Iterator i = sourceNeurons.iterator(); i.hasNext(); ) {
            Neuron source = (Neuron) i.next();
            for (Iterator j = targetNeurons.iterator(); j.hasNext(); ) {
                Neuron target = (Neuron) j.next();
                if (Math.random() < excitatoryProbability) {
                    network.addWeight(new ClampedSynapse(source, target));                    
                }
                if (Math.random() < inhibitoryProbability) {
                    ClampedSynapse inhibitory = new ClampedSynapse(source, target);
                    inhibitory.setStrength(-1);
                    network.addWeight(inhibitory);      
                }                
            }
        }
    }
    
}
