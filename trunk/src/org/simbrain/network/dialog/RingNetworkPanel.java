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

import javax.swing.JTextField;

public class RingNetworkPanel extends AbstractNetworkPanel {
	
	private JTextField tfActivation = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	
	public RingNetworkPanel(){
		this.addItem("Activation", tfActivation);
		this.addItem("Increment", tfIncrement);	
		this.addItem("Upper", tfUpBound);
		this.addItem("Lower", tfLowBound);
	}
	
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {

	}
	
    /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {

   }

}
