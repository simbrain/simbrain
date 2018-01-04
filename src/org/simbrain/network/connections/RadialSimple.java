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
import org.simbrain.network.groups.SynapseGroup;

/**
 * For each neuron, consider every neuron in an excitatory and inhibitory radius
 * from it, and make excitatory and inhibitory synapses with them according to
 * some probability.
 *
 * Currently this is not accessible in the GUI, and is only used by some
 * scripts.
 *
 * @author Jeff Yoshimi
 *
 */
public class RadialSimple implements ConnectNeurons {

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
     * Reference to network in which radial connections will be made on loose
     * synapses.
     */
    private Network network;

    /**
     * Reference to source neurons (target neurons are not provided to this type
     * of connection.
     */
    private List<Neuron> sourceNeurons;

    /**
     * @param network the network
     * @param sourceNeurons the source neurons
     */
    public RadialSimple(Network network, List<Neuron> sourceNeurons) {
        super();
        this.network = network;
        this.sourceNeurons = sourceNeurons;
    }


    /**
     * Make the connections.
     *
     * @param looseSynapses whether loose synapses are being connected.
     * @return the new synapses.
     */
    public List<Synapse> connectNeurons(final boolean looseSynapses) {
        ArrayList<Synapse> syns = new ArrayList<Synapse>();
        for (Neuron source : sourceNeurons) {
            makeExcitatory(source, syns, looseSynapses);
            makeInhibitory(source, syns, looseSynapses);
        }
        return syns;
    }

    /**
     * Make an inhibitory neuron, in the sense of connecting this neuron with
     * surrounding neurons via excitatory connections.
     *
     * @param source source neuron
     */
    private void makeInhibitory(final Neuron source, List<Synapse> syns,
            boolean looseSynapses) {
        for (Neuron target : getNeuronsInRadius(source,
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
                Synapse synapse = new Synapse(source, target);
                synapse.setStrength(-1);
                if (looseSynapses) {
                    network.addSynapse(synapse);
                }
                syns.add(synapse);
            }
        }
    }

    /**
     * Return a list of neurons in a specific radius of a specified neuron.
     *
     * @param source the source neuron.
     * @param radius the radius to search within.
     * @return list of neurons in the given radius.
     */
    private List<Neuron> getNeuronsInRadius(Neuron source, double radius) {
        ArrayList<Neuron> ret = new ArrayList<Neuron>();
        for (Neuron neuron : sourceNeurons) {
            if (network.getEuclideanDist(source, neuron) < radius) {
                ret.add(neuron);
            }
        }
        return ret;
    }

    /**
     * Make an excitatory neuron, in the sense of connecting this neuron with
     * surrounding neurons via excitatory connections.
     *
     * @param source source neuron
     */
    private void makeExcitatory(final Neuron source, List<Synapse> syns,
            boolean looseSynapses) {
        for (Neuron target : getNeuronsInRadius(source,
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
                Synapse synapse = new Synapse(source, target);
                synapse.setStrength(1);
                if (looseSynapses) {
                    network.addSynapse(synapse);
                }
                syns.add(synapse);
            }
            network.fireSynapsesUpdated();
        }
    }

    /**
     * @return the allowSelfConnections
     */
    public boolean isAllowSelfConnections() {
        return allowSelfConnections;
    }

    /**
     * @param allowSelfConnections
     *            the allowSelfConnections to set
     */
    public void setAllowSelfConnections(final boolean allowSelfConnections) {
        this.allowSelfConnections = allowSelfConnections;
    }

    /**
     * @return the excitatoryProbability
     */
    public double getExcitatoryProbability() {
        return excitatoryProbability;
    }

    /**
     * @param excitatoryProbability
     *            the excitatoryProbability to set
     */
    public void setExcitatoryProbability(final double excitatoryProbability) {
        this.excitatoryProbability = excitatoryProbability;
    }

    /**
     * @return the excitatoryRadius
     */
    public double getExcitatoryRadius() {
        return excitatoryRadius;
    }

    /**
     * @param excitatoryRadius
     *            the excitatoryRadius to set
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
     * @param inhibitoryRadius
     *            the inhibitoryRadius to set
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
     * @param inhibitoryProbability
     *            the inhibitoryProbability to set
     */
    public void setInhibitoryProbability(final double inhibitoryProbability) {
        this.inhibitoryProbability = inhibitoryProbability;
    }

    /**
     * @return the baseExcitatorySynapse
     */
    public Synapse getBaseExcitatorySynapse() {
        return baseExcitatorySynapse;
    }

    /**
     * @param baseExcitatorySynapse
     *            the baseExcitatorySynapse to set
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
     * @param baseInhibitorySynapse
     *            the baseInhibitorySynapse to set
     */
    public void setBaseInhibitorySynapse(Synapse baseInhibitorySynapse) {
        this.baseInhibitorySynapse = baseInhibitorySynapse;
    }

    @Override
    public void connectNeurons(SynapseGroup synGroup) {
        // No implementation yet.
    }

    @Override
    public String toString() {
        return "Radial";
    }

}