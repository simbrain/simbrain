/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.connections;

import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;

/**
 * Connect every source neuron to every target neuron.
 *
 * @author jyoshimi
 * @author ztosi
 */
public class AllToAll extends ConnectNeurons {

    /**
     * "Template" synapse to be used when applying the connection.
     */
    private Synapse baseSynapse = Synapse.getTemplateSynapse();

    /** Allows neurons to have a self connection. */
    private boolean allowSelfConnection = true;

    /**
     * Construct all to all connection object.
     * 
     * @param network parent network
     * @param neurons base neurons
     * @param neurons2 target neurons
     */
    public AllToAll(final Network network,
            final List<? extends Neuron> neurons,
            final List<? extends Neuron> neurons2) {
        super(network, neurons, neurons2);
    }

    public AllToAll(final Network network) {
        this.network = network;
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
                // Don't add a connection if there is already one present
                if (Network.getSynapse(source, target) != null) {
                    continue;
                }
                if (!allowSelfConnection) {
                    if (source != target) {
                        Synapse synapse = baseSynapse
                                .instantiateTemplateSynapse(source, target,
                                        network);
                        network.addSynapse(synapse);
                    }
                } else {
                    Synapse synapse = baseSynapse.instantiateTemplateSynapse(
                            source, target, network);
                    network.addSynapse(synapse);
                }
            }
        }
    }

    /**
     * Fully connects two sets of neurons, with weights between min and max.
     *
     * @param sourceNeurons sources
     * @param targetNeurons targets
     * @param min minimum weight value, must be less than zero
     * @param max maximum weight value, must be greater than zero
     * @param exciteProb probability the synapse is excititory (strength on [0,
     *            max)).
     */
    public void connectNeurons(List<Neuron> sourceNeurons,
            List<Neuron> targetNeurons, double min, double max,
            double exciteProb) {
        errorCheck(min, max, exciteProb);
        for (Neuron source : sourceNeurons) {
            for (Neuron target : targetNeurons) {
                // Don't add a connection if there is already one present
                if (Network.getSynapse(source, target) != null) {
                    continue;
                }

                double wt;
                if (Math.random() < exciteProb) {
                    wt = max * Math.random();
                } else {
                    wt = min * Math.random();
                }

                if (!allowSelfConnection) {
                    if (source != target) {
                        Synapse synapse = baseSynapse
                                .instantiateTemplateSynapse(source, target,
                                        network);
                        synapse.setStrength(wt);
                        network.addSynapse(synapse);
                    }
                } else {
                    Synapse synapse = baseSynapse.instantiateTemplateSynapse(
                            source, target, network);
                    synapse.setStrength(wt);
                    network.addSynapse(synapse);
                }
            }
        }
    }

    /**
     * Checks to see if weight parameters are within acceptable ranges.
     *
     * @param min minimum weight value, must be less than zero
     * @param max maximum weight value, must be greater than zero
     * @param exciteProb probability the synapse is excititory (strength on [0,
     *            max)).
     */
    public void errorCheck(double min, double max, double exciteProb) {
        if (!(max >= 0 && min <= 0)) {
            throw new IllegalArgumentException("Max weight must be greater"
                    + " than zero or equal to and min weight must be less than"
                    + " or equal to zero.");
        }
        if (min == 0 && max == 0) {
            throw new IllegalArgumentException("Min and max cannot both be"
                    + " equal to zero");
        }
        if (exciteProb < 0 || exciteProb > 1) {
            throw new IllegalArgumentException("Excitatory probability is"
                    + " not on [0,1]");
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
    public void setBaseSynapse(final Synapse theSynapse) {
        baseSynapse = theSynapse;
    }

    /**
     * @return the allowSelfConnection
     */
    public boolean isAllowSelfConnection() {
        return allowSelfConnection;
    }

    /**
     * @param allowSelfConnection the allowSelfConnection to set
     */
    public void setAllowSelfConnection(boolean allowSelfConnection) {
        this.allowSelfConnection = allowSelfConnection;
    }
}
