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

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.connections.Sparse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.WeightMatrixViewer;
import org.simbrain.network.gui.dialogs.connect.ConnectionPanel;
import org.simbrain.network.gui.dialogs.connect.ConnectionSynapsePropertiesPanel;
import org.simbrain.network.gui.dialogs.connect.connector_panels.DensityBasedConnectionPanel;
import org.simbrain.network.gui.dialogs.synapse.SynapseGroupAdjustmentPanel;
import org.simbrain.network.subnetworks.CompetitiveGroup.SynapseGroupWithLearningRate;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ApplyPanel;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * Dialog for editing synapse groups.
 *
 * @author Jeff Yoshimi
 * @author Zach Tosi
 */
public final class SynapseGroupDialog extends StandardDialog {

    /** Parent network panel. */
    private NetworkPanel networkPanel;

    /** Synapse Group. */
    private SynapseGroup synapseGroup;

    /** Main tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Histogram tab. */
    private JPanel tabAdjust = new JPanel();

    /** Weight Matrix tab. */
    private JPanel tabMatrix = new JPanel();

    /** Label Field. */
    private final JTextField tfSynapseGroupLabel = new JTextField();

    /** Rate Field. */
    private final JTextField tfRateLabel = new JTextField();

    /** Panel with basic neuron group info. */
    private JPanel summaryInfoPanel;

    /** Panel for editing synapses in the group. */
    private ConnectionSynapsePropertiesPanel editSynapsesPanel;

    /** Panel for adjusting the connection object. */
    private ConnectionPanel connectionPanel;

    /** Panel for adjusting the synapse group */
    private SynapseGroupAdjustmentPanel adjustmentPanel;

    /** If true this is a creation panel. Otherwise it is an edit panel. */
    private boolean isCreationPanel = false;

    /** Reference to source neuron group. */
    private NeuronGroup sourceNeuronGroup = null;

    /** Reference to target neuron group. */
    private NeuronGroup targetNeuronGroup = null;

    /**
     * The list of components which are stored here so their tabs can be blanked
     * out. This is what allows the panel to resize when tabs are changed.
     */
    private ArrayList<Component> storedComponents = new ArrayList<Component>();

    /**
     * Creates a synapse group dialog based on a source and target neuron group.
     * This should be used when the synapse group being "edited" doesn't exist
     * yet, i.e. it's being created from the parameters in this panel.
     *
     * @param np
     *            the network panel
     * @param src
     *            the source neuron group
     * @param tar
     *            the target neuron group
     * @return a synapse group dialog for creating a synapse group between the
     *         source and target neuron groups.
     */
    public static SynapseGroupDialog createSynapseGroupDialog(
        final NetworkPanel np, NeuronGroup src, NeuronGroup tar) {
        SynapseGroupDialog sgd = new SynapseGroupDialog(np, src, tar);
        sgd.addListeners();
        sgd.tabbedPane.setSelectedIndex(0);
        return sgd;
    }

    /**
     * Creates a synapse group dialog based on a given synapse group it goes
     * without saying that this means this dialog will be editing the given
     * synapse group.
     *
     * @param np
     *            the network panel
     * @param sg
     *            the synapse group being edited
     * @return a synapse group dialog which can edit the specified synapse group
     */
    public static SynapseGroupDialog createSynapseGroupDialog(
        final NetworkPanel np, final SynapseGroup sg) {
        SynapseGroupDialog sgd = new SynapseGroupDialog(np, sg);
        sgd.addListeners();
        sgd.tabbedPane.setSelectedIndex(0);
        return sgd;
    }

    /**
     * Create a new synapse group connecting the indicated neuron groups.
     *
     * @param src
     *            source neuron group
     * @param tar
     *            target neuron group
     * @param np
     *            parent panel
     */
    private SynapseGroupDialog(final NetworkPanel np, NeuronGroup src,
        NeuronGroup tar) {
        networkPanel = np;
        this.sourceNeuronGroup = src;
        this.targetNeuronGroup = tar;
        isCreationPanel = true;
        init();
    }

    /**
     * Construct the Synapse group dialog.
     *
     * @param np
     *            Parent network panel
     * @param sg
     *            Synapse group being edited
     */
    private SynapseGroupDialog(final NetworkPanel np, final SynapseGroup sg) {
        networkPanel = np;
        synapseGroup = sg;
        init();
    }

    /**
     * Initialize the panel.
     */
    private void init() {

        setTitle("Edit Synapse Group");

        setMinimumSize(new Dimension(isCreationPanel ? 500 : 600, 300));

        fillFieldValues();
        setContentPane(tabbedPane);

        // Summary panel tab
        if (isCreationPanel) {
            synapseGroup = new SynapseGroup(sourceNeuronGroup,
                targetNeuronGroup);
            summaryInfoPanel = new SummaryPanel(synapseGroup);
        } else {
            summaryInfoPanel = ApplyPanel.createApplyPanel(new SummaryPanel(
                synapseGroup));
        }
        JScrollPane summaryScrollWrapper = new JScrollPane(summaryInfoPanel);
        summaryScrollWrapper.setBorder(null);
        storedComponents.add(summaryScrollWrapper);
        tabbedPane.addTab("Properties", summaryScrollWrapper);

        if (isCreationPanel) {
            connectionPanel = ConnectionPanel.createConnectionPanel(this,
                    networkPanel);
            JScrollPane connectWrapper = new JScrollPane(
                    connectionPanel.getMainPanel());
            connectWrapper.setBorder(null);
            storedComponents.add(connectWrapper);
            tabbedPane.addTab("Connection", new JPanel());
        }

        // Tab for editing synapses
        editSynapsesPanel = ConnectionSynapsePropertiesPanel
            .createSynapsePropertiesPanel(this, synapseGroup);
        JScrollPane editSynapseScrollPane = new JScrollPane(
            ((EditablePanel) editSynapsesPanel).getPanel());
        editSynapseScrollPane.setBorder(null);
        storedComponents.add(editSynapseScrollPane);
        tabbedPane.addTab("Edit Synapses", new JPanel());

        // Synapse Adjustment Panel
        JScrollPane adjustSynScrollPane = new JScrollPane(tabAdjust);
        adjustSynScrollPane.setBorder(null);
        storedComponents.add(adjustSynScrollPane);
        tabAdjust.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        adjustmentPanel = SynapseGroupAdjustmentPanel
            .createSynapseGroupAdjustmentPanel(this, synapseGroup);
        tabAdjust.add(adjustmentPanel);
        tabbedPane.addTab("Adjust Weights", new JPanel());

        // Weight Matrix Editor Tabs
        if (!isCreationPanel) {
            if (synapseGroup.getConnectionManager() instanceof Sparse) {
                DensityBasedConnectionPanel sparsePanel = DensityBasedConnectionPanel
                        .createSparsityAdjustmentPanel(
                                (Sparse) synapseGroup.getConnectionManager(),
                                networkPanel);
                storedComponents.add(ApplyPanel.createApplyPanel(sparsePanel));
                tabbedPane.addTab("Sparsity", new JPanel());
            }
            // Weight Matrix
            JScrollPane matrixScrollPane = new JScrollPane(tabMatrix);
            matrixScrollPane.setBorder(null);
            storedComponents.add(matrixScrollPane);
            tabbedPane.addTab("Matrix", new JPanel());
            // Hack... going around synapse group's getSource/TargetNeurons()
            // method
            // because we need to sort the lists...
            // TODO: Sorting necessary? Alternative?
            tabMatrix
                .add(WeightMatrixViewer
                    .getWeightMatrixPanel(new WeightMatrixViewer(
                        synapseGroup.getSourceNeuronGroup()
                            .getNeuronList(), synapseGroup
                            .getTargetNeuronGroup()
                            .getNeuronList(), networkPanel)));
        }

        // Set up help button
        Action helpAction;
        helpAction = new ShowHelpAction("Pages/Network/groups.html");
        addButton(new JButton(helpAction));

        if (!isCreationPanel) {
            // If editing, make this dialog based on a done button, rather than
            // ok and cancel. All edits are done with apply
            setAsDoneDialog();
        }
    }

    /**
     * Add listeners.
     */
    private void addListeners() {
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int selectedTab = ((JTabbedPane) e.getSource())
                    .getSelectedIndex();
                Component current = storedComponents.get(selectedTab);
                int numTabs = storedComponents.size();
                for (int i = 0; i < numTabs; i++) {
                    if (i == selectedTab) {
                        tabbedPane.setComponentAt(i, current);
                        tabbedPane.repaint();
                        continue;
                    } else {
                        JPanel tmpPanel = new JPanel();
                        // Hack...
                        // 100 is a guess as to average px length of tabs
                        // (not their panels, just the tabs)
                        // This is here to prevent "scrunching" of the
                        // tabs when one of the panel's widths is too small
                        // to accommodate all the tabs on one line
                        int minPx = tabbedPane.getTabCount() * 120;
                        if (current.getPreferredSize().width < minPx) {
                            tmpPanel.setPreferredSize(new Dimension(minPx,
                                current.getPreferredSize().height));
                        } else {
                            tmpPanel.setPreferredSize(current
                                .getPreferredSize());
                        }
                        tabbedPane.setComponentAt(i, tmpPanel);
                    }
                }
                tabbedPane.invalidate();
                pack();
            }
        });

    }

    /**
     * Set the initial values of dialog components.
     */
    public void fillFieldValues() {
        if (!isCreationPanel) {
            tfSynapseGroupLabel.setText(synapseGroup.getLabel());
            if (synapseGroup instanceof SynapseGroupWithLearningRate) {
                tfRateLabel.setText(""
                    + ((SynapseGroupWithLearningRate) synapseGroup)
                        .getLearningRate());
            }
        } else {
            tfSynapseGroupLabel.setText("Synapse group");
        }
    }

    /**
     * Commit changes.
     */
    public void commitChanges() {
        if (isCreationPanel) {
            connectionPanel.commitChanges(synapseGroup);

            ((EditablePanel) summaryInfoPanel).commitChanges();
            ((EditablePanel) editSynapsesPanel).commitChanges();
            adjustmentPanel.commitChanges();
            // Create the synapse group.
            synapseGroup.makeConnections();

            // TODO: Spike responders at creation time?
            networkPanel.getNetwork().addGroup(synapseGroup);
            networkPanel.repaint();
        }
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        if (isCreationPanel) {
            commitChanges();
        }
    }

}
