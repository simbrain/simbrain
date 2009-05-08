package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.Iterator;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Connect every source neuron to every target neuron.
 *
 * @author jyoshimi
 */
public class AllToAll extends ConnectNeurons {

    /**
     * The synapse to be used as a basis for the connection. Default to a
     * clamped synapse.
     */
    private Synapse baseSynapse = new ClampedSynapse(null, null);

    /** Allows neurons to have a self connection. */
    public static boolean allowSelfConnection = true;

    public AllToAll(final Network network, final ArrayList neurons, final ArrayList neurons2) {
        super(network, neurons, neurons2);
    }

    /** {@inheritDoc} */
    public AllToAll() {
    }

    @Override
    public String toString() {
        return "All to all";
    }

    
    /** {@inheritDoc} */
    public void connectNeurons() {
        for (Neuron source : sourceNeurons) {
            for (Neuron target : targetNeurons) {
                if (!allowSelfConnection) {
                    if (source != target) {
                        Synapse synapse = baseSynapse.duplicate();
                        synapse.setSource(source);
                        synapse.setTarget(target);
                        network.addSynapse(synapse);
                    }
                } else {
                    Synapse synapse = baseSynapse.duplicate();
                    synapse.setSource(source);
                    synapse.setTarget(target);
                    network.addSynapse(synapse);
                }
            }
        }
    }

    /**
     * @return the baseSynapse
     */
    public Synapse getBaseSynapse() {
        return baseSynapse;
    }

    /**
     * @param baseSynapse the baseSynapse to set
     */
    public void setBaseSynapse(final Synapse baseSynapse) {
        this.baseSynapse = baseSynapse;
    }
}
