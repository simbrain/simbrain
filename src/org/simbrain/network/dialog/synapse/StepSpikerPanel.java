/*
 * Created on Aug 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.simbrain.network.dialog.synapse;

import java.util.ArrayList;

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.interfaces.SpikeResponse;
import org.simnet.synapses.Hebbian;
import org.simnet.synapses.spikeresponders.Step;

/**
 * @author jyoshimi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class StepSpikerPanel extends AbstractSpikeResponsePanel {
	
    public static final String NULL_STRING = "...";

	private JTextField tfResponseHeight = new JTextField();
	private JTextField tfResponseTime = new JTextField();
		
	public StepSpikerPanel(){
		this.addItem("Response height", tfResponseHeight);
		this.addItem("Response time", tfResponseTime);
		fillDefaultValues();
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		
		Step spikeResponder = (Step)spikeResponderList.get(0);
		
		tfResponseHeight.setText(Double.toString(spikeResponder.getResponseHeight()));
		tfResponseTime.setText(Double.toString(spikeResponder.getResponseTime()));
		
		//Handle consistency of multiply selections
		if(!NetworkUtils.isConsistent(spikeResponderList, Step.class, "getResponseHeight")) {
			tfResponseHeight.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(spikeResponderList, Step.class, "getResponseTime")) {
			tfResponseTime.setText(NULL_STRING);
		}

	}
	
	/**
	 * Fill field values to default values for this synapse type
	 */
	public void fillDefaultValues() {
		Step spiker_ref = new Step();
		tfResponseHeight.setText(Double.toString(spiker_ref.getResponseHeight()));
		tfResponseTime.setText(Double.toString(spiker_ref.getResponseTime()));
	}

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {

    		for (int i = 0; i < spikeResponderList.size(); i++) {
             Step step_ref = (Step) spikeResponderList.get(i);
            if (tfResponseHeight.getText().equals(NULL_STRING) == false) {
                step_ref.setResponseHeight(Double.parseDouble(tfResponseHeight.getText()));
            }
            if (tfResponseTime.getText().equals(NULL_STRING) == false) {
                step_ref.setResponseTime(Double.parseDouble(tfResponseTime.getText()));
            }

        }
    }

}
