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
package org.simbrain.network.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JComboBox;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.pnodes.PNodeWeight;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.interfaces.Synapse;
import org.simnet.synapses.Hebbian;
import org.simnet.synapses.StandardSynapse;

/**
 * <b>DialogNetwork</b> is a dialog box for setting the properties of the 
 * Network GUI.
 */
public class SynapseDialog extends StandardDialog implements ActionListener {

	private Box mainPanel = Box.createVerticalBox();
	
	private LabelledItemPanel topPanel = new LabelledItemPanel();
	private AbstractSynapsePanel synapsePanel = new StandardSynapsePanel();	

	private JComboBox cbSynapseType = new JComboBox(Synapse.getTypeList());

	private ArrayList synapse_list = new ArrayList(); // The synapses being modified
	private ArrayList selection_list; // The pnodes which refer to them
	
	/**
	  * This method is the default constructor.
	  */
	 public SynapseDialog(ArrayList selectedSynapses) 
	 {
	 	selection_list = selectedSynapses;
	 	setSynapseList();
	 	init();
	 }
	 

	 public void setSynapseList() {
	 	synapse_list.clear();
		Iterator i = selection_list.iterator();
	 	while(i.hasNext()) {
	 		PNodeWeight w = (PNodeWeight)i.next();
			synapse_list.add(w.getWeight());
		}
	 }
	 

	 /**
	  * Initialises the components on the panel.
	  */
	 private void init()
	 {
		setTitle("synapse Dialog");
		this.setLocation(500, 0); //Sets location of network dialog		

		synapsePanel.setSynapse_list(synapse_list);
		synapsePanel.fillFieldValues();
		
		initsynapseType();
		cbSynapseType.addActionListener(this);
		topPanel.addItem("synapse type", cbSynapseType);

		mainPanel.add(topPanel);
		mainPanel.add(synapsePanel);
		setContentPane(mainPanel);

	 }
	 
	 /**
	  * Initialize the main synapse panel based on the type of the selected synapses
	  */
	 public void initsynapseType() {
	 	Synapse synapse_ref = (Synapse)synapse_list.get(0);
	 	
		if(!NetworkUtils.isConsistent(synapse_list, Synapse.class, "getType")) {
			cbSynapseType.addItem(AbstractSynapsePanel.NULL_STRING);
			cbSynapseType.setSelectedIndex(Synapse.getTypeList().length);
			synapsePanel = new MixedSynapsePanel();
			synapsePanel.setSynapse_list(synapse_list);
			synapsePanel.fillFieldValues();
		} else if (synapse_ref instanceof StandardSynapse) {
			cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(StandardSynapse.getName()));
			synapsePanel = new StandardSynapsePanel();
			synapsePanel.setSynapse_list(synapse_list);
			synapsePanel.fillFieldValues();
		} else if (synapse_ref instanceof Hebbian) {
			cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(Hebbian.getName()));
			synapsePanel = new HebbianSynapsePanel();
			synapsePanel.setSynapse_list(synapse_list);
			synapsePanel.fillFieldValues();
		} 
	 }
	 
	 /**
	  * Change all the synapses from their current type  to the new selected type
	  */
	 public void changesynapses() {
	 	if(cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(StandardSynapse.getName())) {
		 	for (int i = 0; i < synapse_list.size(); i++) {
		 		PNodeWeight p = (PNodeWeight)selection_list.get(i);
		 		StandardSynapse s = new StandardSynapse(p.getWeight());
		 		p.changeWeight(s);
		 	}	 		
	 	} else if(cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(Hebbian.getName())) {
		 	for (int i = 0; i < synapse_list.size(); i++) {
		 		PNodeWeight p = (PNodeWeight)selection_list.get(i);
		 		Hebbian s = new Hebbian(p.getWeight());
		 		p.changeWeight(s);
		 	}	 		
	 	} 
	 }
		
	 /**
	  * Respond to synapse type changes
	  */
	 public void actionPerformed(ActionEvent e) {
	 	if(cbSynapseType.getSelectedItem().equals(StandardSynapse.getName())){
	 		mainPanel.remove(synapsePanel);
			synapsePanel = new StandardSynapsePanel();
			changesynapses(); 
			setSynapseList();
			synapsePanel.setSynapse_list(synapse_list);
			synapsePanel.fillFieldValues();
			mainPanel.add(synapsePanel);
	 	} else if (cbSynapseType.getSelectedItem().equals(Hebbian.getName())) {
	 		mainPanel.remove(synapsePanel);
			synapsePanel = new HebbianSynapsePanel();
			changesynapses();
			setSynapseList();
			synapsePanel.setSynapse_list(synapse_list);
			synapsePanel.fillFieldValues();
	 		mainPanel.add(synapsePanel);
	 	} //Something different for mixed panel... 
	 	pack();
	 }
  
	 
	 /**
	  * Called externally when the dialog is closed,
	  * to commit any changes made
	  */
	 public void commmitChanges() {
	 	synapsePanel.commitChanges();
	 }

}
