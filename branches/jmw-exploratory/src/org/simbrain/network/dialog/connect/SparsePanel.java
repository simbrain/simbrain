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
package org.simbrain.network.dialog.connect;

import javax.swing.JTextField;

import org.simnet.NetworkPreferences;


/**
 * <b>SparsePanel</b> creates a dialog for setting preferences of Sparse neuron connections.
 */
public class SparsePanel extends AbstractConnectionPanel {

    /** Excitatory Probability. */
    private JTextField tfExcite = new JTextField();
    /** Inhibitory Probability. */
    private JTextField tfInhibit = new JTextField();

    /**
     * This method is the default constructor.
     */
    public SparsePanel() {
        this.addItem("Excitatory", tfExcite);
        this.addItem("Inhibitory", tfInhibit);
    }

    /**
     * Populate fields with current data.
     */
    public void commitChanges() {
        NetworkPreferences.setExcitatoryProbability(Double.parseDouble(tfExcite.getText()));
        NetworkPreferences.setInhibitoryProbability(Double.parseDouble(tfInhibit.getText()));
    }

    /**
     * Populates fields with default data.
     */
    public void fillDefaultValues() {
        tfExcite.setText(Double.toString(NetworkPreferences.getExcitatoryProbability()));
        tfInhibit.setText(Double.toString(NetworkPreferences.getInhibitoryProbability()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void fillFieldValues() {
        tfExcite.setText(Double.toString(NetworkPreferences.getExcitatoryProbability()));
        tfInhibit.setText(Double.toString(NetworkPreferences.getInhibitoryProbability()));
    }

}
