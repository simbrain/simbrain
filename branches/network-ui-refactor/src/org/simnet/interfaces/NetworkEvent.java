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

import java.util.EventObject;

/**
 * Network event.  Currently adding or deleting a neuron.
 */
public final class NetworkEvent
    extends EventObject {

    /** Reference to neuron. */
    private Neuron neuron;


    /**
     * Create a new model event.
     *
     * @param net reference to network firing event
     * @param neuron reference to the neuron this event concerns
     */
    public NetworkEvent(final Network net, final Neuron neuron) {
        super(net);
        this.neuron = neuron;
    }

    /**
     * @return Returns the neuron.
     */
    public Neuron getNeuron() {
        return neuron;
    }


}