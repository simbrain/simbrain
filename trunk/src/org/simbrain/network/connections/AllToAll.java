package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.Iterator;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Connect every source neuron to every target neuron.
 *
 * @author jyoshimi
 */
public class AllToAll extends ConnectNeurons {

    /** Allows neurons to have a self connection. */
    private boolean allowSelfConnection = true;

    public AllToAll(final Network network, final ArrayList neurons, final ArrayList neurons2) {
        super(network, neurons, neurons2);
    }

    /** {@inheritDoc} */
    public void connectNeurons() {
        for (Neuron source : sourceNeurons) {
            for (Neuron target : targetNeurons) {
                if (!allowSelfConnection) {
                    if (source != target) {
                    network.addSynapse(new ClampedSynapse(source, target));
                    }
                } else {
                    network.addSynapse(new ClampedSynapse(source, target));
                }
            }
        }
    }
}
