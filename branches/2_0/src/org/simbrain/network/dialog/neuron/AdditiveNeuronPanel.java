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
import org.simnet.neurons.AdditiveNeuron;


/**
 * <b>AdditiveNeuronPanel</b>.
 */
public class AdditiveNeuronPanel extends AbstractNeuronPanel {

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
    private RandomPanel randTab = new RandomPanel(true);

    /** Clipping combo box. */
    private TristateDropDown isClipping = new TristateDropDown();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /**
     * Creates an instance of this panel.
     * @param net Network
     */
    public AdditiveNeuronPanel(final Network net) {
        parentNet = net;

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
    public void fillFieldValues() {
        AdditiveNeuron neuronRef = (AdditiveNeuron) neuronList.get(0);

        tfLambda.setText(Double.toString(neuronRef.getLambda()));
        tfResistance.setText(Double.toString(neuronRef.getResistance()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, AdditiveNeuron.class, "getLambda")) {
            tfLambda.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, AdditiveNeuron.class, "getResistance")) {
            tfResistance.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, AdditiveNeuron.class, "getClipping")) {
            isClipping.setNull();
        }

        if (!NetworkUtils.isConsistent(neuronList, AdditiveNeuron.class, "getAddNoise")) {
            isAddNoise.setNull();
        }

        randTab.fillFieldValues(getRandomizers());
    }

    /**
     * @return List of radomizers.
     */
    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < neuronList.size(); i++) {
            ret.add(((AdditiveNeuron) neuronList.get(i)).getNoiseGenerator());
        }

        return ret;
    }

    /**
     * Fill field values to default values for additive neuron.
     */
    public void fillDefaultValues() {
        AdditiveNeuron neuronRef = new AdditiveNeuron();
        tfLambda.setText(Double.toString(neuronRef.getLambda()));
        tfResistance.setText(Double.toString(neuronRef.getResistance()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
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
            AdditiveNeuron neuronRef = (AdditiveNeuron) neuronList.get(i);

            if (!tfLambda.getText().equals(NULL_STRING)) {
                neuronRef.setLambda(Double.parseDouble(tfLambda.getText()));
            }

            if (!tfResistance.getText().equals(NULL_STRING)) {
                neuronRef.setResistance(Double.parseDouble(tfResistance.getText()));
            }

            if (!isAddNoise.isNull()) {
                neuronRef.setClipping(isClipping.isSelected());
            }

            if (!isAddNoise.isNull()) {
                neuronRef.setAddNoise(isAddNoise.isSelected());
            }

            randTab.commitRandom(neuronRef.getNoiseGenerator());
        }
    }
}
