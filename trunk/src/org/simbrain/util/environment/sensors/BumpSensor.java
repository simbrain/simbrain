package org.simbrain.util.environment.sensors;

import org.simbrain.util.environment.Agent;
/**
 * Very simple bump sensor. Holding off on more sophisticated "touch" sensors in case
 * an existing¯ library can provide it.
 * 
 * TODO:
 *  - Not tested yet 
 * 	Possible extensions:
 * 	- location of bump sensor
 *  - return vector represent impact on agent
 */
public class BumpSensor extends Sensor {

	/** Whether it was bumped. */
	private boolean wasBumped = false;
	
	/** Value to produce when bumped. */
	private double bumpValue = 0;
	
	/** Parent agent. */
	private Agent parent;
	
	/**
	 * Construct bump sensor.
	 *
	 * @param parent
	 * @param name
	 * @param bumpVal
	 */
	public BumpSensor(Agent parent, String name, double bumpVal) {
		super(parent, name);
		this.parent = parent;
		this.bumpValue = bumpVal;
	}

	/**
     * {@inheritDoc}
     */
	public Double getValue() {
		if (wasBumped()) {
			return new Double(bumpValue);
		}
		else return new Double(0);
	}

	/**
     * {@inheritDoc}
     */
	public String getKey() {
		return "Bump-" + bumpValue;
	}

	/**
	 * @return the wasBumped
	 */
	public boolean wasBumped() {
		return wasBumped;
	}

	/**
	 * @param wasBumped the wasBumped to set
	 */
	public void setBumped(boolean wasBumped) {
		this.wasBumped = wasBumped;
	}

	/**
     * {@inheritDoc}
     */
	public void update() {
	}

}
