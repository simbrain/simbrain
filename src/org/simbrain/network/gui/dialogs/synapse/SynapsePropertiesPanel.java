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

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.util.widgets.EditablePanel;

/**
 * This panel combines synapse editing sub-panels and handles changes to one
 * being applied to the others.
 *
 * @author Jeff Yoshimi
 * @author Zach Tosi
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

    /** The basic synapse info panel. */
    private SynapsePropertiesSimple synapseInfoPanel;

    /** The synapse update settings panel. */
    private SpecificSynapseRulePanel updateInfoPanel;

    /** Panel to edit spike responders. */
    private SpikeResponderSettingsPanel editSpikeResponders;

    /**
     * Creates a combined synapse info panel, which includes the basic synapse
     * info panel and a synapse update settings panel. The panel is
     * automatically built and laid out, such that it is immediately ready for
     * display.
     *
     * @param synapseList
     *            the list of synapse synapses either being edited (editing) or
     *            being used to fill the panel with default values (creation).
     * @param parent
     *            the parent window, made available for easy resizing.
     */
    public static SynapsePropertiesPanel createCombinedSynapseInfoPanel(
        final Collection<Synapse> synapseList, final Window parent) {
        return createCombinedSynapseInfoPanel(synapseList, parent,
            DEFAULT_DISPLAY_PARAMS);
    }

    /**
     * Creates a combined synapse info panel, which includes the basic synapse
     * info panel and a synapse update settings panel. The panel is
     * automatically built and laid out, such that it is immediately ready for
     * display. The setting panel's display state is that extra data is by
     * default hidden.
     *
     * @param synapseList
     *            the list of synapses either being edited (editing) or being
     *            used to fill the panel with default values (creation).
     * @param parent
     *            the parent window, made available for easy resizing.
     * @param showSpecificRuleParams
     *            whether or not to display the synapse update rule's details
     *            initially
     */
    public static SynapsePropertiesPanel createCombinedSynapseInfoPanel(
        final Collection<Synapse> synapseList, final Window parent,
        final boolean showSpecificRuleParams) {
        SynapsePropertiesPanel cnip = new SynapsePropertiesPanel(
            synapseList, parent, showSpecificRuleParams);
        cnip.initializeLayout();
        return cnip;
    }

    /**
     * {@link #createCombinedSynapseInfoPanel(List, Window, boolean)}
     *
     * @param synapseList
     *            the list of synapses either being edited (editing) or being
     *            used to fill the panel with default values (creation).
     * @param parent
     *            the parent window, made available for easy resizing.
     * @param showSpecificRuleParams
     *            whether or not to display the synapse update rule's details
     *            initially.
     */
    private SynapsePropertiesPanel(final Collection<Synapse> synapseList,
        final Window parent, final boolean showSpecificRuleParams) {
        synapseInfoPanel = SynapsePropertiesSimple.createBasicSynapseInfoPanel(
            synapseList, parent);
        updateInfoPanel = new SpecificSynapseRulePanel(synapseList, parent,
            showSpecificRuleParams);
        if (SynapseDialog.containsASpikeResponder(synapseList)) {
            editSpikeResponders = new SpikeResponderSettingsPanel(synapseList,
                parent);
        }
    }

    /**
     * Lays out the panel.
     */
    private void initializeLayout() {
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        this.setLayout(layout);
        this.add(synapseInfoPanel);
        this.add(Box.createVerticalStrut(DEFAULT_VGAP));
        this.add(updateInfoPanel);
        if (editSpikeResponders != null) {
            this.add(Box.createVerticalStrut(DEFAULT_VGAP));
            this.add(editSpikeResponders);
        }
    }

    /**
     * {@inheritDoc} <b>Specifically:</b> Commits changes in the basic synapse
     * info panel and the synapse update settings panel.
     */
    @Override
    public boolean commitChanges() {

        boolean success = true;

        // Commit changes specific to the synapse type
        // This must be the first change committed, as other synapse panels
        // make assumptions about the type of the synapse update rule being
        // edited that can result in ClassCastExceptions otherwise.
        success &= updateInfoPanel.commitChanges();

        success &= synapseInfoPanel.commitChanges();

        if (editSpikeResponders != null) {
            success &= editSpikeResponders.commitChanges();
        }

        return success;

    }

    /**
     * @return a template rule assoc
     */
    public SynapseUpdateRule getTemplateSelectedRule() {
        return updateInfoPanel.getTemplateRule();
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void fillFieldValues() {
    }

    /**
     * @return the updateInfoPanel
     */
    public SpecificSynapseRulePanel getUpdateInfoPanel() {
        return updateInfoPanel;
    }

    /**
     * @param updateInfoPanel
     *            the updateInfoPanel to set
     */
    public void setUpdateInfoPanel(SpecificSynapseRulePanel updateInfoPanel) {
        this.updateInfoPanel = updateInfoPanel;
    }

    /**
     * Sets whether or not the basic synapse
     *
     * @param ignoreSetStrength
     */
    public void setIgnoreSetStrength(boolean ignoreSetStrength) {
        synapseInfoPanel.setIgnoreSetStrength(ignoreSetStrength);
    }

    public double getStrength() {
        return synapseInfoPanel.getStrength();
    }

}
