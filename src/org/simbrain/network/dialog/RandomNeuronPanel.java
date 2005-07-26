/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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
import javax.swing.JCheckBox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.simbrain.network.NetworkUtils;

import org.simnet.neurons.RandomNeuron;
import org.simnet.neurons.SigmoidalNeuron;


public class RandomNeuronPanel extends AbstractNeuronPanel implements ActionListener {

    private JComboBox cbDistribution = new JComboBox(RandomNeuron.getFunctionList());
    private JTextField tfUpBound = new JTextField();
    private JTextField tfLowBound = new JTextField();
    private JTextField tfMean = new JTextField();
    private JTextField tfStandardDeviation = new JTextField();
    private JCheckBox isUseBoundsBox = new JCheckBox();
    
    public RandomNeuronPanel(){
        cbDistribution.addActionListener(this);
        isUseBoundsBox.addActionListener(this);
        isUseBoundsBox.setActionCommand("useBounds");
        
        this.addItem("Distribution", cbDistribution);
        this.addItem("Upper bound", tfUpBound);
        this.addItem("Lower bound", tfLowBound);
        this.addItem("Mean value", tfMean);
        this.addItem("Standard deviation", tfStandardDeviation);
        this.addItem("Use bounds", isUseBoundsBox);
    }
    
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		RandomNeuron neuron_ref = (RandomNeuron)neuron_list.get(0);
		
		cbDistribution.setSelectedIndex(neuron_ref.getDistributionIndex());
		tfMean.setText(Double.toString(neuron_ref.getMean()));
		tfLowBound.setText(Double.toString(neuron_ref.getLowerValue()));
		tfUpBound.setText(Double.toString(neuron_ref.getUpperValue()));
		tfStandardDeviation.setText(Double.toString(neuron_ref.getStandardDeviation()));
		isUseBoundsBox.setEnabled(neuron_ref.isUseBounds());

		//Handle consistency of multiple selections
		if (!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "getDistributionIndex")) {
            cbDistribution.addItem(NULL_STRING);
            cbDistribution.setSelectedIndex(RandomNeuron.getFunctionList().length);
        }
		if(!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "getUpperValue")) {
			tfUpBound.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "getLowerValue")) {
			tfLowBound.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "getMean")) {
			tfMean.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "getStandardDeviation")) {
			tfStandardDeviation.setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "isUseBounds")) {
			isUseBoundsBox.setSelected(false);
		}
		init();
	}
	
   /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {

       for (int i = 0; i < neuron_list.size(); i++) {
           RandomNeuron neuron_ref = (RandomNeuron) neuron_list.get(i);

           if (cbDistribution.getSelectedItem().equals(NULL_STRING) == false) {
               neuron_ref.setDistributionIndex(cbDistribution.getSelectedIndex());
           }
           if (tfMean.getText().equals(NULL_STRING) == false) {
               neuron_ref.setMean(Double.parseDouble(tfMean
                       .getText()));
           }
           if (tfUpBound.getText().equals(NULL_STRING) == false) {
               neuron_ref.setUpperValue(Double
                       .parseDouble(tfUpBound.getText()));
           }
           if (tfLowBound.getText().equals(NULL_STRING) == false) {
               neuron_ref.setLowerValue(Double.parseDouble(tfLowBound
                       .getText()));
           }
           if (tfStandardDeviation.getText().equals(NULL_STRING) == false) {
               neuron_ref.setStandardDeviation(Double.parseDouble(tfStandardDeviation
                       .getText()));
           }
           if (isUseBoundsBox.getText().equals(NULL_STRING) == false) {
               neuron_ref.setUseBounds(isUseBoundsBox.isSelected());
           }
       }
   }
	
    /**
     * Enable or disable the upper and lower bounds fields depending on state of rounding button
     *
     */
    private void checkBounds() {
        if (isUseBoundsBox.isSelected() == false) {
            tfLowBound.setEnabled(false);
            tfUpBound.setEnabled(false);
        } else {
            tfLowBound.setEnabled(true);
            tfUpBound.setEnabled(true);
        }
    }
    
    private void init(){
	    if(cbDistribution.getSelectedIndex() == 0){
	        tfUpBound.setEnabled(true);
	        tfLowBound.setEnabled(true);
	        tfMean.setEnabled(false);
	        tfStandardDeviation.setEnabled(false);
	        isUseBoundsBox.setSelected(true);
	        isUseBoundsBox.setEnabled(false);
	    } else if (cbDistribution.getSelectedIndex() == 1){
	        tfMean.setEnabled(true);
	        tfStandardDeviation.setEnabled(true);
	        isUseBoundsBox.setEnabled(true);
	        checkBounds();
	    } 
    }
	
	public void actionPerformed(ActionEvent e){

	    init();
	    if(e.getActionCommand().equals("useBounds")){
	        checkBounds();
	    }
	}
}
