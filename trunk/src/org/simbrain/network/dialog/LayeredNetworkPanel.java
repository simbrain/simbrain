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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

public class LayeredNetworkPanel extends AbstractNetworkPanel {
	
	private JTextField layersNumber = new JTextField();
	private JTextField neuronsPerLayer = new JTextField();
	private String[] orientList = {"Left-to-Right", "Right-to-Left", "Top-to-Bottom", "Bottom-to-Top"};
	private JComboBox orientation = new JComboBox(orientList);
	private JTextField positiveWeightFactor = new JTextField();
	private JTextField negativeWeightFactor = new JTextField();
	private JCheckBox selfConnections = new JCheckBox();
	
	public LayeredNetworkPanel(){
		this.addItem("Number of layers", layersNumber);
		this.addItem("Number of neurons per layer", neuronsPerLayer);
		this.addItem("Orientation", orientation);
		this.addItem("Positive weight factor", positiveWeightFactor);
		this.addItem("Negative weight factor", negativeWeightFactor);
		this.addItem("Allow self connections", selfConnections);
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
