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

import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.dialog.RandomPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.neurons.SigmoidalNeuron;


/**
 * <b>SigmoidalNeuronPanel</b>.
 */
public class SigmoidalNeuronPanel extends AbstractNeuronPanel {

    /** Implementation combo box. */
    private JComboBox cbImplementation = new JComboBox(SigmoidalNeuron.getFunctionList());

    /** Bias field. */
    private JTextField tfBias = new JTextField();

    /** Slope field. */
    private JTextField tfSlope = new JTextField();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private RandomPanel randTab = new RandomPanel(true);

    /** Clipping combo box. */
    private TristateDropDown isClipping = new TristateDropDown();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /**
     * Creates an instance of this panel.
     *
     */
    public SigmoidalNeuronPanel() {
        this.add(tabbedPane);
        mainTab.addItem("Implementation", cbImplementation);
        mainTab.addItem("Bias", tfBias);
        mainTab.addItem("Slope", tfSlope);
        mainTab.addItem("Use clipping", isClipping);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        SigmoidalNeuron neuronRef = (SigmoidalNeuron) neuronList.get(0);

        cbImplementation.setSelectedIndex(neuronRef.getImplementationIndex());
        tfBias.setText(Double.toString(neuronRef.getBias()));
        tfSlope.setText(Double.toString(neuronRef.getSlope()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, SigmoidalNeuron.class, "getImplementationIndex")) {
            if ((cbImplementation.getItemCount() == SigmoidalNeuron.getFunctionList().length)) {
                cbImplementation.addItem(NULL_STRING);
            }

            cbImplementation.setSelectedIndex(SigmoidalNeuron.getFunctionList().length);
        }

        if (!tfBias.getText().equals(NULL_STRING)) {
            neuronRef.setBias(Double.parseDouble(tfBias.getText()));
        }

        if (!NetworkUtils.isConsistent(neuronList, SigmoidalNeuron.class, "getSlope")) {
            tfSlope.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, SigmoidalNeuron.class, "getClipping")) {
            isClipping.setNull();
        }

        if (!NetworkUtils.isConsistent(neuronList, SigmoidalNeuron.class, "getAddNoise")) {
            isAddNoise.setNull();
        }

        randTab.fillFieldValues(getRandomizers());
    }

    /**
     * @return List of randomizers.
     */
    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < neuronList.size(); i++) {
            ret.add(((SigmoidalNeuron) neuronList.get(i)).getNoiseGenerator());
        }

        return ret;
    }

    /**
     * Fill field values to default values for sigmoidal neuron.
     */
    public void fillDefaultValues() {
        SigmoidalNeuron neuronRef = new SigmoidalNeuron();

        cbImplementation.setSelectedIndex(neuronRef.getImplementationIndex());
        tfBias.setText(Double.toString(neuronRef.getBias()));
        tfSlope.setText(Double.toString(neuronRef.getSlope()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < neuronList.size(); i++) {
            SigmoidalNeuron neuronRef = (SigmoidalNeuron) neuronList.get(i);

            if (!cbImplementation.getSelectedItem().equals(NULL_STRING)) {
                neuronRef.setImplementationIndex(cbImplementation.getSelectedIndex());
            }
            if (!tfBias.getText().equals(NULL_STRING)) {
                neuronRef.setBias(Double.parseDouble(tfBias.getText()));
            }

            if (!tfSlope.getText().equals(NULL_STRING)) {
                neuronRef.setSlope(Double.parseDouble(tfSlope.getText()));
            }

            if (!isClipping.isNull()) {
                neuronRef.setClipping(isClipping.isSelected());
            }

            if (!isAddNoise.isNull()) {
                neuronRef.setAddNoise(isAddNoise.isSelected());
            }

            randTab.commitRandom(neuronRef.getNoiseGenerator());
        }
    }
}
