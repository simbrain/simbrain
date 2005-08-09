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

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;

import java.awt.BorderLayout;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.network.NetworkUtils;
import org.simnet.neurons.LogisticNeuron;


public class LogisticNeuronPanel extends AbstractNeuronPanel {

    private LabelledItemPanel topPanel = new LabelledItemPanel();
    private JPanel mainPanel = new JPanel();
    private JPanel labelPanel = new JPanel();
    private JLabel adviceLabel = new JLabel("(for chaos growth rates between 3.3 and 4 are reccomended)");
    private JTextField tfUpperValue = new JTextField();
    private JTextField tfLowerValue = new JTextField();
    private JTextField tfGrowthRate = new JTextField();
    
    public LogisticNeuronPanel(){
        mainPanel.setLayout(new BorderLayout());
        
        topPanel.addItem("Upper value", tfUpperValue);
        topPanel.addItem("Lower value", tfLowerValue);
        topPanel.addItem("Growth rate", tfGrowthRate);
        labelPanel.add(adviceLabel);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(labelPanel, BorderLayout.SOUTH);
        
        this.add(mainPanel);
    }
    
    public void fillFieldValues(){
		LogisticNeuron neuron_ref = (LogisticNeuron)neuron_list.get(0);
		
		tfLowerValue.setText(Double.toString(neuron_ref.getLowerValue()));
		tfUpperValue.setText(Double.toString(neuron_ref.getUpperValue()));
		tfGrowthRate.setText(Double.toString(neuron_ref.getGrowthRate()));

		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(neuron_list, LogisticNeuron.class, "getLowerValue")) {
			tfLowerValue.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, LogisticNeuron.class, "getUpperValue")) {
			tfUpperValue.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, LogisticNeuron.class, "getGrowthRate")) {
			tfGrowthRate.setText(NULL_STRING);
		}
        
    }
    
    public void fillDefaultValues(){
        LogisticNeuron neuronRef = new LogisticNeuron();
		tfLowerValue.setText(Double.toString(neuronRef.getLowerValue()));
		tfUpperValue.setText(Double.toString(neuronRef.getUpperValue()));
		tfGrowthRate.setText(Double.toString(neuronRef.getGrowthRate()));
    }
    
    public void commitChanges(){
        
        for (int i = 0; i < neuron_list.size(); i++) {
            LogisticNeuron neuronRef = (LogisticNeuron) neuron_list.get(i);

            if (tfUpperValue.getText().equals(NULL_STRING) == false) {
                neuronRef.setUpperBound(Double
                        .parseDouble(tfUpperValue.getText()));
            }
            if (tfLowerValue.getText().equals(NULL_STRING) == false) {
                neuronRef.setLowerBound(Double.parseDouble(tfLowerValue
                        .getText()));
            }
            if (tfGrowthRate.getText().equals(NULL_STRING) == false) {
                neuronRef.setGrowthRate(Double.parseDouble(tfGrowthRate
                        .getText()));
            }
        }
    }
}

