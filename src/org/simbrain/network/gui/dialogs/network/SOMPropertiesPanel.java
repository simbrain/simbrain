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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.groups.Group;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.group.GroupPropertiesPanel;
import org.simbrain.network.subnetworks.SOM;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.Utils;

/**
 * <b>SOMPropertiesDialog</b> is a dialog box for setting the properties of a
 * Self organizing map.
 *
 */
public class SOMPropertiesPanel extends JPanel implements
        GroupPropertiesPanel {

    /** Default number of neurons. */
    private static final int DEFAULT_NUM_NEURONS = 16;

    /** Parent Network Panel. */
    private NetworkPanel networkPanel;

    /** Number of neurons field. */
    private JTextField tfNumNeurons = new JTextField();

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** InitAlpha value field. */
    private JTextField tfAlpha = new JTextField();

    /** InitNeighborhoodSize value field. */
    private JTextField tfInitNeighborhoodSize = new JTextField();

    /** AlphaDecayRate value field. */
    private JTextField tfAlphaDecayRate = new JTextField();

    /** NeighborhoodDecayAmount value field. */
    private JTextField tfNeigborhoodDecayAmount = new JTextField();

    /** Current Learning Rate. */
    private JLabel lLearningRate = new JLabel();

    /** Current Neighborhood Size. */
    private JLabel lNeighborhoodSize = new JLabel();

    /** The model subnetwork. */
    private SOM som;

    /** If true this is a creation panel.  Otherwise it is an edit panel. */
    private boolean isCreationPanel;


    /**
     * Constructor for the case where an som is being created.
     *
     * @param np parent network panel
     */
    public SOMPropertiesPanel(final NetworkPanel np) {
        this.networkPanel = np;
        isCreationPanel = true;
        mainPanel.addItem("Number of neurons", tfNumNeurons);
        initPanel();
    }

    /**
     * Constructor for case where an existing som  is being
     * edited.
     *
     * @param np parent network panel
     * @param som network being modified.
     */
    public SOMPropertiesPanel(final NetworkPanel np, final SOM som) {
        this.networkPanel = np;
        this.som = som;
        isCreationPanel = false;
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
        mainPanel.addItem("Learning Rate", lLearningRate);
        mainPanel.addItem("Neighborhood Size", lNeighborhoodSize);
        add(mainPanel);
    }

    @Override
    public void fillFieldValues() {
        // For creation panels use an "empty" som network to harvest
        // default values
        if (isCreationPanel) {
            som = new SOM(null, 1);
            tfNumNeurons.setText("" + DEFAULT_NUM_NEURONS);
        }
        tfAlpha.setText(Double.toString(som.getInitAlpha()));
        tfInitNeighborhoodSize.setText(Double.toString(som
                .getInitNeighborhoodSize()));
        tfAlphaDecayRate.setText(Double.toString(som.getAlphaDecayRate()));
        tfNeigborhoodDecayAmount.setText(Double.toString(som
                .getNeighborhoodDecayAmount()));
        lLearningRate.setText(Utils.round(som.getAlpha(), 2));
        lNeighborhoodSize.setText(Utils.round(som.getNeighborhoodSize(), 2));
    }

    @Override
    public Group commitChanges() {
        if (isCreationPanel) {
            som = new SOM(networkPanel.getNetwork(),
                    Integer.parseInt(tfNumNeurons.getText()));
        }
        som.setInitAlpha(Double.parseDouble(tfAlpha.getText()));
        som.setInitNeighborhoodSize(Double.parseDouble(tfInitNeighborhoodSize
                .getText()));
        som.setAlphaDecayRate(Double.parseDouble(tfAlphaDecayRate.getText()));
        som.setNeighborhoodDecayAmount(Double
                .parseDouble(tfNeigborhoodDecayAmount.getText()));
        return som;
    }

    @Override
    public String getHelpPath() {
        return "Pages/Network/network/som.html";
    }

}
