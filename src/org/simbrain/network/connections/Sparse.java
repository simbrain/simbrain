package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.Iterator;

import org.simbrain.network.NetworkPreferences;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Connect neurons sparsely with some probabilities.
 *
 * @author jyoshimi
 *
 */
public class Sparse extends ConnectNeurons {

    /** Probability connection will be an excitatory weight. */
    private double excitatoryProbability = NetworkPreferences.getExcitatoryProbability();
    /** Probability connection will be an inhibitory weight. */
    private double inhibitoryProbability = NetworkPreferences.getInhibitoryProbability();

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
                    network.addSynapse(new ClampedSynapse(source, target));
                }
                if (Math.random() < inhibitoryProbability) {
                    ClampedSynapse inhibitory = new ClampedSynapse(source, target);
                    inhibitory.setStrength(-1);
                    network.addSynapse(inhibitory);
                }
            }
        }
    }
}
