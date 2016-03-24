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

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.EditablePanel;

/**
 * Panel for editing the properties of neuron, including general properties 
 * (e.g. activation and label) and selecting and editing a specific update rule.
 *
 * Called both from neuron dialog and from other dialogs that use it, e.g. the
 * add neurons (plural) dialog, and neuron group dialogs.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class NeuronPropertiesPanel extends JPanel implements EditablePanel {

    /** The basic neuron info panel. */
    private GeneralNeuronPropertiesPanel generalNeuronPropertiesPanel;

    /** The neuron update settings panel. */
    private UpdateRulePanel updateRulePanel;

    /**
     * The default vertical gap between the basic neuron info panel and the
     * neuron update settings panel.
     */
    private static final int DEFAULT_VGAP = 10;
    
    /** Whether to initially display the update rule panel. */ 
    private static boolean DEFAULT_DISPLAY_UPDATE_RULE_PANEL = true;
       
    /** Static initializer */
    static {
        Properties properties = Utils.getSimbrainProperties();
        if (properties.containsKey("useNativeFileChooser")) {
            DEFAULT_DISPLAY_UPDATE_RULE_PANEL = Boolean.parseBoolean(properties
                .getProperty("initializeNeuronDialogToExpandedState"));
        }
    }
    
    /**
     * Creates a neuron property panel with a default display state.
     *
     * @param neuronList
     *            the list of neurons either being edited (editing) or being
     *            used to fill the panel with default values (creation).
     * @param parent
     *            the parent window, made available for easy resizing.
     * @return the property panel
     */
    public static NeuronPropertiesPanel createNeuronPropertiesPanel(
        final List<Neuron> neuronList, final Window parent) {
        return createNeuronPropertiesPanel(neuronList, parent,
                DEFAULT_DISPLAY_UPDATE_RULE_PANEL);
    }

    /**
     * Create the panel without specifying whether to display id (that is done
     * automatically).
     *
     * @param neuronList
     *            the list of neurons either being edited (editing) or being
     *            used to fill the panel with default values (creation).
     * @param parent
     *            the parent window, made available for easy resizing.
     * @param displayUpdateRuleProperties
     *            whether or not to display the neuron update rule properties
     * @return the property panel
     */
    public static NeuronPropertiesPanel createNeuronPropertiesPanel(
            final List<Neuron> neuronList, final Window parent,
            final boolean displayUpdateRuleProperties) {
        NeuronPropertiesPanel cnip = new NeuronPropertiesPanel(neuronList,
                parent, displayUpdateRuleProperties);
        cnip.initializeLayout();
        return cnip;
    }

    /**
     * {@link #createNeuronPropertiesPanel(List, Window, boolean, boolean)}
     */
    private NeuronPropertiesPanel(final List<Neuron> neuronList,
        final Window parent, final boolean displayUpdateRuleProperties,
        final boolean displayID) {
        generalNeuronPropertiesPanel = GeneralNeuronPropertiesPanel.createPanel(
            neuronList, parent, displayID);
        updateRulePanel = new UpdateRulePanel(neuronList, parent,
                displayUpdateRuleProperties);
    }
    
    /**
     * Construct the panel without specifying whether to display id (that is done
     * automatically).
     */
    private NeuronPropertiesPanel(final List<Neuron> neuronList,
        final Window parent, final boolean displayUpdateRuleProperties) {
        generalNeuronPropertiesPanel = GeneralNeuronPropertiesPanel.createPanel(
            neuronList, parent);
        updateRulePanel = new UpdateRulePanel(neuronList, parent,
                displayUpdateRuleProperties);
    }

    /**
     * Lays out the panel.
     */
    private void initializeLayout() {

        // Respond to update panel combo box changes here, so that general panel can be updated too
        updateRulePanel.getCbNeuronType().addActionListener(
                e -> SwingUtilities.invokeLater(() -> {
                    generalNeuronPropertiesPanel
                            .updateFieldVisibility(updateRulePanel.getNeuronRulePanel().getPrototypeRule());
                        repaint();
                }));

        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        this.setLayout(layout);
        this.add(generalNeuronPropertiesPanel);
        this.add(Box.createVerticalStrut(DEFAULT_VGAP));
        this.add(updateRulePanel);
    }

    /**
     * Commits changes in the two sub-panels.
     */
    @Override
    public boolean commitChanges() {

        boolean success = true;

        // Commit changes specific to the neuron type
        // This must be the first change committed, as other neuron panels
        // make assumptions about the type of the neuron update rule being
        // edited that can result in ClassCastExceptions otherwise.
        success &= updateRulePanel.commitChanges();

        success &= generalNeuronPropertiesPanel.commitChanges();

        return success;

    }

    /**
     * Access to combo box in update rule panel sometimes needed, to add listeners
     * or determine what the current update rule is.
     *
     * @return the update rule panel
     */
    public UpdateRulePanel getUpdateRulePanel() {
        return updateRulePanel;
    }
    
    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void fillFieldValues() {
    }
    
    /**
     * Testing main.
     */
    public static void main(String[] args) {

        // Set up a large inconsistent set of rules.
        List<Neuron> neuronList = new ArrayList<Neuron>();
        Network network = new Network();
        for (int i = 0; i < 10000; i++) {
            Neuron neuron = new Neuron(network);
            LinearRule rule = new LinearRule();
            neuron.setUpdateRule(rule);
            neuronList.add(neuron);
        }
//        ((LinearRule) neuronList.get(1).getUpdateRule()).setSlope(-1); // Add an inconsistency in                                                     // slope
        ((LinearRule) neuronList.get(1).getUpdateRule()).setAddNoise(true); // Add a boolean inconsistency

        // Test the test panel!
        NeuronDialog dialog = NeuronDialog.createNeuronDialog(neuronList);
        
        // Show the test dialog
        dialog.setLocationRelativeTo(null);
        dialog.pack();
        dialog.setVisible(true);
        
        System.out.println("Slope "
                + ((LinearRule) neuronList.get(1).getUpdateRule()).getSlope());
        System.out.println("Bias "
                + ((LinearRule) neuronList.get(1).getUpdateRule()).getBias());
        System.out.println(
                "Add noise " + ((LinearRule) neuronList.get(1).getUpdateRule())
                        .getAddNoise());
    }

}
