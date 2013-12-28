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

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.simbrain.network.groups.Group;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.group.GroupPropertiesPanel;
import org.simbrain.network.subnetworks.SOMGroup;
import org.simbrain.network.subnetworks.SOMNetwork;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>SOMPropertiesDialog</b> is a dialog box for setting the properties of a
 * Self organizing map.
 *
 */
public class SOMPropertiesPanel extends JPanel implements GroupPropertiesPanel {

    /** Default number of neurons. */
    private static final int DEFAULT_NUM_SOM_NEURONS = 16;

    /** Default number of input neurons. */
    private static final int DEFAULT_NUM_INPUT_NEURONS = 10;

    /** Parent Network Panel. */
    private NetworkPanel networkPanel;

    /** Number of neurons field. */
    private JTextField tfNumSOMNeurons = new JTextField();

    /** Number of neurons field. */
    private JTextField tfNumInputNeurons = new JTextField();

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Top Panel for creation panels. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** InitAlpha value field. */
    private JTextField tfAlpha = new JTextField();

    /** InitNeighborhoodSize value field. */
    private JTextField tfInitNeighborhoodSize = new JTextField();

    /** AlphaDecayRate value field. */
    private JTextField tfAlphaDecayRate = new JTextField();

    /** NeighborhoodDecayAmount value field. */
    private JTextField tfNeigborhoodDecayAmount = new JTextField();

    /** The model subnetwork. */
    private Group som;

    /**
     * Whether the panel is for creating an SOM group, network, or editing a
     * group.
     */
    public static enum SOMPropsPanelType {
        CREATE_GROUP, CREATE_NETWORK, EDIT_GROUP
    };

    /** The type of this panel. */
    private final SOMPropsPanelType panelType;

    /**
     * Constructor for the case where an som group is being created.
     *
     * @param np parent network panel
     * @param panelType whether this is a network or group creation panel. Edit
     *            is not an acceptable argument.
     */
    public SOMPropertiesPanel(final NetworkPanel np,
            final SOMPropsPanelType panelType) {
        // TODO Error if paneltype is edit
        this.networkPanel = np;
        this.panelType = panelType;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if (panelType == SOMPropsPanelType.CREATE_GROUP) {
            topPanel.addItem("Number of neurons", tfNumSOMNeurons);
        } else if (panelType == SOMPropsPanelType.CREATE_NETWORK) {
            topPanel.addItem("Number of SOM neurons", tfNumSOMNeurons);
            topPanel.addItem("Number of input neurons", tfNumInputNeurons);
        }
        add(topPanel);
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        add(separator);
        initPanel();
    }

    /**
     * Constructor for case where an existing som group is being edited.
     *
     * @param np parent network panel
     * @param som network being modified.
     */
    public SOMPropertiesPanel(final NetworkPanel np, final SOMGroup som) {
        this.networkPanel = np;
        this.som = som;
        panelType = SOMPropsPanelType.EDIT_GROUP;
        initPanel();
    }

    /**
     * Initialize the panel.
     */
    private void initPanel() {
        fillFieldValues();
        mainPanel.addItem("Initial Learning Rate", tfAlpha);
        mainPanel.addItem("Initial Neighborhood Size", tfInitNeighborhoodSize);
        mainPanel.addItem("Learning Decay Rate", tfAlphaDecayRate);
        mainPanel
                .addItem("Neighborhood Decay Amount", tfNeigborhoodDecayAmount);
        add(mainPanel);
    }

    @Override
    public void fillFieldValues() {
        // For creation panels use an "empty" som network to harvest
        // default values
        if (panelType == SOMPropsPanelType.CREATE_GROUP) {
            som = new SOMGroup(null, 1);
            tfNumSOMNeurons.setText("" + DEFAULT_NUM_SOM_NEURONS);
            fillSOMGroupFieldValues();
        } else if (panelType == SOMPropsPanelType.CREATE_NETWORK) {
            som = new SOMNetwork(null, 1, 1);
            tfNumSOMNeurons.setText("" + DEFAULT_NUM_SOM_NEURONS);
            tfNumInputNeurons.setText("" + DEFAULT_NUM_INPUT_NEURONS);
            fillSOMNetworkFieldValues();
        } else if (panelType == SOMPropsPanelType.EDIT_GROUP) {
            fillSOMGroupFieldValues();
        }
    }

    /**
     * Fill field values for a SOM group.
     */
    private void fillSOMGroupFieldValues() {
        tfAlpha.setText(Double.toString(((SOMGroup) som).getInitAlpha()));
        tfInitNeighborhoodSize.setText(Double.toString(((SOMGroup) som)
                .getInitNeighborhoodSize()));
        tfAlphaDecayRate.setText(Double.toString(((SOMGroup) som)
                .getAlphaDecayRate()));
        tfNeigborhoodDecayAmount.setText(Double.toString(((SOMGroup) som)
                .getNeighborhoodDecayAmount()));

    }

    /**
     * Fill field values for a SOM network.
     */
    private void fillSOMNetworkFieldValues() {
        tfAlpha.setText(Double.toString(((SOMNetwork) som).getSom()
                .getInitAlpha()));
        tfInitNeighborhoodSize.setText(Double.toString(((SOMNetwork) som)
                .getSom().getInitNeighborhoodSize()));
        tfAlphaDecayRate.setText(Double.toString(((SOMNetwork) som).getSom()
                .getAlphaDecayRate()));
        tfNeigborhoodDecayAmount.setText(Double.toString(((SOMNetwork) som)
                .getSom().getNeighborhoodDecayAmount()));

    }

    @Override
    public Group commitChanges() {
        if (panelType == SOMPropsPanelType.CREATE_GROUP) {
            som = new SOMGroup(networkPanel.getNetwork(),
                    Integer.parseInt(tfNumSOMNeurons.getText()));
            commitSOMGroupFieldValues();
        } else if (panelType == SOMPropsPanelType.CREATE_NETWORK) {
            som = new SOMNetwork(networkPanel.getNetwork(),
                    Integer.parseInt(tfNumSOMNeurons.getText()),
                    Integer.parseInt(tfNumInputNeurons.getText()));
            commitSOMNetworkFieldValues();
        } else if (panelType == SOMPropsPanelType.EDIT_GROUP) {
            commitSOMGroupFieldValues();
        }
        return som;
    }

    /**
     * Commit values for a SOM Group.
     */
    private void commitSOMGroupFieldValues() {
        ((SOMGroup) som).setInitAlpha(Double.parseDouble(tfAlpha.getText()));
        ((SOMGroup) som).setInitNeighborhoodSize(Double
                .parseDouble(tfInitNeighborhoodSize.getText()));
        ((SOMGroup) som).setAlphaDecayRate(Double.parseDouble(tfAlphaDecayRate
                .getText()));
        ((SOMGroup) som).setNeighborhoodDecayAmount(Double
                .parseDouble(tfNeigborhoodDecayAmount.getText()));

    }

    /**
     * Commit values for a SOM Network.
     */
    private void commitSOMNetworkFieldValues() {
        ((SOMNetwork) som).getSom().setInitAlpha(
                Double.parseDouble(tfAlpha.getText()));
        ((SOMNetwork) som).getSom().setInitNeighborhoodSize(
                Double.parseDouble(tfInitNeighborhoodSize.getText()));
        ((SOMNetwork) som).getSom().setAlphaDecayRate(
                Double.parseDouble(tfAlphaDecayRate.getText()));
        ((SOMNetwork) som).getSom().setNeighborhoodDecayAmount(
                Double.parseDouble(tfNeigborhoodDecayAmount.getText()));
    }

    @Override
    public String getHelpPath() {
        return "Pages/Network/network/som.html";
    }

}
