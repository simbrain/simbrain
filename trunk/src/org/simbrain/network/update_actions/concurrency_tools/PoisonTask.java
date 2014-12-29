package org.simbrain.network.update_actions.concurrency_tools;

public class PoisonTask implements Task {

	@Override
	public void perform() {
		return;
	}

	@Override
	public boolean isPoison() {
		return true;
	}

}
