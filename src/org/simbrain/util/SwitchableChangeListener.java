package org.simbrain.util;

import javax.swing.event.ChangeListener;

public abstract class SwitchableChangeListener implements ChangeListener{
	
	private boolean enabled = true;
	
	public void enable(){
		enabled = true;
	}
	
	public void disable(){
		enabled = false;
	}
	
	public boolean isEnabled(){
		return enabled;
	}
	
}
