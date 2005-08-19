/*
 * Created on Aug 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.simbrain.network.dialog.synapse;

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
	
	private JTextField tfResponseHeight = new JTextField();
	private JTextField tfResponseTime = new JTextField();
	
	private Step spiker_ref;
	
	public StepSpikerPanel(){
		this.addItem("Response height", tfResponseHeight);
		this.addItem("Response time", tfResponseTime);
		fillDefaultValues();
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		
//		synapse_ref = (Hebbian)synapse_list.get(0);
//		
//		tfMomentum.setText(Double.toString(synapse_ref.getMomentum()));
//		
//		//Handle consistency of multiply selections
//		if(!NetworkUtils.isConsistent(synapse_list, Hebbian.class, "getMomentum")) {
//			tfMomentum.setText(NULL_STRING);
//		}
//
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

//        for (int i = 0; i < synapse_list.size(); i++) {
//            Hebbian synapse_ref = (Hebbian) synapse_list.get(i);
//
//            if (tfMomentum.getText().equals(NULL_STRING) == false) {
//                synapse_ref.setMomentum(Double
//                        .parseDouble(tfMomentum.getText()));
//            }
//        }
    }

}
