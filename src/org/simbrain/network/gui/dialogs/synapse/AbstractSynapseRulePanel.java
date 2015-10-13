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
package org.simbrain.network.gui.dialogs.synapse;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.HebbianCPCARulePanel;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.HebbianRulePanel;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.HebbianThresholdRulePanel;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.OjaRulePanel;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.STDPRulePanel;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.ShortTermPlasticityRulePanel;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.StaticSynapsePanel;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.SubtractiveNormalizationRulePanel;
import org.simbrain.network.synapse_update_rules.HebbianCPCARule;
import org.simbrain.network.synapse_update_rules.HebbianRule;
import org.simbrain.network.synapse_update_rules.HebbianThresholdRule;
import org.simbrain.network.synapse_update_rules.OjaRule;
import org.simbrain.network.synapse_update_rules.STDPRule;
import org.simbrain.network.synapse_update_rules.ShortTermPlasticityRule;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.synapse_update_rules.SubtractiveNormalizationRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>AbstractSynapsePanel</b> is the parent class for all panels used to set
 * parameters of specific synapse update rules.
 */
public abstract class AbstractSynapseRulePanel extends JPanel {

    /**
     * A mapping of available update rules to their respective panels. Used as a
     * reference (especially for combo-boxes) by GUI classes.
     */
    public static final LinkedHashMap<String, AbstractSynapseRulePanel> RULE_MAP =
        new LinkedHashMap<String, AbstractSynapseRulePanel>();

    // Populate synapse rule map
    static {
        // TODO: Backwards compatibility. Remove for 3.0 after converting all
        // sims
        RULE_MAP.put(new StaticSynapseRule().getDescription(),
            new StaticSynapsePanel());
        RULE_MAP
            .put(new HebbianRule().getDescription(), new HebbianRulePanel());
        RULE_MAP.put(new HebbianCPCARule().getDescription(),
            new HebbianCPCARulePanel());
        RULE_MAP.put(new HebbianThresholdRule().getDescription(),
            new HebbianThresholdRulePanel());
        RULE_MAP.put(new OjaRule().getDescription(), new OjaRulePanel());
        //RULE_MAP.put(new ShortTermPlasticityRule().getDescription(),
        //    new ShortTermPlasticityRulePanel());
        RULE_MAP.put(new STDPRule().getDescription(), new STDPRulePanel());
        RULE_MAP.put(new SubtractiveNormalizationRule().getDescription(),
            new SubtractiveNormalizationRulePanel());
    }

    /**
     * A flag used to indicate whether this panel will be replacing synapse
     * update rules or simply writing to them. In cases where the panel
     * represents the same rule as the rule of each of the synapses (i.e.
     * Hebbian panel &#38; Hebbian synapses) the synapses' update rules are edited,
     * not replaced. However, if the panel does not correspond to the synapse
     * update rule of the synapses being edited, then new SynapseUpdateRule
     * objects are created, and replace the old rule. This optimization prevents
     * multiple redundant "instanceof" checks.
     */
    private boolean replacing = true;

    /** Main panel. */
    private final LabelledItemPanel mainPanel = new LabelledItemPanel();

    /**
     * Adds an item.
     *
     * @param text
     *            label of item to add
     * @param comp
     *            component to add
     */
    public void addItem(final String text, final JComponent comp) {
        mainPanel.addItem(text, comp);
    }

    /**
     * Add item label.
     *
     * @param text
     *            label to add
     * @param comp
     *            component to apply label
     */
    public void addItemLabel(final JLabel text, final JComponent comp) {
        mainPanel.addItemLabel(text, comp);
    }

    /**
     * This method is the default constructor.
     */
    public AbstractSynapseRulePanel() {
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * 
     * @return
     */
    public abstract AbstractSynapseRulePanel deepCopy();

    /**
     * Populate fields with current data.
     *
     * @param ruleList
     *            the list of rules from which variables will be displayed
     */
    public abstract void
        fillFieldValues(final List<SynapseUpdateRule> ruleList);

    /**
     * Populate fields with default data.
     */
    public abstract void fillDefaultValues();

    /**
     * Commit changes to the panel to the synapse update rule of a template
     * synapse. Generally this method is used in the synapse creation process
     * rather than the synapse editing process (not counting calls within this
     * class). In the creation case, the template synapse including the rule
     * edited by this panel is copied. There is no reason outside convention for
     * this to be the case. Do NOT use this method repeatedly to commit changes
     * to multiple synapses, {@link #commitChanges(Collection)} does this much more
     * efficiently.
     *
     * @param synapse
     *            the synapse being edited.
     */
    public abstract void commitChanges(final Synapse synapse);

    /**
     * Commit changes to the panel to the synapse update rules of the synapses
     * being edited. Generally this method is used in the synapse editing
     * process rather than the synapse creation process, but there is no reason
     * outside convention for this to be the case.
     *
     * @param synapses
     *            the synapses being edited
     */
    public abstract void commitChanges(final Collection<Synapse> synapses);

    /**
     * Edits synapse update rules that already exist. This is the alternative to
     * replacing the rules and occurs when the synapse update rules being edited
     * are the same type as the panel. {@link #replacing} is the flag for
     * whether the synapse rules are replaced by a new rule before this rule is
     * called.
     *
     * @param synapses
     *            the neurons whose rules are being <b>edited</b>, not replaced.
     */
    protected abstract void writeValuesToRules(
        final Collection<Synapse> synapses);

    /**
     * Are we replacing rules or editing them? Replacing happens when
     * {@link #commitChanges(Collection)} is called on a synapse panel whose rule is
     * different from the rules of the synapses being edited.
     *
     * @return replacing or editing
     */
    protected boolean isReplace() {
        return replacing;
    }

    /**
     * Tells this panel whether it is going to be editing synapse update rules,
     * or creating new ones and replacing the update rule of each of the
     * synapses being edited.
     *
     * @param replace
     *            tell the panel if it's replacing rules or editing them
     */
    protected void setReplace(boolean replace) {
        this.replacing = replace;
    }

    /**
     * @return the ruleList
     */
    public static String[] getRuleList() {
        return RULE_MAP.keySet().toArray(new String[RULE_MAP.size()]);
    }

    /**
     * Each synapse panel contains a static final subclass of SynapseUpdateRule
     * variable called a prototype rule. The specific subclass of
     * SynapseUpdateRule corresponds to the rule specified by the panel name.
     * Used so that other classes can query specific properties of the rule the
     * panel edits. Also used internally to make deep copies.
     *
     * @return an instance of the synapse rule which corresponds to the panel.
     */
    public abstract SynapseUpdateRule getPrototypeRule();

    /**
     * Add notes or other text to bottom of panel. Can be html formatted.
     *
     * @param text
     *            Text to add to bottom of panel
     */
    public void addBottomText(final String text) {
        JPanel labelPanel = new JPanel();
        JLabel theLabel = new JLabel(text);
        labelPanel.add(theLabel);
        this.add(labelPanel, BorderLayout.SOUTH);
    }

}
