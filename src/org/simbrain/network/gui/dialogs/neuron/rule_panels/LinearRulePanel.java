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
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>LinearNeuronPanel</b>.
 */
public class LinearRulePanel extends AbstractNeuronPanel {

    /** Slope field. */
    private JTextField tfSlope = new JTextField();

    /** Bias field. */
    private JTextField tfBias = new JTextField();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private RandomPanelNetwork randTab = new RandomPanelNetwork(true);

    /** Clipping combo box. */
    private TristateDropDown isClipping = new TristateDropDown();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /**
     * Creates an instance of this panel.
     *
     */
    public LinearRulePanel() {
        super();
        this.add(tabbedPane);
        mainTab.addItem("Slope", tfSlope);
        mainTab.addItem("Bias", tfBias);
        mainTab.addItem("Use clipping", isClipping);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
        
    	LinearRule neuronRef = (LinearRule) ruleList.get(0);

        //(Below) Handle consistency of multiple selections
        
        // Handle Slope
        if (!NetworkUtils.isConsistent(ruleList, LinearRule.class, "getSlope")) 
            tfSlope.setText(NULL_STRING);
        else
        	tfSlope.setText(Double.toString(neuronRef.getSlope()));

        // Handle Bias
        if (!NetworkUtils.isConsistent(ruleList, LinearRule.class, "getBias"))
            tfBias.setText(NULL_STRING);
        else
        	tfBias.setText(Double.toString(neuronRef.getBias()));

        // Handle Clipping
        if (!NetworkUtils.isConsistent(ruleList, LinearRule.class,
                "getClipping")) 
            isClipping.setNull();
        else
        	isClipping.setSelected(neuronRef.getClipping());

        // Handle Noise
        if (!NetworkUtils.isConsistent(ruleList, LinearRule.class,
                "getAddNoise")) 
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
            ret.add(((LinearRule) ruleList.get(i)).getNoiseGenerator());
        }

        return ret;
    }

    /**
     * Fill field values to default values for linear neuron.
     */
    public void fillDefaultValues() {
        LinearRule neuronRef = new LinearRule();
        tfSlope.setText(Double.toString(neuronRef.getSlope()));
        tfBias.setText(Double.toString(neuronRef.getBias()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randTab.fillDefaultValues();
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(Neuron neuron) {
		
		LinearRule neuronRef = new LinearRule();
		
		// Slope
        if (!tfSlope.getText().equals(NULL_STRING))
            neuronRef.setSlope(Double.parseDouble(tfSlope.getText()));      

        // Bias
        if (!tfBias.getText().equals(NULL_STRING))
            neuronRef.setBias(Double.parseDouble(tfBias.getText()));
        
        // Clipping?
        if (!isClipping.isNull())
            neuronRef.setClipping(isClipping.isSelected());
        
        // Noise
        if (!isAddNoise.isNull()) {
            neuronRef.setAddNoise(isAddNoise.isSelected());
        }

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
