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

import org.simbrain.network.NetworkUtils;
import org.simnet.neurons.RandomNeuron;


public class RandomNeuronPanel extends AbstractNeuronPanel {

	RandomPanel rp = new RandomPanel();
    
    public RandomNeuronPanel(){
    		this.add(rp);
    }
    
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		RandomNeuron neuron_ref = (RandomNeuron)neuron_list.get(0);
		
		rp.getCbDistribution().setSelectedIndex(neuron_ref.getDistributionIndex());
		rp.getTfMean().setText(Double.toString(neuron_ref.getMean()));
		rp.getTfLowBound().setText(Double.toString(neuron_ref.getLowerValue()));
		rp.getTfUpBound().setText(Double.toString(neuron_ref.getUpperValue()));
		rp.getTfStandardDeviation().setText(Double.toString(neuron_ref.getStandardDeviation()));
		rp.getIsUseBoundsBox().setEnabled(neuron_ref.isUseBounds());

		//Handle consistency of multiple selections
		if (!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "getDistributionIndex")) {
            rp.getCbDistribution().addItem(NULL_STRING);
            rp.getCbDistribution().setSelectedIndex(RandomNeuron.getFunctionList().length);
        }
		if(!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "getUpperValue")) {
			rp.getTfUpBound().setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "getLowerValue")) {
			rp.getTfLowBound().setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "getMean")) {
			rp.getTfMean().setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "getStandardDeviation")) {
			rp.getTfStandardDeviation().setText(NULL_STRING);
		}
		if(!NetworkUtils.isConsistent(neuron_list, RandomNeuron.class, "isUseBounds")) {
			rp.getIsUseBoundsBox().setSelected(false);
		}
		rp.init();
	}
	
   /**
	 * Called externally when the dialog is closed, to commit any changes made
	 */
	public void commitChanges() {

       for (int i = 0; i < neuron_list.size(); i++) {
           RandomNeuron neuron_ref = (RandomNeuron) neuron_list.get(i);

           if (rp.getCbDistribution().getSelectedItem().equals(NULL_STRING) == false) {
               neuron_ref.setDistributionIndex(rp.getCbDistribution().getSelectedIndex());
           }
           if (rp.getTfMean().getText().equals(NULL_STRING) == false) {
               neuron_ref.setMean(Double.parseDouble(rp.getTfMean()
                       .getText()));
           }
           if (rp.getTfUpBound().getText().equals(NULL_STRING) == false) {
               neuron_ref.setUpperValue(Double
                       .parseDouble(rp.getTfUpBound().getText()));
           }
           if (rp.getTfLowBound().getText().equals(NULL_STRING) == false) {
               neuron_ref.setLowerValue(Double.parseDouble(rp.getTfLowBound().getText()));
           }
           if (rp.getTfStandardDeviation().getText().equals(NULL_STRING) == false) {
               neuron_ref.setStandardDeviation(Double.parseDouble(rp.getTfStandardDeviation()
                       .getText()));
           }
           if (rp.getIsUseBoundsBox().getText().equals(NULL_STRING) == false) {
               neuron_ref.setUseBounds(rp.getIsUseBoundsBox().isSelected());
           }
       }
   }
	

}
