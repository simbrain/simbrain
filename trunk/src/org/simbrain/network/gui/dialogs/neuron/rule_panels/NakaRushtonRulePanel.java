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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanelNetwork;
import org.simbrain.network.neuron_update_rules.NakaRushtonRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>NakaRushtonNeuronPanel</b>.
 */
public class NakaRushtonRulePanel extends AbstractNeuronPanel implements
		ActionListener {

	/** Steepness field. */
	private JTextField tfSteepness = new JTextField();

	/** Semi saturation field. */
	private JTextField tfSemiSaturation = new JTextField();

	/** Time constant field. */
	private JTextField tfTimeConstant = new JTextField();

	/** Noise combo box. */
	private TristateDropDown tsNoise = new TristateDropDown();

	/** Tabbed pane. */
	private JTabbedPane tabbedPane = new JTabbedPane();

	/** Use adaptation combo box. */
	private TristateDropDown tsUseAdaptation = new TristateDropDown();

	/** Adaptation time constant. */
	private JTextField tfAdaptationTime = new JTextField();

	/** Adaptation parameter. */
	private JTextField tfAdaptationParam = new JTextField();

	/** Main tab. */
	private LabelledItemPanel mainTab = new LabelledItemPanel();

	/** Random tab. */
	private RandomPanelNetwork randTab = new RandomPanelNetwork(true);

	/** A reference to the neuron update rule being edited. */
	private static final NakaRushtonRule prototypeRule =
			new NakaRushtonRule();

	/**
	 * Creates a new Naka-Rushton neuron panel.
	 */
	public NakaRushtonRulePanel() {
		super();
		tsUseAdaptation.addActionListener(this);

		this.add(tabbedPane);
		mainTab.addItem("Steepness", tfSteepness);
		mainTab.addItem("Semi-saturation constant", tfSemiSaturation);
		mainTab.addItem("Time constant", tfTimeConstant);
		mainTab.addItem("Add noise", tsNoise);
		mainTab.addItem("Use Adaptation", tsUseAdaptation);
		mainTab.addItem("Adaptation parameter", tfAdaptationParam);
		mainTab.addItem("Adaptation time constant", tfAdaptationTime);
		tabbedPane.add(mainTab, "Main");
		tabbedPane.add(randTab, "Noise");
	}

	/**
	 * Checks for using adaptation and enables or disables adaptation field
	 * accordingly.
	 */
	private void checkUsingAdaptation() {
		if (tsUseAdaptation.isSelected()) {
			tfAdaptationTime.setEnabled(true);
			tfAdaptationParam.setEnabled(true);
		} else {
			tfAdaptationTime.setEnabled(false);
			tfAdaptationParam.setEnabled(false);
		}
	}

	/**
	 * Populate fields with current data.
	 */
	public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

		NakaRushtonRule neuronRef = (NakaRushtonRule) ruleList.get(0);

		// (Below) Handle consistency of multiple selections

		// Handle Time Constant
		if (!NetworkUtils.isConsistent(ruleList, NakaRushtonRule.class,
				"getTimeConstant"))
			tfTimeConstant.setText(NULL_STRING);
		else
			tfTimeConstant.setText(Double.toString(neuronRef
					.getTimeConstant()));

		// Handle Semi-Saturation Constant
		if (!NetworkUtils.isConsistent(ruleList, NakaRushtonRule.class,
				"getSemiSaturationConstant"))
			tfSemiSaturation.setText(NULL_STRING);
		else
			tfSemiSaturation.setText(Double.toString(neuronRef
					.getSemiSaturationConstant()));

		// Handle Steepness
		if (!NetworkUtils.isConsistent(ruleList, NakaRushtonRule.class,
				"getSteepness"))
			tfSteepness.setText(NULL_STRING);
		else
			tfSteepness
					.setText(Double.toString(neuronRef.getSteepness()));

		// Handle Noise
		if (!NetworkUtils.isConsistent(ruleList, NakaRushtonRule.class,
				"getAddNoise"))
			tsNoise.setNull();
		else
			tsNoise.setSelected(neuronRef.getAddNoise());

		// Handle Use Adaptation
		if (!NetworkUtils.isConsistent(ruleList, NakaRushtonRule.class,
				"getUseAdaptation"))
			tsUseAdaptation.setNull();
		else
			tsUseAdaptation.setSelected(neuronRef.getUseAdaptation());

		// Handle Adaptation Time
		if (!NetworkUtils.isConsistent(ruleList, NakaRushtonRule.class,
				"getAdaptationTimeConstant"))
			tfAdaptationTime.setText(NULL_STRING);
		else
			tfAdaptationTime.setText(Double.toString(neuronRef
					.getAdaptationTimeConstant()));

		// Handle Adaptation Parameter
		if (!NetworkUtils.isConsistent(ruleList, NakaRushtonRule.class,
				"getAdaptationParameter"))
			tfAdaptationParam.setText(NULL_STRING);
		else
			tfAdaptationParam.setText(Double.toString(neuronRef
					.getAdaptationParameter()));

		// Use Adaptation
		checkUsingAdaptation();

		randTab.fillFieldValues(getRandomizers(ruleList));

	}

	/**
	 * @return an arraylist of randomizers.
	 */
	private ArrayList<Randomizer> getRandomizers(
			List<NeuronUpdateRule> ruleList) {
		ArrayList<Randomizer> ret = new ArrayList<Randomizer>();
		for (NeuronUpdateRule rule : ruleList) {
			if (rule instanceof NakaRushtonRule) {
				ret.add(((NakaRushtonRule) rule).getNoiseGenerator());
			}
		}
		return ret;
	}

	/**
	 * Fill field values to default values for this synapse type.
	 */
	public void fillDefaultValues() {
		checkUsingAdaptation();
		tfSemiSaturation.setText(Double.toString(prototypeRule
				.getSemiSaturationConstant()));
		tfSteepness
				.setText(Double.toString(prototypeRule.getSteepness()));
		tfTimeConstant.setText(Double.toString(prototypeRule
				.getTimeConstant()));
		tsNoise.setSelected(prototypeRule.getAddNoise());
		tsUseAdaptation.setSelected(prototypeRule.getUseAdaptation());
		tfAdaptationTime.setText(Double.toString(prototypeRule
				.getAdaptationTimeConstant()));
		tfAdaptationParam.setText(Double.toString(prototypeRule
				.getAdaptationParameter()));
		randTab.fillDefaultValues();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(Neuron neuron) {

		NakaRushtonRule neuronRef;

		if (neuron.getUpdateRule() instanceof NakaRushtonRule) {
			neuronRef = (NakaRushtonRule) neuron.getUpdateRule();
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

			NakaRushtonRule neuronRef = prototypeRule.deepCopy();

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

		NakaRushtonRule neuronRef = (NakaRushtonRule) rule;

		// Time Constant
		if (!tfTimeConstant.getText().equals(NULL_STRING))
			neuronRef.setTimeConstant(Double.parseDouble(tfTimeConstant
					.getText()));

		// Semi-Saturation
		if (!tfSemiSaturation.getText().equals(NULL_STRING))
			neuronRef.setSemiSaturationConstant(Double
					.parseDouble(tfSemiSaturation.getText()));

		// Steepness
		if (!tfSteepness.getText().equals(NULL_STRING))
			neuronRef.setSteepness(Double.parseDouble(tfSteepness
					.getText()));

		// Use Adaptation
		if (!tsUseAdaptation.isNull())
			neuronRef.setUseAdaptation(tsUseAdaptation.isSelected());

		// Adaptation Time
		if (!tfAdaptationTime.getText().equals(NULL_STRING))
			neuronRef.setAdaptationTimeConstant(Double
					.parseDouble(tfAdaptationTime.getText()));

		// Adaptation Parameter
		if (!tfAdaptationParam.getText().equals(NULL_STRING))
			neuronRef.setAdaptationParameter(Double
					.parseDouble(tfAdaptationParam.getText()));

		// Noise?
		if (!tsNoise.isNull()) {
			neuronRef.setAddNoise(tsNoise.isSelected());
			if (tsNoise.getSelectedIndex() == TristateDropDown.getTRUE())
				randTab.commitRandom(neuronRef.getNoiseGenerator());
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NeuronUpdateRule getPrototypeRule() {
		return prototypeRule.deepCopy();
	}

	/**
	 * Responds to actions performed.
	 * 
	 * @param e
	 *            Action event
	 */
	public void actionPerformed(final ActionEvent e) {
		Object o = e.getSource();

		if (o == tsUseAdaptation) {
			checkUsingAdaptation();
		}
	}

}
