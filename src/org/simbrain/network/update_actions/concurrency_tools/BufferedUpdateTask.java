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

	private final int hostSize;
	
	/**
	 * @param hosts
	 */
	public BufferedUpdateTask(final Neuron[] hosts) {
		this.hosts = hosts;
		this.hostSize = hosts.length;
	}

	/**
	 * {@inheritDoc}
	 * Updates the neurons (does not send their buffered value to their
	 * activation) then updates all afferent synapses to the neurons in
	 * question.
	 */
	@Override
	public void perform() {
		for (int i = 0; i < hostSize; i++) {
			if (hosts[i] == null) {
				break;
			}
			hosts[i].update();
			hosts[i].updateFanIn();
		}
	}

	public Neuron[] getHosts() {
		return hosts;
	}

}
