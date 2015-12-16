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

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.LogisticGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.RandomGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.SinusoidalGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.StochasticGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.BinaryRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.ContinuousSigmoidalRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.DecayRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.FitzhughNagumoRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.IACRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.IntegrateAndFireRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.IzhikevichRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.LinearRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.MorrisLecarRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.NakaRushtonRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.ProductRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.SigmoidalRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.SpikingThresholdRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.ThreeValueRulePanel;
import org.simbrain.network.neuron_update_rules.AdExIFRule;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.neuron_update_rules.ContinuousSigmoidalRule;
import org.simbrain.network.neuron_update_rules.DecayRule;
import org.simbrain.network.neuron_update_rules.FitzhughNagumo;
import org.simbrain.network.neuron_update_rules.IACRule;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.network.neuron_update_rules.IzhikevichRule;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.MorrisLecarRule;
import org.simbrain.network.neuron_update_rules.NakaRushtonRule;
import org.simbrain.network.neuron_update_rules.ProductRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule;
import org.simbrain.network.neuron_update_rules.ThreeValueRule;
import org.simbrain.network.neuron_update_rules.activity_generators.LogisticRule;
import org.simbrain.network.neuron_update_rules.activity_generators.RandomNeuronRule;
import org.simbrain.network.neuron_update_rules.activity_generators.SinusoidalRule;
import org.simbrain.network.neuron_update_rules.activity_generators.StochasticRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.randomizer.Randomizer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * <b>AbstractNeuronPanel</b> is the parent class for all panels used to set
 * parameters of specific neuron rule types.
 * <p/>
 * Optimization has been emphasized for methods intended for neuron creation
 * rather than editing on the assumption that the former will be far more common
 * for large numbers of neurons.
 */
@SuppressWarnings("serial")
public abstract class AbstractNeuronRulePanel extends JPanel {

    /**
     * Associations between names of rules and panels for editing them.
     */
    public static final LinkedHashMap<String, AbstractNeuronRulePanel> RULE_MAP =
            new LinkedHashMap<String, AbstractNeuronRulePanel>();

    // Populate the Rule Map.  Place items in alphabetical order so they appear that way in the GUI combo box.
    static {
        RULE_MAP.put(new AdExIFRule().getDescription(), new AdExIFRulePanel());
        RULE_MAP.put(new BinaryRule().getDescription(), new BinaryRulePanel());
        RULE_MAP.put(new DecayRule().getDescription(), new DecayRulePanel());
        RULE_MAP.put(new FitzhughNagumo().getDescription(), new FitzhughNagumoRulePanel());
        RULE_MAP.put(new IACRule().getDescription(), new IACRulePanel());
        RULE_MAP.put(new IntegrateAndFireRule().getDescription(),
                new IntegrateAndFireRulePanel());
        RULE_MAP.put(new IzhikevichRule().getDescription(),
                new IzhikevichRulePanel());
        RULE_MAP.put(new LinearRule().getDescription(), new LinearRulePanel());
        RULE_MAP.put(new MorrisLecarRule().getDescription(), new MorrisLecarRulePanel());
        RULE_MAP.put(new NakaRushtonRule().getDescription(),
                new NakaRushtonRulePanel());
        RULE_MAP.put(new ProductRule().getDescription(), new ProductRulePanel());
        RULE_MAP.put(new ContinuousSigmoidalRule().getDescription(),
                ContinuousSigmoidalRulePanel.createContinuousSigmoidalRulePanel());
        RULE_MAP.put(new SigmoidalRule().getDescription(),
                SigmoidalRulePanel.createSigmoidalRulePanel());
        RULE_MAP.put(new SpikingThresholdRule().getDescription(),
                new SpikingThresholdRulePanel());
        RULE_MAP.put(new ThreeValueRule().getDescription(),
                new ThreeValueRulePanel());
    }

    /**
     * Associations between names of activity generators and panels for editing
     * them.
     */
    public static final LinkedHashMap<String, AbstractNeuronRulePanel> GENERATOR_MAP =
            new LinkedHashMap<String, AbstractNeuronRulePanel>();

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
     * represents the same rule as the rule (i.e. Linear panel &#38; linear neurons)
     * the neurons' update rules are edited, not replaced. However, if the panel
     * does not correspond to the currently used neuron update rule, new
     * NeuronUpdateRule objects are created, and replace the old rule. This
     * optimization prevents multiple "instanceof" checks.
     */
    private boolean replacing = true;

    /**
     * This method is the default constructor.
     */
    public AbstractNeuronRulePanel() {
        this.setLayout(new BorderLayout());
    }

    /**
     * Populate fields with current data.
     *
     * @param ruleList the list of rules being used to determine which values should
     *                 be used to fill the fields with data.
     */
    public abstract void fillFieldValues(final List<NeuronUpdateRule> ruleList);

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
     *
     * @param neuron the neuron to which changes are being committed to.
     */
    public abstract void commitChanges(final Neuron neuron);

    /**
     * Called externally when the dialog is closed, to commit any changes made
     * to many neurons simultaneously. This method by default overwrites the
     * neurons' update rules. To change this behavior set {@link #replacing} to
     * <b> false </b>, indicating to the panel that it is editing rather than
     * changing/replacing existing neuron update rules.
     *
     * @param neurons the list of neurons which are being edited and to which
     *                changes based on the values in the fields of this panel will
     *                be committed
     */
    public abstract void commitChanges(final List<Neuron> neurons);

    /**
     * Edits neuron update rules that already exist. This is the alternative to
     * replacing the rules and occurs when the neuron update rules being edited
     * are the same type as the panel. {@link #replacing} is the flag for
     * whether this method is used for committing or the rules are deleted and
     * replaced entirely, in which case this method is not called.
     *
     * @param neurons the neurons whose rules are being <b>edited</b>, not replaced.
     */
    protected abstract void writeValuesToRules(final List<Neuron> neurons);

    /**
     * Override to add custom notes or other text to bottom of panel. Can be
     * html formatted.
     *
     * @param text Text to be added
     */
    public void addBottomText(final String text) {
        JPanel labelPanel = new JPanel();
        JLabel theLabel = new JLabel(text);
        labelPanel.add(theLabel);
        this.add(labelPanel, BorderLayout.SOUTH);
    }

    /**
     * Each neuron panel contains a static final subclass of NeuronUpdateRule
     * variable called a prototype rule. The specific subclass of
     * NeuronUpdateRule corresponds to the rule specified by the panel name.
     * Used so that other classes can query specific properties of the rule the
     * panel edits. Also used internally to make deep copies.
     *
     * @return an instance of the neuron rule which corresponds to the panel.
     */
    protected abstract NeuronUpdateRule getPrototypeRule();

    /**
     * Are we replacing rules or editing them? Replacing happens when
     * {@link #commitChanges(List)} is called on a neuron panel whose rule is
     * different from the rules of the neurons being edited.
     *
     * @return replacing or editing
     */
    protected boolean isReplace() {
        return replacing;
    }

    /**
     * Tells this panel whether it is going to be editing neuron update rules,
     * or creating new ones and replacing the update rule of each of the neurons
     * being edited.
     *
     * @param replace used to tell the panel if it's being used to replace neuron
     *                update rules.
     */
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
        return GENERATOR_MAP.keySet().toArray(new String[RULE_MAP.size()]);
    }

    /**
     * @return List of randomizers.
     */
    public static ArrayList<Randomizer> getRandomizers(
            List<NeuronUpdateRule> ruleList) throws ClassCastException {
        ArrayList<Randomizer> ret = new ArrayList<Randomizer>();
        for (int i = 0; i < ruleList.size(); i++) {
            ret.add(((NoisyUpdateRule) ruleList.get(i)).getNoiseGenerator());
        }
        return ret;
    }

}