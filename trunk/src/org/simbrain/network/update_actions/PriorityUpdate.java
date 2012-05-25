/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.update_actions;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.UpdateAction;

/**
 * Loose neurons (neurons not in groups) are updated in accordance with an
 * ordered priority list. User sets the priority for each neuron. Default
 * priority value is 0. Elements with smaller priority value are updated first.
 *
 * @author jyoshimi
 */
public class PriorityUpdate implements UpdateAction {

    /** Reference to network to update. */
    private Network network;

    /**
     * @param network
     */
    public PriorityUpdate(Network network) {
        this.network = network;
    }

	@Override
    public void invoke() {
        network.updateNeuronsByPriority();
        network.updateAllSynapses();
    }

	@Override
    public String getDescription() {
        return "Priority update";
    }

	@Override
	public String getLongDescription() {
        return "Priority update of loose items";
	}
}
