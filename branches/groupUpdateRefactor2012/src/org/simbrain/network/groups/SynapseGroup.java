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
package org.simbrain.network.groups;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;

/**
 * A group of synapses.
 */
public class SynapseGroup extends Group {

  /** Set of synapses. */  // TODO: Why a set?
  private Set<Synapse> synapseList = new HashSet<Synapse>();


    /** @see Group */
    public SynapseGroup(final RootNetwork net, final List<Synapse> synapseList) {
        super(net);
        for (Synapse synapse : synapseList) {
            synapseList.add(synapse);
        }
    }


}
