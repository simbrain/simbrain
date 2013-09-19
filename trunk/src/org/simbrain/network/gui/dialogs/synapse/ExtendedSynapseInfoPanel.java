package org.simbrain.network.gui.dialogs.synapse;

import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkUtils;

/**
 * 
 * @author ztosi
 * 
 */
public class ExtendedSynapseInfoPanel extends JPanel {

	/** Null string. */
	public static final String NULL_STRING = "...";

	/** Increment field. */
	private JTextField tfIncrement = new JTextField();

	/** Upper bound field. */
	private JTextField tfUpBound = new JTextField();

	/** Lower bound field. */
	private JTextField tfLowBound = new JTextField();

	/** Delay field. */
	private JTextField tfDelay = new JTextField();

	/** The synapses being modified. */
	private List<Synapse> synapseList;

	/**
	 * 
	 * @param synapseList
	 */
	public ExtendedSynapseInfoPanel(List<Synapse> synapseList) {
		this.synapseList = synapseList;
		fillFieldValues();
		initializeLayout();
	}

	/**
	 * Lays out the panel
	 */
	private void initializeLayout() {
		GridLayout gl = new GridLayout(0, 2);
		gl.setVgap(5);
		setLayout(gl);
		add(new JLabel("Upper Bound:"));
		add(tfUpBound);
		add(new JLabel("Lower Bound"));
		add(tfLowBound);
		add(new JLabel("Increment:"));
		add(tfIncrement);
		add(new JLabel("Delay:"));
		add(tfDelay);
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	}

	/**
	 * Fills the values of the text fields based on the corresponding values of
	 * the synapses to be edited.
	 */
	public void fillFieldValues() {

		Synapse synapseRef = synapseList.get(0);

		// Handle Upper Bound
		if (!NetworkUtils.isConsistent(synapseList, Synapse.class,
				"getUpperBound"))
			tfUpBound.setText(NULL_STRING);
		else
			tfUpBound
					.setText(Double.toString(synapseRef.getUpperBound()));

		// Handle Lower Bound
		if (!NetworkUtils.isConsistent(synapseList, Synapse.class,
				"getLowerBound"))
			tfLowBound.setText(NULL_STRING);
		else
			tfLowBound
					.setText(Double.toString(synapseRef.getLowerBound()));

		// Handle Increment
		if (!NetworkUtils.isConsistent(synapseList, Synapse.class,
				"getIncrement"))
			tfIncrement.setText(NULL_STRING);
		else
			tfIncrement
					.setText(Double.toString(synapseRef.getIncrement()));

		// Handle Delay
		if (!NetworkUtils.isConsistent(synapseList, Synapse.class,
				"getDelay"))
			tfDelay.setText(NULL_STRING);
		else
			tfDelay.setText(Integer.toString(synapseRef.getDelay()));

	}

	/**
	 * Uses the values from text fields to alter corresponding values in the
	 * synapse(s) being edited. Called externally to apply changes.
	 */
	public void commitChanges() {
		for (int i = 0; i < synapseList.size(); i++) {

			Synapse synapseRef = synapseList.get(i);

			// Upper Bound
			if (!tfUpBound.getText().equals(NULL_STRING))
				synapseRef.setUpperBound(Double.parseDouble(tfUpBound
						.getText()));

			// Lower Bound
			if (!tfLowBound.getText().equals(NULL_STRING))
				synapseRef.setLowerBound(Double.parseDouble(tfLowBound
						.getText()));

			// Increment
			if (!tfIncrement.getText().equals(NULL_STRING))
				synapseRef.setIncrement(Double.parseDouble(tfIncrement
						.getText()));

			// Delay
			if (!tfDelay.getText().equals(NULL_STRING))
				synapseRef.setDelay(Integer.parseInt(tfDelay.getText()));
		}
	}

}
