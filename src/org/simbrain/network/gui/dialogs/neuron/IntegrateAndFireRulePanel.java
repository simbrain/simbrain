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
package org.simbrain.network.gui.dialogs.neuron;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanelNetwork;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>IntegrateAndFireNeuronPanel</b>.
 */
public class IntegrateAndFireRulePanel extends AbstractNeuronPanel {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Time constant field. */
    private JTextField tfTimeConstant = new JTextField();

    /** Threshold field. */
    private JTextField tfThreshold = new JTextField();

    /** Reset field. */
    private JTextField tfReset = new JTextField();

    /** Resistance field. */
    private JTextField tfResistance = new JTextField();

    /** Resting potential field. */
    private JTextField tfRestingPotential = new JTextField();

    /** Random tab. */
    private RandomPanelNetwork randTab = new RandomPanelNetwork(true);

    /** Clipping combo box. */
    private TristateDropDown isClipping = new TristateDropDown();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /**
     * Creates a new instance of the integrate and fire neuron panel.
     */
    public IntegrateAndFireRulePanel() {
        super();

        this.add(tabbedPane);
        mainTab.addItem("Resistance", tfResistance);
        mainTab.addItem("Resting potential", tfRestingPotential);
        mainTab.addItem("Reset potential", tfReset);
        mainTab.addItem("Threshold", tfThreshold);
        mainTab.addItem("Time constant", tfTimeConstant);
        mainTab.addItem("Use clipping", isClipping);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

        IntegrateAndFireRule neuronRef = (IntegrateAndFireRule) ruleList.get(0);
        
        //(Below) Handle consistency of multiple selections
        
        // Handle Resting Potential
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
                "getRestingPotential")) 
            tfRestingPotential.setText(NULL_STRING);
        else
        	tfRestingPotential.setText(Double.toString(neuronRef
                    .getRestingPotential()));

        // Handle Resistance
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
                "getResistance"))
            tfResistance.setText(NULL_STRING);
        else
        	tfResistance.setText(Double.toString(neuronRef.getResistance()));

        // Handle Clipping
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
                "getClipping"))
            isClipping.setNull();
        else
        	isClipping.setSelected(neuronRef.getClipping());
        
        // Handle Add Noise
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
                "getAddNoise"))
            isAddNoise.setNull();
        else
        	isAddNoise.setSelected(neuronRef.getAddNoise());
        

        // Handle Reset Potential
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
                "getResetPotential"))
            tfReset.setText(NULL_STRING);
        else
        	tfReset.setText(Double.toString(neuronRef.getResetPotential()));
       

        // Handle Time Constant
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
                "getTimeConstant"))
            tfTimeConstant.setText(NULL_STRING);
        else
        	tfTimeConstant.setText(Double.toString(neuronRef.
        			getTimeConstant()));
        

        // Handle Threshold
        if (!NetworkUtils.isConsistent(ruleList, IntegrateAndFireRule.class,
                "getThreshold"))
            tfThreshold.setText(NULL_STRING);
        else
        	tfThreshold.setText(Double.toString(neuronRef.getThreshold()));
        

        randTab.fillFieldValues(getRandomizers(ruleList));
        
    }

    /**
     * @return List of randomizers.
     */
    private ArrayList<Randomizer> getRandomizers(
    		List<NeuronUpdateRule> ruleList) {
        ArrayList<Randomizer> ret = new ArrayList<Randomizer>();
        for (int i = 0; i < ruleList.size(); i++) {
            ret.add(((IntegrateAndFireRule) ruleList.get(i))
                    .getNoiseGenerator());
        }
        return ret;
    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        IntegrateAndFireRule neuronRef = new IntegrateAndFireRule();
        tfRestingPotential.setText(Double.toString(neuronRef
                .getRestingPotential()));
        tfResistance.setText(Double.toString(neuronRef.getResistance()));
        tfReset.setText(Double.toString(neuronRef.getResetPotential()));
        tfThreshold.setText(Double.toString(neuronRef.getThreshold()));
        tfTimeConstant.setText(Double.toString(neuronRef.getTimeConstant()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * {@inheritDoc}
     */
	@Override
	public void commitChanges(Neuron neuron) {
		
		IntegrateAndFireRule neuronRef = new IntegrateAndFireRule();	
		
		// Resistance
		if (!tfResistance.getText().equals(NULL_STRING))
            neuronRef.setResistance(Double.parseDouble(tfResistance
                    .getText()));
 
		// Resting Potential
        if (!tfRestingPotential.getText().equals(NULL_STRING))
            neuronRef.setRestingPotential(Double
                    .parseDouble(tfRestingPotential.getText()));
        
        // Clipping?
        if (!isClipping.isNull())
            neuronRef.setClipping(isClipping.isSelected());
      
        // Noise?
        if (!isAddNoise.isNull())
            neuronRef.setAddNoise(isAddNoise.isSelected());
       
        // Reset Potential
        if (!tfReset.getText().equals(NULL_STRING))
            neuronRef.setResetPotential(Double.parseDouble(tfReset
                    .getText()));
        
        // Threshold
        if (!tfThreshold.getText().equals(NULL_STRING))
            neuronRef
                    .setThreshold(Double.parseDouble(tfThreshold.getText()));
        
        // Time Constant
        if (!tfTimeConstant.getText().equals(NULL_STRING))
            neuronRef.setTimeConstant(Double.parseDouble(tfTimeConstant
                    .getText()));
        
        randTab.commitRandom(neuronRef.getNoiseGenerator());
        
        neuron.setUpdateRule(neuronRef);
		
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void commitChanges(List<Neuron> neurons) {	
        for(Neuron n : neurons) {
        	commitChanges(n);
        }
	}

}
