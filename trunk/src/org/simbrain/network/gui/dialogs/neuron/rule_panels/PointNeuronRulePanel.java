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

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.neuron_update_rules.PointNeuronRule;
import org.simbrain.network.neuron_update_rules.PointNeuronRule.OutputFunction;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>PointNeuronPanel</b>
 * TODO: Check this class for consistency. Excitatory normalization was removed
 * because it was both unused and had no accessor methods in PointNeuronRule.
 */
public class PointNeuronRulePanel extends AbstractNeuronPanel {

    /** Excitatory Reversal field. */
    private JTextField tfER = new JTextField();

    /** Inhibitory Reversal field. */
    private JTextField tfIR = new JTextField();

    /** Leak Reversal field. */
    private JTextField tfLR = new JTextField();

    /** Leak Conductance field. */
    private JTextField tfLC = new JTextField();

    /** Output function. */
    private JComboBox<OutputFunction> cbOutputFunction =
    		new JComboBox<OutputFunction>();
    
    {
    	cbOutputFunction.addItem(OutputFunction.DISCRETE_SPIKING);
    	cbOutputFunction.addItem(OutputFunction.LINEAR);
    	cbOutputFunction.addItem(OutputFunction.NOISY_RATE_CODE);
    	cbOutputFunction.addItem(OutputFunction.NONE);
    	cbOutputFunction.addItem(OutputFunction.RATE_CODE);
    }

    /** Threshold for output function. */
    private JTextField tfThreshold = new JTextField();

    /** Gain for output function. */
    private JTextField tfGain = new JTextField();

    /** Bias for excitatory inputs. */
    private JTextField tfBias = new JTextField();

    
    /** 
     * Time averaging for excitatory inputs.
     * TODO: Rename to "Time Constant"? Inconsistency between name in the rule
     * panel and name in the rule model class.
     */
    private JTextField tfTimeAveraging = new JTextField();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Inputs tab. */
    private LabelledItemPanel inputsTab = new LabelledItemPanel();

    /** Output Function tab. */
    private LabelledItemPanel outputFunctionTab = new LabelledItemPanel();

    /**
     * Creates an instance of this panel.
     */
    public PointNeuronRulePanel() {
        super();
        this.add(tabbedPane);
        mainTab.addItem("Excitatory reversal", tfER);
        mainTab.addItem("Inhibitory reversal", tfIR);
        mainTab.addItem("Leak reversal", tfLR);
        mainTab.addItem("Leak conductance", tfLC);
        outputFunctionTab.addItem("Output function", cbOutputFunction);
        outputFunctionTab.addItem("Threshold", tfThreshold);
        outputFunctionTab.addItem("Gain", tfGain);
        inputsTab.addItem("Bias", tfBias);
        inputsTab.addItem("Time averaging time constant", tfTimeAveraging);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(inputsTab, "Inputs");
        tabbedPane.add(outputFunctionTab, "Output Function");
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
        
    	PointNeuronRule neuronRef = (PointNeuronRule) ruleList.get(0);

        //(Below) Handle consistency of multiple selections
        
        // Handle Excitatory Reversal
        if (!NetworkUtils.isConsistent(ruleList, PointNeuronRule.class,
                "getExcitatoryReversal"))
            tfER.setText(NULL_STRING);
        else
        	tfER.setText(Double.toString(neuronRef.getExcitatoryReversal()));
        
        // Handle Inhibitory Reversal
        if (!NetworkUtils.isConsistent(ruleList, PointNeuronRule.class,
                "getInhibitoryReversal"))
            tfIR.setText(NULL_STRING);
        else
        	tfIR.setText(Double.toString(neuronRef.getInhibitoryReversal()));

        // Handle Leak Reversal
        if (!NetworkUtils.isConsistent(ruleList, PointNeuronRule.class,
                "getLeakReversal"))
            tfLR.setText(NULL_STRING);
        else
        	tfLR.setText(Double.toString(neuronRef.getLeakReversal()));

        // Handle Leak Conductance
        if (!NetworkUtils.isConsistent(ruleList, PointNeuronRule.class,
                "getLeakConductance"))
            tfLC.setText(NULL_STRING);
        else
        	tfLC.setText(Double.toString(neuronRef.getLeakConductance()));
        
        //TODO: Was there a reason this wasn't handled previously?
        // Handle Output Function
        if(!NetworkUtils.isConsistent(ruleList, PointNeuronRule.class,
        		"getOutputFunction")) {
        	cbOutputFunction.addItem(OutputFunction.NULL_STRING);
        	cbOutputFunction.setSelectedIndex(cbOutputFunction.getItemCount());
        } else 
        	cbOutputFunction.setSelectedItem(neuronRef.getOutputFunction());
        
        // Handles Threshold Potential
        if (!NetworkUtils.isConsistent(ruleList, PointNeuronRule.class,
                "getThresholdPotential"))
            tfThreshold.setText(NULL_STRING);
        else
        	tfThreshold.setText(Double.toString(neuronRef.getThresholdPotential()));
        
        // Handle Gain
        if (!NetworkUtils.isConsistent(ruleList, PointNeuronRule.class,
                "getGain"))
            tfGain.setText(NULL_STRING);
        else
        	tfGain.setText(Double.toString(neuronRef.getGain()));
        
        // Handle Bias
        if (!NetworkUtils.isConsistent(ruleList, PointNeuronRule.class,
                "getBias"))
            tfBias.setText(NULL_STRING);
        else
        	tfBias.setText(Double.toString(neuronRef.getBias()));
        
        //TODO: Was there a reason this wasn't handled previously?
        // Handle Time Averaging
        if(!NetworkUtils.isConsistent(ruleList, PointNeuronRule.class,
        		"getNetTimeConstant"))
        	tfTimeAveraging.setText(NULL_STRING);
        else
        	tfTimeAveraging
            .setText(Double.toString(neuronRef.getNetTimeConstant()));
        
    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        PointNeuronRule neuronRef = new PointNeuronRule();
        tfER.setText(Double.toString(neuronRef.getExcitatoryReversal()));
        tfIR.setText(Double.toString(neuronRef.getInhibitoryReversal()));
        tfLR.setText(Double.toString(neuronRef.getLeakReversal()));
        tfLC.setText(Double.toString(neuronRef.getLeakConductance()));
        // cbOutputFunction.setSelectedIndex(neuronRef.getOutputFunction());
        tfThreshold.setText(Double.toString(neuronRef.getThresholdPotential()));
        tfGain.setText(Double.toString(neuronRef.getGain()));
        tfBias.setText(Double.toString(neuronRef.getBias()));
        tfTimeAveraging
                .setText(Double.toString(neuronRef.getNetTimeConstant()));
        // tfNormFactor.setText(Double.toString(neuronRef.getNormFactor()));
    }

    /**
     * {@inheritDoc}
     */
	@Override
	public void commitChanges(Neuron neuron) {
		
		PointNeuronRule neuronRef = new PointNeuronRule();
	
		// Excitatory Reversal	
        if (!tfER.getText().equals(NULL_STRING))
            neuronRef.setExcitatoryReversal(Double.parseDouble(tfER
                    .getText()));
        
        // Inhibitory Reversal
        if (!tfIR.getText().equals(NULL_STRING))
            neuronRef.setInhibitoryReversal(Double.parseDouble(tfIR
                    .getText()));
        
        // Leak Reversal
        if (!tfLR.getText().equals(NULL_STRING))
            neuronRef.setLeakReversal(Double.parseDouble(tfLR.getText()));
        
        // Leak Conductance
        if (!tfLC.getText().equals(NULL_STRING))
            neuronRef
                    .setLeakConductance(Double.parseDouble(tfLC.getText()));
        
        // Output Function
        if (!cbOutputFunction.getSelectedItem().toString().equals(NULL_STRING))
        	neuronRef.setOutputFunction((OutputFunction) cbOutputFunction
                .getSelectedItem());
        
        // Threshold Potential
        if (!tfThreshold.getText().equals(NULL_STRING))
            neuronRef.setThresholdPotential(Double.parseDouble(tfThreshold
                    .getText()));
        
        // Gain
        if (!tfGain.getText().equals(NULL_STRING))
            neuronRef.setGain(Double.parseDouble(tfGain.getText()));
        
        // Bias
        if (!tfBias.getText().equals(NULL_STRING))
            neuronRef.setBias(Double.parseDouble(tfBias.getText()));
        
        // Time Averaging
        if (!tfTimeAveraging.getText().equals(NULL_STRING))
            neuronRef.setNetTimeConstant((Double
                    .parseDouble(tfTimeAveraging.getText())));
        
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
