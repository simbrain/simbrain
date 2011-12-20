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
package org.simbrain.network.gui.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronPanel;
import org.simbrain.network.util.RandomSource;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;

/**
 * <b>RandomPanel</b> an interface for setting parameters of randomly generated
 * data; linked with "random source," which is a generalized source of random
 * data. Random data are needed by multiple neurons and synapses in simbrain;
 * this class prevents that functionality from being implemented redundantly.
 */
public class RandomPanel extends LabelledItemPanel implements ActionListener {

    /** Distribution combo box. */
    private JComboBox cbDistribution = new JComboBox(RandomSource.getFunctionList());

    /** Upper bound field. */
    private JTextField tfUpBound = new JTextField();

    /** Lower bound field. */
    private JTextField tfLowBound = new JTextField();

    /** Mean value field. */
    private JTextField tfMean = new JTextField();

    /** Standard deviation field. */
    private JTextField tfStandardDeviation = new JTextField();

    /** Clipping combo box. */
    private TristateDropDown tsClipping = new TristateDropDown();

    /** Null string. */
    private String nullString = AbstractNeuronPanel.NULL_STRING;

    /**
     * This method is the default constructor.
     *
     * @param useLocalBounds Should local bounds be used
     */
    public RandomPanel(final boolean useLocalBounds) {
        cbDistribution.addActionListener(this);
        tsClipping.addActionListener(this);
        tsClipping.setActionCommand("useBounds");

        this.addItem("Distribution", cbDistribution);

        if (useLocalBounds) {
            this.addItem("Upper bound", tfUpBound);
            this.addItem("Lower bound", tfLowBound);
        }

        this.addItem("Mean value", tfMean);
        this.addItem("Standard deviation", tfStandardDeviation);
        this.addItem("Use clipping", tsClipping);

        init();
    }

    /**
     * Initializes the panel.
     */
    public void init() {
        if (cbDistribution.getSelectedIndex() == RandomSource.UNIFORM) {
            tfUpBound.setEnabled(true);
            tfLowBound.setEnabled(true);
            tfMean.setEnabled(false);
            tfStandardDeviation.setEnabled(false);
            tsClipping.setSelectedIndex(TristateDropDown.getTRUE());
            tsClipping.setEnabled(false);
        } else if (cbDistribution.getSelectedIndex() == RandomSource.GAUSSIAN) {
            tfMean.setEnabled(true);
            tfStandardDeviation.setEnabled(true);
            tsClipping.setEnabled(true);
            checkBounds();
        }
    }

    /**
     * Enable or disable the upper and lower bounds fields depending on state of rounding button.
     */
    private void checkBounds() {
        if (tsClipping.getSelectedIndex() == TristateDropDown.getFALSE()) {
            tfLowBound.setEnabled(false);
            tfUpBound.setEnabled(false);
        } else {
            tfLowBound.setEnabled(true);
            tfUpBound.setEnabled(true);
        }
    }

    /** @see ActionListener */
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("useBounds")) {
            checkBounds();
        }

        init();
    }

    /**
     * Populates the fields with current values.
     *
     * @param randomizers List of randomizers
     */
    public void fillFieldValues(final ArrayList randomizers) {
        RandomSource rand = (RandomSource) randomizers.get(0);

        cbDistribution.setSelectedIndex(rand.getDistributionIndex());
        tsClipping.setSelected(rand.getClipping());
        tfLowBound.setText(Double.toString(rand.getLowerBound()));
        tfUpBound.setText(Double.toString(rand.getUpperBound()));
        tfStandardDeviation.setText(Double.toString(rand.getStandardDeviation()));
        tfMean.setText(Double.toString(rand.getMean()));

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getDistributionIndex")) {
            if ((cbDistribution.getItemCount() == RandomSource.getFunctionList().length)) {
                cbDistribution.addItem(nullString);
            }

            cbDistribution.setSelectedIndex(RandomSource.getFunctionList().length);
        }

        if (!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getClipping")) {
            tsClipping.setNull();
        }

        if (!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getLowerBound")) {
            tfLowBound.setText(nullString);
        }

        if (!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getUpperBound")) {
            tfUpBound.setText(nullString);
        }

        if (!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getStandardDeviation")) {
            tfStandardDeviation.setText(nullString);
        }

        if (!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getMean")) {
            tfMean.setText(nullString);
        }
    }

    /**
     * Fills fields with default values.
     */
    public void fillDefaultValues() {
        RandomSource rand = new RandomSource();
        cbDistribution.setSelectedIndex(rand.getDistributionIndex());
        tsClipping.setSelected(rand.getClipping());
        tfLowBound.setText(Double.toString(rand.getLowerBound()));
        tfUpBound.setText(Double.toString(rand.getUpperBound()));
        tfStandardDeviation.setText(Double.toString(rand.getStandardDeviation()));
        tfMean.setText(Double.toString(rand.getMean()));
    }

    /**
     * Called externally when dialog is being closed.
     *
     * @param rand Random soruce
     */
    public void commitRandom(final RandomSource rand) {
        if (!cbDistribution.getSelectedItem().equals(nullString)) {
            rand.setDistributionIndex(cbDistribution.getSelectedIndex());
        }

        if (!tfLowBound.getText().equals(nullString)) {
            rand.setLowerBound(Double.parseDouble(tfLowBound.getText()));
        }

        if (!tfUpBound.getText().equals(nullString)) {
            rand.setUpperBound(Double.parseDouble(tfUpBound.getText()));
        }

        if (!tfStandardDeviation.getText().equals(nullString)) {
            rand.setStandardDeviation(Double.parseDouble(tfStandardDeviation.getText()));
        }

        if (!tfMean.getText().equals(nullString)) {
            rand.setMean(Double.parseDouble(tfMean.getText()));
        }

        if (!(tsClipping.getSelectedIndex() == TristateDropDown.getNULL())) {
            rand.setClipping(tsClipping.isSelected());
        }
    }

    /**
     * @return Returns the cbDistribution.
     */
    public JComboBox getCbDistribution() {
        return cbDistribution;
    }

    /**
     * @param cbDistribution The cbDistribution to set.
     */
    public void setCbDistribution(final JComboBox cbDistribution) {
        this.cbDistribution = cbDistribution;
    }

    /**
     * @return Returns the isUseBoundsBox.
     */
    public TristateDropDown getTsClipping() {
        return tsClipping;
    }

    /**
     * @param isUseBoundsBox The isUseBoundsBox to set.
     */
    public void setTsClipping(final TristateDropDown isUseBoundsBox) {
        this.tsClipping = isUseBoundsBox;
    }

    /**
     * @return Returns the tfLowBound.
     */
    public JTextField getTfLowBound() {
        return tfLowBound;
    }

    /**
     * @param tfLowBound The tfLowBound to set.
     */
    public void setTfLowBound(final JTextField tfLowBound) {
        this.tfLowBound = tfLowBound;
    }

    /**
     * @return Returns the tfMean.
     */
    public JTextField getTfMean() {
        return tfMean;
    }

    /**
     * @param tfMean The tfMean to set.
     */
    public void setTfMean(final JTextField tfMean) {
        this.tfMean = tfMean;
    }

    /**
     * @return Returns the tfStandardDeviation.
     */
    public JTextField getTfStandardDeviation() {
        return tfStandardDeviation;
    }

    /**
     * @param tfStandardDeviation The tfStandardDeviation to set.
     */
    public void setTfStandardDeviation(final JTextField tfStandardDeviation) {
        this.tfStandardDeviation = tfStandardDeviation;
    }

    /**
     * @return Returns the tfUpBound.
     */
    public JTextField getTfUpBound() {
        return tfUpBound;
    }

    /**
     * @param tfUpBound The tfUpBound to set.
     */
    public void setTfUpBound(final JTextField tfUpBound) {
        this.tfUpBound = tfUpBound;
    }
}
