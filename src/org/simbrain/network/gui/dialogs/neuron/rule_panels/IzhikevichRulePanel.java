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
import org.simbrain.network.neuron_update_rules.IzhikevichRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * <b>IzhikevichNeuronPanel</b>.
 */
public class IzhikevichRulePanel extends AbstractNeuronPanel {

	/** A field. */
	private JTextField tfA = new JTextField();

	/** B field. */
	private JTextField tfB = new JTextField();

	/** C field. */
	private JTextField tfC = new JTextField();

	/** D field. */
	private JTextField tfD = new JTextField();

	/** Add noise combo box. */
	private TristateDropDown tsNoise = new TristateDropDown();

	/** Tabbed pane. */
	private JTabbedPane tabbedPane = new JTabbedPane();

	/** Main tab. */
	private LabelledItemPanel mainTab = new LabelledItemPanel();

	/** Random tab. */
	private RandomPanelNetwork randTab = new RandomPanelNetwork(true);

	/** A reference to the neuron update rule being edited. */
	private static final IzhikevichRule prototypeRule =
			new IzhikevichRule();

	/**
	 * Creates an instance of this panel.
	 */
	public IzhikevichRulePanel() {
		super();
		this.add(tabbedPane);
		mainTab.addItem("A", tfA);
		mainTab.addItem("B", tfB);
		mainTab.addItem("C", tfC);
		mainTab.addItem("D", tfD);
		mainTab.addItem("Add noise", tsNoise);
		tabbedPane.add(mainTab, "Main");
		tabbedPane.add(randTab, "Noise");
		this.addBottomText("<html>For a list of useful parameter settings<p>"
				+ "press the \"Help\" Button.</html>");
	}

	/**
	 * Populate fields with current data.
	 */
	public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

		IzhikevichRule neuronRef = (IzhikevichRule) ruleList.get(0);

		// (Below) Handle consistency of multiple selections

		// Handle A
		if (!NetworkUtils.isConsistent(ruleList, IzhikevichRule.class,
				"getA"))
			tfA.setText(NULL_STRING);
		else
			tfA.setText(Double.toString(neuronRef.getA()));

		// Handle B
		if (!NetworkUtils.isConsistent(ruleList, IzhikevichRule.class,
				"getB"))
			tfB.setText(NULL_STRING);
		else
			tfB.setText(Double.toString(neuronRef.getB()));

		// Handle C
		if (!NetworkUtils.isConsistent(ruleList, IzhikevichRule.class,
				"getC"))
			tfC.setText(NULL_STRING);
		else
			tfC.setText(Double.toString(neuronRef.getC()));

		// Handle D
		if (!NetworkUtils.isConsistent(ruleList, IzhikevichRule.class,
				"getD"))
			tfD.setText(NULL_STRING);
		else
			tfD.setText(Double.toString(neuronRef.getD()));

		// Handle Noise
		if (!NetworkUtils.isConsistent(ruleList, IzhikevichRule.class,
				"getAddNoise"))
			tsNoise.setNull();
		else
			tsNoise.setSelected(neuronRef.getAddNoise());

		randTab.fillFieldValues(getRandomizers(ruleList));

	}

	/**
	 * @return List of randomizers.
	 */
	private ArrayList<Randomizer> getRandomizers(
			List<NeuronUpdateRule> ruleList) {
		ArrayList<Randomizer> ret = new ArrayList<Randomizer>();
		for (int i = 0; i < ruleList.size(); i++) {
			ret.add(((IzhikevichRule) ruleList.get(i))
					.getNoiseGenerator());
		}
		return ret;
	}

	/**
	 * Populate fields with default data.
	 */
	public void fillDefaultValues() {
		tfA.setText(Double.toString(prototypeRule.getA()));
		tfB.setText(Double.toString(prototypeRule.getB()));
		tfC.setText(Double.toString(prototypeRule.getC()));
		tfD.setText(Double.toString(prototypeRule.getD()));
		tsNoise.setSelected(prototypeRule.getAddNoise());
		randTab.fillDefaultValues();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(Neuron neuron) {

		if (!(neuron.getUpdateRule() instanceof IzhikevichRule)) {
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
			IzhikevichRule neuronRef = prototypeRule.deepCopy();
			for (Neuron n : neurons) {
				n.setUpdateRule(neuronRef.deepCopy());
			}
		} 
		
		writeValuesToRules(neurons);

	}

	/**
	 * 
	 */
	@Override
	protected void writeValuesToRules(List<Neuron> neurons) {
		int numNeurons = neurons.size();
		
		// A
		double a = doubleParsable(tfA);
		if (!Double.isNaN(a)) {
			for (int i = 0; i < numNeurons; i++) {
				((IzhikevichRule) neurons.get(i).getUpdateRule()).setA(a);
			}
		}
		
		// B
		double b = doubleParsable(tfB);
		if (!Double.isNaN(b)) {
			for (int i = 0; i < numNeurons; i++) {
				((IzhikevichRule) neurons.get(i).getUpdateRule()).setB(b);
			}
		}
		
		// C
		double c = doubleParsable(tfC);
		if (!Double.isNaN(c)) {
			for (int i = 0; i < numNeurons; i++) {
				((IzhikevichRule) neurons.get(i).getUpdateRule()).setC(c);
			}
		}
		
		// D
		double d = doubleParsable(tfD);
		if (!Double.isNaN(d)) {
			for (int i = 0; i < numNeurons; i++) {
				((IzhikevichRule) neurons.get(i).getUpdateRule()).setD(d);
			}
		}
		
		// Add Noise?
		if(!tsNoise.isNull()) {
			boolean addNoise = tsNoise.getSelectedIndex()
					== TristateDropDown.getTRUE();
			for (int i = 0; i < numNeurons; i++) {
				((IzhikevichRule) neurons.get(i).getUpdateRule())
				.setAddNoise(addNoise);


			}  		
			if (addNoise) {
				for (int i = 0; i < numNeurons; i++) {
					randTab.commitRandom(((IzhikevichRule) neurons.get(i)
							.getUpdateRule()).getNoiseGenerator());
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public IzhikevichRule getPrototypeRule() {
		return prototypeRule.deepCopy();
	}
	
}
