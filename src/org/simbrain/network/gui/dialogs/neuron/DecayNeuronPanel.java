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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.RandomPanel;
import org.simbrain.network.neurons.DecayNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;


/**
 * <b>DecayNeuronPanel</b>.
 */
public class DecayNeuronPanel extends AbstractNeuronPanel implements ActionListener {

    /** Relative absolute combo box. */
    private TristateDropDown cbRelAbs = new TristateDropDown("Relative", "Absolute");

    /** Decay amount field. */
    private JTextField tfDecayAmount = new JTextField();

    /** Decay fraction field. */
    private JTextField tfDecayFraction = new JTextField();

    /** Base line field. */
    private JTextField tfBaseLine = new JTextField();

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
     * This method is the default constructor.
     */
    public DecayNeuronPanel() {
        cbRelAbs.addActionListener(this);
        cbRelAbs.setActionCommand("relAbs");

        this.add(tabbedPane);
        mainTab.addItem("", cbRelAbs);
        mainTab.addItem("Base line", tfBaseLine);
        mainTab.addItem("Decay amount", tfDecayAmount);
        mainTab.addItem("Decay fraction", tfDecayFraction);
        mainTab.addItem("Use clipping", isClipping);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
        checkBounds();
    }

    /**
     * Responds to actions performed.
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("relAbs")) {
            checkBounds();
        }
    }

    /**
     * Checks the relative absolute bounds.
     */
    private void checkBounds() {
        if (cbRelAbs.getSelectedIndex() == 0) {
            tfDecayAmount.setEnabled(false);
            tfDecayFraction.setEnabled(true);
        } else {
            tfDecayFraction.setEnabled(false);
            tfDecayAmount.setEnabled(true);
        }
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        DecayNeuron neuronRef = (DecayNeuron) neuronList.get(0);

        cbRelAbs.setSelectedIndex(neuronRef.getRelAbs());
        tfBaseLine.setText(Double.toString(neuronRef.getBaseLine()));
        tfDecayAmount.setText(Double.toString(neuronRef.getDecayAmount()));
        tfDecayFraction.setText(Double.toString(neuronRef.getDecayFraction()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, DecayNeuron.class, "getRelAbs")) {
            cbRelAbs.setNull();
        }

        if (!NetworkUtils.isConsistent(neuronList, DecayNeuron.class, "getBaseLine")) {
            tfBaseLine.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, DecayNeuron.class, "getDecayFraction")) {
            tfDecayFraction.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, DecayNeuron.class, "getDecayAmount")) {
            tfDecayAmount.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, DecayNeuron.class, "getClipping")) {
            isClipping.setNull();
        }

        if (!NetworkUtils.isConsistent(neuronList, DecayNeuron.class, "getAddNoise")) {
            isAddNoise.setNull();
        }

        randTab.fillFieldValues(getRandomizers());
    }

    /**
     * @return A list of randomizers.
     */
    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < neuronList.size(); i++) {
            ret.add(((DecayNeuron) neuronList.get(i)).getNoiseGenerator());
        }

        return ret;
    }

    /**
     * Fill field values to default values for additive neuron.
     */
    public void fillDefaultValues() {
        DecayNeuron neuronRef = new DecayNeuron();
        cbRelAbs.setSelectedIndex(neuronRef.getRelAbs());
        tfBaseLine.setText(Double.toString(neuronRef.getBaseLine()));
        tfDecayAmount.setText(Double.toString(neuronRef.getDecayFraction()));
        tfDecayFraction.setText(Double.toString(neuronRef.getDecayFraction()));
        isClipping.setSelected(neuronRef.getClipping());
        isAddNoise.setSelected(neuronRef.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < neuronList.size(); i++) {
            DecayNeuron neuronRef = (DecayNeuron) neuronList.get(i);

            if (!cbRelAbs.isNull()) {
                neuronRef.setRelAbs(cbRelAbs.getSelectedIndex());
            }

            if (!tfDecayAmount.getText().equals(NULL_STRING)) {
                neuronRef.setDecayAmount(Double.parseDouble(tfDecayAmount.getText()));
            }

            if (!tfBaseLine.getText().equals(NULL_STRING)) {
                neuronRef.setBaseLine(Double.parseDouble(tfBaseLine.getText()));
            }

            if (!tfDecayFraction.getText().equals(NULL_STRING)) {
                neuronRef.setDecayFraction(Double.parseDouble(tfDecayFraction.getText()));
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
