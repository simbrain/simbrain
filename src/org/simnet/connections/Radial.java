package org.simnet.connections;

import java.util.ArrayList;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.synapses.ClampedSynapse;

/**
 * Build a network by designating each source neuron either an excitatory or
 * an inhibitory neuron, and building out from there. Target neurons are not used.
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
    private Synapse exctitatorySynapse = new ClampedSynapse();
    /** Probability of designating a given synapse excitatory. If not, it's inhibitory */
    private double excitatoryProbability = .8;
    /** Radius within which to connect excitatory neurons. */
    private double excitatoryRadius = 75;

    /** Template synapse for inhibitory synapses. */
    private Synapse inhibitorySynapse = new ClampedSynapse();
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
        exctitatorySynapse.setStrength(1);
        inhibitorySynapse.setStrength(-1);
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
                Synapse weight = inhibitorySynapse.duplicate();
                weight.setStrength(-1);
                weight.setSource(source);
                weight.setTarget(target);
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
                Synapse weight = exctitatorySynapse.duplicate();
                weight.setSource(source);
                weight.setTarget(target);
                network.addSynapse(weight);
            }
        }
    }

}
