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
package org.simbrain.network.dialog;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.neurons.SigmoidalNeuron;

public class SigmoidalNeuronPanel extends AbstractNeuronPanel {

    private JComboBox cbImplementation = new JComboBox(SigmoidalNeuron.getFunctionList());
    private JTextField tfUpAsymptote = new JTextField();
    private JTextField tfLowAsymptote = new JTextField();
    private JTextField tfInflectionPoint = new JTextField();
    private JTextField tfInflectionPointSlope = new JTextField();
    private JTextField tfDecayRate = new JTextField();
    
    public SigmoidalNeuronPanel(){
        this.addItem("Implementation", cbImplementation);
        this.addItem("Upper asymptote", tfUpAsymptote);
        this.addItem("Lower asymptote", tfLowAsymptote);
        this.addItem("Inflection point", tfInflectionPoint);
        this.addItem("Inflection point slope", tfInflectionPointSlope);
        this.addItem("Decay rate", tfDecayRate);
    }
    
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		SigmoidalNeuron neuron_ref = (SigmoidalNeuron)neuron_list.get(0);
		
		cbImplementation.setSelectedIndex(neuron_ref.getImplementationIndex());
		tfLowAsymptote.setText(Double.toString(neuron_ref.getLowerAsymptote()));
		tfUpAsymptote.setText(Double.toString(neuron_ref.getUpperAsymptote()));
		tfInflectionPoint.setText(Double.toString(neuron_ref.getInflectionPoint()));
		tfInflectionPointSlope.setText(Double.toString(neuron_ref.getInflectionPointSlope()));
		tfDecayRate.setText(Double.toString(neuron_ref.getDecayRate()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, SigmoidalNeuron.class, "getImplementationIndex")) {
			cbImplementation.addItem(NULL_STRING);
			cbImplementation.setSelectedIndex(SigmoidalNeuron.getFunctionList().length);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, SigmoidalNeuron.class, "getLowerAsymptote")) {
			tfLowAsymptote.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, SigmoidalNeuron.class, "getUpperAsymptote")) {
			tfUpAsymptote.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, SigmoidalNeuron.class, "getInflectionPoint")) {
			tfInflectionPoint.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, SigmoidalNeuron.class, "getInflectionPointSlope")) {
			tfInflectionPointSlope.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, SigmoidalNeuron.class, "getDecayRate")) {
			tfDecayRate.setText(NULL_STRING);
		}	

	}
	

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {

        for (int i = 0; i < neuron_list.size(); i++) {
            SigmoidalNeuron neuron_ref = (SigmoidalNeuron) neuron_list.get(i);

            if (cbImplementation.getSelectedItem().equals(NULL_STRING) == false) {
                neuron_ref.setImplementationIndex(cbImplementation.getSelectedIndex());
            }
            if (tfUpAsymptote.getText().equals(NULL_STRING) == false) {
                neuron_ref.setUpperAsymptote(Double
                        .parseDouble(tfUpAsymptote.getText()));
            }
            if (tfLowAsymptote.getText().equals(NULL_STRING) == false) {
                neuron_ref.setLowerAsymptote(Double.parseDouble(tfLowAsymptote
                        .getText()));
            }
            if (tfInflectionPoint.getText().equals(NULL_STRING) == false) {
                neuron_ref.setInflectionPoint(Double.parseDouble(tfInflectionPoint
                        .getText()));
            }
            if (tfInflectionPointSlope.getText().equals(NULL_STRING) == false) {
                neuron_ref.setInflectionPointSlope(Double.parseDouble(tfInflectionPointSlope.getText()));
            }
            if (tfDecayRate.getText().equals(NULL_STRING) == false) {
                neuron_ref.setDecayRate(Double.parseDouble(tfDecayRate.getText()));
            }
        }
    }
}
