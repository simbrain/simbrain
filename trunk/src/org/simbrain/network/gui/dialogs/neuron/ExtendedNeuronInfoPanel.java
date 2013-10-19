/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.neuron;

import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkUtils;

/**
 * 
 * A panel containing more detailed generic information about neurons. Generally
 * speaking, this panel is not meant to exist in a dialog by itself, it is a set
 * of commonly used (hence generic) neuron value fields which is shared by
 * multiple complete dialogs.
 * 
 * Values included are: Activation ceiling and floor, label, priority and
 * increment.
 * 
 * @author ztosi
 * 
 */
public class ExtendedNeuronInfoPanel extends JPanel {

	/** Null string. */
	public static final String NULL_STRING = "...";

	/** Increment field. */
	private final JTextField tfIncrement = new JTextField();

	/** Upper bound field. */
	private final JTextField tfUpBound = new JTextField();

	/** Lower bound field. */
	private final JTextField tfLowBound = new JTextField();

	/** Priority Field. */
	private final JTextField tfPriority = new JTextField();

	/** The neurons being modified. */
	private List<Neuron> neuronList;

	/**
	 * 
	 * @param neuronList
	 */
	public ExtendedNeuronInfoPanel(List<Neuron> neuronList) {
		this.neuronList = neuronList;
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
		add(new JLabel("Lower Bound:"));
		add(tfLowBound);
		add(new JLabel("Increment: "));
		add(tfIncrement);
		add(new JLabel("Priority:"));
		add(tfPriority);
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	}

	/**
	 * Fills the values of the text fields based on the corresponding values of
	 * the neurons to be edited.
	 */
	public void fillFieldValues() {

		Neuron neuronRef = neuronList.get(0);

		// Handle Increment
		if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
				"getIncrement"))
			tfIncrement.setText(NULL_STRING);
		else
			tfIncrement
					.setText(Double.toString(neuronRef.getIncrement()));

		// Handle Priority
		if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
				"getUpdatePriority"))
			tfPriority.setText(NULL_STRING);
		else
			tfPriority.setText(Integer.toString(neuronRef
					.getUpdatePriority()));

		// Handle Lower Bound
		if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
				"getLowerBound"))
			tfLowBound.setText(NULL_STRING);
		else
			tfLowBound
					.setText(Double.toString(neuronRef.getLowerBound()));

		// Handle Upper Bound
		if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
				"getUpperBound"))
			tfUpBound.setText(NULL_STRING);
		else
			tfUpBound.setText(Double.toString(neuronRef.getUpperBound()));

	}

	/**
	 * Uses the values from text fields to alter corresponding values in the
	 * neuron(s) being edited. Called externally to apply changes.
	 */
	public void commitChanges() {

		for (int i = 0; i < neuronList.size(); i++) {

			Neuron neuronRef = neuronList.get(i);

			// Increment
			if (!tfIncrement.getText().equals(NULL_STRING))
				neuronRef.setIncrement(Double.parseDouble(tfIncrement
						.getText()));

			// Priority
			if (!tfPriority.getText().equals(NULL_STRING))
				neuronRef.setUpdatePriority(Integer.parseInt(tfPriority
						.getText()));

			// Upper Bound
			if (!tfUpBound.getText().equals(NULL_STRING))
				neuronRef.setUpperBound(Double.parseDouble(tfUpBound
						.getText()));

			// Lower Bound
			if (!tfLowBound.getText().equals(NULL_STRING))
				neuronRef.setLowerBound(Double.parseDouble(tfLowBound
						.getText()));

		}

	}

}
