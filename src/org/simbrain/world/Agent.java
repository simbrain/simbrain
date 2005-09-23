/*
 * Created on Jun 16, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.world;


/**
 * 
 * <b>Agent</b>
 */
public interface Agent {
	public String getName();
	public World getParentWorld();
	public double getStimulus(String[] sensor_id);
	public void setMotorCommand(String[] commandList, double value);
}
