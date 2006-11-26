package org.simnet.connections;

import java.util.ArrayList;
import java.util.Iterator;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.synapses.ClampedSynapse;

/**
 * Connect every source neuron to every target neuron.
 *
 * @author jyoshimi
 */
public class AllToAll extends ConnectNeurons {

    public AllToAll(final Network network, final ArrayList neurons, final ArrayList neurons2) {
        super(network, neurons, neurons2);
    }

    /** {@inheritDoc} */
    public void connectNeurons() {
        for (Iterator i = sourceNeurons.iterator(); i.hasNext(); ) {
            Neuron source = (Neuron) i.next();
            for (Iterator j = targetNeurons.iterator(); j.hasNext(); ) {
                Neuron target = (Neuron) j.next();
                network.addWeight(new ClampedSynapse(source, target));
            }
        }
    }
}
