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

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.simbrain.util.ComboBoxRenderer;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;

/**
 * <b>DialogWorldEntity</b> displays the dialog box for settable values
 * of creatures and entities within a world environment.
 */
public class DialogOdorWorldEntity extends StandardDialog implements ActionListener{

    private LabelledItemPanel topPanel = new LabelledItemPanel();
	private OdorWorldEntity entityRef = null;
	private Box mainPanel = Box.createVerticalBox();
	private JTextField tfEntityName = new JTextField();
	private JComboBox cbImageName = new JComboBox(OdorWorldEntity.imagesRenderer());
	private ComboBoxRenderer cbRenderer = new ComboBoxRenderer();
	
	public PanelStimulus stimPanel = null;
	public PanelAgent agentPanel = null;

	private LabelledItemPanel miscPanel = new LabelledItemPanel();
	private JTextField bitesToDie = new JTextField();
	private JCheckBox edible = new JCheckBox();
	private JSlider resurrectionProb = new JSlider();


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
		
		this.fillFieldValues();
		
		topPanel.addItem("Image", cbImageName);

		bitesToDie.setColumns(2);
		edible.addActionListener(this);
		resurrectionProb.setMajorTickSpacing(25);
		resurrectionProb.setPaintLabels(true);
		resurrectionProb.setPaintTicks(true);
		resurrectionProb.setMaximum(100);
		resurrectionProb.setMinimum(0);


        cbRenderer.setPreferredSize(new Dimension(35, 35));
		cbImageName.setRenderer(cbRenderer);
	    
		if(entityRef instanceof OdorWorldAgent){
			setTitle("Entity Dialog - " + entityRef.getName());
			topPanel.addItem("Entity", tfEntityName);
		    stimPanel = new PanelStimulus(entityRef);
		    agentPanel = new PanelAgent((OdorWorldAgent)entityRef);
		    stimPanel.getTabbedPane().addTab("Agent", agentPanel);
			mainPanel.add(topPanel);
			mainPanel.add(stimPanel);
			setContentPane(mainPanel);
		} else {
			setTitle("Entity Dialog");
		    stimPanel = new PanelStimulus(entityRef);
		    mainPanel.add(topPanel);
		    mainPanel.add(stimPanel);
			setContentPane(mainPanel);
		}
		
		miscPanel.addItem("Edible",edible);
		miscPanel.addItem("Bites to die",bitesToDie);
		miscPanel.addItem("Resurrection Probability", resurrectionProb);
		stimPanel.getTabbedPane().addTab("Miscellaneous",miscPanel);
	}
	
	private void fillFieldValues(){
	    tfEntityName.setText(entityRef.getName());
		cbImageName.setSelectedIndex(entityRef.getImageNameIndex(entityRef.getImageName()));
		edible.setSelected(entityRef.getEdible());
		bitesToDie.setText((new Integer(entityRef.getBitesToDie())).toString());
		bitesToDie.setEnabled(entityRef.getEdible());
		resurrectionProb.setValue(entityRef.getResurrectionProb());
	}
	
	public void commitChanges(){

		entityRef.setEdible(edible.isSelected());
		if(!edible.isSelected())
			entityRef.setBites(0);
		entityRef.setBitesToDie(Integer.parseInt(bitesToDie.getText()));
		entityRef.setResurrectionProb(resurrectionProb.getValue());

		
		if(entityRef.getName().equals(tfEntityName.getText()) == false) {
			if (Utils.containsName(entityRef.getParent().getEntityNames(), tfEntityName.getText()) == false) {
			    entityRef.setName(tfEntityName.getText());	
				ArrayList a = new ArrayList();
				a.add(entityRef);
			    entityRef.parent.getParentWorkspace().removeAgentsFromCouplings(a);
			    entityRef.parent.getParentWorkspace().attachAgentsToCouplings();
			    entityRef.parent.getParentWorkspace().resetCommandTargets();
			} else {
				JOptionPane.showMessageDialog(null, "The name \"" + tfEntityName.getText() + "\" already exists.", "Warning",
			            JOptionPane.ERROR_MESSAGE);		
					
			}		
		}
		entityRef.setImageName(cbImageName.getSelectedItem().toString());
	}
	
	/**
	 * Respond to button pressing events
	 */
	public void actionPerformed(ActionEvent e) {

		Object o = e.getSource();

		if (o == edible){
			bitesToDie.setEnabled(edible.isSelected());
		}

	}

}
