package org.simbrain.network.update_actions.concurrency_tools;

public interface Task {

	void perform();
	
	boolean isPoison();
	
}
