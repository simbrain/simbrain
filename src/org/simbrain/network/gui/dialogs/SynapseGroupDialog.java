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
package org.simbrain.network.gui.dialogs;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.WeightMatrixViewer;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * Dialog for editing synapse groups.
 *
 * @author Jeff Yoshimi
 */
public class SynapseGroupDialog extends StandardDialog {

    /** Parent network panel. */
    private NetworkPanel networkPanel;

    /** Synapse Group. */
    private SynapseGroup synapseGroup;

    /** Main tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** General properties. */
    private JPanel tabMain = new JPanel();

    /** Histogram tab. */
    private JPanel tabAdjust = new JPanel();

    /** Weight Matrix tab. */
    private JPanel tabMatrix = new JPanel();

    /** Panel for specific group types. Null for bare Synapse group. */
    private JPanel specificSynapseGroupPanel;

    /** Label Field. */
    private final JTextField tfSynapseLabel = new JTextField();

    /** Main properties panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /**
     * Construct the Synapse group dialog.
     *
     * @param np Parent network panel
     * @param sg Synapse group being edited
     */
    public SynapseGroupDialog(final NetworkPanel np, final SynapseGroup sg) {
        networkPanel = np;
        synapseGroup = sg;
        setTitle("Edit Synapse Group");

        // setSpecificGroup();
        fillFieldValues();
        setContentPane(tabbedPane);

        // Generic group properties
        tabbedPane.addTab("Properties", tabMain);
        tabMain.add(mainPanel);
        mainPanel.addItem("Id:", new JLabel(synapseGroup.getId()));
        mainPanel.addItem("Label:", tfSynapseLabel);

        // If this is a subclass of Synapse group, add a tab for editing those
        // properties
        // initializeSpecificGroupTab();

        // Adjust weights
        tabbedPane.addTab("Adjust weights", tabAdjust);
        tabAdjust.add(new SynapseAdjustmentPanel(networkPanel, synapseGroup
                .getSynapseList()));

        // Weight Matrix
        tabbedPane.addTab("Matrix", tabMatrix);
        tabMatrix.add(new WeightMatrixViewer(synapseGroup.getSourceNeurons(),
                synapseGroup.getTargetNeurons(), networkPanel));

        // TODO: Help button

    }

    // /**
    // * Setes the specificSynapseGroupPanel based on the underlying group.
    // */
    // private void setSpecificGroup() {
    // if (SynapseGroup instanceof Competitive) {
    // specificSynapseGroupPanel = new CompetitivePropertiesPanel(networkPanel,
    // false,
    // (Competitive) SynapseGroup);
    // }
    // }

    /**
     * Add a tab for specific Synapse group rules.
     */
    private void initializeSpecificGroupTab() {
        if (specificSynapseGroupPanel == null) {
            return;
        } else {
            tabbedPane.addTab("" + synapseGroup.getClass().getSimpleName()
                    + " properties", specificSynapseGroupPanel);
        }
    }

    /**
     * Set the initial values of dialog components.
     */
    public void fillFieldValues() {

        tfSynapseLabel.setText(synapseGroup.getLabel());

        // if (specificSynapseGroupPanel != null) {
        // ((GroupPropertiesPanel) specificSynapseGroupPanel).fillFieldValues();
        // }
    }

    /**
     * Commit changes.
     */
    public void commitChanges() {
        synapseGroup.setLabel(tfSynapseLabel.getText());
        if (specificSynapseGroupPanel != null) {
            ((GroupPropertiesPanel) specificSynapseGroupPanel).commitChanges();
        }
        networkPanel.repaint();
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

}
