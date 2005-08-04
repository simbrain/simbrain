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
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;

import org.simnet.interfaces.Neuron;
import org.simnet.neurons.AdditiveNeuron;
import org.simnet.neurons.BinaryNeuron;
import org.simnet.neurons.ClampedNeuron;
import org.simnet.neurons.LinearNeuron;
import org.simnet.neurons.PassiveNeuron;
import org.simnet.neurons.PiecewiseLinearNeuron;
import org.simnet.neurons.RandomNeuron;
import org.simnet.neurons.SigmoidalNeuron;
import org.simnet.neurons.StandardNeuron;


/**
 * <b>DialogNetwork</b> is a dialog box for setting the properties of the 
 * Network GUI.
 */
public class NeuronDialog extends StandardDialog implements ActionListener {

    public static final String NULL_STRING = "...";
    
	private Box mainPanel = Box.createVerticalBox();
	
	private LabelledItemPanel topPanel = new LabelledItemPanel();
	private AbstractNeuronPanel neuronPanel;	
	
	private JComboBox cbNeuronType = new JComboBox(Neuron.getTypeList());
	private JTextField tfNeuronName = new JTextField();
	private JTextField tfActivation = new JTextField();
	private JTextField tfIncrement = new JTextField();

	private ArrayList neuron_list = new ArrayList(); // The neurons being modified
	private ArrayList selection_list; // The pnodes which refer to them
	
	/**
	 * 
	 * @param selectedNeurons the pnode_neurons being adjusted
	 */
	 public NeuronDialog(ArrayList selectedNeurons) 
	 {
	 	selection_list = selectedNeurons;
	 	setNeuronList();
	 	init();
	 }
	 
	 /**
	  * Get the logic neurons from the pnodeNeurons
	  */
	 private void setNeuronList() {
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

		
		// Set name of dialog
		if(selection_list.size() == 1){
			tfNeuronName.setText(((PNodeNeuron)selection_list.get(0)).getName());
			topPanel.addItem("Neuron Name", tfNeuronName);
		} else {
		    tfNeuronName.setText("...");
		    tfNeuronName.setEditable(false);
		}

		initNeuronType();
		neuronPanel.setNeuron_list(neuron_list);
		fillFieldValues();
		cbNeuronType.addActionListener(this);
		topPanel.addItem("Activation", tfActivation);
		topPanel.addItem("Increment", tfIncrement);
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
		} else if (neuron_ref instanceof LinearNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(LinearNeuron.getName()));
			neuronPanel = new LinearNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		} else if (neuron_ref instanceof PiecewiseLinearNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(PiecewiseLinearNeuron.getName()));
			neuronPanel = new PiecewiseLinearNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		} else if (neuron_ref instanceof SigmoidalNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(SigmoidalNeuron.getName()));
			neuronPanel = new SigmoidalNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		} else if (neuron_ref instanceof RandomNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(RandomNeuron.getName()));
			neuronPanel = new RandomNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		} else if (neuron_ref instanceof PassiveNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(PassiveNeuron.getName()));
			neuronPanel = new PassiveNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		} else if (neuron_ref instanceof ClampedNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(ClampedNeuron.getName()));
			neuronPanel = new ClampedNeuronPanel();
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
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(LinearNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		LinearNeuron b = new LinearNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(PiecewiseLinearNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		PiecewiseLinearNeuron b = new PiecewiseLinearNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(SigmoidalNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		SigmoidalNeuron b = new SigmoidalNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(RandomNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		RandomNeuron b = new RandomNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(PassiveNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		PassiveNeuron b = new PassiveNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(ClampedNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		ClampedNeuron b = new ClampedNeuron(p.getNeuron());
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
			neuronPanel.fillDefaultValues();
			mainPanel.add(neuronPanel);
	 	} else if (cbNeuronType.getSelectedItem().equals(BinaryNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new BinaryNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 	}  else if (cbNeuronType.getSelectedItem().equals(AdditiveNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new AdditiveNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 	} else if (cbNeuronType.getSelectedItem().equals(LinearNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new LinearNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 	}  else if (cbNeuronType.getSelectedItem().equals(PiecewiseLinearNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new PiecewiseLinearNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 	}  else if (cbNeuronType.getSelectedItem().equals(SigmoidalNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new SigmoidalNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 	}  else if (cbNeuronType.getSelectedItem().equals(RandomNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new RandomNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 	}  else if (cbNeuronType.getSelectedItem().equals(PassiveNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new PassiveNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 	}  else if (cbNeuronType.getSelectedItem().equals(ClampedNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new ClampedNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 	}
	 	//Something different for mixed panel... 
	 	pack();
	 }
  
	 /**
	  * Set the initial values of dialog components
	  */
	 private void fillFieldValues() {
        Neuron neuron_ref = (Neuron) neuron_list.get(0);
        tfActivation.setText(Double.toString(neuron_ref.getActivation()));
        tfIncrement.setText(Double.toString(neuron_ref.getIncrement()));
        neuronPanel.fillFieldValues();

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuron_list, Neuron.class,
                "getActivation")) {
            tfActivation.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuron_list, Neuron.class,
                "getIncrement")) {
            tfIncrement.setText(NULL_STRING);
        }
    }

	 /**
	  * Set the neuron name.  Only used for the case of a single node.
	  *
	  */
	 private void setNeuronName() {
		if (selection_list.size() == 1) {
			String theName = tfNeuronName.getText();
			PNodeNeuron theNode = (PNodeNeuron) selection_list.get(0);
			if (theNode.getName().equals(theName) == false) {
				if (Utils.containsName(theNode.parentPanel.getNeuronNames(),theName) == false) {
					theNode.setName(theName);
				} else {
					JOptionPane.showMessageDialog(null, "The name \"" + theName
							+ "\" already exists.", "Warning",
							JOptionPane.ERROR_MESSAGE);

				}
			}
		}

	 }
	 
	 /**
      * Called externally when the dialog is closed, to commit any changes made
      */
    public void commmitChanges() {

    	    setNeuronName();
    	    
    	    for (int i = 0; i < neuron_list.size(); i++) {
    	        Neuron neuron_ref = (Neuron) neuron_list.get(i);
    			if (tfActivation.getText().equals(NULL_STRING) == false) {
    				neuron_ref.setActivation(Double.parseDouble(tfActivation.getText()));
    			}
    			if (tfIncrement.getText().equals(NULL_STRING) == false) {
    				neuron_ref.setIncrement(Double.parseDouble(tfIncrement.getText()));
    			}    	    	
    	    }
	    changeNeurons();
	    setNeuronList();
		neuronPanel.setNeuron_list(neuron_list);
		neuronPanel.commitChanges();
    }

}
