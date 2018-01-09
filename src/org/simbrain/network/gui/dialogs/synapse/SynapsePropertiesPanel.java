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

import java.awt.Window;
import java.util.Collection;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.dialogs.neuron.GeneralNeuronPropertiesPanel;
import org.simbrain.network.gui.dialogs.neuron.UpdateRulePanel;
import org.simbrain.util.widgets.EditablePanel;

/**
 * This panel combines synapse editing sub-panels and handles changes to one
 * being applied to the others.
 * 
 * TODO: See NeuronPropertiesPanel docs
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
public class SynapsePropertiesPanel extends JPanel implements EditablePanel {

    /**
     * The default vertical gap between the basic synapse info panel and the
     * synapse update settings panel.
     */
    private static final int DEFAULT_VGAP = 10;

    /**
     * The default initial display state of the panel's learning rule panel.
     */
    private static final boolean DEFAULT_DISPLAY_PARAMS = false;

    /** Panel to edit general synapse properties. */
    private GeneralSynapsePropertiesPanel generalSynapsePropertiesPanel;

    /** Panel to edit specific synapse type */
    private SynapseRulePanel synapseRulePanel;

    /** Panel to edit spike responders. */
    private SpikeResponderSettingsPanel editSpikeResponders;

    /**
     * Creates a synapse property panel with a default display state.
     *
     * @param synapseList the list of synapse synapses either being edited
     *            (editing) or being used to fill the panel with default values
     *            (creation).
     * @param parent the parent window, made available for easy resizing.
     * @return
     */
    public static SynapsePropertiesPanel createSynapsePropertiesPanel(
            final Collection<Synapse> synapseList, final Window parent) {
        return createSynapsePropertiesPanel(synapseList, parent,
                DEFAULT_DISPLAY_PARAMS);
    }

    /**
     * Create the panel without specifying whether to display id (that is done
     * automatically).
     *
     * @param synapseList the list of synapses either being edited (editing) or
     *            being used to fill the panel with default values (creation).
     * @param parent the parent window, made available for easy resizing.
     * @param showSpecificRuleParams whether or not to display the synapse
     *            update rule's details initially
     * @return
     */
    public static SynapsePropertiesPanel createSynapsePropertiesPanel(
            final Collection<Synapse> synapseList, final Window parent,
            final boolean showSpecificRuleParams) {
        SynapsePropertiesPanel cnip = new SynapsePropertiesPanel(synapseList,
                parent, showSpecificRuleParams, true);
        cnip.initializeLayout();
        return cnip;
    }

    /**
     * {@link #createSynapsePropertiesPanel(List, Window, boolean)}
     *
     * @param synapseList the list of synapses either being edited (editing) or
     *            being used to fill the panel with default values (creation).
     * @param parent the parent window, made available for easy resizing.
     * @param showSpecificRuleParams whether or not to display the synapse
     *            update rule's details initially.
     */
    private SynapsePropertiesPanel(final Collection<Synapse> synapseList,
            final Window parent, final boolean showSpecificRuleParams,
            final boolean displayID) {
        synapseRulePanel = new SynapseRulePanel(synapseList, parent,
                showSpecificRuleParams);
        if (SynapseDialog.targsUseSynapticInputs(synapseList)) {
            editSpikeResponders = new SpikeResponderSettingsPanel(synapseList,
                    parent);
        }
        generalSynapsePropertiesPanel = GeneralSynapsePropertiesPanel
                .createPanel(synapseList, parent, displayID);
        synapseRulePanel = new SynapseRulePanel(synapseList, parent,
                showSpecificRuleParams);
    }

    /**
     * Lays out the panel.
     */
    private void initializeLayout() {
        // Respond to update panel combo box changes here, so that general panel
        // can be updated too
        synapseRulePanel.getCbSynapseType()
                .addActionListener(e -> SwingUtilities.invokeLater(() -> {
                    // generalSynapsePropertiesPanel
                    // .updateFieldVisibility(synapseRulePanel.getSynapsePanel().getPrototypeRule());
                    repaint();
                }));

        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        this.setLayout(layout);
        this.add(generalSynapsePropertiesPanel);
        this.add(Box.createVerticalStrut(DEFAULT_VGAP));
        this.add(synapseRulePanel);
        if (editSpikeResponders != null) {
            this.add(Box.createVerticalStrut(DEFAULT_VGAP));
            this.add(editSpikeResponders);
        }
    }

    /**
     * Commits changes in the two or three sub-panels.
     */
    @Override
    public boolean commitChanges() {

        boolean success = true;

        // Commit changes specific to the synapse type
        // This must be the first change committed, as other synapse panels
        // make assumptions about the type of the synapse update rule being
        // edited that can result in ClassCastExceptions otherwise.
        success &= synapseRulePanel.commitChanges();

        success &= generalSynapsePropertiesPanel.commitChanges();

        if (editSpikeResponders != null) {
            success &= editSpikeResponders.commitChanges();
        }

        return success;

    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void fillFieldValues() {
    }

    /**
     * @return the generalSynapsePropertiesPanel
     */
    public GeneralSynapsePropertiesPanel getGeneralSynapsePropertiesPanel() {
        return generalSynapsePropertiesPanel;
    }

    /**
     * @return the synapseRulePanel
     */
    public SynapseRulePanel getSynapseRulePanel() {
        return synapseRulePanel;
    }

    // TODO?
    // public void fillFieldValues(SynapseGroup synapseGroup, Polarity polarity)
    // {
    // synapseInfoPanel.fillFieldValues(synapseGroup, polarity);
    // }

    // /**
    // * @return the panel governing the editing of spike responders.
    // */
    // public SpikeResponderSettingsPanel getEditSpikeRespondersPanel() {
    // return editSpikeResponders;
    // }

}
