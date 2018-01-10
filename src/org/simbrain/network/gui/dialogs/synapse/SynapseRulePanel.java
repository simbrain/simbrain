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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.synapse_update_rules.HebbianCPCARule;
import org.simbrain.network.synapse_update_rules.HebbianRule;
import org.simbrain.network.synapse_update_rules.HebbianThresholdRule;
import org.simbrain.network.synapse_update_rules.OjaRule;
import org.simbrain.network.synapse_update_rules.PfisterGerstner2006Rule;
import org.simbrain.network.synapse_update_rules.STDPRule;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.synapse_update_rules.SubtractiveNormalizationRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
import org.simbrain.util.widgets.EditablePanel;

/**
 * A panel for setting the synapse type and changing the parameters of the
 * selected update rule.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 *
 */
@SuppressWarnings("serial")
public class SynapseRulePanel extends JPanel implements EditablePanel {

    /** The synapses being modified. */
    private final Collection<Synapse> synapseCollection;

    /** Null string. */
    public static final String NULL_STRING = "...";

    /**
     * The default display state of the synapse panel. Currently, True, that is,
     * by default, the synapse panel corresponding to the rule in the combo box
     * is visible.
     */
    private static final boolean DEFAULT_SP_DISPLAY_STATE = true;

    /** Synapse type combo box. */
    private final JComboBox<String> cbSynapseType = new JComboBox<String>(
            RULE_MAP.keySet().toArray(new String[RULE_MAP.size()]));

    /** Synapse panel. */
    private AbstractSynapseRulePanel synapsePanel;

    /** For showing/hiding the synapse panel. */
    private final DropDownTriangle displaySPTriangle;

    /**
     * The originally displayed abstract synapse panel. If the currently
     * displayed panel is not the same as the starting panel, then we can be
     * sure that we are not editing synapse update rules, but rather are
     * replacing them.
     */
    private final AbstractSynapseRulePanel startingPanel;

    /**
     * A reference to the parent window, for resizing after panel content
     * changes.
     */
    private final Window parent;

    /**
     * A mapping of available update rules to their respective panels. Used as a
     * reference (especially for combo-boxes) by GUI classes.
     */
    public static final LinkedHashMap<String, AbstractSynapseRulePanel> RULE_MAP = new LinkedHashMap<String, AbstractSynapseRulePanel>();

    // Populate synapse rule map
    static {
        RULE_MAP.put(new StaticSynapseRule().getName(),
                new SynapseRulePropertiesPanel(new StaticSynapseRule()));
        RULE_MAP.put(new HebbianRule().getName(),
                new SynapseRulePropertiesPanel(new HebbianRule()));
        RULE_MAP.put(new HebbianCPCARule().getName(),
                new SynapseRulePropertiesPanel(new HebbianCPCARule()));
        RULE_MAP.put(new HebbianThresholdRule().getName(),
                new SynapseRulePropertiesPanel(new HebbianThresholdRule()));
        RULE_MAP.put(new OjaRule().getName(),
                new SynapseRulePropertiesPanel(new OjaRule()));
        RULE_MAP.put(new PfisterGerstner2006Rule().getName(),
                new SynapseRulePropertiesPanel(new PfisterGerstner2006Rule()));
        // RULE_MAP.put(new ShortTermPlasticityRule().getDescription(),
        // new ShortTermPlasticityRulePanel());
        RULE_MAP.put(new STDPRule().getName(),
                new SynapseRulePropertiesPanel(new STDPRule()));
        RULE_MAP.put(new SubtractiveNormalizationRule().getName(),
                new SynapseRulePropertiesPanel(
                        new SubtractiveNormalizationRule()));
    }

    /**
     * Create the panel with specified starting visibility.
     *
     * @param synapseList the list of synapses being edited
     * @param parent parent window referenced for resizing via "Pack"
     */
    public SynapseRulePanel(Collection<Synapse> synapseList,
            final Window parent) {
        this(synapseList, parent, DEFAULT_SP_DISPLAY_STATE);
    }

    /**
     * Construct the panel with default starting visibility.
     *
     * @param synapseList the list of synapses being edited
     * @param parent parent window referenced for resizing via "Pack"
     * @param startingState the starting state of whether or not details of the
     *            rule are initially visible
     */
    public SynapseRulePanel(Collection<Synapse> synapseList,
            final Window parent, boolean startingState) {
        this.synapseCollection = synapseList;
        this.parent = parent;
        displaySPTriangle = new DropDownTriangle(UpDirection.LEFT,
                startingState, "Settings", "Settings", parent);
        initSynapseType();
        startingPanel = synapsePanel;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        initializeLayout();
        addListeners();
    }

    /**
     * Lays out this panel.
     */
    private void initializeLayout() {

        Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        JPanel tPanel = new JPanel();
        tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.X_AXIS));
        tPanel.add(cbSynapseType);
        tPanel.add(Box.createHorizontalStrut(20));
        tPanel.add(displaySPTriangle);

        tPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tPanel.setBorder(padding);
        this.add(tPanel);

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        synapsePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        synapsePanel.setBorder(padding);
        synapsePanel.setVisible(displaySPTriangle.isDown());
        this.add(synapsePanel);

        TitledBorder tb2 = BorderFactory.createTitledBorder("Update Rule");
        this.setBorder(tb2);

    }

    /**
     * Adds the listeners to this dialog.
     */
    private void addListeners() {
        displaySPTriangle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                synapsePanel.setVisible(displaySPTriangle.isDown());
                repaint();
                parent.pack();
            }
        });

        cbSynapseType.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                synapsePanel = RULE_MAP.get(cbSynapseType.getSelectedItem())
                        .deepCopy();

                // Is the current panel different from the starting panel?
                boolean replace = synapsePanel != startingPanel;

                if (replace) {
                    // If so we have to fill the new panel with default values
                    synapsePanel.fillDefaultValues();
                }

                // Tell the new panel whether it will have to replace
                // synapse update rules or edit them upon commit.
                synapsePanel.setReplace(replace);

                repaintPanel();
                parent.pack();

            }

        });

    }

    /**
     * Called to repaint the panel based on changes in the to the selected
     * synapse type.
     */
    public void repaintPanel() {
        removeAll();
        initializeLayout();
        repaint();
    }

    /**
     * Initialize the main synapse panel based on the type of the selected
     * synapses. Assumes > 0 synapses are selected.
     */
    private void initSynapseType() {
        Iterator<Synapse> synIter = synapseCollection.iterator();
        Synapse protoSyn = synIter.next();
        boolean discrepancy = false;
        while (synIter.hasNext()) {
            if (!protoSyn.getLearningRule().getClass()
                    .equals(synIter.next().getLearningRule().getClass())) {
                discrepancy = true;
                break;
            }
        }
        if (discrepancy) {
            cbSynapseType.addItem(SimbrainConstants.NULL_STRING);
            cbSynapseType.setSelectedIndex(cbSynapseType.getItemCount() - 1);
            // Simply to serve as an empty panel
            synapsePanel = new SynapseRulePropertiesPanel();
        } else {
            List<SynapseUpdateRule> synapseList = Synapse
                    .getRuleList(synapseCollection);
            String synapseName = synapseList.get(0).getName();
            synapsePanel = RULE_MAP.get(synapseName).deepCopy();
            synapsePanel.fillFieldValues(synapseList);
            cbSynapseType.setSelectedItem(synapseName);
        }
    }

    /**
     * @return the name of the selected synapse update rule
     */
    public JComboBox<String> getCbSynapseType() {
        return cbSynapseType;
    }

    /**
     * @return a template synapse update rule object associated with the
     *         selected synapse update rule panel.
     */
    public SynapseUpdateRule getTemplateRule() {
        SynapseUpdateRule rule = synapsePanel.getPrototypeRule().deepCopy();
        Synapse s = Synapse.getTemplateSynapse();
        s.setLearningRule(rule);
        synapsePanel.writeValuesToRules(Collections.singleton(s));
        return rule;
    }

    /**
     * @return the currently displayed synapse panel
     */
    public AbstractSynapseRulePanel getSynapsePanel() {
        return synapsePanel;
    }

    /**
     * @param synapsePanel set the currently displayed synapse panel to the
     *            specified panel
     */
    public void setSynapsePanel(AbstractSynapseRulePanel synapsePanel) {
        this.synapsePanel = synapsePanel;
    }

    @Override
    public void fillFieldValues() {
    }

    @Override
    public boolean commitChanges() {
        synapsePanel.commitChanges(synapseCollection);
        return true; // TODO:Finish implementation of CommittablePanel interface
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

}
