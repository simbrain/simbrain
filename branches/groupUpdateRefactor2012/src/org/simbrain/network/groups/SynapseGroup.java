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
package org.simbrain.network.groups;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;

/**
 * A group of synapses.
 */
public class SynapseGroup extends Group {

    /** Set of synapses. */
    // TODO: Why a set?
    private final Set<Synapse> synapseList = new HashSet<Synapse>();

    /** @see Group */
    public SynapseGroup(final RootNetwork net, final List<Synapse> list) {
        super(net);
        for (Synapse synapse : list) {
            synapseList.add(synapse);
        }
    }

    /**
     * Add synapse.
     * 
     * @param synapse synapse to add
     */
    public void addSynapse(Synapse synapse) {
        synapseList.add(synapse);
    }

    /**
     * Delete a synapse.
     * 
     * @param toDelete synapse to delete
     */
    public void deleteSynapse(Synapse toDelete) {
        synapseList.remove(toDelete);
        // parent.fireGroupChanged(this, this);
    }

    /**
     * @return a list of weights
     */
    public List<Synapse> getSynapseList() {
        return new ArrayList<Synapse>(synapseList);
    }

    /**
     * Update group. Override for special updating.
     */
    public void update() {
        updateAllSynapses();
    }

    /**
     * Update all synapses.
     */
    public void updateAllSynapses() {
        for (Synapse synapse : synapseList) {
            synapse.update();
        }
    }

    //REDO: Add label / id info below
    
    @Override
    public String toString() {
        String ret = new String();
        ret += ("Synapse group with " + this.getSynapseList().size() + " synapse(s),");
        return ret;
    }
    
    @Override
    public boolean isEmpty() {
        return synapseList.isEmpty();
    }
}
