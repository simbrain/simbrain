/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.dialog.network;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;


/**
 * <b>LayeredNetworkPanel</b>
 */
public class LayeredNetworkPanel extends AbstractNetworkPanel {
    private JTextField layersNumber = new JTextField();
    private JTextField neuronsPerLayer = new JTextField();
    private String[] orientList = {"Left-to-Right", "Right-to-Left", "Top-to-Bottom", "Bottom-to-Top" };
    private JComboBox orientation = new JComboBox(orientList);
    private JTextField positiveWeightFactor = new JTextField();
    private JTextField negativeWeightFactor = new JTextField();
    private JCheckBox selfConnections = new JCheckBox();

    public LayeredNetworkPanel() {
        this.addItem("Number of layers", layersNumber);
        this.addItem("Number of neurons per layer", neuronsPerLayer);
        this.addItem("Orientation", orientation);
        this.addItem("Positive weight factor", positiveWeightFactor);
        this.addItem("Negative weight factor", negativeWeightFactor);
        this.addItem("Allow self connections", selfConnections);
    }

    /**
     * Populate fields with current data
     */
    public void fillFieldValues() {
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {
    }
}
