/*
 * Part of Simbrain--a java-based neural network kit
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
package org.simbrain.network.update_actions;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;

/**
 * Buffered update of all network models.  Write values to appropriate buffers.  Then read all values from them,
 * which allows for deterministic updating independently of update order.
 *
 * @author jyoshimi
 */
public class BufferedUpdate implements NetworkUpdateAction {

    /**
     * Reference to network to update.
     */
    private Network network;

    /**
     * @param network
     */
    public BufferedUpdate(Network network) {
        this.network = network;
    }

    @Override
    public void invoke() {
        network.bufferedUpdateAllNeurons(); // TODO: Rename this
        //network.clearInputs();
    }

    @Override
    public String getDescription() {
        return "Loose neurons (buffered) and synapses";
    }

    @Override
    public String getLongDescription() {
        return "Buffered update of loose items";
    }

}
