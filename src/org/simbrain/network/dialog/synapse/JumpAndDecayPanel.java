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

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.synapses.spikeresponders.JumpAndDecay;

/**
 * 
 * <b>JumpAndDecayPanel</b>
 */
public class JumpAndDecayPanel extends AbstractSpikeResponsePanel {
    
    private JTextField tfJumpHeight = new JTextField();
    private JTextField tfBaseLine = new JTextField();
    private JTextField tfDecayRate = new JTextField();
    
    public JumpAndDecayPanel(){
        tfJumpHeight.setColumns(6);
        this.addItem("Jump height", tfJumpHeight);
        this.addItem("Base-line", tfBaseLine);
        this.addItem("Decay rate", tfDecayRate);
    }

    public void fillFieldValues() {
        JumpAndDecay spikeResponder = (JumpAndDecay)spikeResponderList.get(0);
        
        tfJumpHeight.setText(Double.toString(spikeResponder.getJumpHeight()));
        tfBaseLine.setText(Double.toString(spikeResponder.getBaseLine()));
        tfDecayRate.setText(Double.toString(spikeResponder.getDecayRate()));
        
        //Handle consistency of multiply selections
        if(!NetworkUtils.isConsistent(spikeResponderList, JumpAndDecay.class, "getJumpHeight")) {
            tfJumpHeight.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(spikeResponderList, JumpAndDecay.class, "getBaseLine")) {
            tfBaseLine.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(spikeResponderList, JumpAndDecay.class, "getDecayRate")) {
            tfDecayRate.setText(NULL_STRING);
        }

    }

    public void fillDefaultValues() {
        JumpAndDecay spiker_ref = new JumpAndDecay();
        tfJumpHeight.setText(Double.toString(spiker_ref.getJumpHeight()));
        tfBaseLine.setText(Double.toString(spiker_ref.getBaseLine()));
        tfDecayRate.setText(Double.toString(spiker_ref.getDecayRate()));
    }

    public void commitChanges() {

        for (int i = 0; i < spikeResponderList.size(); i++) {
            JumpAndDecay spiker_ref = (JumpAndDecay) spikeResponderList.get(i);
            if (tfJumpHeight.getText().equals(NULL_STRING) == false) {
                spiker_ref.setJumpHeight(Double.parseDouble(tfJumpHeight
                        .getText()));
            }
            if (tfBaseLine.getText().equals(NULL_STRING) == false) {
                spiker_ref.setBaseLine(Double.parseDouble(tfBaseLine
                        .getText()));
            }
            if (tfDecayRate.getText().equals(NULL_STRING) == false) {
                spiker_ref.setDecayRate(Double.parseDouble(tfDecayRate
                        .getText()));
            }
        }
    }
}
