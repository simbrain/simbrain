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
package org.simbrain.network.gui.dialogs.network;

import org.simbrain.network.groups.Group;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.group.GroupPropertiesPanel;
import org.simbrain.network.subnetworks.CompetitiveGroup;
import org.simbrain.network.subnetworks.CompetitiveGroup.UpdateMethod;
import org.simbrain.network.subnetworks.CompetitiveNetwork;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.widgets.EditablePanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <b>CompetitivePropertiesDialog</b> is a panel box for setting the properties
 * of a competitive network. Can either be used to create a new competitive
 * network or to edit an existing competitive network.
 */
@SuppressWarnings("serial")
public class CompetitivePropertiesPanel extends JPanel implements ActionListener, GroupPropertiesPanel, EditablePanel {

    /**
     * Default number of competitive neurons.
     */
    private static final int DEFAULT_NUM_COMPETITIVE_NEURONS = 5;

    /**
     * Default number of input neurons.
     */
    private static final int DEFAULT_NUM_INPUT_NEURONS = 3;

    /**
     * Parent Network Panel.
     */
    private NetworkPanel networkPanel;

    /**
     * Main Panel.
     */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /**
     * Top Panel for creation panels.
     */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /**
     * Update method.
     */
    private JComboBox<UpdateMethod> updateMethod = new JComboBox<UpdateMethod>(UpdateMethod.values());

    /**
     * Number of neurons field.
     */
    private JTextField tfNumCompetitiveNeurons = new JTextField();

    /**
     * Number of neurons field.
     */
    private JTextField tfNumInputNeurons = new JTextField();

    /**
     * Epsilon value field.
     */
    private JTextField tfEpsilon = new JTextField();

    /**
     * Winner value field.
     */
    private JTextField tfWinnerValue = new JTextField();

    /**
     * Loser value field.
     */
    private JTextField tfLoserValue = new JTextField();

    /**
     * Leaky epsilon value.
     */
    private JTextField tfLeakyEpsilon = new JTextField();

    /**
     * Leaky learning check box.
     */
    private JCheckBox cbUseLeakyLearning = new JCheckBox();

    /**
     * Decay percent.
     */
    private JTextField tfSynpaseDecayPercent = new JTextField();

    /**
     * Normalize inputs check box.
     */
    private JCheckBox cbNormalizeInputs = new JCheckBox();

    /**
     * The model subnetwork.
     */
    private Group competitive;

    /**
     * Whether the panel is for creating a competitive group, network, or
     * editing a competitive group.
     */
    public static enum CompetitivePropsPanelType {
        CREATE_GROUP, CREATE_NETWORK, EDIT_GROUP
    }

    ;

    /**
     * The type of this panel.
     */
    private final CompetitivePropsPanelType panelType;

    /**
     * A factory method for creating a competitive properties panel for cases
     * where the panel is being created.
     *
     * @param np        the network panel
     * @param panelType the type of panel (CREATE_GROUP or CREATE_NETWORK)
     * @return a competitive properties panel of the give type
     * @throws IllegalArgumentException if CompetitivePropsPanelType.EDIT_GROUP is passed into this
     *                                  constructor as the panelType parameter
     */
    public static CompetitivePropertiesPanel createCompetitivePropertiesPanel(final NetworkPanel np, final CompetitivePropsPanelType panelType) throws IllegalArgumentException {

        CompetitivePropertiesPanel cpp = new CompetitivePropertiesPanel(np, panelType);
        cpp.addListeners();
        return cpp;
    }

    /**
     * Creates a competitive properties panel from a CompetitiveGroup allowing
     * that group to be edited.
     *
     * @param np          the network panel
     * @param competitive the competitive group being edited
     * @return a CompetitivePropertiesPanel which can edit values in the
     * CompetitiveGroup and whose fields represent its values.
     */
    public static CompetitivePropertiesPanel createCompetitivePropertiesPanel(final NetworkPanel np, final CompetitiveGroup competitive) {
        CompetitivePropertiesPanel cpp = new CompetitivePropertiesPanel(np, competitive);
        cpp.addListeners();
        return cpp;
    }

    /**
     * Constructor for the case where a competitive network is being created.
     *
     * @param np        parent network panel
     * @param panelType whether this is a network or group creation panel. Edit is not
     *                  an acceptable argument.
     * @throws IllegalArgumentException if CompetitivePropsPanelType.EDIT_GROUP is passed into this
     *                                  constructor as the panelType parameter
     */
    private CompetitivePropertiesPanel(final NetworkPanel np, final CompetitivePropsPanelType panelType) throws IllegalArgumentException {
        if (panelType.equals(CompetitivePropsPanelType.EDIT_GROUP)) {
            throw new IllegalArgumentException("No competitive group to" + " edit.");
        }
        this.networkPanel = np;
        this.panelType = panelType;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if (panelType == CompetitivePropsPanelType.CREATE_GROUP) {
            topPanel.addItem("Number of neurons", tfNumCompetitiveNeurons);
        } else if (panelType == CompetitivePropsPanelType.CREATE_NETWORK) {
            topPanel.addItem("Number of competitive neurons", tfNumCompetitiveNeurons);
            topPanel.addItem("Number of input neurons", tfNumInputNeurons);
        }
        add(topPanel);
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        add(separator);
        initPanel();
    }

    /**
     * Constructor for the case where an existing competitive network is being
     * edited.
     *
     * @param np          parent network panel
     * @param competitive Competitive network being modified.
     */
    private CompetitivePropertiesPanel(final NetworkPanel np, final CompetitiveGroup competitive) {
        this.networkPanel = np;
        this.competitive = competitive;
        panelType = CompetitivePropsPanelType.EDIT_GROUP;
        initPanel();
    }

    /**
     * Initialize the panel.
     */
    private void initPanel() {
        fillFieldValues();
        mainPanel.addItem("UpdateMethod", updateMethod);
        mainPanel.addItem("Epsilon", tfEpsilon);
        mainPanel.addItem("Winner Value", tfWinnerValue);
        mainPanel.addItem("Loser Value", tfLoserValue);
        mainPanel.addItem("Use Leaky Learning", cbUseLeakyLearning);
        mainPanel.addItem("Leaky Epsilon", tfLeakyEpsilon);
        mainPanel.addItem("Normalize Inputs", cbNormalizeInputs);
        mainPanel.addItem("Synapse Decay Percent", tfSynpaseDecayPercent);
        checkLeakyEpsilon();
        enableFieldBasedOnUpdateMethod();
        add(mainPanel);
    }

    /**
     * Adds internal listeners to the panel.
     */
    private void addListeners() {
        updateMethod.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableFieldBasedOnUpdateMethod();
            }
        });
        cbUseLeakyLearning.addActionListener(this);
        cbUseLeakyLearning.setActionCommand("useLeakyLearning");
    }

    @Override
    public void fillFieldValues() {
        // For creation panels use an "empty" competitive network to harvest
        // default values
        if (panelType == CompetitivePropsPanelType.CREATE_GROUP) {
            competitive = new CompetitiveGroup(null, 1);
            tfNumCompetitiveNeurons.setText("" + DEFAULT_NUM_COMPETITIVE_NEURONS);
            fillCompetitiveGroupFieldValues();
        } else if (panelType == CompetitivePropsPanelType.CREATE_NETWORK) {
            competitive = new CompetitiveNetwork(null, 1, 1, networkPanel.getWhereToAdd());
            tfNumCompetitiveNeurons.setText("" + DEFAULT_NUM_COMPETITIVE_NEURONS);
            tfNumInputNeurons.setText("" + DEFAULT_NUM_INPUT_NEURONS);
            fillCompetitiveNetworkFieldValues();
        } else if (panelType == CompetitivePropsPanelType.EDIT_GROUP) {
            fillCompetitiveGroupFieldValues();
        }
    }

    /**
     * Fill field values for a Competitive Group.
     */
    private void fillCompetitiveGroupFieldValues() {
        updateMethod.setSelectedItem(((CompetitiveGroup) competitive).getUpdateMethod());
        tfEpsilon.setText(Double.toString(((CompetitiveGroup) competitive).getLearningRate()));
        tfLoserValue.setText(Double.toString(((CompetitiveGroup) competitive).getLoseValue()));
        tfWinnerValue.setText(Double.toString(((CompetitiveGroup) competitive).getWinValue()));
        tfLeakyEpsilon.setText(Double.toString(((CompetitiveGroup) competitive).getLeakyLearningRate()));
        tfSynpaseDecayPercent.setText(Double.toString(((CompetitiveGroup) competitive).getSynpaseDecayPercent()));
        cbUseLeakyLearning.setSelected(((CompetitiveGroup) competitive).getUseLeakyLearning());
        cbNormalizeInputs.setSelected(((CompetitiveGroup) competitive).getNormalizeInputs());
    }

    /**
     * Fill field values for a Competitive Network.
     */
    private void fillCompetitiveNetworkFieldValues() {
        updateMethod.setSelectedItem(((CompetitiveNetwork) competitive).getCompetitive().getUpdateMethod());
        tfEpsilon.setText(Double.toString(((CompetitiveNetwork) competitive).getCompetitive().getLearningRate()));
        tfLoserValue.setText(Double.toString(((CompetitiveNetwork) competitive).getCompetitive().getLoseValue()));
        tfWinnerValue.setText(Double.toString(((CompetitiveNetwork) competitive).getCompetitive().getWinValue()));
        tfLeakyEpsilon.setText(Double.toString(((CompetitiveNetwork) competitive).getCompetitive().getLeakyLearningRate()));
        tfSynpaseDecayPercent.setText(Double.toString(((CompetitiveNetwork) competitive).getCompetitive().getSynpaseDecayPercent()));
        cbUseLeakyLearning.setSelected(((CompetitiveNetwork) competitive).getCompetitive().getUseLeakyLearning());
        cbNormalizeInputs.setSelected(((CompetitiveNetwork) competitive).getCompetitive().getNormalizeInputs());
    }

    /**
     * @see java.awt.event.ActionListener
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("useLeakyLearning")) {
            checkLeakyEpsilon();
        }

    }

    /**
     * Does what it says :)
     */
    private void enableFieldBasedOnUpdateMethod() {
        if (updateMethod.getSelectedItem() == CompetitiveGroup.UpdateMethod.ALVAREZ_SQUIRE) {
            tfSynpaseDecayPercent.setEnabled(true);
        } else if (updateMethod.getSelectedItem() == CompetitiveGroup.UpdateMethod.RUMM_ZIPSER) {
            tfSynpaseDecayPercent.setEnabled(false);
        }
    }

    /**
     * Checks whether or not to enable leaky epsilon.
     */
    private void checkLeakyEpsilon() {
        if (cbUseLeakyLearning.isSelected()) {
            tfLeakyEpsilon.setEnabled(true);
        } else {
            tfLeakyEpsilon.setEnabled(false);
        }
    }

    @Override
    public String getHelpPath() {
        return "Pages/Network/network/competitivenetwork.html";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean commitChanges() {
        try {
            if (panelType == CompetitivePropsPanelType.CREATE_GROUP) {
                competitive = new CompetitiveGroup(networkPanel.getNetwork(), Integer.parseInt(tfNumCompetitiveNeurons.getText()));
                commitCompetitiveGroupFieldValues();
            } else if (panelType == CompetitivePropsPanelType.CREATE_NETWORK) {
                competitive = new CompetitiveNetwork(networkPanel.getNetwork(), Integer.parseInt(tfNumCompetitiveNeurons.getText()), Integer.parseInt(tfNumInputNeurons.getText()), networkPanel.getWhereToAdd());
                commitCompetitiveNetworkFieldValues();
            } else if (panelType == CompetitivePropsPanelType.EDIT_GROUP) {
                commitCompetitiveGroupFieldValues();
            }
        } catch (NumberFormatException nfe) {
            return false; // Failure
        }
        return true; // Success
    }

    /**
     * Commit values for a competitive group.
     */
    private void commitCompetitiveGroupFieldValues() {
        ((CompetitiveGroup) competitive).setUpdateMethod((UpdateMethod) updateMethod.getSelectedItem());
        ((CompetitiveGroup) competitive).setLearningRate(Double.parseDouble(tfEpsilon.getText()));
        ((CompetitiveGroup) competitive).setWinValue(Double.parseDouble(tfWinnerValue.getText()));
        ((CompetitiveGroup) competitive).setLoseValue(Double.parseDouble(tfLoserValue.getText()));
        ((CompetitiveGroup) competitive).setSynpaseDecayPercent(Double.parseDouble(tfSynpaseDecayPercent.getText()));
        ((CompetitiveGroup) competitive).setLeakyLearningRate(Double.parseDouble(tfLeakyEpsilon.getText()));
        ((CompetitiveGroup) competitive).setUseLeakyLearning(cbUseLeakyLearning.isSelected());
        ((CompetitiveGroup) competitive).setNormalizeInputs(cbNormalizeInputs.isSelected());
    }

    /**
     * Commit values for a competitive network
     */
    private void commitCompetitiveNetworkFieldValues() {
        ((CompetitiveNetwork) competitive).getCompetitive().setUpdateMethod((UpdateMethod) updateMethod.getSelectedItem());
        ((CompetitiveNetwork) competitive).getCompetitive().setLearningRate(Double.parseDouble(tfEpsilon.getText()));
        ((CompetitiveNetwork) competitive).getCompetitive().setWinValue(Double.parseDouble(tfWinnerValue.getText()));
        ((CompetitiveNetwork) competitive).getCompetitive().setLoseValue(Double.parseDouble(tfLoserValue.getText()));
        ((CompetitiveNetwork) competitive).getCompetitive().setSynpaseDecayPercent(Double.parseDouble(tfSynpaseDecayPercent.getText()));
        ((CompetitiveNetwork) competitive).getCompetitive().setLeakyLearningRate(Double.parseDouble(tfLeakyEpsilon.getText()));
        ((CompetitiveNetwork) competitive).getCompetitive().setUseLeakyLearning(cbUseLeakyLearning.isSelected());
        ((CompetitiveNetwork) competitive).getCompetitive().setNormalizeInputs(cbNormalizeInputs.isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Group getGroup() {
        return competitive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getPanel() {
        return this;
    }

}
