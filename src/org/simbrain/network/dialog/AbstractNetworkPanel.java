package org.simbrain.network.dialog;


import org.simbrain.util.LabelledItemPanel;

public abstract class AbstractNetworkPanel extends LabelledItemPanel {


	/**
	 * Populate fields with current data
	 */
	public abstract void fillFieldValues();

	 /**
	  * Called externally when the dialog is closed,
	  * to commit any changes made
	  */
	public abstract void commitChanges();


}