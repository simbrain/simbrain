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
import java.util.Collections;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanelNetwork;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronPanel;
import org.simbrain.network.neuron_update_rules.NakaRushtonRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * <b>NakaRushtonNeuronPanel</b>.
 */
public class NakaRushtonRulePanel extends AbstractNeuronPanel implements
		ActionListener {

	/** Steepness field. */
	private JTextField tfSteepness = new JTextField();

	/** Ceiling */
	private JTextField tfUpbound = new JTextField();

	/** Floor */
	private JTextField tfLowbound = new JTextField();

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
		mainTab.addItem("Max Value", tfUpbound);
		mainTab.addItem("Min Value", tfLowbound);
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

		// Handle Lower Value
		if (!NetworkUtils.isConsistent(ruleList, NakaRushtonRule.class,
				"getFloor"))
			tfLowbound.setText(NULL_STRING);
		else
			tfLowbound.setText(Double.toString(neuronRef.getFloor()));

		// Handle Upper Value
		if (!NetworkUtils.isConsistent(ruleList, NakaRushtonRule.class,
				"getCeiling"))
			tfUpbound.setText(NULL_STRING);
		else
			tfUpbound.setText(Double.toString(neuronRef.getCeiling()));

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
		tfUpbound.setText(Double.toString(prototypeRule.getCeiling()));
		tfLowbound.setText(Double.toString(prototypeRule.getFloor()));
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

		if (!(neuron.getUpdateRule() instanceof NakaRushtonRule)) {
			neuron.setUpdateRule(prototypeRule.deepCopy());
		}

		writeValuesToRules(Collections.singletonList(neuron));

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(List<Neuron> neurons) {

		if (isReplace()) {
			NakaRushtonRule neuronRef = prototypeRule.deepCopy();
			for (Neuron n : neurons) {
				n.setUpdateRule(neuronRef.deepCopy());
			}
		}

		writeValuesToRules(neurons);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeValuesToRules(List<Neuron> neurons) {
		int numNeurons = neurons.size();

		// Steepness
		double steepness = doubleParsable(tfSteepness);
		if (!Double.isNaN(steepness)) {
			for (int i = 0; i < numNeurons; i++) {
				((NakaRushtonRule) neurons.get(i).getUpdateRule())
						.setSteepness(steepness);
			}
		}

		// Lower Value
		double lv = doubleParsable(tfLowbound);
		if (!Double.isNaN(lv)) {
			for (int i = 0; i < numNeurons; i++) {
				((NakaRushtonRule) neurons.get(i).getUpdateRule())
						.setFloor(lv);
			}
		}

		// Upper Value
		double uv = doubleParsable(tfUpbound);
		if (!Double.isNaN(uv)) {
			for (int i = 0; i < numNeurons; i++) {
				((NakaRushtonRule) neurons.get(i).getUpdateRule())
						.setCeiling(uv);
			}
		}

		// Semi-Saturation
		double semiSaturation = doubleParsable(tfSemiSaturation);
		if (!Double.isNaN(semiSaturation)) {
			for (int i = 0; i < numNeurons; i++) {
				((NakaRushtonRule) neurons.get(i).getUpdateRule())
						.setSemiSaturationConstant(semiSaturation);
			}
		}

		// Time Constant
		double timeConstant = doubleParsable(tfTimeConstant);
		if (!Double.isNaN(timeConstant)) {
			for (int i = 0; i < numNeurons; i++) {
				((NakaRushtonRule) neurons.get(i).getUpdateRule())
						.setTimeConstant(timeConstant);
			}
		}

		// Use Adaptation?
		if (!tsUseAdaptation.isNull()) {
			boolean adaptation =
					tsUseAdaptation.getSelectedIndex() == TristateDropDown
							.getTRUE();
			for (int i = 0; i < numNeurons; i++) {
				((NakaRushtonRule) neurons.get(i).getUpdateRule())
						.setUseAdaptation(adaptation);
			}

			if (adaptation) {

				// Adaptation Time Constant
				double adaptationTime = doubleParsable(tfAdaptationTime);
				if (!Double.isNaN(adaptationTime)) {
					for (int i = 0; i < numNeurons; i++) {
						((NakaRushtonRule) neurons.get(i).getUpdateRule())
								.setAdaptationTimeConstant(adaptationTime);
					}
				}

				// Adaptation Parameter
				double adaptationParameter =
						doubleParsable(tfAdaptationParam);
				if (!Double.isNaN(adaptationParameter)) {
					for (int i = 0; i < numNeurons; i++) {
						((NakaRushtonRule) neurons.get(i).getUpdateRule())
								.setAdaptationParameter(adaptationParameter);
					}
				}
			}

		}

		// Add Noise?
		if (!tsNoise.isNull()) {
			boolean addNoise =
					tsNoise.getSelectedIndex() == TristateDropDown
							.getTRUE();
			for (int i = 0; i < numNeurons; i++) {
				((NakaRushtonRule) neurons.get(i).getUpdateRule())
						.setAddNoise(addNoise);

			}
			if (addNoise) {
				for (int i = 0; i < numNeurons; i++) {
					randTab.commitRandom(((NakaRushtonRule) neurons
							.get(i).getUpdateRule()).getNoiseGenerator());
				}
			}
		}

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NeuronUpdateRule getPrototypeRule() {
		return prototypeRule.deepCopy();
	}

}
