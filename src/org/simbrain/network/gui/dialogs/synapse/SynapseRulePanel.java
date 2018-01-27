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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

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
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
import org.simbrain.util.widgets.EditablePanel;

/**
 * A panel for setting the synapse type and changing the parameters of the
 * selected update rule.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class SynapseRulePanel extends JPanel implements EditablePanel {

    /**
     * The synapses being modified.
     */
    private final Collection<Synapse> synapseCollection;

    /**
     * Null string.
     */
    public static final String NULL_STRING = "...";

    /**
     * The default display state of the synapse panel. Currently, True, that is,
     * by default, the synapse panel corresponding to the rule in the combo box
     * is visible.
     */
    private static final boolean DEFAULT_SP_DISPLAY_STATE = true;

    /**
     * Synapse type combo box.
     */
    private final JComboBox<String> cbSynapseType = new JComboBox<String>(
        RULE_MAP.keySet().toArray(new String[RULE_MAP.size()]));

    /**
     * Panel for editing synapses.
     */
    private AnnotatedPropertyEditor synapsePanel;

    /**
     * For showing/hiding the synapse panel.
     */
    private final DropDownTriangle displaySPTriangle;

    /**
     * The originally displayed abstract synapse panel. If the currently
     * displayed panel is not the same as the starting panel, then we can be
     * sure that we are not editing synapse update rules, but rather are
     * replacing them.
     */
    private final AnnotatedPropertyEditor startingPanel;

    /**
     * A reference to the parent window, for resizing after panel content
     * changes.
     */
    private final Window parent;

    /**
     * A mapping of available update rules to their respective panels.
     */
    public static final LinkedHashMap<String, AnnotatedPropertyEditor> RULE_MAP = new LinkedHashMap();

    /**
     * Add new synapse rules here.
     */
    static {
        RULE_MAP.put(new StaticSynapseRule().getName(),
            new AnnotatedPropertyEditor(new StaticSynapseRule()));
        RULE_MAP.put(new HebbianRule().getName(),
            new AnnotatedPropertyEditor(new HebbianRule()));
        RULE_MAP.put(new HebbianCPCARule().getName(),
            new AnnotatedPropertyEditor(new HebbianCPCARule()));
        RULE_MAP.put(new HebbianThresholdRule().getName(),
            new AnnotatedPropertyEditor(new HebbianThresholdRule()));
        RULE_MAP.put(new OjaRule().getName(),
            new AnnotatedPropertyEditor(new OjaRule()));
        RULE_MAP.put(new PfisterGerstner2006Rule().getName(),
            new AnnotatedPropertyEditor(new PfisterGerstner2006Rule()));
        // RULE_MAP.put(new ShortTermPlasticityRule().getDescription(),
        // new ShortTermPlasticityRulePanel());
        RULE_MAP.put(new STDPRule().getName(),
            new AnnotatedPropertyEditor(new STDPRule()));
        RULE_MAP.put(new SubtractiveNormalizationRule().getName(),
            new AnnotatedPropertyEditor(
                new SubtractiveNormalizationRule()));
    }

    /**
     * Create the panel with default starting visibility.
     *
     * @param synapseList the list of synapses being edited
     * @param parent      parent window referenced for resizing via "Pack"
     */
    public SynapseRulePanel(Collection<Synapse> synapseList,
                            final Window parent) {
        this(synapseList, parent, DEFAULT_SP_DISPLAY_STATE);
    }

    /**
     * Construct the panel with specified starting visibility.
     *
     * @param synapseList   the list of synapses being edited
     * @param parent        parent window referenced for resizing via "Pack"
     * @param startingState whether or not the dropdown showing rule parameters
     *                      should be visible by default.
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

        // Combo box and drop down triangle
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.add(cbSynapseType);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(displaySPTriangle);
        topPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.setBorder(padding);
        this.add(topPanel);
        this.add(Box.createRigidArea(new Dimension(0, 5)));

        // The drop-down synapse panel
        synapsePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        synapsePanel.setBorder(padding);
        synapsePanel.setVisible(displaySPTriangle.isDown());
        this.add(synapsePanel);

        // Border
        TitledBorder tb2 = BorderFactory.createTitledBorder("Update Rule");
        this.setBorder(tb2);

    }

    /**
     * Initialize the main synapse panel based on the type of the selected
     * synapses. Assumes > 0 synapses are selected.
     */
    private void initSynapseType() {
        Iterator<Synapse> synIter = synapseCollection.iterator();
        Synapse synapseRef = synIter.next();

        // Check whether the set of synapses being edited are of the
        // same type or not
        boolean discrepancy = false;
        while (synIter.hasNext()) {
            if (!synapseRef.getLearningRule().getClass()
                .equals(synIter.next().getLearningRule().getClass()))
            {
                discrepancy = true;
                break;
            }
        }

        // If they are different types, display combo box as null
        if (discrepancy) {
            cbSynapseType.addItem(SimbrainConstants.NULL_STRING);
            cbSynapseType.setSelectedIndex(cbSynapseType.getItemCount() - 1);
            // Simply to serve as an empty panel
            synapsePanel = new AnnotatedPropertyEditor(Collections.EMPTY_LIST);
        } else {
            // If they are the same type, use the appropriate editor panel.
            // Later if ok is pressed the values from that panel will be written
            // to the rules
            String synapseName = synapseRef.getLearningRule().getName();
            synapsePanel = RULE_MAP.get(synapseName);
            List<EditableObject> ruleList = synapseCollection.stream()
                .map(Synapse::getLearningRule).collect(Collectors.toList());
            synapsePanel.fillFieldValues(ruleList);
            cbSynapseType.setSelectedItem(synapseName);
        }
    }

    /**
     * Adds the listeners to this dialog.
     */
    private void addListeners() {

        // Respond to triangle drop down clicks
        displaySPTriangle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                synapsePanel.setVisible(displaySPTriangle.isDown());
                repaint();
                parent.pack();
            }
        });

        // Respond to combo box changes
        cbSynapseType.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                synapsePanel = RULE_MAP.get(cbSynapseType.getSelectedItem());

                // Is the current panel different from the starting panel?
                // If so, we  are changing the synapse type
                boolean replace = synapsePanel != startingPanel;

                if (replace) {
                    // If so we have to fill the new panel with default values
                    synapsePanel.fillDefaultValues();
                }

                //TODO
                // Tell the new panel whether it will have to replace
                // synapse update rules or edit them upon commit.
                //synapsePanel.setReplace(replace);

                repaintPanel();
                parent.pack();

            }

        });

    }

    @Override
    public boolean commitChanges() {

        SynapseUpdateRule selectedRule = (SynapseUpdateRule) synapsePanel.getEditedObject();

        // If an inconsistent set of objects is being edited return with no action
        if (selectedRule == null) {
            return true;
        }

        // TODO: Replace check
        for (Synapse s : synapseCollection) {
            // Only replace if this is a different rule (otherwise when
            // editing multiple rules with different parameter values which
            // have not been set those values will be replaced with the
            // default).
            if (!s.getLearningRule().getClass()
                .equals(selectedRule.getClass()))
            {
                s.setLearningRule(selectedRule.deepCopy());
            }
        }

        // TODO: Think about this in relation to above
        List<EditableObject> ruleList = synapseCollection.stream()
            .map(Synapse::getLearningRule).collect(Collectors.toList());
        synapsePanel.commitChanges(ruleList);
        return true;
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
     * @return the name of the selected synapse update rule
     */
    public JComboBox<String> getCbSynapseType() {
        return cbSynapseType;
    }

    @Override
    public void fillFieldValues() {
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

}
