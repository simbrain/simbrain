package org.simbrain.network.connections;

import java.util.ArrayList;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * For each neuron, consider every neuron in an exictatory and inhibitory radius from it, and 
 * make excitatory and inhibitory synapses with them.
 * 
 * TODO: More complex connection making functions
 *       Custom randomization?
 *
 * @author jyoshimi
 *
 */
public class Radial extends ConnectNeurons {

    /** Probability of designating a given synapse excitatory. If not, it's inhibitory */
    private double excitatoryRatio = .4;
    
    /** Whether to allow self-connections. */
    private boolean allowSelfConnections = false;

    /** Template synapse for excitatory synapses. */
    //private Synapse exctitatorySynapse = new ClampedSynapse(null, null); // TODO

    /** Probability of designating a given synapse excitatory. If not, it's inhibitory */
    private double excitatoryProbability = .8;
    
    /** Radius within which to connect excitatory neurons. */
    private double excitatoryRadius = 75;

    /** Template synapse for inhibitory synapses. */
    //private Synapse inhibitorySynapse = new ClampedSynapse(null, null); // TODO
    
    /** Radius within which to connect inhibitory neurons. */
    private double inhibitoryRadius = 40;
    
    /** Probability of designating a given synapse excitatory. If not, it's inhibitory */
    private double inhibitoryProbability = .8;

    /**
     * See super class description.
     *
     * @param network network with neurons to be connected.
     * @param neurons source neurons.
     * @param neurons2 target neurons.
     */
    public Radial(final Network network, final ArrayList neurons, final ArrayList neurons2) {
        super(network, neurons, neurons2);
    }

    /** {@inheritDoc} */
    public Radial() {
    }
    
    @Override
    public String toString() {
        return "Radial";
    }
    
    /** @inheritDoc */
    public void connectNeurons() {
        for (Neuron source : sourceNeurons) {
            double rand = Math.random();
            if (rand < excitatoryRatio) {
                makeExcitatory(source);
            }  else {
                makeInhibitory(source);
            }
        }
    }

    /**
     * Make an inhibitory neuron, in the sense of connecting this neuron with surrounding neurons via
     * excitatory connections.
     *
     * @param source source neuron
     */
    private void makeInhibitory(final Neuron source) {
        for (Neuron target : network.getNeuronsInRadius(source, inhibitoryRadius)) {
            if (!allowSelfConnections) {
                if (source == target) {
                    continue;
                }
            }
            if (Math.random() < inhibitoryProbability) {
                Synapse weight = new ClampedSynapse(source, target);
                weight.setStrength(-1);
                network.addSynapse(weight);
            }
        }
    }

    /**
     * Make an excitatory neuron, in the sense of connecting this neuron with surrounding neurons via
     * excitatory connections.
     *
     * @param source source neuron
     */
    private void makeExcitatory(final Neuron source) {
        for (Neuron target : network.getNeuronsInRadius(source, excitatoryRadius)) {
            if (!allowSelfConnections) {
                if (source == target) {
                    continue;
                }
            }
            if (Math.random() < excitatoryProbability) {
                Synapse weight = new ClampedSynapse(source, target);
                weight.setStrength(1);
                network.addSynapse(weight);
            }
        }
    }

    /**
     * @return the excitatoryRatio
     */
    public double getExcitatoryRatio() {
        return excitatoryRatio;
    }

    /**
     * @param excitatoryRatio the excitatoryRatio to set
     */
    public void setExcitatoryRatio(double excitatoryRatio) {
        this.excitatoryRatio = excitatoryRatio;
    }

    /**
     * @return the allowSelfConnections
     */
    public boolean isAllowSelfConnections() {
        return allowSelfConnections;
    }

    /**
     * @param allowSelfConnections the allowSelfConnections to set
     */
    public void setAllowSelfConnections(boolean allowSelfConnections) {
        this.allowSelfConnections = allowSelfConnections;
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
     * @return the excitatoryRadius
     */
    public double getExcitatoryRadius() {
        return excitatoryRadius;
    }

    /**
     * @param excitatoryRadius the excitatoryRadius to set
     */
    public void setExcitatoryRadius(double excitatoryRadius) {
        this.excitatoryRadius = excitatoryRadius;
    }

    /**
     * @return the inhibitoryRadius
     */
    public double getInhibitoryRadius() {
        return inhibitoryRadius;
    }

    /**
     * @param inhibitoryRadius the inhibitoryRadius to set
     */
    public void setInhibitoryRadius(double inhibitoryRadius) {
        this.inhibitoryRadius = inhibitoryRadius;
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
