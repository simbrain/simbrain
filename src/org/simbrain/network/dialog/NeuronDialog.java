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
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.interfaces.Neuron;
import org.simnet.neurons.AdditiveNeuron;
import org.simnet.neurons.BinaryNeuron;
import org.simnet.neurons.StandardNeuron;


/**
 * <b>DialogNetwork</b> is a dialog box for setting the properties of the 
 * Network GUI.
 */
public class NeuronDialog extends StandardDialog implements ActionListener {

	private Box mainPanel = Box.createVerticalBox();
	
	private LabelledItemPanel topPanel = new LabelledItemPanel();
	private AbstractNeuronPanel neuronPanel = new StandardNeuronPanel();	
	
	private JComboBox cbNeuronType = new JComboBox(Neuron.getTypeList());
	private JTextField tfNeuronName = new JTextField();

	private ArrayList neuron_list = new ArrayList(); // The neurons being modified
	private ArrayList selection_list; // The pnodes which refer to them
	
	/**
	  * This method is the default constructor.
	  */
	 public NeuronDialog(ArrayList selectedNeurons) 
	 {
	 	selection_list = selectedNeurons;
	 	setNeuronList();
	 	init();
	 }
	 
	 public void setNeuronList() {
	 	neuron_list.clear();
		Iterator i = selection_list.iterator();
	 	while(i.hasNext()) {
	 		PNodeNeuron n = (PNodeNeuron)i.next();
			neuron_list.add(n.getNeuron());
		}
	 }

	 /**
	  * Initialises the components on the panel.
	  */
	 private void init()
	 {
	    if (selection_list.size() == 1) {
	        setTitle("Neuron Dialog - " + ((PNodeNeuron)selection_list.get (0)).getName());
	    } else {
	        setTitle("Neuron Dialog");
	    }
		this.setLocation(500, 0); //Sets location of network dialog		

		neuronPanel.setNeuron_list(neuron_list);
		neuronPanel.fillFieldValues();
		if(selection_list.size() == 1){
			tfNeuronName.setText(((PNodeNeuron)selection_list.get(0)).getName());
		} else {
		    tfNeuronName.setText("...");
		    tfNeuronName.setEditable(false);
		}
		
		initNeuronType();
		cbNeuronType.addActionListener(this);
		topPanel.addItem("Neuron Name", tfNeuronName);
		topPanel.addItem("Neuron type", cbNeuronType);
		mainPanel.add(topPanel);
		mainPanel.add(neuronPanel);
		setContentPane(mainPanel);

	 }
	 
	 
	 /**
	  * Initialize the main neuron panel based on the type of the selected neurons
	  */
	 public void initNeuronType() {
	 	Neuron neuron_ref = (Neuron)neuron_list.get(0);
	 	
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getType")) {
			cbNeuronType.addItem(AbstractNeuronPanel.NULL_STRING);
			cbNeuronType.setSelectedIndex(Neuron.getTypeList().length);
			neuronPanel = new MixedNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		} else if (neuron_ref instanceof StandardNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(StandardNeuron.getName()));
			neuronPanel = new StandardNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		} else if (neuron_ref instanceof BinaryNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(BinaryNeuron.getName()));
			neuronPanel = new BinaryNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		} else if (neuron_ref instanceof AdditiveNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(AdditiveNeuron.getName()));
			neuronPanel = new AdditiveNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		}
	 }
	 
	 /**
	  * Change all the neurons from their current type  to the new selected type
	  */
	 public void changeNeurons() {
	 	if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(StandardNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		StandardNeuron b = new StandardNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(BinaryNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		BinaryNeuron b = new BinaryNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(AdditiveNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		AdditiveNeuron b = new AdditiveNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	} 
	 }
		
	 /**
	  * Respond to neuron type changes
	  */
	 public void actionPerformed(ActionEvent e) {
	 	if(cbNeuronType.getSelectedItem().equals(StandardNeuron.getName())){
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new StandardNeuronPanel();
			changeNeurons(); 
			setNeuronList();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
			mainPanel.add(neuronPanel);
	 	} else if (cbNeuronType.getSelectedItem().equals(BinaryNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new BinaryNeuronPanel();
			changeNeurons();
			setNeuronList();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
	 		mainPanel.add(neuronPanel);
	 	}  else if (cbNeuronType.getSelectedItem().equals(AdditiveNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new AdditiveNeuronPanel();
			changeNeurons();
			setNeuronList();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
	 		mainPanel.add(neuronPanel);
	 	} //Something different for mixed panel... 
	 	pack();
	 }
  
	 
	 /**
	  * Called externally when the dialog is closed,
	  * to commit any changes made
	  */
	 public void commmitChanges() {
		 if (selection_list.size() == 1) {
		     ((PNodeNeuron)selection_list.get(0)).setName(tfNeuronName.getText());
		 }
	 	 neuronPanel.commitChanges();
	 }

}
