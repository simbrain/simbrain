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

import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.dialog.RandomPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.interfaces.Network;
import org.simnet.neurons.IntegrateAndFireNeuron;


/**
 * <b>IntegrateAndFireNeuronPanel</b>.
 */
public class IntegrateAndFireNeuronPanel extends AbstractNeuronPanel {
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
    /** Time step field. */
    private JTextField tfTimeStep = new JTextField();
    /** Random tab. */
    private RandomPanel randTab = new RandomPanel(true);
    /** Clipping combo box. */
    private TristateDropDown isClipping = new TristateDropDown();
    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /**
     * Creates a new instance of the integrate and fire neuron panel.
     * @param net Network
     */
    public IntegrateAndFireNeuronPanel(final Network net) {
        parentNet = net;

        this.add(tabbedPane);
        mainTab.addItem("Time step", tfTimeStep);
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
    public void fillFieldValues() {
        IntegrateAndFireNeuron neuronRef = (IntegrateAndFireNeuron) neuronList.get(0);

        tfRestingPotential.setText(Double.toString(neuronRef.getRestingPotential()));
        tfResistance.setText(Double.toString(neuronRef.getResistance()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tfReset.setText(Double.toString(neuronRef.getResetPotential()));
        tfThreshold.setText(Double.toString(neuronRef.getThreshold()));
        tfTimeConstant.setText(Double.toString(neuronRef.getTimeConstant()));
        isAddNoise.setSelected(neuronRef.getAddNoise());
        isClipping.setSelected(neuronRef.getClipping());

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, IntegrateAndFireNeuron.class, "getRestingPotential")) {
            tfRestingPotential.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, IntegrateAndFireNeuron.class, "getResistance")) {
            tfResistance.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, IntegrateAndFireNeuron.class, "getClipping")) {
            isClipping.setNull();
        }

        if (!NetworkUtils.isConsistent(neuronList, IntegrateAndFireNeuron.class, "getAddNoise")) {
            isAddNoise.setNull();
        }

        if (!NetworkUtils.isConsistent(neuronList, IntegrateAndFireNeuron.class, "getResetPotential")) {
            tfReset.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, IntegrateAndFireNeuron.class, "getTimeConstant")) {
            tfTimeConstant.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, IntegrateAndFireNeuron.class, "getThreshold")) {
            tfThreshold.setText(NULL_STRING);
        }

        randTab.fillFieldValues(getRandomizers());
    }

    /**
     * @return List of randomizers.
     */
    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < neuronList.size(); i++) {
            ret.add(((IntegrateAndFireNeuron) neuronList.get(i)).getNoiseGenerator());
        }

        return ret;
    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        IntegrateAndFireNeuron neuronRef = new IntegrateAndFireNeuron();
        tfRestingPotential.setText(Double.toString(neuronRef.getRestingPotential()));
        tfResistance.setText(Double.toString(neuronRef.getResistance()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tfReset.setText(Double.toString(neuronRef.getResetPotential()));
        tfThreshold.setText(Double.toString(neuronRef.getThreshold()));
        tfTimeConstant.setText(Double.toString(neuronRef.getTimeConstant()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        parentNet.setTimeStep(Double.parseDouble(tfTimeStep.getText()));

        for (int i = 0; i < neuronList.size(); i++) {
            IntegrateAndFireNeuron neuronRef = (IntegrateAndFireNeuron) neuronList.get(i);

            if (!tfResistance.getText().equals(NULL_STRING)) {
                neuronRef.setResistance(Double.parseDouble(tfResistance.getText()));
            }

            if (!tfRestingPotential.getText().equals(NULL_STRING)) {
                neuronRef.setRestingPotential(Double.parseDouble(tfRestingPotential.getText()));
            }

            if (!isClipping.isNull()) {
                neuronRef.setClipping(isClipping.isSelected());
            }

            if (!isAddNoise.isNull()) {
                neuronRef.setAddNoise(isAddNoise.isSelected());
            }

            if (!tfReset.getText().equals(NULL_STRING)) {
                neuronRef.setResetPotential(Double.parseDouble(tfReset.getText()));
            }

            if (!tfThreshold.getText().equals(NULL_STRING)) {
                neuronRef.setThreshold(Double.parseDouble(tfThreshold.getText()));
            }

            if (!tfTimeConstant.getText().equals(NULL_STRING)) {
                neuronRef.setTimeConstant(Double.parseDouble(tfTimeConstant.getText()));
            }

            randTab.commitRandom(neuronRef.getNoiseGenerator());
        }
    }
}
