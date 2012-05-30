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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

/**
 * For each neuron, consider every neuron in an excitatory and inhibitory radius
 * from it, and make excitatory and inhibitory synapses with them.
 * 
 * TODO: More complex connection making functions Custom randomization? Ability
 * to connect in a range (e.g, between 109 and 209 units away)
 *
 * @author jyoshimi
 *
 */
public class Radial extends ConnectNeurons {

    /** Whether to allow self-connections. */
    private boolean allowSelfConnections = false;

    /** Template synapse for excitatory synapses. */
    private Synapse baseExcitatorySynapse = Synapse.getTemplateSynapse();

    /**
     * Probability of designating a given synapse excitatory. If not, it's
     * inhibitory.
     */
    private double excitatoryProbability = .8;

    /** Radius within which to connect excitatory neurons. */
    private double excitatoryRadius = 100;

    /** Template synapse for inhibitory synapses. */
    private Synapse baseInhibitorySynapse = Synapse.getTemplateSynapse();

    /** Radius within which to connect inhibitory neurons. */
    private double inhibitoryRadius = 80;

    /**
     * Probability of designating a given synapse excitatory. If not, it's
     * inhibitory.
     */
    private double inhibitoryProbability = .8;

    /**
     * See super class description.
     *
     * @param network network with neurons to be connected.
     * @param neurons source neurons.
     * @param neurons2 target neurons.
     */
    public Radial(final Network network, final List<? extends Neuron> neurons,
            final List<? extends Neuron> neurons2) {
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
    public List<Synapse> connectNeurons() {
    	ArrayList<Synapse> syns = new ArrayList<Synapse>();
        for (Neuron source : sourceNeurons) {
            makeExcitatory(source, syns);
            makeInhibitory(source, syns);
        }
        return syns;
    }

    /**
     * Make an inhibitory neuron, in the sense of connecting this neuron with
     * surrounding neurons via excitatory connections.
     *
     * @param source source neuron
     */
    private void makeInhibitory(final Neuron source, List<Synapse> syns) {
        for (Neuron target : network.getNeuronsInRadius(source,
                inhibitoryRadius)) {
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
                Synapse synapse = baseInhibitorySynapse
                        .instantiateTemplateSynapse(source, target, network);
                synapse.setStrength(-1);
                network.addSynapse(synapse);
                syns.add(synapse);
            }
        }
    }

    /**
     * Make an excitatory neuron, in the sense of connecting this neuron with
     * surrounding neurons via excitatory connections.
     *
     * @param source source neuron
     */
    private void makeExcitatory(final Neuron source, List<Synapse> syns) {
        for (Neuron target : network.getNeuronsInRadius(source,
                excitatoryRadius)) {
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
                Synapse synapse = baseExcitatorySynapse
                        .instantiateTemplateSynapse(source, target, network);
                synapse.setStrength(1);
                network.addSynapse(synapse);
                syns.add(synapse);
            }
        }
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
    public void setAllowSelfConnections(
            final boolean allowSelfConnections) {
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
    public void setExcitatoryProbability(
            final double excitatoryProbability) {
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
    public void setExcitatoryRadius(final double excitatoryRadius) {
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
    public void setInhibitoryRadius(final double inhibitoryRadius) {
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
    public void setInhibitoryProbability(
            final double inhibitoryProbability) {
        this.inhibitoryProbability = inhibitoryProbability;
    }

    /**
     * @return the baseExcitatorySynapse
     */
    public Synapse getBaseExcitatorySynapse() {
        return baseExcitatorySynapse;
    }

    /**
     * @param baseExcitatorySynapse the baseExcitatorySynapse to set
     */
    public void setBaseExcitatorySynapse(Synapse baseExcitatorySynapse) {
        this.baseExcitatorySynapse = baseExcitatorySynapse;
    }

    /**
     * @return the baseInhibitorySynapse
     */
    public Synapse getBaseInhibitorySynapse() {
        return baseInhibitorySynapse;
    }

    /**
     * @param baseInhibitorySynapse the baseInhibitorySynapse to set
     */
    public void setBaseInhibitorySynapse(Synapse baseInhibitorySynapse) {
        this.baseInhibitorySynapse = baseInhibitorySynapse;
    }

}
