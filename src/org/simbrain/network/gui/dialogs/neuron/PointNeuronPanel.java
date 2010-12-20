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

import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.neurons.PointNeuron;
import org.simbrain.network.neurons.PointNeuron.OutputFunction;
import org.simbrain.util.LabelledItemPanel;


/**
 * <b>PointNeuronPanel</b>.
 */
public class PointNeuronPanel extends AbstractNeuronPanel {

    /** Excitatory Reversal field. */
    private JTextField tfER = new JTextField();

    /** Inhibitory Reversal field. */
    private JTextField tfIR = new JTextField();

    /**  Leak Reversal field. */
    private JTextField tfLR = new JTextField();

    /** Leak Conductance field. */
    private JTextField tfLC = new JTextField();

    /** Output function. */
    private JComboBox cbOutputFunction = new JComboBox(new OutputFunction[] {
            OutputFunction.DISCRETE_SPIKING, OutputFunction.LINEAR, OutputFunction.NOISY_RATE_CODE, 
            OutputFunction.NONE, OutputFunction.RATE_CODE, });

    /** Threshold for output function. */
    private JTextField tfThreshold = new JTextField();

    /** Gain for output function. */
    private JTextField tfGain = new JTextField();

    /** Bias for excitatory inputs.   */
    private JTextField tfBias = new JTextField();

    /** Time averaging for excitatory inputs.  */
    private JTextField tfTimeAveraging = new JTextField();

    /** A normalization factor for excitatory inputs. */
    private JTextField tfNormFactor = new JTextField();

    /** Time step field. */
    private JTextField tfTimeStep = new JTextField();

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
    public PointNeuronPanel(RootNetwork network) {
        super(network);
        this.add(tabbedPane);
        mainTab.addItem("Time step", tfTimeStep);
        mainTab.addItem("Excitatory reversal", tfER);
        mainTab.addItem("Inhibitory reversal", tfIR);
        mainTab.addItem("Leak reversal", tfLR);
        mainTab.addItem("Leak conductance", tfLC);
        outputFunctionTab.addItem("Output function", cbOutputFunction);
        outputFunctionTab.addItem("Threshold", tfThreshold);
        outputFunctionTab.addItem("Gain", tfGain);
        inputsTab.addItem("Bias", tfBias);
        inputsTab.addItem("Time averaging time constant", tfTimeAveraging);
        inputsTab.addItem("Excitatory normalization factor", tfNormFactor);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(inputsTab, "Inputs");
        tabbedPane.add(outputFunctionTab, "Output Function");
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        PointNeuron neuronRef = (PointNeuron) ruleList.get(0);

        tfTimeStep.setText(Double.toString(parentNet.getRootNetwork().getTimeStep()));

        tfER.setText(Double.toString(neuronRef.getExcitatoryReversal()));
        tfIR.setText(Double.toString(neuronRef.getInhibitoryReversal()));
        tfLR.setText(Double.toString(neuronRef.getLeakReversal()));
        tfLC.setText(Double.toString(neuronRef.getLeakConductance()));
        cbOutputFunction.setSelectedItem(neuronRef.getOutputFunction());
        tfThreshold.setText(Double.toString(neuronRef.getThresholdPotential()));
        tfGain.setText(Double.toString(neuronRef.getGain()));
        tfBias.setText(Double.toString(neuronRef.getBias()));
        tfTimeAveraging.setText(Double.toString(neuronRef.getNetTimeConstant()));
//        tfNormFactor.setText(Double.toString(neuronRef.getNormFactor()));
        
        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(ruleList, PointNeuron.class, "getExcitatoryReversal")) {
            tfER.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, PointNeuron.class, "getInhibitoryReversal")) {
            tfIR.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, PointNeuron.class, "getLeakReversal")) {
            tfLR.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, PointNeuron.class, "getLeakConductance")) {
            tfLC.setText(NULL_STRING);
        }
//        if (!cbOutputFunction.getSelectedItem().equals(NULL_STRING)) {
//            neuronRef.setOutputFunction(cbOutputFunction.getSelectedIndex());
//        }
        if (!NetworkUtils.isConsistent(ruleList, PointNeuron.class, "getThresholdPotential")) {
            tfThreshold.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, PointNeuron.class, "getGain")) {
            tfGain.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(ruleList, PointNeuron.class, "getBias")) {
            tfBias.setText(NULL_STRING);
        }
    }


    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        PointNeuron neuronRef = new PointNeuron();
        tfTimeStep.setText(Double.toString(parentNet.getRootNetwork().getTimeStep()));
        tfER.setText(Double.toString(neuronRef.getExcitatoryReversal()));
        tfIR.setText(Double.toString(neuronRef.getInhibitoryReversal()));
        tfLR.setText(Double.toString(neuronRef.getLeakReversal()));
        tfLC.setText(Double.toString(neuronRef.getLeakConductance()));
        //cbOutputFunction.setSelectedIndex(neuronRef.getOutputFunction());
        tfThreshold.setText(Double.toString(neuronRef.getThresholdPotential()));
        tfGain.setText(Double.toString(neuronRef.getGain()));
        tfBias.setText(Double.toString(neuronRef.getBias()));
        tfTimeAveraging.setText(Double.toString(neuronRef.getNetTimeConstant()));
//        tfNormFactor.setText(Double.toString(neuronRef.getNormFactor()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        parentNet.getRootNetwork().setTimeStep(Double.parseDouble(tfTimeStep.getText()));

        for (int i = 0; i < ruleList.size(); i++) {
            PointNeuron neuronRef = (PointNeuron) ruleList.get(i);

            if (!tfER.getText().equals(NULL_STRING)) {
                neuronRef.setExcitatoryReversal(Double.parseDouble(tfER.getText()));
            }
            if (!tfIR.getText().equals(NULL_STRING)) {
                neuronRef.setInhibitoryReversal(Double.parseDouble(tfIR.getText()));
            }
            if (!tfLR.getText().equals(NULL_STRING)) {
                neuronRef.setLeakReversal(Double.parseDouble(tfLR.getText()));
            }
            if (!tfLC.getText().equals(NULL_STRING)) {
                neuronRef.setLeakConductance(Double.parseDouble(tfLC.getText()));
            }
//            if (!cbOutputFunction.getSelectedItem().toString().equals(NULL_STRING)) {
                neuronRef.setOutputFunction((OutputFunction) cbOutputFunction.getSelectedItem());
//            }
            if (!tfThreshold.getText().equals(NULL_STRING)) {
                neuronRef.setThresholdPotential(Double.parseDouble(tfThreshold.getText()));
            }
            if (!tfGain.getText().equals(NULL_STRING)) {
                neuronRef.setGain(Double.parseDouble(tfGain.getText()));
            }
            if (!tfBias.getText().equals(NULL_STRING)) {
                neuronRef.setBias(Double.parseDouble(tfBias.getText()));
            }
            if (!tfTimeAveraging.getText().equals(NULL_STRING)) {
                neuronRef.setNetTimeConstant((Double.parseDouble(tfTimeAveraging.getText())));
            }
//            if (!tfNormFactor.getText().equals(NULL_STRING)) {
//                neuronRef.setNormFactor(Double.parseDouble(tfNormFactor.getText()));
//            }

        }
    }
}
