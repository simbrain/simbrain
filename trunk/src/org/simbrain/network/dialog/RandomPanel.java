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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.LabelledItemPanel;
import org.simnet.neurons.LinearNeuron;
import org.simnet.neurons.RandomNeuron;
import org.simnet.util.RandomSource;

/**
 * @author jyoshimi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RandomPanel extends LabelledItemPanel implements ActionListener { 

	private JComboBox cbDistribution = new JComboBox(RandomSource.getFunctionList());
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	private JTextField tfMean = new JTextField();
	private JTextField tfStandardDeviation = new JTextField();
	private JCheckBox isUseBoundsBox = new JCheckBox();
	
	String NULL_STRING = AbstractNeuronPanel.NULL_STRING;


	public RandomPanel() {
        cbDistribution.addActionListener(this);
        isUseBoundsBox.addActionListener(this);
        isUseBoundsBox.setActionCommand("useBounds");
        
        this.addItem("Distribution", cbDistribution);
        this.addItem("Upper bound", tfUpBound);
        this.addItem("Lower bound", tfLowBound);
        this.addItem("Mean value", tfMean);
        this.addItem("Standard deviation", tfStandardDeviation);
        this.addItem("Use bounds", isUseBoundsBox);

        init();
	}
    
    public void init(){
	    if(cbDistribution.getSelectedIndex() == RandomSource.UNIFORM){
	        tfUpBound.setEnabled(true);
	        tfLowBound.setEnabled(true);
	        tfMean.setEnabled(false);
	        tfStandardDeviation.setEnabled(false);
	        isUseBoundsBox.setSelected(true);
	        isUseBoundsBox.setEnabled(false);
	    } else if (cbDistribution.getSelectedIndex() == RandomSource.GAUSSIAN){
	        tfMean.setEnabled(true);
	        tfStandardDeviation.setEnabled(true);	
	        isUseBoundsBox.setEnabled(true);
	        checkBounds();
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

	public void actionPerformed(ActionEvent e){

	    if(e.getActionCommand().equals("useBounds")){
	        checkBounds();
	    }
	    init();
	}
	
    public void fillFieldValues(ArrayList randomizers){
    	
    		RandomSource rand = (RandomSource)randomizers.get(0);
    		
    		cbDistribution.setSelectedIndex(rand.getDistributionIndex());
    		isUseBoundsBox.setSelected(rand.isUseBounds());
    		tfLowBound.setText(Double.toString(rand.getLowerBound()));
    		tfUpBound.setText(Double.toString(rand.getUpperBound()));
        tfStandardDeviation.setText(Double.toString(rand.getStandardDeviation()));
        tfMean.setText(Double.toString(rand.getMean()));
         
		//Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getDistributionIndex")) {
			cbDistribution.addItem(NULL_STRING);
			cbDistribution.setSelectedIndex(RandomSource.getFunctionList().length);
		}
		
		if(!NetworkUtils.isConsistent(randomizers, RandomSource.class, "isUseBounds")) {
			isUseBoundsBox.setSelected(false);		
		}	
		if(!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getLowerBound")) {
			tfLowBound.setText(NULL_STRING);		
		}	
		if(!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getUpperBound")) {
			tfUpBound.setText(NULL_STRING);		
		}	
		if(!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getStandardDeviation")) {
			tfStandardDeviation.setText(NULL_STRING);
		}	
		if(!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getMean")) {
			tfMean.setText(NULL_STRING);
		}	
    }
		
    public void fillDefaultValues() {
    		RandomSource rand = new RandomSource();
    		cbDistribution.setSelectedIndex(rand.getDistributionIndex());
    		isUseBoundsBox.setSelected(rand.isUseBounds());
    		tfLowBound.setText(Double.toString(rand.getLowerBound()));
    		tfUpBound.setText(Double.toString(rand.getUpperBound()));
        tfStandardDeviation.setText(Double.toString(rand.getStandardDeviation()));
        tfMean.setText(Double.toString(rand.getMean()));    		
    }
    
    public RandomSource getRandomSource() {
    		RandomSource rand = new RandomSource();
    		if (cbDistribution.getSelectedItem().equals(NULL_STRING) == false) {
        		rand.setDistributionIndex(cbDistribution.getSelectedIndex());
    		}
    		if (tfLowBound.getText().equals(NULL_STRING) == false) {
    			rand.setLowerBound(Double.parseDouble(tfLowBound.getText()));
    		}
    		if (tfUpBound.getText().equals(NULL_STRING) == false) {
    	   		rand.setUpperBound(Double.parseDouble(tfUpBound.getText()));
    	   	}
    		if (tfStandardDeviation.getText().equals(NULL_STRING) == false) {
        	    tfStandardDeviation.setText(Double.toString(rand.getStandardDeviation()));
    	   	}
    		if (tfMean.getText().equals(NULL_STRING) == false) {
    	        tfMean.setText(Double.toString(rand.getMean()));    		
    	   	}
        
    		// TODO: Not sure how to handle checkboxes which are inconsistent
    		rand.setUseBounds(isUseBoundsBox.isSelected());

    		return rand;
    		
    }
    
	/**
	 * @return Returns the cbDistribution.
	 */
	public JComboBox getCbDistribution() {
		return cbDistribution;
	}
	/**
	 * @param cbDistribution The cbDistribution to set.
	 */
	public void setCbDistribution(JComboBox cbDistribution) {
		this.cbDistribution = cbDistribution;
	}
	/**
	 * @return Returns the isUseBoundsBox.
	 */
	public JCheckBox getIsUseBoundsBox() {
		return isUseBoundsBox;
	}
	/**
	 * @param isUseBoundsBox The isUseBoundsBox to set.
	 */
	public void setIsUseBoundsBox(JCheckBox isUseBoundsBox) {
		this.isUseBoundsBox = isUseBoundsBox;
	}
	/**
	 * @return Returns the tfLowBound.
	 */
	public JTextField getTfLowBound() {
		return tfLowBound;
	}
	/**
	 * @param tfLowBound The tfLowBound to set.
	 */
	public void setTfLowBound(JTextField tfLowBound) {
		this.tfLowBound = tfLowBound;
	}
	/**
	 * @return Returns the tfMean.
	 */
	public JTextField getTfMean() {
		return tfMean;
	}
	/**
	 * @param tfMean The tfMean to set.
	 */
	public void setTfMean(JTextField tfMean) {
		this.tfMean = tfMean;
	}
	/**
	 * @return Returns the tfStandardDeviation.
	 */
	public JTextField getTfStandardDeviation() {
		return tfStandardDeviation;
	}
	/**
	 * @param tfStandardDeviation The tfStandardDeviation to set.
	 */
	public void setTfStandardDeviation(JTextField tfStandardDeviation) {
		this.tfStandardDeviation = tfStandardDeviation;
	}
	/**
	 * @return Returns the tfUpBound.
	 */
	public JTextField getTfUpBound() {
		return tfUpBound;
	}
	/**
	 * @param tfUpBound The tfUpBound to set.
	 */
	public void setTfUpBound(JTextField tfUpBound) {
		this.tfUpBound = tfUpBound;
	}
}