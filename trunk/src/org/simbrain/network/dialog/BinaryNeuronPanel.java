/*
 * Created on Oct 5, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author Kyle Baron
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

package org.simbrain.network.dialog;

import javax.swing.*;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.LabelledItemPanel;
import org.simnet.interfaces.ActivationRule;
import org.simnet.interfaces.Neuron;
import org.simnet.neurons.BinaryNeuron;

public class BinaryNeuronPanel extends AbstractNeuronPanel {
	
	private JTextField tfActivation = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	private JTextField tfThreshold = new JTextField();
	
	public BinaryNeuronPanel(){
		this.addItem("Activation", tfActivation);
		this.addItem("Increment", tfIncrement);	
		this.addItem("Upper", tfUpBound);
		this.addItem("Lower", tfLowBound);
		this.addItem("Threshold", tfThreshold);
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		BinaryNeuron neuron_ref = (BinaryNeuron)neuron_list.get(0);
		
		tfActivation.setText(Double.toString(neuron_ref.getActivation()));
		tfLowBound.setText(Double.toString(neuron_ref.getLowerBound()));
		tfUpBound.setText(Double.toString(neuron_ref.getUpperBound()));
		tfIncrement.setText(Double.toString(neuron_ref.getIncrement()));
		tfThreshold.setText(Double.toString(neuron_ref.getThreshold()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "getActivation")) {
			tfActivation.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "getLowerBound")) {
			tfLowBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "getUpperBound")) {
			tfUpBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "getIncrement")) {
			tfIncrement.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, BinaryNeuron.class, "getThreshold")) {
			tfThreshold.setText(NULL_STRING);
		}
	}
	
    /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {
   	
	for (int i = 0; i < neuron_list.size(); i++) {
		BinaryNeuron neuron_ref = (BinaryNeuron) neuron_list.get(i);

		if (tfActivation.getText().equals(NULL_STRING) == false) {
			neuron_ref.setActivation(
				Double.parseDouble(tfActivation.getText()));
		}
		if (tfUpBound.getText().equals(NULL_STRING) == false) {
			neuron_ref.setUpperBound(
				Double.parseDouble(tfUpBound.getText()));
		}
		if (tfLowBound.getText().equals(NULL_STRING) == false) {
			neuron_ref.setLowerBound(
				Double.parseDouble(tfLowBound.getText()));
		}
		if (tfThreshold.getText().equals(NULL_STRING) == false) {
			neuron_ref.setThreshold(
				Double.parseDouble(tfThreshold.getText()));
		}
		if (tfIncrement.getText().equals(NULL_STRING) == false) {
			neuron_ref.setIncrement(
				Double.parseDouble(tfIncrement.getText()));
		}
	}

   }

}
