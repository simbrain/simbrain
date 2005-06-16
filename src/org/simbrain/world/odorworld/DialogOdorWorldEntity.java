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

package org.simbrain.world.odorworld;

import javax.swing.Box;
import javax.swing.JTabbedPane;

import org.simbrain.util.StandardDialog;

/**
 * <b>DialogWorldEntity</b> displays the dialog box for settable values
 * of creatures and entities within a world environment.
 */
public class DialogOdorWorldEntity extends StandardDialog{

	private OdorWorldEntity entityRef = null;
	private Box mainPanel = Box.createVerticalBox();
	private JTabbedPane tabbedPane = new JTabbedPane();

	PanelStimulus stimPanel = null;
	PanelAgent agentPanel = null;


	/**
	 * Create and show the world entity dialog box
	 * 
	 * @param we reference to the world entity whose smell signature is being adjusted
	 */
	public DialogOdorWorldEntity(OdorWorldEntity we) {

		entityRef = we;
		init();
	}

	/**
	 * Create and initialise instances of panel componets.
	 */
	private void init() {
		setTitle("Entity Dialog");
		this.setLocation(600, 150);
		
	    stimPanel = new PanelStimulus(entityRef);
	    agentPanel = new PanelAgent((OdorWorldAgent)entityRef);
	    
		if(entityRef instanceof OdorWorldAgent){
			tabbedPane.addTab("Stimulus", stimPanel);
			tabbedPane.addTab("Agent", agentPanel);
			setContentPane(tabbedPane);
		} else {
		    mainPanel.add(stimPanel);
			setContentPane(mainPanel);
		}
	}
}
