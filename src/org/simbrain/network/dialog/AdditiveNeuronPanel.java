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
import org.simnet.neurons.AdditiveNeuron;
import org.simnet.neurons.BinaryNeuron;

public class AdditiveNeuronPanel extends AbstractNeuronPanel {
	
	private JTextField tfActivation = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfLambda = new JTextField();
	private JTextField tfResistance = new JTextField();
	private JTextField tfTimeStep = new JTextField();
	
	public AdditiveNeuronPanel(){
		this.addItem("Activation", tfActivation);
		this.addItem("Increment", tfIncrement);	
		this.addItem("Lambda", tfLambda);
		this.addItem("Resistance", tfResistance);
		this.addItem("Time Step", tfTimeStep);
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		AdditiveNeuron neuron_ref = (AdditiveNeuron)neuron_list.get(0);
		
		tfActivation.setText(Double.toString(neuron_ref.getActivation()));
		tfIncrement.setText(Double.toString(neuron_ref.getIncrement()));
		tfLambda.setText(Double.toString(neuron_ref.getLambda()));
		tfResistance.setText(Double.toString(neuron_ref.getResistance()));
		tfTimeStep.setText(Double.toString(neuron_ref.getTimeStep()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getActivation")) {
			tfActivation.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getIncrement")) {
			tfIncrement.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getLambda")) {
			tfLambda.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getResistance")) {
			tfResistance.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, AdditiveNeuron.class, "getTimeStep")) {
			tfTimeStep.setText(NULL_STRING);
		}
	}
	
    /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {
   	
	for (int i = 0; i < neuron_list.size(); i++) {
		AdditiveNeuron neuron_ref = (AdditiveNeuron ) neuron_list.get(i);

		if (tfActivation.getText().equals(NULL_STRING) == false) {
			neuron_ref.setActivation(
				Double.parseDouble(tfActivation.getText()));
		}
		if (tfIncrement.getText().equals(NULL_STRING) == false) {
			neuron_ref.setIncrement(
				Double.parseDouble(tfIncrement.getText()));
		}
		if (tfLambda.getText().equals(NULL_STRING) == false) {
			neuron_ref.setLambda(
				Double.parseDouble(tfLambda.getText()));
		}
		if (tfResistance.getText().equals(NULL_STRING) == false) {
			neuron_ref.setResistance(
				Double.parseDouble(tfResistance.getText()));
		}
		if (tfTimeStep.getText().equals(NULL_STRING) == false) {
			neuron_ref.setTimeStep(
				Double.parseDouble(tfTimeStep.getText()));
		}
	}

   }

}
