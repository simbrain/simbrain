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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;

/**
 * Connect every source neuron to every target neuron.
 * 
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class AllToAll extends DensityBasedConnector {

    /** {@inheritDoc}. By definition is always 1.0 for All To All. */
    private final double connectionDensity = 1.0;

    /** {@inheritDoc} */
    public AllToAll() {
        super();
    }

    /**
     * Construct all to all connection object.
     * 
     * @param network
     *            parent network
     * @param neurons
     *            base neurons
     * @param neurons2
     *            target neurons
     */
    public AllToAll(boolean allowSelfConnect) {
        this.selfConnectionAllowed = allowSelfConnect;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "All to all";
    }

    /**
     * Connects every source neuron to every target neuron. The only exception
     * being that if the source and target neuron lists are the same, then no
     * connection will be made between a neuron and itself if self connections
     * aren't allowed. Will produce n^2 synapses if self connections are allowed
     * and n(n-1) if they are not.
     * 
     * @param sourceNeurons
     * @param targetNeurons
     * @param allowSelfConnection
     * @param looseSynapses
     * @return
     */
    public static List<Synapse> connectAllToAll(
        final List<Neuron> sourceNeurons, final List<Neuron> targetNeurons,
        final boolean recurrent, final boolean allowSelfConnection,
        final boolean looseSynapses) {
        ArrayList<Synapse> syns = new ArrayList<Synapse>(
            (int) (targetNeurons.size() * sourceNeurons.size()));
        if (recurrent && !allowSelfConnection) {
            int i = 0;
            int j;
            for (Neuron source : sourceNeurons) {
                j = 0;
                for (Neuron target : targetNeurons) {
                    if (i != j) {
                        Synapse s = new Synapse(source, target);
                        syns.add(s);
                    }
                    j++;
                }
                i++;
            }
        } else {
            for (Neuron source : sourceNeurons) {
                for (Neuron target : targetNeurons) {
                    Synapse s = new Synapse(source, target);
                    syns.add(s);
                }
            }
        }
        // If loose add directly to the network.
        if (looseSynapses) {
            for (Synapse s : syns) {
                s.getSource().getNetwork().addSynapse(s);
            }
        }
        return syns;
    }

    /**
     * AllToAll is in some respects a special case of VariableDensityConnector,
     * with connection density always set to 1.
     */
    @Override
    public double getConnectionDensity() {
        return connectionDensity;
    }

    /**
     * Throws and catches an UnsupportedOperationException if the
     * specifiedConnectionDensity does not equal 1.0. This is because by
     * definition the connection density of the connections resulting from an
     * AllToAll connector must have a density of 1.0. If the
     * connectionDensityParameter is 1.0, nothing happens. This functionality is
     * provided so that "instanceof" checks can be avoided.
     */
    public Collection<Synapse> setConnectionDensity(double connectionDensity) {
        try {
            if (connectionDensity != 1.0) {
                throw new UnsupportedOperationException(
                    "The connection density of"
                        + " an AllToAll connector cannot be"
                        + " changed, by definition it is always 1.0");
            }
        } catch (UnsupportedOperationException uoe) {
            uoe.printStackTrace();

        }
        return null;
    }

    /**
     * Connects neurons such that every source neuron is connected to every
     * target neuron. The only exception to this case is if the source neuron
     * group is the target neuron group and self-connections are not allowed.
     * 
     * @param synGroup
     *            the synapse group to which the synapses created by this
     *            connection class will be added.
     */
    @Override
    public void connectNeurons(SynapseGroup synGroup) {
        List<Synapse> syns = connectAllToAll(synGroup.getSourceNeurons(),
            synGroup.getTargetNeurons(), synGroup.isRecurrent(),
            selfConnectionAllowed, false);
        // Set the capacity of the synapse group's list to accomodate the
        // synapses this group will add.
        synGroup.preAllocateSynapses(synGroup.getSourceNeuronGroup().size()
            * synGroup.getTargetNeuronGroup().size());
        for (Synapse s : syns) {
            synGroup.addNewSynapse(s);
        }
    }

    /**
     * @return if neurons are allowed to connect to themselves i.e. a synapse
     *         where the source and target neuron are the same neuron is
     *         allowed.
     */
    @Override
    public boolean isSelfConnectionAllowed() {
        return selfConnectionAllowed;
    }

    /**
     * Set whether or not self connections are allowed.
     * 
     * @param allowSelfConnect
     */
    @Override
    public void setSelfConnectionAllowed(boolean allowSelfConnect) {
        this.selfConnectionAllowed = allowSelfConnect;
    }

}
