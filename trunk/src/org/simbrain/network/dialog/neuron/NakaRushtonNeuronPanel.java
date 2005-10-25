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
import org.simnet.neurons.NakaRushtonNeuron;


/**
 * <b>NakaRushtonNeuronPanel</b>
 */
public class NakaRushtonNeuronPanel extends AbstractNeuronPanel {
    private JTextField tfMaxSpikeRate = new JTextField();
    private JTextField tfSteepness = new JTextField();
    private JTextField tfSemiSaturation = new JTextField();
    private JTextField tfTimeStep = new JTextField();
    private JTextField tfTimeConstant = new JTextField();
    private TristateDropDown tsNoise = new TristateDropDown();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private LabelledItemPanel mainTab = new LabelledItemPanel();
    private RandomPanel randTab = new RandomPanel(true);

    public NakaRushtonNeuronPanel(Network net) {
        parentNet = net;

        this.add(tabbedPane);
        mainTab.addItem("Time step", tfTimeStep);
        mainTab.addItem("Maximum spike rate", tfMaxSpikeRate);
        mainTab.addItem("Steepness", tfSteepness);
        mainTab.addItem("Semi-saturation constant", tfSemiSaturation);
        mainTab.addItem("Time constant", tfTimeConstant);
        mainTab.addItem("Add noise", tsNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    public void fillFieldValues() {
        NakaRushtonNeuron neuron_ref = (NakaRushtonNeuron) neuron_list.get(0);

        tfMaxSpikeRate.setText(Double.toString(neuron_ref.getMaximumSpikeRate()));
        tfSemiSaturation.setText(Double.toString(neuron_ref.getSemiSaturationConstant()));
        tfSteepness.setText(Double.toString(neuron_ref.getSteepness()));
        tfTimeConstant.setText(Double.toString(neuron_ref.getTimeConstant()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tsNoise.setSelected(neuron_ref.getAddNoise());

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuron_list, NakaRushtonNeuron.class, "getMaximumSpikeRate")) {
            tfMaxSpikeRate.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuron_list, NakaRushtonNeuron.class, "getTimeConstant")) {
            tfTimeConstant.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuron_list, NakaRushtonNeuron.class, "getSemiSaturationConstant")) {
            tfSemiSaturation.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuron_list, NakaRushtonNeuron.class, "getSteepness")) {
            tfSteepness.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuron_list, NakaRushtonNeuron.class, "getAddNoise")) {
            tsNoise.setNull();
        }

        randTab.fillFieldValues(getRandomizers());
    }

    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < neuron_list.size(); i++) {
            ret.add(((NakaRushtonNeuron) neuron_list.get(i)).getNoiseGenerator());
        }

        return ret;
    }

    public void fillDefaultValues() {
        NakaRushtonNeuron neuron_ref = new NakaRushtonNeuron();
        tfMaxSpikeRate.setText(Double.toString(neuron_ref.getMaximumSpikeRate()));
        tfSemiSaturation.setText(Double.toString(neuron_ref.getSemiSaturationConstant()));
        tfSteepness.setText(Double.toString(neuron_ref.getSteepness()));
        tfTimeConstant.setText(Double.toString(neuron_ref.getTimeConstant()));
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tsNoise.setSelected(neuron_ref.getAddNoise());
        randTab.fillDefaultValues();
    }

    public void commitChanges() {
        parentNet.setTimeStep(Double.parseDouble(tfTimeStep.getText()));

        for (int i = 0; i < neuron_list.size(); i++) {
            NakaRushtonNeuron neuron_ref = (NakaRushtonNeuron) neuron_list.get(i);

            if (tfMaxSpikeRate.getText().equals(NULL_STRING) == false) {
                neuron_ref.setMaximumSpikeRate(Double.parseDouble(tfMaxSpikeRate.getText()));
            }

            if (tfTimeConstant.getText().equals(NULL_STRING) == false) {
                neuron_ref.setTimeConstant(Double.parseDouble(tfTimeConstant.getText()));
            }

            if (tfSemiSaturation.getText().equals(NULL_STRING) == false) {
                neuron_ref.setSemiSaturationConstant(Double.parseDouble(tfSemiSaturation.getText()));
            }

            if (tfSteepness.getText().equals(NULL_STRING) == false) {
                neuron_ref.setSteepness(Double.parseDouble(tfSteepness.getText()));
            }

            if (tsNoise.isNull() == false) {
                neuron_ref.setAddNoise(tsNoise.isSelected());
            }

            randTab.commitRandom(neuron_ref.getNoiseGenerator());
        }
    }
}
