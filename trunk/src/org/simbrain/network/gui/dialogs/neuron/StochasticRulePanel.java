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

import java.util.List;

import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.neuron_update_rules.StochasticRule;

/**
 * <b>StochasticNeuronPanel</b>.
 */
public class StochasticRulePanel extends AbstractNeuronPanel {

    /** Firing probability field. */
    private JTextField tfFiringProbability = new JTextField();

    /**
     * Creates an instance of this panel.
     *
     */
    public StochasticRulePanel() {
        super();
        this.addItem("Firing probability", tfFiringProbability);
        this.addBottomText("<html>\"Firing probability\" is the probability of <p> the neuron's"
                + " state taking on the upper bound value.</html>");
    }

    /**
     * Populates the fields with current data.
     */
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
        
    	StochasticRule neuronRef = (StochasticRule) ruleList.get(0);

        //(Below) Handle consistency of multiple selections
        
        // Handle Firing Probability
        if (!NetworkUtils.isConsistent(ruleList, StochasticRule.class,
                "getFiringProbability"))
            tfFiringProbability.setText(NULL_STRING);
        else
        	tfFiringProbability.setText(Double.toString(neuronRef
                    .getFiringProbability()));
        
    }

    /**
     * Populates the fields with default data.
     */
    public void fillDefaultValues() {
        StochasticRule neuronRef = new StochasticRule();
        tfFiringProbability.setText(Double.toString(neuronRef
                .getFiringProbability()));
    }


    /**
     * {@inheritDoc}
     */
	@Override
	public void commitChanges(Neuron neuron) {
		
		StochasticRule neuronRef;
		
		if(neuron.getUpdateRule() instanceof StochasticRule) {
			neuronRef = (StochasticRule) neuron.getUpdateRule();
		} else {
			neuronRef = new StochasticRule();
			neuron.setUpdateRule(neuronRef);
		}
		
		// Firing Probability
        if (!tfFiringProbability.getText().equals(NULL_STRING))
            neuronRef.setFiringProbability(Double
                    .parseDouble(tfFiringProbability.getText()));
	
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void commitChanges(List<Neuron> neurons) {
		
		StochasticRule neuronRef = new StochasticRule();
		
		// Firing Probability
        if (!tfFiringProbability.getText().equals(NULL_STRING))
            neuronRef.setFiringProbability(Double
                    .parseDouble(tfFiringProbability.getText()));
        
        for(Neuron n : neurons) {
        	n.setUpdateRule(neuronRef);
        }
		
	}
}
