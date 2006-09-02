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
package org.simbrain.network.dialog.synapse;

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.TristateDropDown;
import org.simnet.synapses.ShortTermPlasticitySynapse;


/**
 * <b>ShortTermPlasticitySynapsePanel</b>.
 */
public class ShortTermPlasticitySynapsePanel extends AbstractSynapsePanel {

    /** Baseline strength field. */
    private JTextField tfBaseLineStrength = new JTextField();

    /** Firing threshold field. */
    private JTextField tfFiringThreshold = new JTextField();

    /** Bump rate field. */
    private JTextField tfBumpRate = new JTextField();

    /** Decay rate field. */
    private JTextField tfDecayRate = new JTextField();

    /** Plasticity type combo box. */
    private TristateDropDown cbPlasticityType = new TristateDropDown("Depression", "Facilitation");

    /** Synapse reference. */
    private ShortTermPlasticitySynapse synapseRef;

    /**
     * Creates a short term plasticity synapse panel.
     */
    public ShortTermPlasticitySynapsePanel() {
        this.addItem("Plasticity type", cbPlasticityType);
        this.addItem("Base-line-strength", tfBaseLineStrength);
        this.addItem("Firing threshold", tfFiringThreshold);
        this.addItem("Growth-rate", tfBumpRate);
        this.addItem("Decay-rate", tfDecayRate);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        synapseRef = (ShortTermPlasticitySynapse) synapseList.get(0);

        cbPlasticityType.setSelectedIndex(synapseRef.getPlasticityType());
        tfBaseLineStrength.setText(Double.toString(synapseRef.getBaseLineStrength()));
        tfFiringThreshold.setText(Double.toString(synapseRef.getFiringThreshold()));
        tfBumpRate.setText(Double.toString(synapseRef.getBumpRate()));
        tfDecayRate.setText(Double.toString(synapseRef.getDecayRate()));

        //Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(synapseList, ShortTermPlasticitySynapse.class, "getPlasticityType")) {
            cbPlasticityType.setNull();
        }

        if (!NetworkUtils.isConsistent(synapseList, ShortTermPlasticitySynapse.class, "getBaseLineStrength")) {
            tfBaseLineStrength.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapseList, ShortTermPlasticitySynapse.class, "getFiringThreshold")) {
            tfFiringThreshold.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapseList, ShortTermPlasticitySynapse.class, "getBumpRate")) {
            tfBumpRate.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapseList, ShortTermPlasticitySynapse.class, "getDecayRate")) {
            tfDecayRate.setText(NULL_STRING);
        }
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        ShortTermPlasticitySynapse synapseRef = new ShortTermPlasticitySynapse();
        cbPlasticityType.setSelectedIndex(synapseRef.getPlasticityType());
        tfBaseLineStrength.setText(Double.toString(synapseRef.getBaseLineStrength()));
        tfFiringThreshold.setText(Double.toString(synapseRef.getFiringThreshold()));
        tfBumpRate.setText(Double.toString(synapseRef.getBumpRate()));
        tfDecayRate.setText(Double.toString(synapseRef.getDecayRate()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < synapseList.size(); i++) {
            ShortTermPlasticitySynapse synapseRef = (ShortTermPlasticitySynapse) synapseList.get(i);

            if (!cbPlasticityType.isNull()) {
                synapseRef.setPlasticityType(cbPlasticityType.getSelectedIndex());
            }

            if (!tfBaseLineStrength.getText().equals(NULL_STRING)) {
                synapseRef.setBaseLineStrength(Double.parseDouble(tfBaseLineStrength.getText()));
            }

            if (!tfFiringThreshold.getText().equals(NULL_STRING)) {
                synapseRef.setFiringThreshold(Double.parseDouble(tfFiringThreshold.getText()));
            }

            if (!tfBumpRate.getText().equals(NULL_STRING)) {
                synapseRef.setBumpRate(Double.parseDouble(tfBumpRate.getText()));
            }

            if (!tfDecayRate.getText().equals(NULL_STRING)) {
                synapseRef.setDecayRate(Double.parseDouble(tfDecayRate.getText()));
            }
        }
    }
}
