/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.neuron;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.MainLayoutPanel;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * A dialog for adding multiple neurons to the network. User can specify a
 * neuron type and a layout.
 *
 * TODO: Merge this and NeuronDialog? They're almost the same class and we could
 * just add a Creation/Editing enum to tell it whether or not to display certain
 * fields.
 *
 * @author ztosi
 * @author jyoshimi
 */
public class AddNeuronsDialog extends StandardDialog {

    /** Default. */
    private static final long serialVersionUID = 1L;

    /** The default layout. */
    private static final Layout DEFAULT_LAYOUT = new GridLayout();

    /** The default neuron. */
    private static final NeuronUpdateRule DEFAULT_NEURON = new LinearRule();

    /** Default number of neurons. */
    private static final int DEFAULT_NUM_NEURONS = 25;

    /** The network panel neurons will be added to. */
    private final NetworkPanel networkPanel;

    /** The base neuron to copy. */
    private Neuron baseNeuron;

    /**
     * Help Button. Links to information about the currently selected neuron
     * update rule.
     */
    private final JButton helpButton = new JButton("Help");

    /** Show Help Action. The action executed by the help button */
    private ShowHelpAction helpAction;

    /** Item panel where options will be displayed. */
    private final Box addNeuronsPanel = Box.createVerticalBox();

    /** Text field where desired number of neurons is entered. */
    private final JTextField numNeurons = new JTextField(""
            + DEFAULT_NUM_NEURONS);

    /**
     * The panel containing basic information on the neurons as well as options
     * for setting their update rule and its parameters.
     */
    private NeuronPropertiesPanel combinedNeuronInfoPanel;

    /** A panel where layout settings can be edited. */
    private MainLayoutPanel selectLayout;

    /**
     * A panel for editing whether or not the neurons will be added as a group.
     */
    private NeuronGroupPanelLite groupPanel;

    /** A List of the neurons being added to the network. */
    private final List<Neuron> addedNeurons = new ArrayList<Neuron>();

    /**
     * A factory method that creates an AddNeuronsDialog to prevent references
     * to "this" from escaping during construction.
     *
     * @param networkPanel
     *            the network panel neurons will be added to.
     * @return an AddNeuronsDialog
     */
    public static AddNeuronsDialog createAddNeuronsDialog(
            final NetworkPanel networkPanel) {
        final AddNeuronsDialog addND = new AddNeuronsDialog(networkPanel);
        addND.combinedNeuronInfoPanel = NeuronPropertiesPanel
                .createNeuronPropertiesPanel(
                        Collections.singletonList(addND.baseNeuron), addND,
                        false);
        addND.init();
        addND.combinedNeuronInfoPanel.getUpdateRulePanel().getCbNeuronType()
                .addActionListener(e -> SwingUtilities
                        .invokeLater(() -> addND.updateHelp()));
        return addND;
    }

    /**
     * Constructs the dialog.
     *
     * @param networkPanel
     *            the panel the neurons are being added to.
     */
    private AddNeuronsDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        baseNeuron = new Neuron(networkPanel.getNetwork(), DEFAULT_NEURON);
        networkPanel.clearSelection();
    }

    /**
     * Initializes the add neurons panel with default settings.
     */
    private void init() {
        setTitle("Add Neurons...");

        JPanel basicsPanel = new JPanel(new GridBagLayout());
        basicsPanel.setBorder(BorderFactory.createTitledBorder("Quantity"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.8;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        basicsPanel.add(new JLabel("Number of Neurons:"), gbc);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(5, 3, 0, 0);
        gbc.weightx = 0.2;
        gbc.gridx = 1;
        basicsPanel.add(numNeurons, gbc);
        addNeuronsPanel.add(basicsPanel);

        addNeuronsPanel.add(Box.createVerticalStrut(10));

        addNeuronsPanel.add(combinedNeuronInfoPanel);

        addNeuronsPanel.add(Box.createVerticalStrut(10));

        selectLayout = new MainLayoutPanel(DEFAULT_LAYOUT, true, this);
        selectLayout.setAlignmentX(CENTER_ALIGNMENT);
        addNeuronsPanel.add(selectLayout);

        addNeuronsPanel.add(Box.createVerticalStrut(10));
        groupPanel = new NeuronGroupPanelLite(networkPanel);
        groupPanel.setAlignmentX(CENTER_ALIGNMENT);
        addNeuronsPanel.add(groupPanel);

        setContentPane(addNeuronsPanel);
        this.addButton(helpButton);
    }

    /**
     * Adds the neurons to the panel.
     */
    private void addNeurons() {
        double number = Utils.doubleParsable(numNeurons);
        if (!Double.isNaN(number)) {
            number = (int) number;
            Network net = networkPanel.getNetwork();
            for (int i = 0; i < number; i++) {
                addedNeurons.add(new Neuron(net, baseNeuron));
            }
            networkPanel.addNeuronsToPanel(addedNeurons,
                    selectLayout.getCurrentLayout());
        }
    }

    /**
     * Adds the neurons to the panel as a group.
     */
    private void addGroup() {
        addNeurons();
        NeuronGroup ng = groupPanel.generateNeuronGroup();
        if (ng != null) {
            networkPanel.getNetwork().transferNeuronsToGroup(addedNeurons, ng);
            networkPanel.getNetwork().addGroup(ng);
            ng.setLayout(selectLayout.getCurrentLayout());
            networkPanel.repaint();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void closeDialogOk() {
        super.closeDialogOk();
        combinedNeuronInfoPanel.commitChanges();
        selectLayout.commitChanges();
        NeuronUpdateRule dbug = baseNeuron.getUpdateRule();
        dbug.setIncrement(0.1);
        if (groupPanel.getAddToGroup().isSelected()) {
            addGroup();
        } else {
            addNeurons();
        }
        dispose();
    }

    /**
     * {@inheritDoc}
     */
    protected void closeDialogCancel() {
        super.closeDialogCancel();
        dispose();
    }

    /**
     * Set the help page based on the currently selected neuron type.
     */
    private void updateHelp() {
        if (combinedNeuronInfoPanel.getUpdateRulePanel().getCbNeuronType()
                .getSelectedItem() == SimbrainConstants.NULL_STRING) {
            helpAction = new ShowHelpAction("Pages/Network/neuron.html");
        } else {
            String name = (String) combinedNeuronInfoPanel.getUpdateRulePanel()
                    .getCbNeuronType().getSelectedItem();
            helpAction = new ShowHelpAction("Pages/Network/neuron/" + name
                    + ".html");
        }
        helpButton.setAction(helpAction);
    }

    public void tickGroupifyOption(boolean gropify) {
        groupPanel.addToGroup.setSelected(gropify);
    }

    /**
     * A sub-panel which allows a user to put newly created neurons into a
     * neuron group. Options include a new neuron group, already existing neuron
     * group, or no neuron group (loose). The user can also change a group's
     * name from here.
     *
     * @author ztosi
     *
     */
    @SuppressWarnings("serial")
    private class NeuronGroupPanelLite extends JPanel {

        /** Select whether or not to add the neurons in a neuron group. */
        private JCheckBox addToGroup = new JCheckBox();

        /** A label for the neuron group name. */
        private JLabel tfNameLabel = new JLabel("Name: ");

        /**
         * A text box for naming a new neuron group or renaming an existing one.
         */
        private JTextField tfGroupName = new JTextField();

        /**
         * Creates the neuron group sub-panel
         *
         * @param np
         *            a reference to the network panel.
         */
        public NeuronGroupPanelLite(NetworkPanel np) {
            addListeners();
            setLayout(new BorderLayout());

            JPanel subPanel = new JPanel();
            subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.X_AXIS));
            subPanel.add(addToGroup);
            subPanel.add(Box.createHorizontalStrut(20));
            subPanel.add(tfNameLabel);
            tfGroupName.setEnabled(addToGroup.isSelected());
            subPanel.add(tfGroupName);
            subPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            this.add(subPanel, BorderLayout.CENTER);
            setBorder(BorderFactory.createTitledBorder("Group"));

        }

        /**
         * Adds (internal) listeners to the panel.
         */
        private void addListeners() {
            addToGroup.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent arg0) {

                    tfGroupName.setEnabled(addToGroup.isSelected());
                    String dName = networkPanel.getNetwork()
                            .getGroupIdGenerator().getId();
                    tfGroupName.setText(dName);

                }

            });

        }

        /**
         * Generates the neuron group with the attributes from the panel.
         * Returns null if the {@link #addToGroup addToGroup} check-box is not
         * selected.
         *
         * @return
         */
        public NeuronGroup generateNeuronGroup() {
            if (addToGroup.isSelected()) {
                NeuronGroup ng = new NeuronGroup(networkPanel.getNetwork());
                ng.setLabel(tfGroupName.getText());
                return ng;
            } else {
                return null;
            }
        }

        /**
         * @return the addToGroup check-box
         */
        public JCheckBox getAddToGroup() {
            return addToGroup;
        }

    }

}
