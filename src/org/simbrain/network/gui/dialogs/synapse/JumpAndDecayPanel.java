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
package org.simbrain.network.gui.dialogs.synapse;

import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.synapse_update_rules.spikeresponders.JumpAndDecay;


/**
 * <b>JumpAndDecayPanel</b>.
 */
public class JumpAndDecayPanel extends AbstractSpikeResponsePanel {

    /** Jump height field. */
    private JTextField tfJumpHeight = new JTextField();

    /** Base line field. */
    private JTextField tfBaseLine = new JTextField();

    /** Decay rate field. */
    private JTextField tfDecayRate = new JTextField();

    /**
     * This method is the default constructor.
     */
    public JumpAndDecayPanel() {
        tfJumpHeight.setColumns(6);
        this.addItem("Jump height", tfJumpHeight);
        this.addItem("Base-line", tfBaseLine);
        this.addItem("Decay rate", tfDecayRate);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        JumpAndDecay spikeResponder = (JumpAndDecay) spikeResponderList.get(0);

        tfJumpHeight.setText(Double.toString(spikeResponder.getJumpHeight()));
        tfBaseLine.setText(Double.toString(spikeResponder.getBaseLine()));
        tfDecayRate.setText(Double.toString(spikeResponder.getDecayRate()));

        //Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(spikeResponderList, JumpAndDecay.class, "getJumpHeight")) {
            tfJumpHeight.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(spikeResponderList, JumpAndDecay.class, "getBaseLine")) {
            tfBaseLine.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(spikeResponderList, JumpAndDecay.class, "getDecayRate")) {
            tfDecayRate.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        JumpAndDecay spikerRef = new JumpAndDecay();
        tfJumpHeight.setText(Double.toString(spikerRef.getJumpHeight()));
        tfBaseLine.setText(Double.toString(spikerRef.getBaseLine()));
        tfDecayRate.setText(Double.toString(spikerRef.getDecayRate()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < spikeResponderList.size(); i++) {
            JumpAndDecay spikerRef = (JumpAndDecay) spikeResponderList.get(i);

            if (!tfJumpHeight.getText().equals(NULL_STRING)) {
                spikerRef.setJumpHeight(Double.parseDouble(tfJumpHeight.getText()));
            }

            if (!tfBaseLine.getText().equals(NULL_STRING)) {
                spikerRef.setBaseLine(Double.parseDouble(tfBaseLine.getText()));
            }

            if (!tfDecayRate.getText().equals(NULL_STRING)) {
                spikerRef.setDecayRate(Double.parseDouble(tfDecayRate.getText()));
            }
        }
    }
}
