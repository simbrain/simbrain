/*
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.List;

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
 *       Ability to connect in a range (e.g, between 109 and 209 units away)
 *
 * @author jyoshimi
 *
 */
public class Radial extends ConnectNeurons {

    /** Probability of designating a given synapse excitatory. If not, it's inhibitory */
    private static double percentExcitatory = .6;

    /** Whether to allow self-connections. */
    private static boolean allowSelfConnections = false;

    /** Template synapse for excitatory synapses. */
    private static Synapse baseExcitatorySynapse = new ClampedSynapse(null, null);

    /** Probability of designating a given synapse excitatory. If not, it's inhibitory */
    private static double excitatoryProbability = .8;

    /** Radius within which to connect excitatory neurons. */
    private static double excitatoryRadius = 100;

    /** Template synapse for inhibitory synapses. */
    private static Synapse baseInhibitorySynapse = new ClampedSynapse(null, null);

    /** Radius within which to connect inhibitory neurons. */
    private static double inhibitoryRadius = 80;
    
    /** Probability of designating a given synapse excitatory. If not, it's inhibitory */
    private static double inhibitoryProbability = .8;

    /**
     * See super class description.
     *
     * @param network network with neurons to be connected.
     * @param neurons source neurons.
     * @param neurons2 target neurons.
     */
    public Radial(final Network network, final List<? extends Neuron> neurons, final ArrayList<? extends Neuron> neurons2) {
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
            if (rand < percentExcitatory) {
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
            if (!sourceNeurons.contains(target)) {
                continue;
            }
            // Don't add a connection if there is already one present
            if (Network.getSynapse(source, target) != null) {
                continue;
            }
            if (!allowSelfConnections) {
                if (source == target) {
                    continue;
                }
            }
            if (Math.random() < inhibitoryProbability) {
                Synapse synapse = baseInhibitorySynapse.duplicate();
                synapse.setSource(source);
                synapse.setTarget(target);
                synapse.setStrength(-1);
                network.addSynapse(synapse);
            }
        }
    }

    /**
     * Make an excitatory neuron, in the sense of connecting this neuron with
     * surrounding neurons via excitatory connections.
     *
     * @param source source neuron
     */
    private void makeExcitatory(final Neuron source) {
        for (Neuron target : network.getNeuronsInRadius(source, excitatoryRadius)) {
            if (!sourceNeurons.contains(target)) {
                continue;
            }
            // Don't add a connection if there is already one present
            if (Network.getSynapse(source, target) != null) {
                continue;
            }
            if (!allowSelfConnections) {
                if (source == target) {
                    continue;
                }
            }
            if (Math.random() < excitatoryProbability) {
                Synapse synapse = baseExcitatorySynapse.duplicate();
                synapse.setSource(source);
                synapse.setTarget(target);
                synapse.setStrength(1);
                network.addSynapse(synapse);
            }
        }
    }

    /**
     * @return the excitatoryRatio
     */
    public static double getPercentExcitatory() {
        return percentExcitatory;
    }

    /**
     * @param percentExcitatory the excitatoryRatio to set
     */
    public static void setPercentExcitatory(final double percentExcitatory) {
        Radial.percentExcitatory = percentExcitatory;
    }

    /**
     * @return the allowSelfConnections
     */
    public static boolean isAllowSelfConnections() {
        return allowSelfConnections;
    }

    /**
     * @param allowSelfConnections the allowSelfConnections to set
     */
    public static void setAllowSelfConnections(final boolean allowSelfConnections) {
        Radial.allowSelfConnections = allowSelfConnections;
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
        Radial.excitatoryProbability = excitatoryProbability;
    }

    /**
     * @return the excitatoryRadius
     */
    public static double getExcitatoryRadius() {
        return excitatoryRadius;
    }

    /**
     * @param excitatoryRadius the excitatoryRadius to set
     */
    public static void setExcitatoryRadius(final double excitatoryRadius) {
        Radial.excitatoryRadius = excitatoryRadius;
    }

    /**
     * @return the inhibitoryRadius
     */
    public static double getInhibitoryRadius() {
        return inhibitoryRadius;
    }

    /**
     * @param inhibitoryRadius the inhibitoryRadius to set
     */
    public static void setInhibitoryRadius(final double inhibitoryRadius) {
        Radial.inhibitoryRadius = inhibitoryRadius;
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
        Radial.inhibitoryProbability = inhibitoryProbability;
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
        Radial.baseExcitatorySynapse = baseExcitatorySynapse;
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
        Radial.baseInhibitorySynapse = baseInhibitorySynapse;
    }

}
