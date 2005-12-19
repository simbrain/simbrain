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
import org.simnet.neurons.IACNeuron;


/**
 * <b>IACNeuronPanel</b>
 */
public class IACNeuronPanel extends AbstractNeuronPanel {
    private LabelledItemPanel mainPanel = new LabelledItemPanel();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private JTextField tfDecay = new JTextField();
    private JTextField tfRest = new JTextField();
    private RandomPanel rand_tab = new RandomPanel(true);
    private TristateDropDown isClipping = new TristateDropDown();
    private TristateDropDown isAddNoise = new TristateDropDown();

    public IACNeuronPanel() {
        this.add(tabbedPane);
        mainPanel.addItem("Decay", tfDecay);
        mainPanel.addItem("Rest", tfRest);
        mainPanel.addItem("Use clipping", isClipping);
        mainPanel.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainPanel, "Main");
        tabbedPane.add(rand_tab, "Noise");
    }

    /**
     * Populate fields with current data
     */
    public void fillFieldValues() {
        IACNeuron neuron_ref = (IACNeuron) neuron_list.get(0);

        tfDecay.setText(Double.toString(neuron_ref.getDecay()));
        tfRest.setText(Double.toString(neuron_ref.getRest()));
        isAddNoise.setSelected(neuron_ref.getAddNoise());
        isClipping.setSelected(neuron_ref.getClipping());
        isAddNoise.setSelected(neuron_ref.getAddNoise());

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuron_list, IACNeuron.class, "getDecay")) {
            tfDecay.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuron_list, IACNeuron.class, "getRest")) {
            tfRest.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuron_list, IACNeuron.class, "getClipping")) {
            isClipping.setNull();
        }

        if (!NetworkUtils.isConsistent(neuron_list, IACNeuron.class, "getAddNoise")) {
            isAddNoise.setNull();
        }

        rand_tab.fillFieldValues(getRandomizers());
    }

    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < neuron_list.size(); i++) {
            ret.add(((IACNeuron) neuron_list.get(i)).getNoiseGenerator());
        }

        return ret;
    }

    /**
     * Fill field values to default values for binary neuron
     */
    public void fillDefaultValues() {
        IACNeuron neuron_ref = new IACNeuron();
        tfDecay.setText(Double.toString(neuron_ref.getDecay()));
        tfRest.setText(Double.toString(neuron_ref.getRest()));
        isClipping.setSelected(neuron_ref.getClipping());
        isAddNoise.setSelected(neuron_ref.getAddNoise());
        rand_tab.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {
        for (int i = 0; i < neuron_list.size(); i++) {
            IACNeuron neuron_ref = (IACNeuron) neuron_list.get(i);

            if (tfDecay.getText().equals(NULL_STRING) == false) {
                neuron_ref.setDecay(Double.parseDouble(tfDecay.getText()));
            }

            if (tfRest.getText().equals(NULL_STRING) == false) {
                neuron_ref.setRest(Double.parseDouble(tfRest.getText()));
            }

            if (isClipping.isNull() == false) {
                neuron_ref.setClipping(isClipping.isSelected());
            }

            if (isAddNoise.isNull() == false) {
                neuron_ref.setAddNoise(isAddNoise.isSelected());
            }

            rand_tab.commitRandom(neuron_ref.getNoiseGenerator());
        }
    }
}
