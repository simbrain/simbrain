/*
 * Created on May 31, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.coupling;

import javax.swing.JMenuItem;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
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
