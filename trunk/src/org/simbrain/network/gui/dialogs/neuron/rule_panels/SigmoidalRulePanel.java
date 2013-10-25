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

import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanelNetwork;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronPanel;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule.SigmoidType;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>SigmoidalNeuronPanel</b>.
 */
public class SigmoidalRulePanel extends AbstractNeuronPanel {

	/** Implementation combo box. */
	private JComboBox<SigmoidType> cbImplementation =
			new JComboBox<SigmoidType>(new SigmoidType[] {
					SigmoidType.ARCTAN, SigmoidType.LOGISTIC,
					SigmoidType.TANH });

	/** Bias field. */
	private JTextField tfBias = new JTextField();

	/** Slope field. */
	private JTextField tfSlope = new JTextField();

	/** Ceiling */
	private JTextField tfUpbound = new JTextField();

	/** Floor */
	private JTextField tfLowbound = new JTextField();

	/** Tabbed pane. */
	private JTabbedPane tabbedPane = new JTabbedPane();

	/** Main tab. */
	private LabelledItemPanel mainTab = new LabelledItemPanel();

	/** Random tab. */
	private RandomPanelNetwork randTab = new RandomPanelNetwork(true);

	/** Add noise combo box. */
	private TristateDropDown isAddNoise = new TristateDropDown();

	/** A reference to the neuron rule being edited. */
	private static final SigmoidalRule prototypeRule =
			new SigmoidalRule();

	/**
	 * Creates an instance of this panel.
	 * 
	 */
	public SigmoidalRulePanel() {
		super();
		this.add(tabbedPane);
		mainTab.addItem("Implementation", cbImplementation);
		mainTab.addItem("Bias", tfBias);
		mainTab.addItem("Slope", tfSlope);
		mainTab.addItem("Upper Bound", tfUpbound);
		mainTab.addItem("Lower Bound", tfLowbound);
		mainTab.addItem("Add Noise", isAddNoise);
		tabbedPane.add(mainTab, "Main");
		tabbedPane.add(randTab, "Noise");
	}

	/**
	 * Populate fields with current data.
	 */
	public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

		SigmoidalRule neuronRef = (SigmoidalRule) ruleList.get(0);

		// (Below) Handle consistency of multiple selections

		// Handle Implementation/Type
		if (!NetworkUtils.isConsistent(ruleList, SigmoidalRule.class,
				"getType")) {
			if ((cbImplementation.getItemCount() == SigmoidType.values().length - 1)) {
				cbImplementation.addItem(SigmoidType.NULL_STRING);
			}
			cbImplementation
					.setSelectedIndex(SigmoidType.values().length - 1);
		} else
			cbImplementation.setSelectedItem(neuronRef.getType());

		// Handle Bias
		if (!NetworkUtils.isConsistent(ruleList, SigmoidalRule.class,
				"getBias"))
			tfBias.setText(NULL_STRING);
		else
			tfBias.setText(Double.toString(neuronRef.getBias()));

		// Handle Slope
		if (!NetworkUtils.isConsistent(ruleList, SigmoidalRule.class,
				"getSlope"))
			tfSlope.setText(NULL_STRING);
		else
			tfSlope.setText(Double.toString(neuronRef.getSlope()));

		// Handle Lower Value
		if (!NetworkUtils.isConsistent(ruleList, SigmoidalRule.class,
				"getFloor"))
			tfLowbound.setText(NULL_STRING);
		else
			tfLowbound.setText(Double.toString(neuronRef.getFloor()));

		// Handle Upper Value
		if (!NetworkUtils.isConsistent(ruleList, SigmoidalRule.class,
				"getCeiling"))
			tfUpbound.setText(NULL_STRING);
		else
			tfUpbound.setText(Double.toString(neuronRef.getCeiling()));

		// Handle Noise
		if (!NetworkUtils.isConsistent(ruleList, SigmoidalRule.class,
				"getAddNoise"))
			isAddNoise.setNull();
		else
			isAddNoise.setSelected(neuronRef.getAddNoise());

		randTab.fillFieldValues(getRandomizers(ruleList));

	}

	/**
	 * @return List of randomizers.
	 */
	private ArrayList<Randomizer> getRandomizers(
			List<NeuronUpdateRule> ruleList) {
		ArrayList<Randomizer> ret = new ArrayList<Randomizer>();

		for (int i = 0; i < ruleList.size(); i++) {
			ret.add(((SigmoidalRule) ruleList.get(i)).getNoiseGenerator());
		}

		return ret;
	}

	/**
	 * Fill field values to default values for sigmoidal neuron.
	 */
	public void fillDefaultValues() {
		cbImplementation.setSelectedItem(prototypeRule.getType());
		tfBias.setText(Double.toString(prototypeRule.getBias()));
		tfSlope.setText(Double.toString(prototypeRule.getSlope()));
		tfUpbound.setText(Double.toString(prototypeRule.getCeiling()));
		tfLowbound.setText(Double.toString(prototypeRule.getFloor()));
		isAddNoise.setSelected(prototypeRule.getAddNoise());
		randTab.fillDefaultValues();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(Neuron neuron) {

		if (!(neuron.getUpdateRule() instanceof SigmoidalRule)) {
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
			SigmoidalRule neuronRef = prototypeRule.deepCopy();
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

		// Implementation: Logistic/Tanh/Arctan
		if (!cbImplementation.getSelectedItem().equals(
				SigmoidType.NULL_STRING)) {
			for (int i = 0; i < numNeurons; i++) {
				((SigmoidalRule) neurons.get(i).getUpdateRule())
						.setType((SigmoidType) cbImplementation
								.getSelectedItem());
			}
		}

		// Bias
		double bias = doubleParsable(tfBias);
		if (!Double.isNaN(bias)) {
			for (int i = 0; i < numNeurons; i++) {
				((SigmoidalRule) neurons.get(i).getUpdateRule())
						.setBias(bias);
			}
		}

		// Slope
		double slope = doubleParsable(tfSlope);
		if (!Double.isNaN(slope)) {
			for (int i = 0; i < numNeurons; i++) {
				((SigmoidalRule) neurons.get(i).getUpdateRule())
						.setSlope(slope);
			}
		}

		// Lower Value
		double lv = doubleParsable(tfLowbound);
		if (!Double.isNaN(lv)) {
			for (int i = 0; i < numNeurons; i++) {
				((SigmoidalRule) neurons.get(i).getUpdateRule())
						.setFloor(lv);
			}
		}

		// Upper Value
		double uv = doubleParsable(tfUpbound);
		if (!Double.isNaN(uv)) {
			for (int i = 0; i < numNeurons; i++) {
				((SigmoidalRule) neurons.get(i).getUpdateRule())
						.setCeiling(uv);
			}
		}

		// Add Noise?
		if (!isAddNoise.isNull()) {
			boolean addNoise =
					isAddNoise.getSelectedIndex() == TristateDropDown
							.getTRUE();
			for (int i = 0; i < numNeurons; i++) {
				((SigmoidalRule) neurons.get(i).getUpdateRule())
						.setAddNoise(addNoise);
			}
			if (addNoise) {
				for (int i = 0; i < numNeurons; i++) {
					randTab.commitRandom(((SigmoidalRule) neurons.get(i)
							.getUpdateRule()).getNoiseGenerator());
				}
			}
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
