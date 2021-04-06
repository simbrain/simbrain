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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A dialog for adding multiple neurons to the network. User can specify a
 * neuron type and a layout.
 *
 * @author ztosi
 * @author jyoshimi
 */
public class AddNeuronsDialog extends StandardDialog {

    /**
     * The default layout.
     */
    private static final Layout DEFAULT_LAYOUT = new GridLayout();

    /**
     * The default neuron.
     */
    private static final NeuronUpdateRule DEFAULT_NEURON = new LinearRule();

    /**
     * Default number of neurons.
     */
    private static final int DEFAULT_NUM_NEURONS = 25;

    /**
     * The network panel neurons will be added to.
     */
    private final NetworkPanel networkPanel;

    /**
     * The base neuron to copy.
     */
    private Neuron baseNeuron;

    /**
     * Help Button. Links to information about the currently selected neuron
     * update rule.
     */
    private final JButton helpButton = new JButton("Help");

    /**
     * Item panel where options will be displayed.
     */
    private final Box addNeuronsPanel = Box.createVerticalBox();

    /**
     * Text field where desired number of neurons is entered.
     */
    private final JTextField numNeurons = new JTextField("" + DEFAULT_NUM_NEURONS);

    /**
     * The panel containing basic information on the neurons as well as options
     * for setting their update rule and its parameters.
     */
    private AnnotatedPropertyEditor combinedNeuronInfoPanel;

    /**
     * Layout object.
     */
    private Layout.LayoutObject layoutObject = new Layout.LayoutObject();

    /**
     * A panel where layout settings can be edited.
     */
    private AnnotatedPropertyEditor selectLayout;

    /**
     * A panel for editing whether or not the neurons will be added as a group.
     */
    private NeuronGroupPanelLite groupPanel;

    /**
     * A List of the neurons being added to the network.
     */
    private final List<Neuron> addedNeurons = new ArrayList<Neuron>();

    /**
     * A factory method that creates an AddNeuronsDialog to prevent references
     * to "this" from escaping during construction.
     *
     * @param networkPanel the network panel neurons will be added to.
     * @return an AddNeuronsDialog
     */
    public static AddNeuronsDialog createAddNeuronsDialog(final NetworkPanel networkPanel) {
        final AddNeuronsDialog addND = new AddNeuronsDialog(networkPanel);
        addND.combinedNeuronInfoPanel = new AnnotatedPropertyEditor(Collections.singletonList(addND.baseNeuron));
        addND.combinedNeuronInfoPanel.setBorder(BorderFactory.createTitledBorder("Neuron Type"));
        addND.init();
        return addND;
    }

    /**
     * Constructs the dialog.
     *
     * @param networkPanel the panel the neurons are being added to.
     */
    private AddNeuronsDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        baseNeuron = new Neuron(networkPanel.getNetwork(), DEFAULT_NEURON);
        networkPanel.getSelectionManager().clear();
    }

    /**
     * Initializes the add neurons panel with default settings.
     */
    private void init() {

        setTitle("Add Neurons...");

        // Basics Sub-Panel
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

        // Neuron Properties Panel
        addNeuronsPanel.add(combinedNeuronInfoPanel);
        combinedNeuronInfoPanel.setDetailTrianglesOpen(false);

        // Layout Panel
        layoutObject.setLayout(DEFAULT_LAYOUT);
        selectLayout = new AnnotatedPropertyEditor(layoutObject);
        selectLayout.setDetailTrianglesOpen(false);
        addNeuronsPanel.add(selectLayout);

        // Group Panel
        groupPanel = new NeuronGroupPanelLite(networkPanel);
        addNeuronsPanel.add(groupPanel);

        // Final setup
        setContentPane(new JScrollPane(addNeuronsPanel));
        this.addButton(helpButton);
    }

    /**
     * Adds the neurons to the panel.
     *
     * @param inGroup if true, add them in a group.
     */
    private void addNeurons(boolean inGroup) {
        double number = Utils.doubleParsable(numNeurons);
        if (!Double.isNaN(number)) {
            Network net = networkPanel.getNetwork();
            for (int i = 0; i < number; i++) {
                addedNeurons.add(new Neuron(net, baseNeuron));
            }
            if (inGroup) {
                NeuronGroup ng = new NeuronGroup(networkPanel.getNetwork(), addedNeurons);
                ng.setLayout(layoutObject.getLayout());
                networkPanel.getNetwork().addNetworkModel(ng);
                ng.applyLayout();
                ng.setLabel(groupPanel.tfGroupName.getText());
            } else {
                layoutObject.getLayout().layoutNeurons(addedNeurons);
                addedNeurons.forEach(networkPanel.getNetwork()::addNetworkModel);
            }
        }
    }


    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        combinedNeuronInfoPanel.commitChanges();
        selectLayout.commitChanges();
        addNeurons(groupPanel.getAddToGroup().isSelected());
        dispose();
    }

    @Override
    protected void closeDialogCancel() {
        super.closeDialogCancel();
        dispose();
    }

    /**
     * A sub-panel which allows a user to put newly created neurons into a
     * neuron group. Options include a new neuron group, already existing neuron
     * group, or no neuron group (loose). The user can also change a group's
     * name from here.
     *
     * @author ztosi
     */
    @SuppressWarnings("serial")
    private class NeuronGroupPanelLite extends JPanel {

        /**
         * Select whether or not to add the neurons in a neuron group.
         */
        private JCheckBox addToGroup = new JCheckBox();

        /**
         * A label for the neuron group name.
         */
        private JLabel tfNameLabel = new JLabel("Name: ");

        /**
         * A text box for naming a new neuron group or renaming an existing one.
         */
        private JTextField tfGroupName = new JTextField();

        /**
         * Creates the neuron group sub-panel
         *
         * @param np a reference to the network panel.
         */
        public NeuronGroupPanelLite(NetworkPanel np) {
            addListeners();
            setLayout(new BorderLayout());
            addToGroup.setSelected(true);

            JPanel groupPanel = new JPanel();
            groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.X_AXIS));
            groupPanel.add(addToGroup);
            groupPanel.add(Box.createHorizontalStrut(20));
            groupPanel.add(tfNameLabel);
            tfGroupName.setEnabled(addToGroup.isSelected());
            String dName = networkPanel.getNetwork()
                    .getIdManager().getProposedId(NeuronGroup.class);
            tfGroupName.setText(dName);
            groupPanel.add(tfGroupName);
            groupPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            this.add(groupPanel, BorderLayout.CENTER);
            setBorder(BorderFactory.createTitledBorder("Group"));

        }

        /**
         * Adds (internal) listeners to the panel.
         */
        private void addListeners() {
            addToGroup.addActionListener(evt -> {
                tfGroupName.setEnabled(addToGroup.isSelected());
                String dName = networkPanel.getNetwork()
                        .getIdManager().getProposedId(NeuronGroup.class);
                tfGroupName.setText(dName);
            });
        }

        public JCheckBox getAddToGroup() {
            return addToGroup;
        }

    }

}
