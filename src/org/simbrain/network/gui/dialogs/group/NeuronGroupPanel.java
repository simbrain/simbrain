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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.MainLayoutPanel;
import org.simbrain.network.gui.dialogs.network.CompetitivePropertiesPanel;
import org.simbrain.network.gui.dialogs.network.SOMPropertiesPanel;
import org.simbrain.network.gui.dialogs.network.WTAPropertiesPanel;
import org.simbrain.network.gui.dialogs.neuron.BasicNeuronInfoPanel;
import org.simbrain.network.gui.dialogs.neuron.NeuronUpdateSettingsPanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.subnetworks.Competitive;
import org.simbrain.network.subnetworks.SOM;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;

/**
 * Main tabbed panel for editing all neuron groups. Specific neuron panels are
 * included as part of this.
 *
 * @author Jeff Yoshimi
 */
public class NeuronGroupPanel extends JPanel implements GroupPropertiesPanel {

    // TODO
    // - Change in a way that prepares for possible future where multiple can be
    // edited at once?

    /**
     * Default number of neurons (Specific neuron group panels specify this in
     * their property panels, so this is the default for bare neuron panels or
     * otherwise unspecified cases).
     */
    private static final int DEFAULT_NUM_NEURONS = 10;

    /** Network Topology. */
    private JTextField tfNumNeurons = new JTextField();

    /** Parent network panel. */
    private NetworkPanel networkPanel;

    /** Neuron Group. */
    private NeuronGroup neuronGroup;

    /** Main tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** General properties. */
    private JPanel tabMain = new JPanel();

    /** Miscellaneous tab. */
    private JPanel tabActivation = new JPanel();

    /** Miscellaneous tab. */
    private JPanel tabLayout = new JPanel();

    /** Layout panel. */
    private MainLayoutPanel layoutPanel;

    /** Panel for specific group types. Null for bare neuron group. */
    private JPanel specificNeuronGroupPanel;

    /** Label Field. */
    private final JTextField tfNeuronGroupLabel = new JTextField(7);

    /** Main properties panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Panel to edit neuron basic info. */
    private BasicNeuronInfoPanel editBasicNeuronInfo;

    /** Panel to edit neuron update rule. */
    private NeuronUpdateSettingsPanel editNeuronType;

    /** If true this is a creation panel. Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * Constructor for the case where a neuron group is being created.
     *
     * @param np Parent network panel
     * @param parentDialog parent dialog containing this panel.
     */
    public NeuronGroupPanel(final NetworkPanel np,
            final StandardDialog parentDialog) {
        networkPanel = np;
        isCreationPanel = true;
        mainPanel.addItem("Number of neurons", tfNumNeurons);
        initPanel(parentDialog);
    }

    /**
     * Constructor for case where an existing neuron group is being edited.
     *
     * @param np Parent network panel
     * @param ng neuron group being edited
     * @param parentDialog parent dialog containing this panel.
     */
    public NeuronGroupPanel(final NetworkPanel np, final NeuronGroup ng,
            final StandardDialog parentDialog) {
        networkPanel = np;
        neuronGroup = ng;
        isCreationPanel = false;
        initPanel(parentDialog);
    }

    /**
     * Initialize the panel.
     *
     * @param parentDialog the parent window
     */
    private void initPanel(final StandardDialog parentDialog) {

        // Set up group specific properties
        setSpecificGroup();

        // Layout panel
        layoutPanel = new MainLayoutPanel(false, parentDialog);
        if (!isCreationPanel) {
            ActionListener layoutAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    applyLayout();
                }
            };
            layoutPanel.setUseApplyButton(true, layoutAction);
        }
        tabLayout.add(layoutPanel);

        // Fill field values and create temp neuron group if needed
        fillFieldValues();

        // Set title
        if (!isCreationPanel) {
            if (specificNeuronGroupPanel == null) {
                parentDialog.setTitle("Edit Neuron Group");
            } else {
                parentDialog.setTitle("Edit "
                        + neuronGroup.getClass().getSimpleName());
            }
        }

        // Set up neuron edit panels
        Box editNeurons = Box.createVerticalBox();
        if (!isCreationPanel) {
            editBasicNeuronInfo = new BasicNeuronInfoPanel(
                    neuronGroup.getNeuronList(), parentDialog);
            editNeuronType = new NeuronUpdateSettingsPanel(
                    neuronGroup.getNeuronList(), parentDialog);
        } else {
            Neuron baseNeuron = new Neuron(networkPanel.getNetwork(),
                    new LinearRule());
            editBasicNeuronInfo = new BasicNeuronInfoPanel(
                    Collections.singletonList(baseNeuron), parentDialog);
            editNeuronType = new NeuronUpdateSettingsPanel(
                    Collections.singletonList(baseNeuron), parentDialog);
        }
        editNeurons.add(editBasicNeuronInfo);
        editNeurons.add(editNeuronType);

        // Set up main property panel
        if (!isCreationPanel) {
            mainPanel.addItem("Id:", new JLabel(neuronGroup.getId()));
            mainPanel.addItem("Neurons:", new JLabel(""
                    + neuronGroup.getNeuronList().size()));
        }
        mainPanel.addItem("Label:", tfNeuronGroupLabel);
        tabMain.add(mainPanel);

        // Add all tabs
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, tabbedPane);
        this.setMinimumSize(new Dimension(200,300));
        tabbedPane.addTab("Basics", tabMain);
        initializeSpecificGroupTab();
        tabbedPane.addTab("Neurons", editNeurons);
        tabbedPane.addTab("Layout", tabLayout);

        // Set up help button
        Action helpAction;
        if (specificNeuronGroupPanel != null) {
            helpAction = new ShowHelpAction(
                    ((GroupPropertiesPanel) specificNeuronGroupPanel)
                            .getHelpPath());
        } else {
            helpAction = new ShowHelpAction(this.getHelpPath());
        }
        parentDialog.addButton(new JButton(helpAction));
    }

    /**
     * Sets the specificNeuronGroupPanel based on the underlying group.
     */
    private void setSpecificGroup() {
        if (neuronGroup instanceof Competitive) {
            specificNeuronGroupPanel = new CompetitivePropertiesPanel(
                    networkPanel, (Competitive) neuronGroup);
        } else if (neuronGroup instanceof WinnerTakeAll) {
            specificNeuronGroupPanel = new WTAPropertiesPanel(networkPanel,
                    (WinnerTakeAll) neuronGroup);
        } else if (neuronGroup instanceof SOM) {
            specificNeuronGroupPanel = new SOMPropertiesPanel(networkPanel,
                    (SOM) neuronGroup);
        }
    }

    /**
     * Add a tab for specific neuron group rules.
     */
    private void initializeSpecificGroupTab() {
        if (specificNeuronGroupPanel == null) {
            return;
        } else {
            tabbedPane.addTab("Properties", specificNeuronGroupPanel);
        }
    }

    @Override
    public void fillFieldValues() {

        if (isCreationPanel) {
            tfNumNeurons.setText("" + DEFAULT_NUM_NEURONS);
            // Temporary. for fill field values....
            neuronGroup = new NeuronGroup(networkPanel.getNetwork(),
                    networkPanel.getLastClickedPosition(),
                    Integer.parseInt(tfNumNeurons.getText()));
        }

        tfNeuronGroupLabel.setText(neuronGroup.getLabel());
        if (specificNeuronGroupPanel != null) {
            ((GroupPropertiesPanel) specificNeuronGroupPanel).fillFieldValues();
        }

        // For backwards compatibility
        if (neuronGroup.getLayout() == null) {
            neuronGroup.setLayout(NeuronGroup.DEFAULT_LAYOUT);
        }

        layoutPanel.setCurrentLayout(neuronGroup.getLayout());
    }

    @Override
    public NeuronGroup commitChanges() {
        if (isCreationPanel) {
            neuronGroup = new NeuronGroup(networkPanel.getNetwork(),
                    networkPanel.getLastClickedPosition(),
                    Integer.parseInt(tfNumNeurons.getText()));
            editBasicNeuronInfo.commitChanges(neuronGroup.getNeuronList());
            editNeuronType.getNeuronPanel().commitChanges(
                    neuronGroup.getNeuronList());
            applyLayout();
        } else {
            editBasicNeuronInfo.commitChanges();
            editNeuronType.commitChanges();
        }
        if (!tfNeuronGroupLabel.getText().isEmpty()) {
            neuronGroup.setLabel(tfNeuronGroupLabel.getText());
        }
        if (specificNeuronGroupPanel != null) {
            ((GroupPropertiesPanel) specificNeuronGroupPanel).commitChanges();
        }

        networkPanel.repaint();
        return neuronGroup;
    }

    @Override
    public String getHelpPath() {
        return "Pages/Network/groups.html";
    }

    /**
     * Helper method to apply layout to neurons in this group.
     */
    private void applyLayout() {
        layoutPanel.commitChanges();
        neuronGroup.setLayout(layoutPanel.getCurrentLayout());
        neuronGroup.applyLayout();
    }

}
