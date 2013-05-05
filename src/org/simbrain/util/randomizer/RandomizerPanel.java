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
package org.simbrain.util.randomizer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;

/**
 * <b>RandomizerPanel</b> an interface for setting parameters of a randomizer object.
 */
public class RandomizerPanel extends LabelledItemPanel implements ActionListener {

    /** Distribution combo box. */
    private JComboBox cbDistribution = new JComboBox(
            Randomizer.getFunctionList());

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

    /**
     * This method is the default constructor.
     *
     * @param useLocalBounds Should local bounds be used
     */
    public RandomizerPanel(final boolean useLocalBounds) {
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
        if (cbDistribution.getSelectedIndex() == Randomizer.UNIFORM) {
            tfUpBound.setEnabled(true);
            tfLowBound.setEnabled(true);
            tfMean.setEnabled(false);
            tfStandardDeviation.setEnabled(false);
            tsClipping.setSelectedIndex(TristateDropDown.getTRUE());
            tsClipping.setEnabled(false);
        } else if (cbDistribution.getSelectedIndex() == Randomizer.GAUSSIAN) {
            tfMean.setEnabled(true);
            tfStandardDeviation.setEnabled(true);
            tsClipping.setEnabled(true);
            checkBounds();
        }
    }

    /**
     * Enable or disable the upper and lower bounds fields depending on state of
     * rounding button.
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
        Randomizer rand = (Randomizer) randomizers.get(0);

        cbDistribution.setSelectedIndex(rand.getDistributionIndex());
        tsClipping.setSelected(rand.getClipping());
        tfLowBound.setText(Double.toString(rand.getLowerBound()));
        tfUpBound.setText(Double.toString(rand.getUpperBound()));
        tfStandardDeviation
                .setText(Double.toString(rand.getStandardDeviation()));
        tfMean.setText(Double.toString(rand.getMean()));

    }

    /**
     * Fills fields with values from a Random Source.
     *
     * @param rand
     */
    public void fillFieldValues(Randomizer rand) {
        cbDistribution.setSelectedIndex(rand.getDistributionIndex());
        tsClipping.setSelected(rand.getClipping());
        tfLowBound.setText(Double.toString(rand.getLowerBound()));
        tfUpBound.setText(Double.toString(rand.getUpperBound()));
        tfStandardDeviation
                .setText(Double.toString(rand.getStandardDeviation()));
        tfMean.setText(Double.toString(rand.getMean()));
    }

    /**
     * Fills fields with default values.
     */
    public void fillDefaultValues() {
        Randomizer rand = new Randomizer();
        cbDistribution.setSelectedIndex(rand.getDistributionIndex());
        tsClipping.setSelected(rand.getClipping());
        tfLowBound.setText(Double.toString(rand.getLowerBound()));
        tfUpBound.setText(Double.toString(rand.getUpperBound()));
        tfStandardDeviation
                .setText(Double.toString(rand.getStandardDeviation()));
        tfMean.setText(Double.toString(rand.getMean()));
    }

    /**
     * Called externally when dialog is being closed.
     *
     * @param rand Random soruce
     */
    public void commitRandom(final Randomizer rand) {
        rand.setDistributionIndex(cbDistribution.getSelectedIndex());
        rand.setLowerBound(Double.parseDouble(tfLowBound.getText()));
        rand.setUpperBound(Double.parseDouble(tfUpBound.getText()));
        if (tfStandardDeviation.isEnabled()) {
            rand.setStandardDeviation(Double.parseDouble(tfStandardDeviation
                    .getText()));
        }
        rand.setMean(Double.parseDouble(tfMean.getText()));
        rand.setClipping(tsClipping.isSelected());
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
