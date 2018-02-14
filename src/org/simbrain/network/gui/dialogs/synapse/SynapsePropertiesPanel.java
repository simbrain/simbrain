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

import org.simbrain.network.core.Synapse;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.EditablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * This panel combines synapse editing sub-panels and handles changes to one
 * being applied to the others.
 * <p>
 * TODO: See NeuronPropertiesPanel docs
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
public final class SynapsePropertiesPanel extends JPanel implements EditablePanel {

    /**
     * Vertical gap between panel elements.
     */
    private static final int DEFAULT_VGAP = 10;

    /**
     * The synapses being modified.
     */
    private final List<Synapse> synapseList;

    /**
     * Panel to edit general synapse properties
     */
    private AnnotatedPropertyEditor generalSynapseProperties;

    /**
     * Panel to edit specific synapse type
     */
    private SynapseRulePanel synapseRulePanel;

    /**
     * Panel to edit spike responders.
     */
    private SpikeResponderSettingsPanel editSpikeResponders;

    /**
     * Creates a synapse property panel with a default display state.
     *
     * @param synapseList the list of synapse synapses either being edited
     *                    (editing) or being used to fill the panel with default values
     *                    (creation).
     * @param parent      the parent window, made available for easy resizing.
     * @return the panel
     */
    public static SynapsePropertiesPanel createSynapsePropertiesPanel(final List<Synapse> synapseList, final Window parent) {
        return new SynapsePropertiesPanel(synapseList, parent);
    }

    /**
     * {@link #createSynapsePropertiesPanel(List, Window)}
     *
     * @param synapseList the list of synapses either being edited (editing) or
     *                    being used to fill the panel with default values (creation).
     * @param parent      the parent window, made available for easy resizing.
     */
    private SynapsePropertiesPanel(final List<Synapse> synapseList, final Window parent) {

        this.synapseList = synapseList;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Show id if editing one synapse
        if (synapseList.size() == 1) {
            JLabel idlabel = new JLabel(synapseList.get(0).getId());
            idlabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            this.add(idlabel);
            this.add(new JSeparator(SwingConstants.HORIZONTAL));
        }

        // General Synapse Properties
        generalSynapseProperties = new AnnotatedPropertyEditor(synapseList);
        this.add(generalSynapseProperties);
        this.add(Box.createVerticalStrut(DEFAULT_VGAP));

        // Synapse Rule panel
        synapseRulePanel = new SynapseRulePanel(synapseList, parent);

        // // Respond to update panel combo box changes here, so that general
        // panel
        // // can be updated too
        // synapseRulePanel.getCbSynapseType()
        // .addActionListener(e -> SwingUtilities.invokeLater(() -> {
        // // generalSynapsePropertiesPanel
        // //
        // .updateFieldVisibility(synapseRulePanel.getSynapsePanel().getPrototypeRule());
        // repaint();
        // }));

        this.add(synapseRulePanel);

        // Spike Responders
        if (SynapseDialog.targsUseSynapticInputs(synapseList)) {
            editSpikeResponders = new SpikeResponderSettingsPanel(synapseList, parent);
        }
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

        generalSynapseProperties.commitChanges();

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
     * @return the synapseRulePanel
     */
    public SynapseRulePanel getSynapseRulePanel() {
        return synapseRulePanel;
    }

}
