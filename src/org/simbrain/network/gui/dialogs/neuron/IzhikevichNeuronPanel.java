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

import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.RootNetwork;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanel;
import org.simbrain.network.neurons.IzhikevichNeuron;
import org.simbrain.network.util.RandomSource;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;


/**
 * <b>IzhikevichNeuronPanel</b>.
 */
public class IzhikevichNeuronPanel extends AbstractNeuronPanel {

    /** A field. */
    private JTextField tfA = new JTextField();

    /** B field. */
    private JTextField tfB = new JTextField();

    /** C field. */
    private JTextField tfC = new JTextField();

    /** D field. */
    private JTextField tfD = new JTextField();

    /** Time step field. */
    private JTextField tfTimeStep = new JTextField();

    /** Add noise combo box. */
    private TristateDropDown tsNoise = new TristateDropDown();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private RandomPanel randTab = new RandomPanel(true);

    /**
     * Creates an instance of this panel.
     */
    public IzhikevichNeuronPanel(RootNetwork network) {
        super(network);
        this.add(tabbedPane);
        mainTab.addItem("Time step", tfTimeStep);
        mainTab.addItem("A", tfA);
        mainTab.addItem("B", tfB);
        mainTab.addItem("C", tfC);
        mainTab.addItem("D", tfD);
        mainTab.addItem("Add noise", tsNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
        this.addBottomText("<html>For a list of useful parameter settings<p>"
                        + "press the \"Help\" Button.</html>");
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        IzhikevichNeuron neuronRef = (IzhikevichNeuron) ruleList.get(0);

        tfTimeStep.setText(Double.toString(parentNet.getRootNetwork().getTimeStep()));

        tfA.setText(Double.toString(neuronRef.getA()));
        tfB.setText(Double.toString(neuronRef.getB()));
        tfC.setText(Double.toString(neuronRef.getC()));
        tfD.setText(Double.toString(neuronRef.getD()));
        tsNoise.setSelected(neuronRef.getAddNoise());

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(ruleList, IzhikevichNeuron.class, "getA")) {
            tfA.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, IzhikevichNeuron.class, "getB")) {
            tfB.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, IzhikevichNeuron.class, "getC")) {
            tfC.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, IzhikevichNeuron.class, "getD")) {
            tfD.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(ruleList, IzhikevichNeuron.class, "getAddNoise")) {
            tsNoise.setNull();
        }

        randTab.fillFieldValues(getRandomizers());
    }

    /**
     * @return List of randomizers.
     */
    private ArrayList<RandomSource> getRandomizers() {
        ArrayList<RandomSource> ret = new ArrayList<RandomSource>();
        for (int i = 0; i < ruleList.size(); i++) {
            ret.add(((IzhikevichNeuron) ruleList.get(i)).getNoiseGenerator());
        }
        return ret;
    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        IzhikevichNeuron neuronRef = new IzhikevichNeuron();
        tfTimeStep.setText(Double.toString(parentNet.getRootNetwork().getTimeStep()));
        tfA.setText(Double.toString(neuronRef.getA()));
        tfB.setText(Double.toString(neuronRef.getB()));
        tfC.setText(Double.toString(neuronRef.getC()));
        tfD.setText(Double.toString(neuronRef.getD()));
        tsNoise.setSelected(neuronRef.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        parentNet.getRootNetwork().setTimeStep(Double.parseDouble(tfTimeStep.getText()));

        for (int i = 0; i < ruleList.size(); i++) {
            IzhikevichNeuron neuronRef = (IzhikevichNeuron) ruleList.get(i);

            if (!tfA.getText().equals(NULL_STRING)) {
                neuronRef.setA(Double.parseDouble(tfA.getText()));
            }

            if (!tfB.getText().equals(NULL_STRING)) {
                neuronRef.setB(Double.parseDouble(tfB.getText()));
            }

            if (!tfC.getText().equals(NULL_STRING)) {
                neuronRef.setC(Double.parseDouble(tfC.getText()));
            }

            if (!tfD.getText().equals(NULL_STRING)) {
                neuronRef.setD(Double.parseDouble(tfD.getText()));
            }

            if (!tsNoise.isNull()) {
                neuronRef.setAddNoise(tsNoise.isSelected());
            }

            randTab.commitRandom(neuronRef.getNoiseGenerator());
        }
    }
}
