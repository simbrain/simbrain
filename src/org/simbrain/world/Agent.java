/*
 * Created on Jun 16, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.world;

import java.awt.event.ActionListener;

import javax.swing.JMenu;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface Agent {
	public double getStimulus(String[] sensor_id);
	public void setMotorCommand(String[] commandList, double value);
	public JMenu getMotorCommandMenu(ActionListener al);
	public JMenu getSensorIdMenu(ActionListener al);
}
