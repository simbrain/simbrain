package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.Iterator;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Connect neurons sparsely with some probabilities.
 *
 * @author jyoshimi
 *
 */
public class Sparse extends ConnectNeurons {

    /** Probability connection will be an excitatory weight. */
    public static double excitatoryProbability = .1;
    /** Probability connection will be an inhibitory weight. */
    public static double inhibitoryProbability = .1;
    
    /** Base excitatory synapse. */
    private static Synapse baseExcitatorySynapse = new ClampedSynapse(null, null);

    /** Base inhibitory synapse. */
    private static Synapse baseInhibitorySynapse = new ClampedSynapse(null, null);
    
    {
        baseExcitatorySynapse.setStrength(10);
        baseInhibitorySynapse.setStrength(-10);
    }


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
    
    /** {@inheritDoc} */
    public Sparse() {}

    @Override
    public String toString() {
        return "Sparse";
    }


    /** @inheritDoc */
    public void connectNeurons() {
        for (Iterator i = sourceNeurons.iterator(); i.hasNext(); ) {
            Neuron source = (Neuron) i.next();
            for (Iterator j = targetNeurons.iterator(); j.hasNext(); ) {
                Neuron target = (Neuron) j.next();
                if (Math.random() < excitatoryProbability) {
                    Synapse synapse = baseExcitatorySynapse.duplicate();
                    synapse.setSource(source);
                    synapse.setTarget(target);
                    network.addSynapse(synapse);
                }
                if (Math.random() < inhibitoryProbability) {
                    Synapse synapse = baseInhibitorySynapse.duplicate();
                    synapse.setSource(source);
                    synapse.setTarget(target);
                    network.addSynapse(synapse);
                }
            }
        }
    }

    /**
     * @return the excitatoryProbability
     */
    public static double getExcitatoryProbability() {
        return excitatoryProbability;
    }

    /**
     * @param excitatoryProbability the excitatoryProbability to set
     */
    public static void setExcitatoryProbability(final double excitatoryProbability) {
        Sparse.excitatoryProbability = excitatoryProbability;
    }

    /**
     * @return the inhibitoryProbability
     */
    public static double getInhibitoryProbability() {
        return inhibitoryProbability;
    }

    /**
     * @param inhibitoryProbability the inhibitoryProbability to set
     */
    public static void setInhibitoryProbability(final double inhibitoryProbability) {
        Sparse.inhibitoryProbability = inhibitoryProbability;
    }

    /**
     * @return the baseExcitatorySynapse
     */
    public static Synapse getBaseExcitatorySynapse() {
        return baseExcitatorySynapse;
    }

    /**
     * @param baseExcitatorySynapse the baseExcitatorySynapse to set
     */
    public static void setBaseExcitatorySynapse(final Synapse baseExcitatorySynapse) {
        Sparse.baseExcitatorySynapse = baseExcitatorySynapse;
    }

    /**
     * @return the baseInhibitorySynapse
     */
    public static Synapse getBaseInhibitorySynapse() {
        return baseInhibitorySynapse;
    }

    /**
     * @param baseInhibitorySynapse the baseInhibitorySynapse to set
     */
    public static void setBaseInhibitorySynapse(final Synapse baseInhibitorySynapse) {
        Sparse.baseInhibitorySynapse = baseInhibitorySynapse;
    }
}
