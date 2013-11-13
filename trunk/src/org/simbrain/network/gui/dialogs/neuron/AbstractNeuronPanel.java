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

import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.LogisticGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.RandomGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.SinusoidalGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.StochasticGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.AdditiveRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.BinaryRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.DecayRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.IACRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.IntegrateAndFireRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.IzhikevichRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.LinearRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.NakaRushtonRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.PointNeuronRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.SigmoidalRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.SpikingThresholdRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.ThreeValueRulePanel;
import org.simbrain.network.neuron_update_rules.AdditiveRule;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.neuron_update_rules.DecayRule;
import org.simbrain.network.neuron_update_rules.IACRule;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.network.neuron_update_rules.IzhikevichRule;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.NakaRushtonRule;
import org.simbrain.network.neuron_update_rules.PointNeuronRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule;
import org.simbrain.network.neuron_update_rules.ThreeValueRule;
import org.simbrain.network.neuron_update_rules.activity_generators.LogisticRule;
import org.simbrain.network.neuron_update_rules.activity_generators.RandomNeuronRule;
import org.simbrain.network.neuron_update_rules.activity_generators.SinusoidalRule;
import org.simbrain.network.neuron_update_rules.activity_generators.StochasticRule;

/**
 * <b>AbstractNeuronPanel</b> is the parent class for all panels used to set
 * parameters of specific neuron rule types.
 * 
 * Optimization has been emphasized for methods intended for neuron creation
 * rather than editing on the assumption that the former will be far more common
 * for large numbers of neurons.
 */
public abstract class AbstractNeuronPanel extends JPanel {

	/** Null string. */
	public static final String NULL_STRING = "...";

	/** Associations between names of rules and panels for editing them. */
	public static final LinkedHashMap<String, AbstractNeuronPanel> RULE_MAP =
			new LinkedHashMap<String, AbstractNeuronPanel>();

	// Populate the Rule Map
	static {
		RULE_MAP.put(new AdditiveRule().getDescription(),
				new AdditiveRulePanel());
		RULE_MAP.put(new BinaryRule().getDescription(),
				new BinaryRulePanel());
		RULE_MAP.put(new DecayRule().getDescription(),
				new DecayRulePanel());
		RULE_MAP.put(new IACRule().getDescription(), new IACRulePanel());
		RULE_MAP.put(new IntegrateAndFireRule().getDescription(),
				new IntegrateAndFireRulePanel());
		RULE_MAP.put(new IzhikevichRule().getDescription(),
				new IzhikevichRulePanel());
		RULE_MAP.put(new LinearRule().getDescription(),
				new LinearRulePanel());
		RULE_MAP.put(new NakaRushtonRule().getDescription(),
				new NakaRushtonRulePanel());
		RULE_MAP.put(new PointNeuronRule().getDescription(),
				new PointNeuronRulePanel());
		RULE_MAP.put(new SigmoidalRule().getDescription(),
				new SigmoidalRulePanel());
		RULE_MAP.put(new SpikingThresholdRule().getDescription(),
				new SpikingThresholdRulePanel());
		RULE_MAP.put(new ThreeValueRule().getDescription(),
				new ThreeValueRulePanel());
	}

	/**
	 * Associations between names of activity generators and panels for editing
	 * them.
	 */
	public static final LinkedHashMap<String, AbstractNeuronPanel> GENERATOR_MAP =
			new LinkedHashMap<String, AbstractNeuronPanel>();

	// Populate the Generator Map
	static {
		GENERATOR_MAP.put(new LogisticRule().getDescription(),
				new LogisticGeneratorPanel());
		GENERATOR_MAP.put(new RandomNeuronRule().getDescription(),
				new RandomGeneratorPanel());
		GENERATOR_MAP.put(new SinusoidalRule().getDescription(),
				new SinusoidalGeneratorPanel());
		GENERATOR_MAP.put(new StochasticRule().getDescription(),
				new StochasticGeneratorPanel());
	}

	/**
	 * A flag used to indicate whether this panel will be replacing neuron
	 * update rules or simply writing to them. In cases where the panel
	 * represents the same rule as the rule (i.e. Linear panel & linear neurons)
	 * the neurons' update rules are edited, not replaced. However, if the panel
	 * does not correspond to the currently used neuron update rule, new
	 * NeuronUpdateRule objects are created, and replace the old rule. This
	 * optimization prevents multiple "instanceof" checks.
	 */
	private boolean replacing = true;

	/**
	 * This method is the default constructor.
	 */
	public AbstractNeuronPanel() {
		this.setLayout(new BorderLayout());
	}

	/**
	 * Populate fields with current data.
	 */
	public abstract void fillFieldValues(
			final List<NeuronUpdateRule> ruleList);

	/**
	 * Populate fields with default data.
	 */
	public abstract void fillDefaultValues();

	/**
	 * Called to commit changes to a single neuron. Usually this is a template
	 * neuron intended to be copied for the purpose of creating many new
	 * neurons. Using this method to commit changes to many neurons is not
	 * recommended. Instead pass a list of the neurons to be changed into
	 * {@link #commitChanges(List) commitChanges}.
	 */
	public abstract void commitChanges(final Neuron neuron);

	/**
	 * Called externally when the dialog is closed, to commit any changes made
	 * to many neurons simultaneously. This method by default overwrites the
	 * neurons' update rules. To change this behavior set {@link #replacing} to
	 * <b> false </b>, indicating to the panel that it is editing rather than
	 * changing/replacing existing neuron update rules.
	 */
	public abstract void commitChanges(final List<Neuron> neuron);

	protected abstract void
			writeValuesToRules(final List<Neuron> neurons);

	/**
	 * Override to add custom notes or other text to bottom of panel. Can be
	 * html formatted.
	 * 
	 * @param text
	 *            Text to be added
	 */
	public void addBottomText(final String text) {
		JPanel labelPanel = new JPanel();
		JLabel theLabel = new JLabel(text);
		labelPanel.add(theLabel);
		this.add(labelPanel, BorderLayout.SOUTH);
	}

	/**
	 * @return the neuron update rule being edited.
	 */
	public abstract NeuronUpdateRule getPrototypeRule();

	protected boolean isReplace() {
		return replacing;
	}

	protected void setReplace(boolean replace) {
		this.replacing = replace;
	}

	/**
	 * @return the rule list
	 */
	public static String[] getRulelist() {
		return RULE_MAP.keySet().toArray(new String[RULE_MAP.size()]);
	}

	/**
	 * @return the generator list
	 */
	public static String[] getGeneratorlist() {
		return GENERATOR_MAP.keySet()
				.toArray(new String[RULE_MAP.size()]);
	}

	/**
	 * TODO: This is a general utility, where is a better place to put it? Tests
	 * whether the string in a text field is parsable into a double. If so, the
	 * double value is returned, if not Double.NaN is returned as a flag that
	 * the (double) parsing failed. This method compactly handles exceptions
	 * thrown by Double.parseDouble(String), so that try/catch fields do not
	 * have to be repeatedly written, and allows the program to continue in the
	 * case that the string is not parsable.
	 * 
	 * @param tField
	 *            The text field to read from and test if its text can be parsed
	 *            into a double value
	 * @return Either the double value of the String in the text field, if it
	 *         can be parsed, or NaN, as a flag, if it cannot be.
	 */
	protected static double doubleParsable(JTextField tField) {
		try {
			return Double.parseDouble(tField.getText());
		} catch (NullPointerException | NumberFormatException ex) {
			return Double.NaN;
		}
	}

}
