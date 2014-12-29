package org.simbrain.network.update_actions.concurrency_tools;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

public class BufferedUpdateTask implements Task {

	//private final Neuron host;

	private Neuron[] hosts = new Neuron[50];

	public BufferedUpdateTask(Neuron[] host) {
		this.hosts = host;
	}

	@Override
	public void perform() {
		for (Neuron host : hosts) {
			if (host == null) {
				break;
			}
			host.update();
			for (Synapse s : host.getFanIn()) {
				s.update();
			}
		}
	}

	@Override
	public boolean isPoison() {
		return false;
	}

}
