package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.Iterator;

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
    private double excitatoryProbability = .1;
    /** Probability connection will be an inhibitory weight. */
    private double inhibitoryProbability = .1;

    //TODO: set weights strengths or synapses
    
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
                    ClampedSynapse excitatory = new ClampedSynapse(source, target);
                    excitatory.setStrength(10);
                    network.addSynapse(excitatory);
                }
                if (Math.random() < inhibitoryProbability) {
                    ClampedSynapse inhibitory = new ClampedSynapse(source, target);
                    inhibitory.setStrength(-10);
                    network.addSynapse(inhibitory);
                }
            }
        }
    }

    /**
     * @return the excitatoryProbability
     */
    public double getExcitatoryProbability() {
        return excitatoryProbability;
    }

    /**
     * @param excitatoryProbability the excitatoryProbability to set
     */
    public void setExcitatoryProbability(double excitatoryProbability) {
        this.excitatoryProbability = excitatoryProbability;
    }

    /**
     * @return the inhibitoryProbability
     */
    public double getInhibitoryProbability() {
        return inhibitoryProbability;
    }

    /**
     * @param inhibitoryProbability the inhibitoryProbability to set
     */
    public void setInhibitoryProbability(double inhibitoryProbability) {
        this.inhibitoryProbability = inhibitoryProbability;
    }
}
