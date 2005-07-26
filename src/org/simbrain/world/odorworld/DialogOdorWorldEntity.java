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

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.util.ComboBoxRenderer;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>DialogWorldEntity</b> displays the dialog box for settable values
 * of creatures and entities within a world environment.
 */
public class DialogOdorWorldEntity extends StandardDialog{

    private LabelledItemPanel topPanel = new LabelledItemPanel();
	private OdorWorldEntity entityRef = null;
	private Box mainPanel = Box.createVerticalBox();
	private JTabbedPane tabbedPane = new JTabbedPane();
	
	private JTextField tfEntityName = new JTextField();
	private JComboBox cbImageName = new JComboBox(OdorWorldEntity.imagesRenderer());
	private ComboBoxRenderer cbRenderer = new ComboBoxRenderer();
	
	public PanelStimulus stimPanel = null;
	public PanelAgent agentPanel = null;


	/**
	 * Create and show the world entity dialog box
	 * 
	 * @param we reference to the world entity whose smell signature is being adjusted
	 */
	public DialogOdorWorldEntity(OdorWorldEntity we) {

		entityRef = we;
		init();
		this.pack();
		this.setLocationRelativeTo(null);
	}

	/**
	 * Create and initialise instances of panel componets.
	 */
	private void init() {
		setTitle("Entity Dialog - " + entityRef.getName());
		
		this.fillFieldValues();
		
		topPanel.addItem("Entity", tfEntityName);
		topPanel.addItem("Image", cbImageName);
		

        cbRenderer.setPreferredSize(new Dimension(35, 35));
		cbImageName.setRenderer(cbRenderer);
	    
		if(entityRef instanceof OdorWorldAgent){
		    stimPanel = new PanelStimulus(entityRef);
		    agentPanel = new PanelAgent((OdorWorldAgent)entityRef);
		    stimPanel.getTabbedPane().addTab("Agent", agentPanel);
			mainPanel.add(topPanel);
			mainPanel.add(stimPanel);
			setContentPane(mainPanel);
		} else {
		    stimPanel = new PanelStimulus(entityRef);
		    mainPanel.add(topPanel);
		    mainPanel.add(stimPanel);
			setContentPane(mainPanel);
		}
	}
	
	private void fillFieldValues(){
	    tfEntityName.setText(entityRef.getName());
		cbImageName.setSelectedIndex(entityRef.getImageNameIndex(entityRef.getImageName()));
	}
	
	private void getChanges(){
	    entityRef.setName(tfEntityName.getText());
		entityRef.setImageName(cbImageName.getSelectedItem().toString());
	}
}
