/*
 * Created on Jun 16, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.world;

import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenu;

import org.simbrain.network.NetworkPanel;

/**
 * 
 * <b>World</b>
 */
public interface World {
	
	public String getType();
	public String getName();
	public ArrayList getAgentList();
	public JMenu getMotorCommandMenu(ActionListener al);
	public JMenu getSensorIdMenu(ActionListener al);

	//	TODO: Is this the right design?
	//		worlds have lists of targets that, when they are
	//		updated, they update
	public void addCommandTarget(NetworkPanel net); 
	public void removeCommandTarget(NetworkPanel net); 
	public ArrayList getCommandTargets();

}
