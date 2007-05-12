/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.interfaces;

/**
 * <b>Group</b> A meaningful group of neurons, synapses, and later, subnetworks.  These
 * elements may be part of other subnetworks.  Extending this class and overriding the update
 * function gives it functoinality.
 *
 * This extends Network not because these should behave like networks but mainly to take advantage of utility
 * methods associated with the Network class.
 *
 * TODO: Next step.  Make groups listen to the networks they "mirror."
 * When the network changes (a neuron is added, removed, changed), so does the network.
 * Perhaps this fires a groupChanged event so the view can update accordingly.
 */
public abstract class Group extends Network {

    /**
     * Construc a model group with a reference to its root network.
     *
     * @param net reference to root network.
     */
    public Group(final RootNetwork net) {
        this.setRootNetwork(net);
    }

}
