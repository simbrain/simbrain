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

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.interfaces.ActivationRule;
import org.simnet.interfaces.Neuron;
import org.simnet.neurons.StandardNeuron;

public class StandardNeuronPanel extends AbstractNeuronPanel {
	
	private JTextField tfActivation = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfDecay = new JTextField();
	private JTextField tfBias = new JTextField();
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	private JComboBox cbActivationRule = new JComboBox(ActivationRule.getList());
	
	public StandardNeuronPanel(){
		this.addItem("Activation", tfActivation);
		this.addItem("Activation Function", cbActivationRule);
		this.addItem("Upper bound", tfUpBound);
		this.addItem("Lower bound", tfLowBound);
		this.addItem("Increment", tfIncrement);
		this.addItem("Bias", tfBias);
		this.addItem("Decay", tfDecay);		
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		Neuron neuron_ref = (Neuron)neuron_list.get(0);
		
		tfActivation.setText(Double.toString(neuron_ref.getActivation()));
		cbActivationRule.setSelectedIndex(ActivationRule.getActivationFunctionIndex(neuron_ref.getActivationFunction().getName()));
		tfLowBound.setText(Double.toString(neuron_ref.getLowerBound()));
		tfUpBound.setText(Double.toString(neuron_ref.getUpperBound()));
		tfIncrement.setText(Double.toString(neuron_ref.getIncrement()));
		tfBias.setText(Double.toString(neuron_ref.getBias()));
		tfDecay.setText(Double.toString(neuron_ref.getDecay()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getActivation")) {
			tfActivation.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getActivationFunctionS")) {
			cbActivationRule.addItem(NULL_STRING);
			cbActivationRule.setSelectedIndex(ActivationRule.getList().length);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getLowerBound")) {
			tfLowBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getUpperBound")) {
			tfUpBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getIncrement")) {
			tfIncrement.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getBias")) {
			tfBias.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, StandardNeuron.class, "getDecay")) {
			tfDecay.setText(NULL_STRING);
		}	

	}
	

    /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {
   	
	for (int i = 0; i < neuron_list.size(); i++) {
		Neuron neuron_ref = (Neuron) neuron_list.get(i);

		if (tfActivation.getText().equals(NULL_STRING) == false) {
			neuron_ref.setActivation(
				Double.parseDouble(tfActivation.getText()));
		}
		if (cbActivationRule.getSelectedItem().equals(NULL_STRING)== false) {
			neuron_ref.setActivationFunction(ActivationRule.getActivationFunction(cbActivationRule.getSelectedItem().toString()));
		}
		if (tfUpBound.getText().equals(NULL_STRING) == false) {
			neuron_ref.setUpperBound(
				Double.parseDouble(tfUpBound.getText()));
		}
		if (tfLowBound.getText().equals(NULL_STRING) == false) {
			neuron_ref.setLowerBound(
				Double.parseDouble(tfLowBound.getText()));
		}
		if (tfIncrement.getText().equals(NULL_STRING) == false) {
			neuron_ref.setIncrement(
				Double.parseDouble(tfIncrement.getText()));
		}
		if (tfDecay.getText().equals(NULL_STRING) == false) {
			neuron_ref.setDecay(
				Double.parseDouble(tfDecay.getText()));
		}
		if (tfBias.getText().equals(NULL_STRING) == false) {
			neuron_ref.setBias(Double.parseDouble(tfBias.getText()));
		}
	   	
	}

   }

}
