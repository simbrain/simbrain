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
import org.simnet.neurons.SigmoidalNeuron;
import org.simnet.neurons.SinusoidalNeuron;


/**
 * <b>SinusoidalNeuronPanel</b>
 */
public class SinusoidalNeuronPanel extends AbstractNeuronPanel {
    private JTextField tfPhase = new JTextField();
    private JTextField tfFrequency = new JTextField();
    private TristateDropDown isAddNoise = new TristateDropDown();
    private LabelledItemPanel mainPanel = new LabelledItemPanel();
    private RandomPanel randPanel = new RandomPanel(true);
    private JTabbedPane tabbedPanel = new JTabbedPane();

    public SinusoidalNeuronPanel() {
        this.add(tabbedPanel);
        mainPanel.addItem("Phase", tfPhase);
        mainPanel.addItem("Frequency", tfFrequency);
        mainPanel.addItem("Add noise", isAddNoise);
        tabbedPanel.add(mainPanel, "Main");
        tabbedPanel.add(randPanel, "Noise");
    }

    public void fillFieldValues() {
        SinusoidalNeuron neuron_ref = (SinusoidalNeuron) neuron_list.get(0);

        tfFrequency.setText(Double.toString(neuron_ref.getFrequency()));
        tfPhase.setText(Double.toString(neuron_ref.getPhase()));
        isAddNoise.setSelected(neuron_ref.getAddNoise());
   
        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuron_list, SinusoidalNeuron.class, "getFrequency")) {
            tfFrequency.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuron_list, SinusoidalNeuron.class, "getPhase")) {
            tfPhase.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuron_list, SinusoidalNeuron.class, "getAddNoise")) {
            isAddNoise.setNull();
        }
        randPanel.fillFieldValues(getRandomizers());
    }

    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < neuron_list.size(); i++) {
            ret.add(((SinusoidalNeuron) neuron_list.get(i)).getNoiseGenerator());
        }

        return ret;
    }


    public void fillDefaultValues() {
        SinusoidalNeuron neuronRef = new SinusoidalNeuron();
        tfFrequency.setText(Double.toString(neuronRef.getFrequency()));
        tfPhase.setText(Double.toString(neuronRef.getPhase()));
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randPanel.fillDefaultValues();
    }

    public void commitChanges() {
        for (int i = 0; i < neuron_list.size(); i++) {
            SinusoidalNeuron neuronRef = (SinusoidalNeuron) neuron_list.get(i);

            if (tfPhase.getText().equals(NULL_STRING) == false) {
                neuronRef.setPhase(Double.parseDouble(tfPhase.getText()));
            }
            if (tfFrequency.getText().equals(NULL_STRING) == false) {
                neuronRef.setFrequency(Double.parseDouble(tfFrequency.getText()));
            }
            if (isAddNoise.isNull() == false) {
                neuronRef.setAddNoise(isAddNoise.isSelected());
            }

            randPanel.commitRandom(neuronRef.getNoiseGenerator());

        }
    }
}
