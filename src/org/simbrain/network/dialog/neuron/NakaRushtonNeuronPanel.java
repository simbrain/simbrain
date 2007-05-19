/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.dialog.RandomPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.interfaces.Network;
import org.simnet.neurons.NakaRushtonNeuron;


/**
 * <b>NakaRushtonNeuronPanel</b>.
 */
public class NakaRushtonNeuronPanel extends AbstractNeuronPanel implements ActionListener {

    /** Steepness field. */
    private JTextField tfSteepness = new JTextField();

    /** Semi saturation field. */
    private JTextField tfSemiSaturation = new JTextField();

    /** Time step field. */
    private JTextField tfTimeStep = new JTextField();

    /** Time constant field. */
    private JTextField tfTimeConstant = new JTextField();

    /** Noise combo box. */
    private TristateDropDown tsNoise = new TristateDropDown();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Use adaptation combo box. */
    private TristateDropDown tsUseAdaptation = new TristateDropDown();

    /** Adaptation time constant. */
    private JTextField tfAdaptationTime = new JTextField();

    /** Adaptation parameter. */
    private JTextField tfAdaptationParam = new JTextField();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private RandomPanel randTab = new RandomPanel(true);

    /**
     * Creates a new  Naka-Rushton neuron panel.
     * @param net Reference to network
     */
    public NakaRushtonNeuronPanel(final Network net) {
        parentNet = net;

        tsUseAdaptation.addActionListener(this);

        this.add(tabbedPane);
        mainTab.addItem("Time step", tfTimeStep);
        mainTab.addItem("Steepness", tfSteepness);
        mainTab.addItem("Semi-saturation constant", tfSemiSaturation);
        mainTab.addItem("Time constant", tfTimeConstant);
        mainTab.addItem("Add noise", tsNoise);
        mainTab.addItem("Use Adaptation", tsUseAdaptation);
        mainTab.addItem("Adaptation parameter", tfAdaptationParam);
        mainTab.addItem("Adaptation time constant", tfAdaptationTime);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Checks for using adaptation and enables or disables adaptation field accordingly.
     */
    private void checkUsingAdaptation() {
        if (tsUseAdaptation.isSelected()) {
            tfAdaptationTime.setEnabled(true);
            tfAdaptationParam.setEnabled(true);
        } else {
            tfAdaptationTime.setEnabled(false);
            tfAdaptationParam.setEnabled(false);
        }
    }
    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        NakaRushtonNeuron neuronRef = (NakaRushtonNeuron) neuronList.get(0);

        tfSemiSaturation.setText(Double.toString(neuronRef.getSemiSaturationConstant()));
        tfSteepness.setText(Double.toString(neuronRef.getSteepness()));
        tfTimeConstant.setText(Double.toString(neuronRef.getTimeConstant()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tsNoise.setSelected(neuronRef.getAddNoise());
        tsUseAdaptation.setSelected(neuronRef.getUseAdaptation());
        tfAdaptationTime.setText(Double.toString(neuronRef.getAdaptationTimeConstant()));
        tfAdaptationParam.setText(Double.toString(neuronRef.getAdaptationParameter()));
        checkUsingAdaptation();

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, NakaRushtonNeuron.class, "getTimeConstant")) {
            tfTimeConstant.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, NakaRushtonNeuron.class, "getSemiSaturationConstant")) {
            tfSemiSaturation.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, NakaRushtonNeuron.class, "getSteepness")) {
            tfSteepness.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, NakaRushtonNeuron.class, "getAddNoise")) {
            tsNoise.setNull();
        }

        if (!NetworkUtils.isConsistent(neuronList, NakaRushtonNeuron.class, "getUseAdaptation")) {
            tsUseAdaptation.setNull();
        }

        if (!NetworkUtils.isConsistent(neuronList, NakaRushtonNeuron.class, "getAdaptationTimeConstant")) {
            tfAdaptationTime.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, NakaRushtonNeuron.class, "getAdaptationParameter")) {
            tfAdaptationParam.setText(NULL_STRING);
        }
        randTab.fillFieldValues(getRandomizers());
    }

    /**
     * @return an arraylist of randomizers.
     */
    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < neuronList.size(); i++) {
            ret.add(((NakaRushtonNeuron) neuronList.get(i)).getNoiseGenerator());
        }

        return ret;
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        NakaRushtonNeuron neuronRef = new NakaRushtonNeuron();
        checkUsingAdaptation();
        tfSemiSaturation.setText(Double.toString(neuronRef.getSemiSaturationConstant()));
        tfSteepness.setText(Double.toString(neuronRef.getSteepness()));
        tfTimeConstant.setText(Double.toString(neuronRef.getTimeConstant()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tsNoise.setSelected(neuronRef.getAddNoise());
        tsUseAdaptation.setSelected(neuronRef.getUseAdaptation());
        tfAdaptationTime.setText(Double.toString(neuronRef.getAdaptationTimeConstant()));
        tfAdaptationParam.setText(Double.toString(neuronRef.getAdaptationParameter()));
        randTab.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        parentNet.setTimeStep(Double.parseDouble(tfTimeStep.getText()));

        for (int i = 0; i < neuronList.size(); i++) {
            NakaRushtonNeuron neuronRef = (NakaRushtonNeuron) neuronList.get(i);

            if (!tfTimeConstant.getText().equals(NULL_STRING)) {
                neuronRef.setTimeConstant(Double.parseDouble(tfTimeConstant.getText()));
            }

            if (!tfSemiSaturation.getText().equals(NULL_STRING)) {
                neuronRef.setSemiSaturationConstant(Double.parseDouble(tfSemiSaturation.getText()));
            }

            if (!tfSteepness.getText().equals(NULL_STRING)) {
                neuronRef.setSteepness(Double.parseDouble(tfSteepness.getText()));
            }

            if (!tsNoise.isNull()) {
                neuronRef.setAddNoise(tsNoise.isSelected());
            }

            if (!tsUseAdaptation.isNull()) {
                neuronRef.setUseAdaptation(tsUseAdaptation.isSelected());
            }

            if (!tfAdaptationTime.getText().equals(NULL_STRING)) {
                neuronRef.setAdaptationTimeConstant(Double.parseDouble(tfAdaptationTime.getText()));
            }

            if (!tfAdaptationParam.getText().equals(NULL_STRING)) {
                neuronRef.setAdaptationParameter(Double.parseDouble(tfAdaptationParam.getText()));
            }

            randTab.commitRandom(neuronRef.getNoiseGenerator());
        }
    }

    /**
     * Responds to actions performed.
     *
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == tsUseAdaptation) {
            checkUsingAdaptation();
        }
    }
}
