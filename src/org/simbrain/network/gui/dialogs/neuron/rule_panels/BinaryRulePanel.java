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
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>BinaryNeuronPanel</b> creates a dialog for setting preferences of binary
 * neurons.
 */
public class BinaryRulePanel extends AbstractNeuronPanel {

	/** Threshold for this neuron. */
	private JTextField tfThreshold = new JTextField();

	/** Bias for this neuron. */
	private JTextField tfBias = new JTextField();

	/** Main tab for neuron prefernces. */
	private LabelledItemPanel mainTab = new LabelledItemPanel();

	/** A reference to the neuron rule being edited. */
	private static final BinaryRule prototypeRule = new BinaryRule();

	/**
	 * Creates binary neuron preferences panel.
	 */
	public BinaryRulePanel() {
		super();
		this.add(mainTab);
		mainTab.addItem("Threshold", tfThreshold);
		mainTab.addItem("Bias", tfBias);
	}

	/**
	 * Populate fields with current data.
	 */
	public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

		BinaryRule neuronRef = (BinaryRule) ruleList.get(0);

		// (Below) Handle consistency of multiple selections

		// Handle Threshold
		if (!NetworkUtils.isConsistent(ruleList, BinaryRule.class,
				"getThreshold"))
			tfThreshold.setText(NULL_STRING);
		else
			tfThreshold
					.setText(Double.toString(neuronRef.getThreshold()));

		// Handle Bias
		if (!NetworkUtils.isConsistent(ruleList, BinaryRule.class,
				"getBias"))
			tfBias.setText(NULL_STRING);
		else
			tfBias.setText(Double.toString(neuronRef.getBias()));

	}

	/**
	 * Fill field values to default values for binary neuron.
	 */
	public void fillDefaultValues() {
		tfThreshold
				.setText(Double.toString(prototypeRule.getThreshold()));
		tfBias.setText(Double.toString(prototypeRule.getBias()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(Neuron neuron) {

		BinaryRule neuronRef;

		if (neuron.getUpdateRule() instanceof BinaryRule) {
			neuronRef = (BinaryRule) neuron.getUpdateRule();
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

			BinaryRule neuronRef = prototypeRule.deepCopy();

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

		BinaryRule neuronRef = (BinaryRule) rule;

		// Threshold
		if (!tfThreshold.getText().equals(NULL_STRING))
			neuronRef.setThreshold(Double.parseDouble(tfThreshold
					.getText()));

		// Bias
		if (!tfBias.getText().equals(NULL_STRING))
			neuronRef.setBias(Double.parseDouble(tfBias.getText()));

	}

	/**
	 * {@inheritDoc}
	 */
	public BinaryRule getPrototypeRule() {
		return prototypeRule.deepCopy();
	}

}
