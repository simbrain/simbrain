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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanelNetwork;
import org.simbrain.network.neuron_update_rules.SinusoidalRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>SinusoidalNeuronPanel</b>.
 */
public class SinusoidalRulePanel extends AbstractNeuronPanel {

	/** Phase field. */
	private JTextField tfPhase = new JTextField();

	/** Frequency field. */
	private JTextField tfFrequency = new JTextField();

	/** Add noise combo box. */
	private TristateDropDown isAddNoise = new TristateDropDown();

	/** Main panel. */
	private LabelledItemPanel mainPanel = new LabelledItemPanel();

	/** Random panel. */
	private RandomPanelNetwork randPanel = new RandomPanelNetwork(true);

	/** Tabbed panel. */
	private JTabbedPane tabbedPanel = new JTabbedPane();

	/** A reference to the neuron rule being edited. */
	private SinusoidalRule neuronRef = new SinusoidalRule();

	/**
	 * Creates an instance of this panel.
	 * 
	 */
	public SinusoidalRulePanel() {
		super();
		this.add(tabbedPanel);
		mainPanel.addItem("Phase", tfPhase);
		mainPanel.addItem("Frequency", tfFrequency);
		mainPanel.addItem("Add noise", isAddNoise);
		tabbedPanel.add(mainPanel, "Main");
		tabbedPanel.add(randPanel, "Noise");
	}

	/**
	 * Populates the field with current data.
	 */
	public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

		neuronRef = (SinusoidalRule) ruleList.get(0);

		// (Below) Handle consistency of multiple selections

		// Handle Frequency
		if (!NetworkUtils.isConsistent(ruleList, SinusoidalRule.class,
				"getFrequency"))
			tfFrequency.setText(NULL_STRING);
		else
			tfFrequency
					.setText(Double.toString(neuronRef.getFrequency()));

		// Handle Phase
		if (!NetworkUtils.isConsistent(ruleList, SinusoidalRule.class,
				"getPhase"))
			tfPhase.setText(NULL_STRING);
		else
			tfPhase.setText(Double.toString(neuronRef.getPhase()));

		// Handle Noise
		if (!NetworkUtils.isConsistent(ruleList, SinusoidalRule.class,
				"getAddNoise"))
			isAddNoise.setNull();
		else
			isAddNoise.setSelected(neuronRef.getAddNoise());

		randPanel.fillFieldValues(getRandomizers(ruleList));

	}

	/**
	 * @return List of randomizers.
	 */
	private ArrayList<Randomizer> getRandomizers(
			List<NeuronUpdateRule> ruleList) {
		ArrayList<Randomizer> ret = new ArrayList<Randomizer>();

		for (int i = 0; i < ruleList.size(); i++) {
			ret.add(((SinusoidalRule) ruleList.get(i))
					.getNoiseGenerator());
		}

		return ret;
	}

	/**
	 * Populates the fields with default data.
	 */
	public void fillDefaultValues() {
		neuronRef = new SinusoidalRule();
		tfFrequency.setText(Double.toString(neuronRef.getFrequency()));
		tfPhase.setText(Double.toString(neuronRef.getPhase()));
		isAddNoise.setSelected(neuronRef.getAddNoise());
		randPanel.fillDefaultValues();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(Neuron neuron) {

		if (neuron.getUpdateRule() instanceof SinusoidalRule) {
			neuronRef = (SinusoidalRule) neuron.getUpdateRule();
		} else {
			neuron.setUpdateRule(neuronRef);
		}

		// Phase
		if (!tfPhase.getText().equals(NULL_STRING))
			neuronRef.setPhase(Double.parseDouble(tfPhase.getText()));

		// Frequency
		if (!tfFrequency.getText().equals(NULL_STRING))
			neuronRef.setFrequency(Double.parseDouble(tfFrequency
					.getText()));

		// Noise
		if (!isAddNoise.isNull())
			neuronRef.setAddNoise(isAddNoise.isSelected());

		randPanel.commitRandom(neuronRef.getNoiseGenerator());

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(List<Neuron> neurons) {

		// Phase
		if (!tfPhase.getText().equals(NULL_STRING))
			neuronRef.setPhase(Double.parseDouble(tfPhase.getText()));

		// Frequency
		if (!tfFrequency.getText().equals(NULL_STRING))
			neuronRef.setFrequency(Double.parseDouble(tfFrequency
					.getText()));

		// Noise
		if (!isAddNoise.isNull())
			neuronRef.setAddNoise(isAddNoise.isSelected());

		randPanel.commitRandom(neuronRef.getNoiseGenerator());

		for (Neuron n : neurons) {
			n.setUpdateRule(neuronRef);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NeuronUpdateRule getPrototypeRule() {
		return neuronRef;
	}

	@Override
	protected void writeValuesToRule(NeuronUpdateRule rule) {
		// TODO Auto-generated method stub

	}

}
