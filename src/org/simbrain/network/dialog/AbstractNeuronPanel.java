package org.simbrain.network.dialog;

import java.util.ArrayList;

import org.simbrain.util.LabelledItemPanel;

public abstract class AbstractNeuronPanel extends LabelledItemPanel {
	public static final String NULL_STRING = "...";

	protected ArrayList neuron_list; // The neurons being modified

	/**
	 * Populate fields with current data
	 */
	public abstract void fillFieldValues();

	 /**
	  * Called externally when the dialog is closed,
	  * to commit any changes made
	  */
	public abstract void commitChanges();

	/**
	 * @return Returns the neuron_list.
	 */
	public ArrayList getNeuron_list() {
		return neuron_list;
	}
	
	/**
	 * @param neuron_list
	 *            The neuron_list to set.
	 */
	public void setNeuron_list(ArrayList neuron_list) {
		this.neuron_list = neuron_list;
	}

}