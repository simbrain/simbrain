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
package org.simbrain.network.update_actions.concurrency_tools;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

/**
 * 
 * @author Zach Tosi
 * 
 * A task that updates neurons according to their update rules, then updates
 * all afferent synapses to that neuron. Neuron's activations are not set
 * from their buffers here.
 *
 */
public class BufferedUpdateTask implements Task {

	/** The host neurons, whose update will constitute this task. */
	private final Neuron[] hosts;

	/**
	 * @param hosts
	 */
	public BufferedUpdateTask(final Neuron[] hosts) {
		this.hosts = hosts;
	}

	/**
	 * {@inheritDoc}
	 * Updates the neurons (does not send their buffered value to their
	 * activation) then updates all afferent synapses to the neurons in
	 * question.
	 */
	@Override
	public void perform() {
		for (Neuron host : hosts) {
			if (host == null) {
				break;
			}
			host.update();
			if (!host.getUpdateRule().isSkipsSynapticUpdates()) {
    			for (Synapse s : host.getFanIn()) {
    				s.update();
    			}
			}
		}
	}

	@Override
	public boolean isPoison() {
		return false;
	}

	@Override
	public boolean isWaiting() {
		return false;
	}

}
