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
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule;

/**
 * <b>ProbabilisticSpikingNeuronPanel</b>. TODO: Deactivated until discussion
 * about "SpikingNeuronUpdateRule".
 */
public class SpikingThresholdRulePanel extends AbstractNeuronPanel {

	/** Time step field. */
	private JTextField tfThreshold = new JTextField();

	/** A reference to the neuron rule being edited. */
	private static final SpikingThresholdRule prototypeRule =
			new SpikingThresholdRule();

	/**
	 * Creates a new instance of the probabilistic spiking neuron panel.
	 * 
	 * @param net
	 *            Network
	 */
	public SpikingThresholdRulePanel() {
		super();
		addItem("Threshold", tfThreshold);
	}

	/**
	 * Populate fields with current data.
	 */
	public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
		SpikingThresholdRule neuronRef =
				(SpikingThresholdRule) ruleList.get(0);

		tfThreshold.setText(Double.toString(neuronRef.getThreshold()));

		// (Below) Handle consistency of multiple selections

		// Handle Threshold
		if (!NetworkUtils.isConsistent(ruleList,
				SpikingThresholdRule.class, "getThreshold")) {
			tfThreshold.setText(NULL_STRING);
		}
	}

	/**
	 * Populate fields with default data.
	 */
	public void fillDefaultValues() {
		tfThreshold
				.setText(Double.toString(prototypeRule.getThreshold()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(Neuron neuron) {

		SpikingThresholdRule neuronRef;

		if (neuron.getUpdateRule() instanceof SpikingThresholdRule) {
			neuronRef = (SpikingThresholdRule) neuron.getUpdateRule();
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

			SpikingThresholdRule neuronRef = prototypeRule.deepCopy();

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

		SpikingThresholdRule neuronRef = (SpikingThresholdRule) rule;

		// Threshold
		if (!tfThreshold.getText().equals(NULL_STRING))
			neuronRef.setThreshold(Double.parseDouble(tfThreshold
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
