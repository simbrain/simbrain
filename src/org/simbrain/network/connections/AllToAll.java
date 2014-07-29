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
import org.simbrain.util.Utils;

/**
 * Connect every source neuron to every target neuron.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class AllToAll implements ConnectNeurons {

    /**
     * The default preference as to whether or not self connections are allowed.
     */
    private static final boolean DEFAULT_SELF_CONNECT_PREF = false;

    /**
     * Whether or not connections where the source and target are the same
     * neuron are allowed. Only applicable if the source and target neuron sets
     * are the same.
     */
    private boolean selfConnectionAllowed = DEFAULT_SELF_CONNECT_PREF;

    /**
     * Construct to all to all connector.
     */
    public AllToAll() {
        super();
    }

    /**
     * Construct all to all connection object.
     *
     * @param allowSelfConnect whether to allow self connections or not
     */
    public AllToAll(boolean allowSelfConnect) {
        this.selfConnectionAllowed = allowSelfConnect;
    }

    /**
     * Returns a short name for this connection type, used in combo boxes.
     *
     * @return the name for this connection type
     */
    public static String getName() {
        return "All to all";
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Connect all to all using underlying connection object to store
     * parameters. Used by quick connect.
     *
     * @param sourceNeurons the source neurons
     * @param targetNeurons the target neurons
     * @return the new synapses
     */
    public List<Synapse> connectAllToAll(List<Neuron> sourceNeurons,
            List<Neuron> targetNeurons) {
        return connectAllToAll(sourceNeurons, targetNeurons,
                Utils.intersects(sourceNeurons, targetNeurons),
                selfConnectionAllowed, true);
    }

    /**
     * Connects every source neuron to every target neuron. The only exception
     * being that if the source and target neuron lists are the same, then no
     * connection will be made between a neuron and itself if self connections
     * aren't allowed. Will produce n^2 synapses if self connections are allowed
     * and n(n-1) if they are not.
     *
     * @param sourceNeurons the source neurons
     * @param targetNeurons the target neurons
     * @param recurrent whether the source and target neurons overlap. Some
     *            classes know ahead of time if the connection will be recurrent
     *            and knowing this allows a slight performance improvement.
     * @param allowSelfConnection whether to allow self-connections
     * @param looseSynapses whether the synapses being connected are loose or in
     *            a synapse group
     * @return the synapses created.
     */
    public static List<Synapse> connectAllToAll(
            final List<Neuron> sourceNeurons, final List<Neuron> targetNeurons,
            final boolean recurrent, final boolean allowSelfConnection,
            final boolean looseSynapses) {
        ArrayList<Synapse> syns = new ArrayList<Synapse>(
                (int) (targetNeurons.size() * sourceNeurons.size()));
        // Optimization: separately handle case where we have to worry about
        // avoiding self-connections, so an equals check is required.
        if (recurrent && !allowSelfConnection) {
            for (Neuron source : sourceNeurons) {
                for (Neuron target : targetNeurons) {
                    if (!(source.equals(target))) {
                        Synapse s = new Synapse(source, target);
                        syns.add(s);
                    }
                }
            }
        } else {
            // The case where we don't need to worry about self-connections
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
     * Connects neurons such that every source neuron is connected to every
     * target neuron. The only exception to this case is if the source neuron
     * group is the target neuron group and self-connections are not allowed.
     *
     * @param synGroup the synapse group to which the synapses created by this
     *            connection class will be added.
     */
    public void connectNeurons(SynapseGroup synGroup) {
        List<Synapse> syns = connectAllToAll(synGroup.getSourceNeurons(),
                synGroup.getTargetNeurons(), synGroup.isRecurrent(),
                selfConnectionAllowed, false);
        // Set the capacity of the synapse group's list to accommodate the
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
    public boolean isSelfConnectionAllowed() {
        return selfConnectionAllowed;
    }

    /**
     * Set whether or not self connections are allowed.
     *
     * @param allowSelfConnect
     */
    public void setSelfConnectionAllowed(boolean allowSelfConnect) {
        this.selfConnectionAllowed = allowSelfConnect;
    }

}
