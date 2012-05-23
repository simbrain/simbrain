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

import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.UpdateAction;

/**
 * Buffered update of loose items (neurons and synapses), i.e. items not in
 * groups.  (Buffered update means order of update does not matter).
 * 
 * @author jyoshimi
 */
public class BufferedUpdate implements UpdateAction {

    /** Reference to network to update. */
    private RootNetwork network;
    
    /**
     * @param network
     */
    public BufferedUpdate(RootNetwork network) {
        this.network = network;
    }

	@Override
    public void invoke() {
        network.bufferedUpdateAllNeurons();
        network.updateAllSynapses();
    }
    
	@Override
    public String getDescription() {
        return "Buffered update";
    }

	@Override
	public String getLongDescription() {
        return "Buffered update of loose items";
	}

}
