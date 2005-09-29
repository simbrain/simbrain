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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.TristateDropDown;
import org.simnet.synapses.DeltaRuleSynapse;
import org.simnet.util.RandomSource;

public class DeltaRuleSynapsePanel extends AbstractSynapsePanel implements ActionListener {

    private DeltaRuleSynapse synapse_ref;
    private TristateDropDown tsInputOutput = new TristateDropDown();
    private JTextField tfMomentum = new JTextField();
    private JTextField tfDesiredOutput = new JTextField();
    
    public DeltaRuleSynapsePanel(){
        tsInputOutput.addActionListener(this);
        tsInputOutput.setActionCommand("useInput");
        
        this.addItem("Use input as desired value?", tsInputOutput);
        this.addItem("Desired output", tfDesiredOutput);
        this.addItem("Momentum", tfMomentum);
        checkInput();
    }
    
    public void fillFieldValues() {
        synapse_ref = (DeltaRuleSynapse)synapse_list.get(0);
        
        tfMomentum.setText(Double.toString(synapse_ref.getMomentum()));
        tfDesiredOutput.setText(Double.toString(synapse_ref.getDesiredOutput()));
        
        //Handle consistency of multiply selections
        if(!NetworkUtils.isConsistent(synapse_list, DeltaRuleSynapse.class, "getMomentum")) {
            tfMomentum.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(synapse_list, DeltaRuleSynapse.class, "getDesiredOutput")) {
            tfDesiredOutput.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(synapse_list, DeltaRuleSynapse.class, "getInputOutput")) {
            tsInputOutput.setNull();
        }

    }
    
    public void actionPerformed(ActionEvent e){

        if(e.getActionCommand().equals("useInput")){
            checkInput();
        }
    }
    
    private void checkInput() {
        if (tsInputOutput.getSelectedIndex() == TristateDropDown.FALSE) {
            tfDesiredOutput.setEnabled(true);
        } else {
            tfDesiredOutput.setEnabled(false);
        }
    }

    public void fillDefaultValues() {
        DeltaRuleSynapse synapse_ref = new DeltaRuleSynapse();
        tsInputOutput.setSelected(synapse_ref.getInputOutput());
        tfMomentum.setText(Double.toString(synapse_ref.getMomentum()));
        tfDesiredOutput.setText(Double.toString(synapse_ref.getDesiredOutput()));

    }

    public void commitChanges() {
        for (int i = 0; i < synapse_list.size(); i++) {
            DeltaRuleSynapse synapse_ref = (DeltaRuleSynapse) synapse_list.get(i);

            if (tfMomentum.getText().equals(NULL_STRING) == false) {
                synapse_ref.setMomentum(Double
                        .parseDouble(tfMomentum.getText()));
            }
            if (tfDesiredOutput.getText().equals(NULL_STRING) == false) {
                synapse_ref.setDesiredOutput(Double
                        .parseDouble(tfDesiredOutput.getText()));
            }
            if ((tsInputOutput.getSelectedIndex() == TristateDropDown.NULL) == false) {
                synapse_ref.setInputOutput(tsInputOutput.isSelected());
            }
        }

    }

}
