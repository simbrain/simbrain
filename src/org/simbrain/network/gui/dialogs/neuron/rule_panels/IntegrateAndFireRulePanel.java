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
import java.util.Collections;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanelNetwork;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronPanel;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>IntegrateAndFireNeuronPanel</b>.
 */
public class IntegrateAndFireRulePanel extends AbstractNeuronPanel {

	/** Tabbed pane. */
	private JTabbedPane tabbedPane = new JTabbedPane();

	/** Main tab. */
	private LabelledItemPanel mainTab = new LabelledItemPanel();

	/** Time constant field. */
	private JTextField tfTimeConstant = new JTextField();

	/** Threshold field. */
	private JTextField tfThreshold = new JTextField();

	/** Reset field. */
	private JTextField tfReset = new JTextField();

	/** Resistance field. */
	private JTextField tfResistance = new JTextField();

	/** Resting potential field. */
	private JTextField tfRestingPotential = new JTextField();

	/** Random tab. */
	private RandomPanelNetwork randTab = new RandomPanelNetwork(true);

	/** Add noise combo box. */
	private TristateDropDown isAddNoise = new TristateDropDown();

	/** A reference to the neuron update rule being edited. */
	private static final IntegrateAndFireRule prototypeRule =
			new IntegrateAndFireRule();

	/**
	 * Creates a new instance of the integrate and fire neuron panel.
	 */
	public IntegrateAndFireRulePanel() {
		super();
		this.add(tabbedPane);
		mainTab.addItem("Resistance", tfResistance);
		mainTab.addItem("Resting potential", tfRestingPotential);
		mainTab.addItem("Reset potential", tfReset);
		mainTab.addItem("Threshold", tfThreshold);
		mainTab.addItem("Time constant", tfTimeConstant);
		mainTab.addItem("Add noise", isAddNoise);
		tabbedPane.add(mainTab, "Main");
		tabbedPane.add(randTab, "Noise");
	}

	/**
	 * Populate fields with current data.
	 */
	public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

		IntegrateAndFireRule neuronRef =
				(IntegrateAndFireRule) ruleList.get(0);

		// (Below) Handle consistency of multiple selections

		// Handle Resting Potential
		if (!NetworkUtils.isConsistent(ruleList,
				IntegrateAndFireRule.class, "getRestingPotential"))
			tfRestingPotential.setText(NULL_STRING);
		else
			tfRestingPotential.setText(Double.toString(neuronRef
					.getRestingPotential()));

		// Handle Resistance
		if (!NetworkUtils.isConsistent(ruleList,
				IntegrateAndFireRule.class, "getResistance"))
			tfResistance.setText(NULL_STRING);
		else
			tfResistance.setText(Double.toString(neuronRef
					.getResistance()));

		// Handle Add Noise
		if (!NetworkUtils.isConsistent(ruleList,
				IntegrateAndFireRule.class, "getAddNoise"))
			isAddNoise.setNull();
		else
			isAddNoise.setSelected(neuronRef.getAddNoise());

		// Handle Reset Potential
		if (!NetworkUtils.isConsistent(ruleList,
				IntegrateAndFireRule.class, "getResetPotential"))
			tfReset.setText(NULL_STRING);
		else
			tfReset.setText(Double.toString(neuronRef.getResetPotential()));

		// Handle Time Constant
		if (!NetworkUtils.isConsistent(ruleList,
				IntegrateAndFireRule.class, "getTimeConstant"))
			tfTimeConstant.setText(NULL_STRING);
		else
			tfTimeConstant.setText(Double.toString(neuronRef
					.getTimeConstant()));

		// Handle Threshold
		if (!NetworkUtils.isConsistent(ruleList,
				IntegrateAndFireRule.class, "getThreshold"))
			tfThreshold.setText(NULL_STRING);
		else
			tfThreshold
					.setText(Double.toString(neuronRef.getThreshold()));

		randTab.fillFieldValues(getRandomizers(ruleList));

	}

	/**
	 * @return List of randomizers.
	 */
	private ArrayList<Randomizer> getRandomizers(
			List<NeuronUpdateRule> ruleList) {
		ArrayList<Randomizer> ret = new ArrayList<Randomizer>();
		for (int i = 0; i < ruleList.size(); i++) {
			ret.add(((IntegrateAndFireRule) ruleList.get(i))
					.getNoiseGenerator());
		}
		return ret;
	}

	/**
	 * Populate fields with default data.
	 */
	public void fillDefaultValues() {
		tfRestingPotential.setText(Double.toString(prototypeRule
				.getRestingPotential()));
		tfResistance.setText(Double.toString(prototypeRule
				.getResistance()));
		tfReset.setText(Double.toString(prototypeRule.getResetPotential()));
		tfThreshold
				.setText(Double.toString(prototypeRule.getThreshold()));
		tfTimeConstant.setText(Double.toString(prototypeRule
				.getTimeConstant()));
		isAddNoise.setSelected(prototypeRule.getAddNoise());
		randTab.fillDefaultValues();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(Neuron neuron) {

		if (!(neuron.getUpdateRule() instanceof IntegrateAndFireRule)) {
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
			IntegrateAndFireRule neuronRef = prototypeRule.deepCopy();
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
		
		// Time Constant
		double timeConstant = doubleParsable(tfTimeConstant);
		if (!Double.isNaN(timeConstant)) {
			for (int i = 0; i < numNeurons; i++) {
				((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
				.setTimeConstant(timeConstant);
			}
		}
		
		// Threshold
		double threshold = doubleParsable(tfThreshold);
		if (!Double.isNaN(threshold)) {
			for (int i = 0; i < numNeurons; i++) {
				((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
				.setThreshold(threshold);
			}
		}
		
		// Reset
		double reset = doubleParsable(tfReset);
		if (!Double.isNaN(reset)) {
			for (int i = 0; i < numNeurons; i++) {
				((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
				.setResetPotential(reset);
			}
		}
		
		// Resistance
		double resistance = doubleParsable(tfResistance);
		if (!Double.isNaN(resistance)) {
			for (int i = 0; i < numNeurons; i++) {
				((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
				.setResistance(resistance);
			}
		}

		// Resting Potential
		double restingPotential = doubleParsable(tfRestingPotential);
		if (!Double.isNaN(restingPotential)) {
			for (int i = 0; i < numNeurons; i++) {
				((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
				.setRestingPotential(restingPotential);
			}
		}

		// Add Noise?
		if(!isAddNoise.isNull()) {
			boolean addNoise = isAddNoise.getSelectedIndex()
					== TristateDropDown.getTRUE();
			for (int i = 0; i < numNeurons; i++) {
				((IntegrateAndFireRule) neurons.get(i).getUpdateRule())
				.setAddNoise(addNoise);


			}  		
			if (addNoise) {
				for (int i = 0; i < numNeurons; i++) {
					randTab.commitRandom(((IntegrateAndFireRule) neurons.get(i)
							.getUpdateRule()).getNoiseGenerator());
				}
			}
		}	

	}

	/**
	 * {@inheritDoc}
	 */
	public IntegrateAndFireRule getPrototypeRule() {
		return prototypeRule.deepCopy();
	}
	
}
