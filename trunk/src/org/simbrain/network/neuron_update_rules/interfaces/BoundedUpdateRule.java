package org.simbrain.network.neuron_update_rules.interfaces;

public interface BoundedUpdateRule {
	

	
	public double getCeiling();
	
	public double getFloor();
	
	public void setCeiling(double ceiling);
	
	public void setFloor(double floor);
	

	
}
