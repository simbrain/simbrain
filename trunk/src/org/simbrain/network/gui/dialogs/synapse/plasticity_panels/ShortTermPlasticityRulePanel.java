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
package org.simbrain.network.gui.dialogs.synapse.plasticity_panels;

import java.util.List;

import javax.swing.JTextField;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.synapse_update_rules.ShortTermPlasticityRule;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * <b>ShortTermPlasticitySynapsePanel</b>.
 */
public class ShortTermPlasticityRulePanel extends AbstractSynapsePanel {

    /** Baseline strength field. */
    private final JTextField tfBaseLineStrength = new JTextField();

    /** Firing threshold field. */
    private final JTextField tfFiringThreshold = new JTextField();

    /** Bump rate field. */
    private final JTextField tfBumpRate = new JTextField();

    /** Decay rate field. */
    private final JTextField tfDecayRate = new JTextField();

    /** Plasticity type combo box. */
    private final TristateDropDown cbPlasticityType = new TristateDropDown(
            "Depression", "Facilitation");

    /** Synapse reference. */
    private ShortTermPlasticityRule synapseRef;

    /**
     * Creates a short term plasticity synapse panel.
     */
    public ShortTermPlasticityRulePanel() {
        this.addItem("Plasticity type", cbPlasticityType);
        this.addItem("Base-line-strength", tfBaseLineStrength);
        this.addItem("Firing threshold", tfFiringThreshold);
        this.addItem("Growth-rate", tfBumpRate);
        this.addItem("Decay-rate", tfDecayRate);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues(List<SynapseUpdateRule> ruleList) {
    	
    	synapseRef = (ShortTermPlasticityRule) ruleList.get(0);    

        //(Below) Handle consistency of multiply selections
        
        // Handle Plasticity Type
        if (!NetworkUtils.isConsistent(ruleList, ShortTermPlasticityRule.class,
                "getPlasticityType")) 
            cbPlasticityType.setNull();
        else
        	cbPlasticityType.setSelectedIndex(synapseRef.getPlasticityType());

        // Handle Base Line Strength
        if (!NetworkUtils.isConsistent(ruleList, ShortTermPlasticityRule.class,
                "getBaseLineStrength")) 
            tfBaseLineStrength.setText(NULL_STRING);
        else
            tfBaseLineStrength.setText(Double.toString(synapseRef
                    .getBaseLineStrength()));

        // Handle Firing Threshold
        if (!NetworkUtils.isConsistent(ruleList, ShortTermPlasticityRule.class,
                "getFiringThreshold")) 
            tfFiringThreshold.setText(NULL_STRING);
        else
            tfFiringThreshold.setText(Double.toString(synapseRef
                    .getFiringThreshold()));

        // Handle Bump Rate
        if (!NetworkUtils.isConsistent(ruleList, ShortTermPlasticityRule.class,
                "getBumpRate")) 
            tfBumpRate.setText(NULL_STRING);
        else
        	tfBumpRate.setText(Double.toString(synapseRef.getBumpRate()));

        // Handle Decay Rate
        if (!NetworkUtils.isConsistent(ruleList, ShortTermPlasticityRule.class,
                "getDecayRate")) 
            tfDecayRate.setText(NULL_STRING);
        else
        	tfDecayRate.setText(Double.toString(synapseRef.getDecayRate()));
        
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
    	
        cbPlasticityType
                .setSelectedIndex(ShortTermPlasticityRule
                		.DEFAULT_PLASTICITY_TYPE);
        tfBaseLineStrength.setText(Double
                .toString(ShortTermPlasticityRule.DEFAULT_BASE_LINE_STRENGTH));
        tfFiringThreshold.setText(Double
                .toString(ShortTermPlasticityRule.DEFAULT_FIRING_THRESHOLD));
        tfBumpRate.setText(Double
                .toString(ShortTermPlasticityRule.DEFAULT_BUMP_RATE));
        tfDecayRate.setText(Double
                .toString(ShortTermPlasticityRule.DEFAULT_DECAY_RATE));
        
    }

    /**
     * {@inheritDoc}
     */
	@Override
	public void commitChanges(final List<Synapse> commitSynapses) {		
		for(Synapse s : commitSynapses) {
			commitChanges(s);
		}   		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void commitChanges(final Synapse templateSynapse) {
		
		synapseRef = new ShortTermPlasticityRule();
		
		  if (!cbPlasticityType.isNull()) {
            synapseRef.setPlasticityType(cbPlasticityType
                    .getSelectedIndex());
        }

        if (!tfBaseLineStrength.getText().equals(NULL_STRING)) {
            synapseRef.setBaseLineStrength(Double
                    .parseDouble(tfBaseLineStrength.getText()));
        }

        if (!tfFiringThreshold.getText().equals(NULL_STRING)) {
            synapseRef.setFiringThreshold(Double
                    .parseDouble(tfFiringThreshold.getText()));
        }

        if (!tfBumpRate.getText().equals(NULL_STRING)) {
            synapseRef
                    .setBumpRate(Double.parseDouble(tfBumpRate.getText()));
        }

        if (!tfDecayRate.getText().equals(NULL_STRING)) {
            synapseRef.setDecayRate(Double.parseDouble(tfDecayRate
                    .getText()));
        }
		
		templateSynapse.setLearningRule(synapseRef);
		
	}
	
}
