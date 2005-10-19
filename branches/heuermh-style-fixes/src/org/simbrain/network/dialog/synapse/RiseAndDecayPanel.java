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
package org.simbrain.network.dialog.synapse;

import org.simbrain.network.NetworkUtils;

import org.simnet.interfaces.Network;

import org.simnet.synapses.spikeresponders.JumpAndDecay;
import org.simnet.synapses.spikeresponders.RiseAndDecay;

import javax.swing.JTextField;


/**
 * <b>RiseAndDecayPanel</b>
 */
public class RiseAndDecayPanel extends AbstractSpikeResponsePanel {
    private JTextField tfMaximumResponse = new JTextField();
    private JTextField tfTimeStep = new JTextField();
    private JTextField tfDecayRate = new JTextField();

    public RiseAndDecayPanel(Network net) {
        parentNet = net;

        tfMaximumResponse.setColumns(6);
        this.addItem("Maximum response", tfMaximumResponse);
        this.addItem("Time step", tfTimeStep);
        this.addItem("Decay rate", tfDecayRate);
    }

    public void fillFieldValues() {
        RiseAndDecay spikeResponder = (RiseAndDecay) spikeResponderList.get(0);

        tfMaximumResponse.setText(Double.toString(spikeResponder.getMaximumResponse()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tfDecayRate.setText(Double.toString(spikeResponder.getDecayRate()));

        //Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(spikeResponderList, RiseAndDecay.class, "getMaximumResponse")) {
            tfMaximumResponse.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(spikeResponderList, RiseAndDecay.class, "getDecayRate")) {
            tfDecayRate.setText(NULL_STRING);
        }
    }

    public void fillDefaultValues() {
        RiseAndDecay spiker_ref = new RiseAndDecay();
        tfMaximumResponse.setText(Double.toString(spiker_ref.getMaximumResponse()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tfDecayRate.setText(Double.toString(spiker_ref.getDecayRate()));
    }

    public void commitChanges() {
        parentNet.setTimeStep(Double.parseDouble(tfTimeStep.getText()));

        for (int i = 0; i < spikeResponderList.size(); i++) {
            RiseAndDecay spiker_ref = (RiseAndDecay) spikeResponderList.get(i);

            if (tfMaximumResponse.getText().equals(NULL_STRING) == false) {
                spiker_ref.setMaximumResponse(Double.parseDouble(tfMaximumResponse.getText()));
            }

            if (tfDecayRate.getText().equals(NULL_STRING) == false) {
                spiker_ref.setDecayRate(Double.parseDouble(tfDecayRate.getText()));
            }
        }
    }
}
