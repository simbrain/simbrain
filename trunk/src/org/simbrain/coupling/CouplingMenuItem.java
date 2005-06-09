/*
 * Created on May 31, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.coupling;

import javax.swing.JMenuItem;

/**
 * <b>CouplingMenuItem</b> allows a menu-itme to carry a reference to an associated 
 * coupling object.  This communication between the various components of simbrains via
 * pop-up menus.
 */
public class CouplingMenuItem extends JMenuItem {
	
	private Coupling coupling;
	
	public CouplingMenuItem(String menuText, Coupling coupling_ref) {
		super(menuText);
		coupling = coupling_ref;
	}

	/**
	 * @return Returns the coupling.
	 */
	public Coupling getCoupling() {
		return coupling;
	}
	/**
	 * @param coupling The coupling to set.
	 */
	public void setCoupling(Coupling coupling) {
		this.coupling = coupling;
	}
}
