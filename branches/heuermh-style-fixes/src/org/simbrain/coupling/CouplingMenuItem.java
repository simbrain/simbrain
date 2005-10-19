/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
