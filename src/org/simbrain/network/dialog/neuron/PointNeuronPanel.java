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
package org.simbrain.network.dialog.neuron;

import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.LabelledItemPanel;
import org.simnet.interfaces.Network;
import org.simnet.neurons.PointNeuron;


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
    private JComboBox cbOutputFunction = new JComboBox(PointNeuron.getFunctionList());
    
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
     * @param net Network
     */
    public PointNeuronPanel(final Network net) {
        this.parentNet = net;
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
        PointNeuron neuronRef = (PointNeuron) neuronList.get(0);

        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));

        tfER.setText(Double.toString(neuronRef.getExcitatoryReversal()));
        tfIR.setText(Double.toString(neuronRef.getInhibitoryReversal()));
        tfLR.setText(Double.toString(neuronRef.getLeakReversal()));
        tfLC.setText(Double.toString(neuronRef.getLeakConductance()));
        cbOutputFunction.setSelectedIndex(neuronRef.getOutputFunction());
        tfThreshold.setText(Double.toString(neuronRef.getThreshold()));
        tfGain.setText(Double.toString(neuronRef.getGain()));
        tfBias.setText(Double.toString(neuronRef.getBias()));
        tfTimeAveraging.setText(Double.toString(neuronRef.getTime_averaging()));
        tfNormFactor.setText(Double.toString(neuronRef.getNorm_factor()));
        
        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, PointNeuron.class, "getExcitatoryReversal")) {
            tfER.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, PointNeuron.class, "getInhibitoryReversal")) {
            tfIR.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, PointNeuron.class, "getLeakReversal")) {
            tfLR.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, PointNeuron.class, "getLeakConductance")) {
            tfLC.setText(NULL_STRING);
        }
        if (!cbOutputFunction.getSelectedItem().equals(NULL_STRING)) {
            neuronRef.setOutputFunction(cbOutputFunction.getSelectedIndex());
        }
        if (!NetworkUtils.isConsistent(neuronList, PointNeuron.class, "getThreshold")) {
            tfThreshold.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuronList, PointNeuron.class, "getGain")) {
            tfGain.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuronList, PointNeuron.class, "getBias")) {
            tfBias.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuronList, PointNeuron.class, "getTime_averaging")) {
            tfTimeAveraging.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(neuronList, PointNeuron.class, "getNorm_factor")) {
            tfNormFactor.setText(NULL_STRING);
        }
    }


    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        PointNeuron neuronRef = new PointNeuron();
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tfER.setText(Double.toString(neuronRef.getExcitatoryReversal()));
        tfIR.setText(Double.toString(neuronRef.getInhibitoryReversal()));
        tfLR.setText(Double.toString(neuronRef.getLeakReversal()));
        tfLC.setText(Double.toString(neuronRef.getLeakConductance()));
        cbOutputFunction.setSelectedIndex(neuronRef.getOutputFunction());
        tfThreshold.setText(Double.toString(neuronRef.getThreshold()));
        tfGain.setText(Double.toString(neuronRef.getGain()));
        tfBias.setText(Double.toString(neuronRef.getBias()));
        tfTimeAveraging.setText(Double.toString(neuronRef.getTime_averaging()));
        tfNormFactor.setText(Double.toString(neuronRef.getNorm_factor()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        parentNet.setTimeStep(Double.parseDouble(tfTimeStep.getText()));

        for (int i = 0; i < neuronList.size(); i++) {
            PointNeuron neuronRef = (PointNeuron) neuronList.get(i);

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
            if (!cbOutputFunction.getSelectedItem().toString().equals(NULL_STRING)) {
                neuronRef.setOutputFunction(cbOutputFunction.getSelectedIndex());
            }
            if (!tfThreshold.getText().equals(NULL_STRING)) {
                neuronRef.setThreshold(Double.parseDouble(tfThreshold.getText()));
            }
            if (!tfGain.getText().equals(NULL_STRING)) {
                neuronRef.setGain(Double.parseDouble(tfGain.getText()));
            }
            if (!tfBias.getText().equals(NULL_STRING)) {
                neuronRef.setGain(Double.parseDouble(tfBias.getText()));
            }
            if (!tfTimeAveraging.getText().equals(NULL_STRING)) {
                neuronRef.setGain(Double.parseDouble(tfTimeAveraging.getText()));
            }
            if (!tfNormFactor.getText().equals(NULL_STRING)) {
                neuronRef.setGain(Double.parseDouble(tfNormFactor.getText()));
            }

        }
    }
}
