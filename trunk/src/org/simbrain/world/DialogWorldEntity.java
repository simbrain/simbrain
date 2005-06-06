/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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

package org.simbrain.world;

import javax.swing.Box;
import javax.swing.JTabbedPane;

import org.simbrain.util.StandardDialog;

/**
 * <b>WorldEntityDialog</b> is a small dialog box used to adjust the "smell signatures" 
 * (arrays of doubles representing the effect an object has on the input nodes
 * of the network) and "detectors" of non-creature and creature entities in the world.
 */
public class DialogWorldEntity extends StandardDialog{

	private WorldEntity entityRef = null;
	private Box mainPanel = Box.createVerticalBox();
	private JTabbedPane tabbedPane = new JTabbedPane();

	PanelStimulus stimPanel = null;
	PanelAgent agentPanel = null;


	/**
	 * Create and show the world entity dialog box
	 * 
	 * @param we reference to the world entity whose smell signature is being adjusted
	 */
	public DialogWorldEntity(WorldEntity we) {

		entityRef = we;
		init();
	}

	/**
	 * This method creates tabs and adds them to the doalog.
	 */
	private void init() {
		setTitle("Entity Dialog");
		this.setLocation(600, 150);
	    
		if(entityRef instanceof Agent){
		    stimPanel = new PanelStimulus(entityRef);
		    agentPanel = new PanelAgent((Agent)entityRef);
			tabbedPane.addTab("Stimulus", stimPanel);
			tabbedPane.addTab("Agent", agentPanel);
			setContentPane(tabbedPane);
		} else {
		    stimPanel = new PanelStimulus(entityRef);
		    mainPanel.add(stimPanel);
			setContentPane(mainPanel);
		}
	}
}
