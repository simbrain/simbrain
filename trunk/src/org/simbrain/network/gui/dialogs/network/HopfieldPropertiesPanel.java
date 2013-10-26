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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.groups.Group;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.group.GroupPropertiesPanel;
import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ShowHelpAction;

/**
 * <b>DiscreteHopfieldPropertiesDialog</b> is a dialog box for setting the
 * properties of a discrete Hopfield network. Can be used in creation or
 * editing.
 */
public class HopfieldPropertiesPanel extends JPanel implements
        GroupPropertiesPanel {

    /** Default number of neurons. */
    private static final int DEFAULT_NUM_NEURONS = 9;

    /** Parent Network Panel. */
    private NetworkPanel networkPanel;

    /** Number of neurons field. */
    private JTextField tfNumNeurons = new JTextField();

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Sequential network update order. */
    public static final int SEQUENTIAL = 0;

    /** Random network update order. */
    public static final int RANDOM = 1;

    /** Network type combo box. */
    private JComboBox cbUpdateOrder = new JComboBox(new String[] {
            "Sequential", "Random" });

    /** The model subnetwork. */
    private Hopfield hopfield;

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction;

    /** If true this is a creation panel.  Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * Constructor for creating Hopfield networks.
     *
     * @param np parent network panel
     */
    public HopfieldPropertiesPanel(final NetworkPanel np) {
        this.networkPanel = np;
        isCreationPanel = true;
        mainPanel.addItem("Number of neurons", tfNumNeurons);
        initPanel();
    }

    /**
     * Constructor for editing.
     *
     * @param np parent network panel
     * @param hop network being modified.
     */
    public HopfieldPropertiesPanel(final NetworkPanel np, final Hopfield hop) {
        this.networkPanel = np;
        this.hopfield = hop;
        isCreationPanel = false;
        initPanel();
    }

    /**
     * Initialize the panel.
     */
    private void initPanel() {
        mainPanel.addItem("Update order", cbUpdateOrder);
        fillFieldValues();
        add(mainPanel);
    }

    /**
     * @return the update order.
     */
    public int getUpdateType() {
        if (cbUpdateOrder.getSelectedIndex() == 0) {
            return SEQUENTIAL;
        } else {
            return RANDOM;
        }
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        if (isCreationPanel) {
            hopfield = new Hopfield(null, 1);
            tfNumNeurons.setText("" + DEFAULT_NUM_NEURONS);
        }
        cbUpdateOrder.setSelectedIndex(hopfield.getUpdateOrder());
    }

    @Override
    public Group commitChanges() {
        if (isCreationPanel) {
            hopfield = new Hopfield(networkPanel.getNetwork(),
                    Integer.parseInt(tfNumNeurons.getText()));
        }
        hopfield.setUpdateOrder(getUpdateType());
        return hopfield;
    }

    @Override
    public String getHelpPath() {
        return "Pages/Network/network/hopfieldnetwork.html";
    }

}
