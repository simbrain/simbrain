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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanelNetwork;
import org.simbrain.network.neuron_update_rules.AdditiveRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>AdditiveNeuronPanel</b>.
 */
public class AdditiveRulePanel extends AbstractNeuronPanel {

    /** Lambda field. */
    private JTextField tfLambda = new JTextField();

    /** Resistance field. */
    private JTextField tfResistance = new JTextField();

    /** Time step field. */
    private JTextField tfTimeStep = new JTextField();

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
     * @param net Network
     */
    public AdditiveRulePanel(Network network) {
        super();
        this.add(tabbedPane);
        mainTab.addItem("Time step", tfTimeStep);
        mainTab.addItem("Lambda", tfLambda);
        mainTab.addItem("Resistance", tfResistance);
        mainTab.addItem("Use clipping", isClipping);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     */
    @Override
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
        AdditiveRule neuronRef = (AdditiveRule) ruleList.get(0);

        tfLambda.setText(Double.toString(neuronRef.getLambda()));
        tfResistance.setText(Double.toString(neuronRef.getResistance()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());

        // Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(ruleList, AdditiveRule.class,
                "getLambda")) {
            tfLambda.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, AdditiveRule.class,
                "getResistance")) {
            tfResistance.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, AdditiveRule.class,
                "getClipping")) {
            isClipping.setNull();
        }

        if (!NetworkUtils.isConsistent(ruleList, AdditiveRule.class,
                "getAddNoise")) {
            isAddNoise.setNull();
        }

        randTab.fillFieldValues(getRandomizers(ruleList));
    }

    /**
     * @return List of radomizers.
     */
    private ArrayList<Randomizer> getRandomizers(List<NeuronUpdateRule> ruleList) {
        ArrayList<Randomizer> ret = new ArrayList<Randomizer>();

        for (int i = 0; i < ruleList.size(); i++) {
            ret.add(((AdditiveRule) ruleList.get(i)).getNoiseGenerator());
        }

        return ret;
    }

    /**
     * Fill field values to default values for additive neuron.
     */
    public void fillDefaultValues() {
        AdditiveRule neuronRef = new AdditiveRule();
        tfLambda.setText(Double.toString(neuronRef.getLambda()));
        tfResistance.setText(Double.toString(neuronRef.getResistance()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * {@inheritDoc}
     */
	@Override
	public void commitChanges(Neuron neuron) {
		
		AdditiveRule neuronRef = new AdditiveRule();
		
		//Lambda
		if (!tfLambda.getText().equals(NULL_STRING)) 
			neuronRef.setLambda(Double.parseDouble(tfLambda.getText()));		

		//Resistance
		if (!tfResistance.getText().equals(NULL_STRING)) 
			neuronRef.setResistance(Double.parseDouble(tfResistance
					.getText()));		

		//Noise On/Of
		if (!isAddNoise.isNull()) 
			neuronRef.setClipping(isClipping.isSelected());		

		//Noise
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
