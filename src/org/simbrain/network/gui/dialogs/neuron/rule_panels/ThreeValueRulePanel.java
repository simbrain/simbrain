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
package org.simbrain.network.gui.dialogs.neuron.rule_panels;

import java.util.List;

import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.neuron_update_rules.ThreeValueRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>ThreeValuedNeuronPanel</b> creates a dialog for setting preferences of
 * three valued neurons.
 */
public class ThreeValueRulePanel extends AbstractNeuronPanel {

	/** Threshold for this neuron. */
	private JTextField tfLowerThreshold = new JTextField();

	/** Bias for this neuron. */
	private JTextField tfBias = new JTextField();

	/** Upper threshold field. */
	private JTextField tfUpperThreshold = new JTextField();

	/** Lower value field. */
	private JTextField tfLowerValue = new JTextField();

	/** Middle value field. */
	private JTextField tfMiddleValue = new JTextField();

	/** Upper value field. */
	private JTextField tfUpperValue = new JTextField();

	/** Main tab for neuron prefernces. */
	private LabelledItemPanel mainTab = new LabelledItemPanel();

	/** A reference to the neuron rule being edited. */
	private static final ThreeValueRule prototypeRule =
			new ThreeValueRule();

	/**
	 * Creates binary neuron preferences panel.
	 */
	public ThreeValueRulePanel() {
		super();
		this.add(mainTab);
		mainTab.addItem("Bias", tfBias);
		mainTab.addItem("Lower threshold", tfLowerThreshold);
		mainTab.addItem("Upper threshold", tfUpperThreshold);
		mainTab.addItem("Lower value", tfLowerValue);
		mainTab.addItem("Middle value", tfMiddleValue);
		mainTab.addItem("Upper value", tfUpperValue);
	}

	/**
	 * Populate fields with current data.
	 */
	public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

		ThreeValueRule neuronRef = (ThreeValueRule) ruleList.get(0);

		// (Below) Handle consistency of multiple selections

		// Handle Lower Threshold
		if (!NetworkUtils.isConsistent(ruleList, ThreeValueRule.class,
				"getLowerThreshold"))
			tfLowerThreshold.setText(NULL_STRING);
		else
			tfLowerThreshold.setText(Double.toString(neuronRef
					.getLowerThreshold()));

		// Handle Upper Threshold
		if (!NetworkUtils.isConsistent(ruleList, ThreeValueRule.class,
				"getUpperThreshold"))
			tfUpperThreshold.setText(NULL_STRING);
		else
			tfUpperThreshold.setText(Double.toString(neuronRef
					.getUpperThreshold()));

		// Handle Bias
		if (!NetworkUtils.isConsistent(ruleList, ThreeValueRule.class,
				"getBias"))
			tfBias.setText(NULL_STRING);
		else
			tfBias.setText(Double.toString(neuronRef.getBias()));

		// Handle Lower Value
		if (!NetworkUtils.isConsistent(ruleList, ThreeValueRule.class,
				"getLowerValue"))
			tfLowerValue.setText(NULL_STRING);
		else
			tfLowerValue.setText(Double.toString(neuronRef
					.getLowerValue()));

		// Handle Middle Value
		if (!NetworkUtils.isConsistent(ruleList, ThreeValueRule.class,
				"getMiddleValue"))
			tfMiddleValue.setText(NULL_STRING);
		else
			tfMiddleValue.setText(Double.toString(neuronRef
					.getMiddleValue()));

		// Handle Upper Value
		if (!NetworkUtils.isConsistent(ruleList, ThreeValueRule.class,
				"getUpperValue"))
			tfUpperValue.setText(NULL_STRING);
		else
			tfUpperValue.setText(Double.toString(neuronRef
					.getUpperValue()));

	}

	/**
	 * Fill field values to default values for binary neuron.
	 */
	public void fillDefaultValues() {
		tfLowerThreshold.setText(Double.toString(prototypeRule
				.getLowerThreshold()));
		tfBias.setText(Double.toString(prototypeRule.getBias()));
		tfUpperThreshold.setText(Double.toString(prototypeRule
				.getUpperThreshold()));
		tfLowerValue.setText(Double.toString(prototypeRule
				.getLowerValue()));
		tfMiddleValue.setText(Double.toString(prototypeRule
				.getMiddleValue()));
		tfUpperValue.setText(Double.toString(prototypeRule
				.getUpperValue()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(Neuron neuron) {

		ThreeValueRule neuronRef;

		if (neuron.getUpdateRule() instanceof ThreeValueRule) {
			neuronRef = (ThreeValueRule) neuron.getUpdateRule();
		} else {
			neuronRef = prototypeRule.deepCopy();
			neuron.setUpdateRule(neuronRef);
		}

		writeValuesToRule(neuronRef);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(List<Neuron> neurons) {

		if (isReplace()) {

			ThreeValueRule neuronRef = prototypeRule.deepCopy();

			writeValuesToRule(neuronRef);

			for (Neuron n : neurons) {
				n.setUpdateRule(neuronRef.deepCopy());
			}

		} else {

			for (Neuron n : neurons) {
				writeValuesToRule(n.getUpdateRule());
			}

		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeValuesToRule(NeuronUpdateRule rule) {

		ThreeValueRule neuronRef = (ThreeValueRule) rule;

		// Lower Threshold
		if (!tfLowerThreshold.getText().equals(NULL_STRING))
			neuronRef.setLowerThreshold(Double
					.parseDouble(tfLowerThreshold.getText()));

		// Upper Threshold
		if (!tfUpperThreshold.getText().equals(NULL_STRING))
			neuronRef.setUpperThreshold(Double
					.parseDouble(tfUpperThreshold.getText()));

		// Bias
		if (!tfBias.getText().equals(NULL_STRING))
			neuronRef.setBias(Double.parseDouble(tfBias.getText()));

		// Lower Value
		if (!tfLowerValue.getText().equals(NULL_STRING))
			neuronRef.setLowerValue(Double.parseDouble(tfLowerValue
					.getText()));

		// Middle Value
		if (!tfMiddleValue.getText().equals(NULL_STRING))
			neuronRef.setMiddleValue(Double.parseDouble(tfMiddleValue
					.getText()));

		// Upper Value
		if (!tfUpperValue.getText().equals(NULL_STRING))
			neuronRef.setUpperValue(Double.parseDouble(tfUpperValue
					.getText()));

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NeuronUpdateRule getPrototypeRule() {
		return prototypeRule.deepCopy();
	}

}
