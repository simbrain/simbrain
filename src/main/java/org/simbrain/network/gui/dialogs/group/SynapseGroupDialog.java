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
package org.simbrain.network.gui.dialogs.group;

import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.WeightMatrixViewer;
import org.simbrain.network.gui.dialogs.connect.ConnectionSelectorPanel;
import org.simbrain.network.gui.dialogs.synapse.SynapseGroupAdjustmentPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;

/**
 * Dialog for editing synapse groups.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
public final class SynapseGroupDialog extends StandardDialog {

    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Synapse Group.
     */
    private SynapseGroup synapseGroup;

    /**
     * Main tabbed pane.
     */
    private final JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Label Field.
     */
    private final JTextField tfSynapseGroupLabel = new JTextField();

    /**
     * The list of components which are stored here so their tabs can be blanked
     * out. This is what allows the panel to resize when tabs are changed.
     */
    private final ArrayList<Component> storedComponents = new ArrayList<>();

    /**
     * Panel for adjusting the connection object.
     */
    private ConnectionSelectorPanel connectionPanel;

    /**
     * Panel for adjusting the synapse group
     */
    private SynapseGroupAdjustmentPanel adjustmentPanel;

    /**
     * Summary info about synapse group.
     */
    private AnnotatedPropertyEditor sgProperties;

    /**
     * If true this is a creation dialog. Otherwise it is an edit dialog.
     */
    private boolean isCreationDialog = false;

    /**
     * Reference to source neuron group.
     */
    private NeuronGroup sourceNeuronGroup = null;

    /**
     * Reference to target neuron group.
     */
    private NeuronGroup targetNeuronGroup = null;

    /**
     * Create a new synapse group connecting the indicated neuron groups.
     *
     * @param src source neuron group
     * @param tar target neuron group
     * @param np  parent panel
     */
    public SynapseGroupDialog(final NetworkPanel np, NeuronGroup src, NeuronGroup tar) {
        networkPanel = np;
        this.sourceNeuronGroup = src;
        this.targetNeuronGroup = tar;
        isCreationDialog = true;
        init();
    }

    /**
     * Construct the Synapse group dialog.
     *
     * @param np Parent network panel
     * @param sg Synapse group being edited
     */
    public SynapseGroupDialog(final NetworkPanel np, final SynapseGroup sg) {
        networkPanel = np;
        synapseGroup = sg;
        this.sourceNeuronGroup = sg.getSourceNeuronGroup();
        this.targetNeuronGroup = sg.getTargetNeuronGroup();
        init();
    }

    /**
     * Initialize the panel.
     */
    private void init() {

        setMinimumSize(new Dimension(500, 300));

        // Make the synapse group
        if (isCreationDialog) {
            setTitle("Create Synapse Group");
            // TODO
            // synapseGroup = SynapseGroup.createSynapseGroup(sourceNeuronGroup, targetNeuronGroup);
            tfSynapseGroupLabel.setText("Synapse group");
        } else {
            setTitle("Edit " + synapseGroup.getLabel());
            setContentPane(tabbedPane);
            tfSynapseGroupLabel.setText(synapseGroup.getLabel());
            sgProperties = new AnnotatedPropertyEditor(synapseGroup);
            JScrollPane summaryScrollWrapper = new JScrollPane(sgProperties);
            summaryScrollWrapper.setBorder(null);
            storedComponents.add(summaryScrollWrapper);
            tabbedPane.addTab("Properties", summaryScrollWrapper);
        }

        // Connectivity panel
        if (isCreationDialog) {
            // connectionPanel = new ConnectionSelectorPanel(this, synapseGroup, null, null, null, networkPanel.getNetwork());
            // JScrollPane connectWrapper = new JScrollPane(connectionPanel);
            // connectWrapper.setBorder(null);
            // setContentPane(connectWrapper);

        } else {
            // connectionPanel = new ConnectionSelectorPanel(this, synapseGroup, null, null, null, networkPanel.getNetwork());
            // var connectionApplyPanel  =  ApplyPanel.createCustomApplyPanel(connectionPanel,
            //         (ActionEvent e) -> {
            //     // connectionPanel.getCurrentConnectionPanel().commitChanges(synapseGroup);
            //     //sumPanel.fillFieldValues(synapseGroup); // TODO
            //     adjustmentPanel.fullUpdate();
            //     // TODO: Update weight matrix when this is pressed
            // });
            // JScrollPane connectWrapper = new JScrollPane(connectionApplyPanel);
            // connectWrapper.setBorder(null);
            // storedComponents.add(connectWrapper);
            // tabbedPane.addTab("Connection Manager", connectWrapper);
        }

        // Weight matrix
        if (!isCreationDialog) {
            if (synapseGroup.size() < 10000) {
                JPanel weightMatrix = new JPanel();
                final JScrollPane matrixScrollPane = new JScrollPane(weightMatrix);
                matrixScrollPane.setBorder(null);
                weightMatrix.add(WeightMatrixViewer.getWeightMatrixPanel(new WeightMatrixViewer(synapseGroup.getSourceNeurons(), synapseGroup.getTargetNeurons(), networkPanel)));
                storedComponents.add(matrixScrollPane);
                tabbedPane.addTab("Matrix", matrixScrollPane);
            }
        }

        // Synapse Adjustment Panel
        if(!isCreationDialog) {
            adjustmentPanel = SynapseGroupAdjustmentPanel.createSynapseGroupAdjustmentPanel(this, synapseGroup, isCreationDialog);
            adjustmentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JScrollPane adjustSynScrollPane = new JScrollPane(adjustmentPanel);
            adjustSynScrollPane.setBorder(null);
            storedComponents.add(adjustSynScrollPane);
            tabbedPane.addTab("Synapse Values", adjustSynScrollPane);
        }

        // Set up help button
        Action helpAction;
        helpAction = new ShowHelpAction("Pages/Network/groups/SynapseGroup.html");
        addButton(new JButton(helpAction));

        // Tab-change events
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateTabSizes(((JTabbedPane) e.getSource()).getSelectedIndex());
            }
        });

        if (!isCreationDialog) {
            // If editing, make this dialog based on a done button, rather than
            // ok and cancel. All edits are done with apply
            setAsDoneDialog();
            updateTabSizes(0);
        }
    }

    /**
     * Add listeners.
     */
    private void updateTabSizes(int selectedTab) {
        Component current = storedComponents.get(selectedTab);
        int numTabs = storedComponents.size();
        for (int i = 0; i < numTabs; i++) {
            if (i == selectedTab) {
                tabbedPane.setComponentAt(i, current);
                tabbedPane.repaint();
            } else {
                JPanel tmpPanel = new JPanel();
                // Hack...
                // 120 is a guess as to average px length of tabs
                // (not their panels, just the tabs)
                // This is here to prevent "scrunching" of the
                // tabs when one of the panel's widths is too small
                // to accommodate all the tabs on one line
                int minPx = tabbedPane.getTabCount() * 120;
                if (current.getPreferredSize().width < minPx) {
                    tmpPanel.setPreferredSize(new Dimension(minPx, current.getPreferredSize().height));
                } else {
                    tmpPanel.setPreferredSize(current.getPreferredSize());
                }
                tabbedPane.setComponentAt(i, tmpPanel);
            }
        }
        pack();
    }


    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        if (isCreationDialog) {
            // TODO: Below needed?
            // connectionPanel.getCurrentConnectionPanel().commitChanges(synapseGroup);
            networkPanel.getNetwork().addNetworkModel(synapseGroup);
            networkPanel.repaint();
        } else {
            // When editing a synapse group most edits are handled by apply buttons
            sgProperties.commitChanges();
        }
    }

    @Override
    public void pack() {
        super.pack();
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int height = gd.getDisplayMode().getHeight();
        if(this.getLocation().y + this.getBounds().height > height) {
            this.setBounds(getLocation().x, getLocation().y, getWidth(), height - getLocation().y);
        }
    }

}