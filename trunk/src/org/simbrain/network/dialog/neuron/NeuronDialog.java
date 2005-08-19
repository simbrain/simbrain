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
package org.simbrain.network.dialog.neuron;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;

import org.simnet.interfaces.Neuron;
import org.simnet.neurons.*;


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
	private JTextField tfActivation = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	private JLabel upperLabel = new JLabel("Upper bound");
	private JLabel lowerLabel = new JLabel("Lower bound");

	private ArrayList neuron_list = new ArrayList(); // The neurons being modified
	private ArrayList selection_list; // The pnodes which refer to them
	
	private boolean neuronsHaveChanged = false;
	
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
	  * Get the logical neurons from the pnodeNeurons
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
	 	setTitle("Neuron Dialog");
	 	setLocation(500,0);

		initNeuronType();
		fillFieldValues();
		cbNeuronType.addActionListener(this);
		
		topPanel.addItem("Activation", tfActivation);
		topPanel.addItem("Increment", tfIncrement);
		String toolTipText = "<html>If text is grayed out, this field is only used for graphics purposes <p> (to determine what colors to use in rendering the neuron.</html>";
		upperLabel.setToolTipText(toolTipText);
		lowerLabel.setToolTipText(toolTipText);
		topPanel.addItemLabel(upperLabel, tfUpBound);
		topPanel.addItemLabel(lowerLabel, tfLowBound);
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
			neuronPanel = new ClampedNeuronPanel(); // Simply to serve as an empty panel
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
		} else if (neuron_ref instanceof ClampedNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(ClampedNeuron.getName()));
			neuronPanel = new ClampedNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		}  else if (neuron_ref instanceof StochasticNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(StochasticNeuron.getName()));
			neuronPanel = new StochasticNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		} else if (neuron_ref instanceof LogisticNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(LogisticNeuron.getName()));
			neuronPanel = new LogisticNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		} else if (neuron_ref instanceof IntegrateAndFireNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(IntegrateAndFireNeuron.getName()));
			neuronPanel = new IntegrateAndFireNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		} else if (neuron_ref instanceof SinusoidalNeuron) {
			cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(SinusoidalNeuron.getName()));
			neuronPanel = new SinusoidalNeuronPanel();
			neuronPanel.setNeuron_list(neuron_list);
			neuronPanel.fillFieldValues();
		}
	 }
	 
	 /**
	  * Change all the neurons from their current type to the new selected type
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
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(ClampedNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		ClampedNeuron b = new ClampedNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	}  else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(StochasticNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		StochasticNeuron b = new StochasticNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(LogisticNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		LogisticNeuron b = new LogisticNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(IntegrateAndFireNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		IntegrateAndFireNeuron b = new IntegrateAndFireNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	} else if(cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(SinusoidalNeuron.getName())) {
		 	for (int i = 0; i < neuron_list.size(); i++) {
		 		PNodeNeuron p = (PNodeNeuron)selection_list.get(i);
		 		SinusoidalNeuron b = new SinusoidalNeuron(p.getNeuron());
		 		p.changeNeuron(b);
		 	}	 		
	 	}
	 }
		
	 /**
	  * Respond to neuron type changes
	  */
	 public void actionPerformed(ActionEvent e) {

	 	neuronsHaveChanged = true;
	 	
	 	if(cbNeuronType.getSelectedItem().equals(StandardNeuron.getName())){
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new StandardNeuronPanel();
			neuronPanel.fillDefaultValues();
			mainPanel.add(neuronPanel);
			setBoundsEnabled(true);
	 	} else if (cbNeuronType.getSelectedItem().equals(BinaryNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new BinaryNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 		setBoundsEnabled(true);
	 	}  else if (cbNeuronType.getSelectedItem().equals(AdditiveNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new AdditiveNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 		setBoundsEnabled(false);
	 	} else if (cbNeuronType.getSelectedItem().equals(LinearNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new LinearNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 		setBoundsEnabled(false);
	 	}  else if (cbNeuronType.getSelectedItem().equals(SigmoidalNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new SigmoidalNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 		setBoundsEnabled(true);
	 	}  else if (cbNeuronType.getSelectedItem().equals(RandomNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new RandomNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 		setBoundsEnabled(true);
	 	}  else if (cbNeuronType.getSelectedItem().equals(ClampedNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new ClampedNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 		setBoundsEnabled(false);
	 	}  else if (cbNeuronType.getSelectedItem().equals(StochasticNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new StochasticNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 		setBoundsEnabled(true);
	 	}  else if (cbNeuronType.getSelectedItem().equals(LogisticNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new LogisticNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 		setBoundsEnabled(true);
	 	}  else if (cbNeuronType.getSelectedItem().equals(IntegrateAndFireNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new IntegrateAndFireNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 		setBoundsEnabled(false);
	 	}  else if (cbNeuronType.getSelectedItem().equals(SinusoidalNeuron.getName())) {
	 		mainPanel.remove(neuronPanel);
			neuronPanel = new SinusoidalNeuronPanel();
			neuronPanel.fillDefaultValues();
	 		mainPanel.add(neuronPanel);
	 		setBoundsEnabled(true	);
	 	}
	 	pack();
	 }
  
	 /**
	  * Set the initial values of dialog components
	  */
	 private void fillFieldValues() {
        Neuron neuron_ref = (Neuron) neuron_list.get(0);
        tfActivation.setText(Double.toString(neuron_ref.getActivation()));
        tfIncrement.setText(Double.toString(neuron_ref.getIncrement()));
		tfLowBound.setText(Double.toString(neuron_ref.getLowerBound()));
		tfUpBound.setText(Double.toString(neuron_ref.getUpperBound()));

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
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getLowerBound")) {
			tfLowBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getUpperBound")) {
			tfUpBound.setText(NULL_STRING);
		}
    }

	 /**
      * Called externally when the dialog is closed, to commit any changes made
      */
    public void commitChanges() {

    	    for (int i = 0; i < neuron_list.size(); i++) {
    	        Neuron neuron_ref = (Neuron) neuron_list.get(i);
    			if (tfActivation.getText().equals(NULL_STRING) == false) {
    				neuron_ref.setActivation(Double.parseDouble(tfActivation.getText()));
    			}
    			if (tfIncrement.getText().equals(NULL_STRING) == false) {
    				neuron_ref.setIncrement(Double.parseDouble(tfIncrement.getText()));
    			}    	    
    			if (tfUpBound.getText().equals(NULL_STRING) == false) {
    				neuron_ref.setUpperBound(Double
    						.parseDouble(tfUpBound.getText()));
    			}
    			if (tfLowBound.getText().equals(NULL_STRING) == false) {
    				neuron_ref.setLowerBound(Double.parseDouble(tfLowBound
    						.getText()));
    			}

    	    }
    	    if (neuronsHaveChanged) {
    		    changeNeurons();
    	    }
    	    
		setNeuronList();    	    	
		neuronPanel.setNeuron_list(neuron_list);
		neuronPanel.commitChanges();
    }

    /**
     * Used to set upper and lower bound text as 
     * "enabled" or not.  When disabled, those fields are
     * only used for graphical purposes, as described in the
     * tool tip
     */
    public void setBoundsEnabled(boolean val) {
   		upperLabel.setEnabled(val);
   		lowerLabel.setEnabled(val);
   	}
    		
}
