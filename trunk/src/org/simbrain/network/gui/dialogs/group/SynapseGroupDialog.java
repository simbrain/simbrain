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
import java.util.Collections;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.WeightMatrixViewer;
import org.simbrain.network.gui.dialogs.SynapseAdjustmentPanel;
import org.simbrain.network.gui.dialogs.synapse.BasicSynapseInfoPanel;
import org.simbrain.network.gui.dialogs.synapse.SpikeResponderSettingsPanel;
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;
import org.simbrain.network.gui.dialogs.synapse.SynapseUpdateSettingsPanel;
import org.simbrain.network.subnetworks.CompetitiveGroup.SynapseGroupWithLearningRate;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * Dialog for editing synapse groups.
 * 
 * @author Jeff Yoshimi
 * @author Zach Tosi
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

    /** Label Field. */
    private final JTextField tfSynapseGroupLabel = new JTextField();

    /** Rate Field. */
    private final JTextField tfRateLabel = new JTextField();

    /** Main properties panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Panel to edit synapse basic info. */
    private BasicSynapseInfoPanel editBasicSynapseInfo;

    /** Panel to edit synapse update rule. */
    private SynapseUpdateSettingsPanel editSynapseType;

    /** Panel to edit spike responders. */
    private SpikeResponderSettingsPanel editSpikeResponders;

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

        setMinimumSize(new Dimension(isCreationPanel ? 300 : 500, 300));

        fillFieldValues();
        setContentPane(tabbedPane);

        // Generic group properties
        JScrollPane mainScrollWrap = new JScrollPane(tabMain);
        storedComponents.add(mainScrollWrap);
        // This is the only tab that isn't passed an empty panel because it
        // is the first tab displayed.
        tabbedPane.addTab("Properties", mainScrollWrap);
        tabMain.add(mainPanel);
        if (!isCreationPanel) {
            mainPanel.addItem("Id:", new JLabel(synapseGroup.getId()));
        }
        tabMain.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.addItem("Label:", tfSynapseGroupLabel);
        tfSynapseGroupLabel.setColumns(12);
        // TODO: As more synapse group types are added generalize
        if (synapseGroup instanceof SynapseGroupWithLearningRate) {
            mainPanel.addItem("Learning Rate:", tfRateLabel);
        }

        // Synapse edit tab
        Box editSynapses = Box.createVerticalBox();
        if (isCreationPanel) {
            Synapse baseSynapse = Synapse.getTemplateSynapse();
            editBasicSynapseInfo = new BasicSynapseInfoPanel(
                    Collections.singletonList(baseSynapse), this);
            editSynapseType = new SynapseUpdateSettingsPanel(
                    Collections.singletonList(baseSynapse), this);
            // Todo: make it possible to edit spike responders at creation time?
        } else {
            editBasicSynapseInfo = new BasicSynapseInfoPanel(
                    synapseGroup.getSynapseList(), this);
            editSynapseType = new SynapseUpdateSettingsPanel(
                    synapseGroup.getSynapseList(), this);
        }
        editSynapses.add(editBasicSynapseInfo);
        editSynapses.add(editSynapseType);
        // Set up spike responder panel if any of the synapses are spike
        // responders
        if (!isCreationPanel) {
            if (SynapseDialog.containsASpikeResponder(synapseGroup
                    .getSynapseList())) {
                editSpikeResponders = new SpikeResponderSettingsPanel(
                        synapseGroup.getSynapseList(), this);
                editSynapses.add(editSpikeResponders);
            }
        }
        editSynapses.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        storedComponents.add(new JScrollPane(editSynapses));
        tabbedPane.addTab("Edit Synapses", new JPanel());

        if (!isCreationPanel) {
            // Synapse Adjustment Panel
            storedComponents.add(new JScrollPane(tabAdjust));
            tabAdjust.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            tabbedPane.addTab("Adjust weights", new JPanel());
            tabAdjust.add(SynapseAdjustmentPanel.createSynapseAdjustmentPanel(
                    networkPanel, synapseGroup.getSynapseList()));

            // Weight Matrix
            tabMatrix.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            storedComponents.add(new JScrollPane(tabMatrix));
            tabbedPane.addTab("Matrix", new JPanel());
            tabMatrix.add(WeightMatrixViewer
                    .getWeightMatrixPanel(new WeightMatrixViewer(synapseGroup
                            .getSourceNeurons(), synapseGroup
                            .getTargetNeurons(), networkPanel)));
        }

        // Set up help button
        Action helpAction;
        helpAction = new ShowHelpAction("Pages/Network/groups.html");
        addButton(new JButton(helpAction));
    }

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
            synapseGroup = new SynapseGroup(networkPanel.getNetwork(),
                    sourceNeuronGroup, targetNeuronGroup);
            synapseGroup.setLabel(tfSynapseGroupLabel.getText());
            editBasicSynapseInfo.commitChanges(synapseGroup.getSynapseList());
            editSynapseType.getSynapsePanel().commitChanges(
                    synapseGroup.getSynapseList());
            networkPanel.getNetwork().addGroup(synapseGroup);
        } else {
            // editBasicSynapseInfo.commitChanges(synapseGroup.getSynapseList());
            editSynapseType.getSynapsePanel().commitChanges(
                    synapseGroup.getSynapseList());
            if (editSpikeResponders != null) {
                editSpikeResponders.getSpikeResponsePanel().commitChanges(
                        synapseGroup.getSynapseList());
            }
        }

        // Special case for synapse groups that have a learning rate
        if (synapseGroup instanceof SynapseGroupWithLearningRate) {
            ((SynapseGroupWithLearningRate) synapseGroup)
                    .setLearningRate(Double.parseDouble(tfRateLabel.getText()));
        }
        networkPanel.repaint();
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

}
