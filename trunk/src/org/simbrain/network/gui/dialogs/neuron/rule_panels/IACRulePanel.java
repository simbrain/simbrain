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
package org.simbrain.network.gui.dialogs.neuron.rule_panels;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanelNetwork;
import org.simbrain.network.neuron_update_rules.IACRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>IACNeuronPanel</b>.
 */
public class IACRulePanel extends AbstractNeuronPanel {

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Decay field. */
    private JTextField tfDecay = new JTextField();

    /** Rest field. */
    private JTextField tfRest = new JTextField();

    /** Random panel. */
    private RandomPanelNetwork randTab = new RandomPanelNetwork(true);

    /** Clipping combo box. */
    private TristateDropDown isClipping = new TristateDropDown();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /**
     * This method is the default constructor.
     *
     */
    public IACRulePanel() {
        super();
        this.add(tabbedPane);
        mainPanel.addItem("Decay", tfDecay);
        mainPanel.addItem("Rest", tfRest);
        mainPanel.addItem("Use clipping", isClipping);
        mainPanel.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainPanel, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     */

    
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
        
    	IACRule neuronRef = (IACRule) ruleList.get(0);

        //(Below) Handle consistency of multiple selections
        
        // Handle Decay
        if (!NetworkUtils.isConsistent(ruleList, IACRule.class, "getDecay")) 
            tfDecay.setText(NULL_STRING);
        else
        	tfDecay.setText(Double.toString(neuronRef.getDecay()));

        // Handle Rest
        if (!NetworkUtils.isConsistent(ruleList, IACRule.class, "getRest")) 
            tfRest.setText(NULL_STRING);
        else
        	tfRest.setText(Double.toString(neuronRef.getRest()));

        // Handle Clipping
        if (!NetworkUtils.isConsistent(ruleList, IACRule.class, "getClipping")) 
            isClipping.setNull();
        else
        	isClipping.setSelected(neuronRef.getClipping());

        // Handle Add Noise
        if (!NetworkUtils.isConsistent(ruleList, IACRule.class, "getAddNoise"))
            isAddNoise.setNull();
        else
        	isAddNoise.setSelected(neuronRef.getAddNoise());

        randTab.fillFieldValues(getRandomizers(ruleList));
        
    }

    /**
     * @return List of randomizers.
     */
    private ArrayList<Randomizer> getRandomizers(
    		List<NeuronUpdateRule> ruleList) {
        ArrayList<Randomizer> ret = new ArrayList<Randomizer>();
        for (int i = 0; i < ruleList.size(); i++) {
            ret.add(((IACRule) ruleList.get(i)).getNoiseGenerator());
        }
        return ret;
    }

    /**
     * Fill field values to default values for binary neuron.
     */
    public void fillDefaultValues() {
        IACRule neuronRef = new IACRule();
        tfDecay.setText(Double.toString(neuronRef.getDecay()));
        tfRest.setText(Double.toString(neuronRef.getRest()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * {@inheritDoc}
     */
	@Override
	public void commitChanges(Neuron neuron) {
		
		IACRule neuronRef = new IACRule();
					
		// Decay
		if (!tfDecay.getText().equals(NULL_STRING)) 
            neuronRef.setDecay(Double.parseDouble(tfDecay.getText()));
        
		// Rest
        if (!tfRest.getText().equals(NULL_STRING))
            neuronRef.setRest(Double.parseDouble(tfRest.getText()));
        
        // Clipping?
        if (!isClipping.isNull())
            neuronRef.setClipping(isClipping.isSelected());
        
        // Noise?
        if (!isAddNoise.isNull())
            neuronRef.setAddNoise(isAddNoise.isSelected());

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
