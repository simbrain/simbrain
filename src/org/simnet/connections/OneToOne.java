package org.simnet.connections;

import java.util.ArrayList;
import java.util.Iterator;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Network;
import org.simnet.synapses.ClampedSynapse;

/**
 * Connect each source neuron to a single target.
 *
 * @author jyoshimi
 *
 */
public class OneToOne extends ConnectNeurons {

    /**
     * See super class description.
     *
     * @param network network with neurons to be connected.
     * @param neurons source neurons.
     * @param neurons2 target neurons.
     */
    public OneToOne(final Network network, final ArrayList neurons, final ArrayList neurons2) {
        super(network, neurons, neurons2);
    }

    /** @inheritDoc */
    public void connectNeurons() {
        Iterator targets = targetNeurons.iterator();
        for (Iterator sources = sourceNeurons.iterator(); sources.hasNext(); ) {
            Neuron source = (Neuron) sources.next();
            if (targets.hasNext()) {
                Neuron target = (Neuron) targets.next();
                network.addWeight(new ClampedSynapse(source, target));                    
            }
        }
    }
}
