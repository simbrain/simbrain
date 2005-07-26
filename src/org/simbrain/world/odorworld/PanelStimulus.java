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

package org.simbrain.world.odorworld;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JButton;

import org.simbrain.util.LabelledItemPanel;

/**
 * <b>PanelStimulus</b> is a panel used to adjust the "smell signatures" 
 * (arrays of doubles representing the effect an object has on the input nodes
 * of the network of non-creature entities in the world. 
 * 
 */

public class PanelStimulus extends LabelledItemPanel implements ActionListener{
	private OdorWorldEntity entityRef = new OdorWorldEntity();
	
	private double[] val_array = null;
	private double randomUpper;
	private double randomLower;
	
	private JTabbedPane tabbedPane = new JTabbedPane();
	private LabelledItemPanel valuesPanel = new LabelledItemPanel();
	private LabelledItemPanel dispersionPanel = new LabelledItemPanel();
	
	private JTextField[] stimulusVals = null;
	private JTextField tfStimulusNum = new JTextField();
	private JButton stimulusButton = new JButton("Change");
	private JTextField tfRandomUpper = new JTextField();
	private JTextField tfRandomLower = new JTextField();
	private JLabel upperLabel = new JLabel("Upper: ");
	private JLabel lowerLabel = new JLabel("Lower: ");
	private JButton randomizeButton = new JButton("Randomize");
	private JPanel addStimulusPanel = new JPanel();
	private JPanel randomSubPanelUpper = new JPanel();
	private JPanel randomSubPanelLower = new JPanel();
	private JPanel randomMainPanel = new JPanel();
	private JPanel stimulusPanel = new JPanel();
	private JScrollPane stimScroller = new JScrollPane(stimulusPanel);
	private JTextField tfPeak = new JTextField();

	private JComboBox cbDecayFunction = new JComboBox(Stimulus.getDecayFunctions());
	private JTextField tfDispersion = new JTextField();
	private JSlider jsNoiseLevel = new JSlider(0,100,50);
	private JRadioButton rbAddNoise = new JRadioButton();
	
    /**
     * Create and populate the stimulus panel
     * 
     * @param we reference to the world entity whoes smell signature is being adjusted.
     */
    public PanelStimulus(OdorWorldEntity we) {
        entityRef = we;

		//Handle stimulus scroller
		val_array = entityRef.getStimulus().getStimulusVector();
		stimulusVals = new JTextField[val_array.length];
		stimulusPanel.setLayout(new GridLayout(val_array.length, 1));
		stimScroller.setPreferredSize(new Dimension(100,125));
		
		//Add Stimulus text field and button
        tfStimulusNum.setColumns(5);
        addStimulusPanel.add(tfStimulusNum);
        addStimulusPanel.add(stimulusButton);
        
        //Add randomize stimulus text field and button
        tfRandomUpper.setColumns(3);
        tfRandomLower.setColumns(3);
        
        randomSubPanelUpper.setLayout(new FlowLayout());
        randomSubPanelUpper.add(lowerLabel); 
        randomSubPanelUpper.add(tfRandomLower);
        randomSubPanelUpper.add(upperLabel);
        randomSubPanelUpper.add(tfRandomUpper);
        randomSubPanelLower.setLayout(new FlowLayout());
        randomSubPanelLower.add(randomizeButton);
        randomMainPanel.setLayout(new BorderLayout());
        randomMainPanel.add(randomSubPanelUpper, BorderLayout.NORTH);
        randomMainPanel.add(randomSubPanelLower, BorderLayout.SOUTH);

		//Turn on labels at major tick marks.
		jsNoiseLevel.setMajorTickSpacing(25);
		jsNoiseLevel.setPaintTicks(true);
		jsNoiseLevel.setPaintLabels(true); 
		
		rbAddNoise.addActionListener(this);
		stimulusButton.setActionCommand("addStimulus");
		stimulusButton.addActionListener(this);
		randomizeButton.setActionCommand("randomize");
		randomizeButton.addActionListener(this);
		
		fillFieldValues();

		this.add(tabbedPane);
		dispersionPanel.addItem("Decay function", cbDecayFunction);
		dispersionPanel.addItem("Dispersion", tfDispersion);
		dispersionPanel.addItem("Peak value", tfPeak);
		dispersionPanel.addItem("Add noise", rbAddNoise);
		dispersionPanel.addItem("Noise level", jsNoiseLevel);
		
		valuesPanel.addItem("Number of stimulus dimensions", addStimulusPanel);
		valuesPanel.addItem("Stimulus values", stimScroller);
		valuesPanel.addItem("Randomize stimulus", randomMainPanel);
		tabbedPane.addTab("Stimulus Values", valuesPanel);
		tabbedPane.addTab("Stimulus Dispersion", dispersionPanel);
	}
    
	/**
	* Populate fields with current data
	*/
	private void fillFieldValues() {
		

		cbDecayFunction.setSelectedIndex(entityRef.getStimulus().getDecayFunctionIndex(entityRef.getStimulus().getDecayFunction()));
		tfDispersion.setText(Double.toString(entityRef.getStimulus().getDispersion()));
		tfPeak.setText(Double.toString(entityRef.getStimulus().getPeak()));
		
		rbAddNoise.setSelected(entityRef.getStimulus().isAddNoise());
		if(entityRef.getStimulus().isAddNoise() == true) {
			jsNoiseLevel.setEnabled(true);
			jsNoiseLevel.setValue((int)(entityRef.getStimulus().getNoiseLevel() * 100));
		} else jsNoiseLevel.setEnabled(false);
		
		//Create stimulus panel
		for (int i = 0; i < val_array.length; i++) {
			stimulusVals[i] = new JTextField("" + val_array[i]);
			stimulusVals[i].setToolTipText("Index:" + (i + 1));
			stimulusPanel.add(stimulusVals[i]);
		}
		
		//Sets initial upper and lower randomizer bounds to current rounded max and min
		// 	values in the stimulus vector
		randomUpper = Double.parseDouble(stimulusVals[0].getText());
		randomLower = Double.parseDouble(stimulusVals[0].getText());
		for(int i = 0; i < val_array.length; i++){

			if((Double.parseDouble(stimulusVals[i].getText())) > randomUpper){
		        randomUpper = Double.parseDouble(stimulusVals[i].getText());
		    }
		    
		    if((Double.parseDouble(stimulusVals[i].getText()) < randomLower)){
		        randomLower = Double.parseDouble(stimulusVals[i].getText());
		    }
		}
		randomUpper = Math.rint(randomUpper);
		randomLower = Math.rint(randomLower);
				
		tfStimulusNum.setText(Integer.toString(val_array.length));
		tfRandomUpper.setText(Double.toString(randomUpper));
		tfRandomLower.setText(Double.toString(randomLower));

	}
	
	/**
	* Set values based on fields 
	*/
	public void getChanges() {

		// Below is needed to reset agent to its last orientation
		if (entityRef instanceof OdorWorldAgent) {
			((OdorWorldAgent)entityRef).setOrientation(((OdorWorldAgent)entityRef).getOrientation());
		}
		
		for (int i = 0; i < val_array.length; i++) {
			val_array[i] = Double.parseDouble(stimulusVals[i].getText());
		}		
	    entityRef.getStimulus().setStimulusVector(val_array);
		entityRef.getStimulus().setDispersion(Double.parseDouble(tfDispersion.getText()));
		entityRef.getStimulus().setDecayFunction(cbDecayFunction.getSelectedItem().toString());
		entityRef.getStimulus().setPeak(Double.parseDouble(tfPeak.getText()));
		
		entityRef.getStimulus().setAddNoise(rbAddNoise.isSelected());
		if(rbAddNoise.isSelected()) {
			entityRef.getStimulus().setNoiseLevel((double)jsNoiseLevel.getValue()/100);
		}
		
	}
	
	/**
	 * Removes text field array
	 *
	 */
	private void removeStimulusPanel(){
	    for (int i = 0; i < stimulusVals.length; i++){
	        stimulusPanel.remove(stimulusVals[i]);
	    }
	}
	
	/**
	 * Populates stimulus panel with new data
	 *
	 */
	private void refreshStimulusPanel(){
	    removeStimulusPanel();

		stimulusVals = new JTextField[val_array.length];
		stimulusPanel.setLayout(new GridLayout(val_array.length, 1));
		
		for (int i = 0; i < val_array.length; i++) {
			stimulusVals[i] = new JTextField("" + val_array[i]);
			stimulusVals[i].setToolTipText("Index:" + (i + 1));
			stimulusPanel.add(stimulusVals[i]);
		}
		
		stimulusPanel.updateUI();
		tfStimulusNum.setText(Integer.toString(val_array.length));
	}
	
	/**
	 * Changes size of array
	 * @param num New size of array
	 */
	private void changeStimulusDimension(int num){
	    double[] newStim = new double[num];
	    
	    for(int i = 0; i < num; i++){
	        if(i < val_array.length){
	            newStim[i] = val_array[i];
	        }else {
	            newStim[i] = 0;
	        }
	    }
	    val_array = newStim;
	}
	
	/**
	 * Randomizes numbers within text field array
	 *
	 */
	private void randomizeStimulus() {

	    if(randomLower >= randomUpper){
	        JOptionPane.showMessageDialog(null, "Upper and lower values out of bounds.",
	                "Warning", JOptionPane.ERROR_MESSAGE);
	        return;
	    }
	    removeStimulusPanel();
	    
		for (int i = 0; i < val_array.length; i++) {
			stimulusVals[i] = new JTextField("" + ((randomUpper - randomLower) * Math.random() + randomLower));
			stimulusVals[i].setToolTipText("Index:" + (i + 1));
			stimulusPanel.add(stimulusVals[i]);
		}
		stimulusPanel.updateUI();
	}
    
	/**
	 * Acton Listener
	 */
	public void actionPerformed(ActionEvent e) {
	    String cmd = e.getActionCommand();
	    
		if(rbAddNoise.isSelected()) {
			jsNoiseLevel.setEnabled(true);
		} else jsNoiseLevel.setEnabled(false);
		
		if(cmd.equals("addStimulus")){
		    changeStimulusDimension(Integer.parseInt(tfStimulusNum.getText()));
		    refreshStimulusPanel();
		} else if(cmd.equals("randomize")){
		    randomUpper = Double.parseDouble(tfRandomUpper.getText());
		    randomLower = Double.parseDouble(tfRandomLower.getText());
		    randomizeStimulus();
		}
	}
    /**
     * @return Returns the tabbedPane.
     */
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
    /**
     * @param tabbedPane The tabbedPane to set.
     */
    public void setTabbedPane(JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }
}
