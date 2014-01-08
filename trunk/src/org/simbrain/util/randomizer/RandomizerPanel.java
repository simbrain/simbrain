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
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;

import org.simbrain.util.widgets.LabelledItem;

/**
 * <b>RandomizerPanel</b> an interface for setting parameters of a randomizer
 * object.
 */
public class RandomizerPanel extends JPanel implements ActionListener {

    /** Distribution combo box. */
    private JComboBox cbDistribution = new JComboBox(
            Randomizer.getFunctionList()) {

        @Override
        public void setSelectedIndex(int index) {
            this.setSelectedItem(Randomizer.getFunctionList()[index]);
            this.firePropertyChange("Distribution", null, null);
        }

    };

    /** Upper bound field. */
    private JFormattedTextField tfUpBound = new JFormattedTextField();

    /** Lower bound field. */
    private JFormattedTextField tfLowBound = new JFormattedTextField();

    /** Mean value field. */
    private JFormattedTextField tfMean = new JFormattedTextField();

    /** Standard deviation field. */
    private JFormattedTextField tfStandardDeviation = new JFormattedTextField();

    /** Clipping combo box. */
    private JCheckBox tsClipping = new JCheckBox();

    /**
     * This method is the default constructor.
     */
    public RandomizerPanel() {

        Box mainPanel = Box.createVerticalBox();
        cbDistribution.addActionListener(this);
        mainPanel.add(new LabelledItem("Distribution", cbDistribution));
        mainPanel.add(new LabelledItem("Ceiling", tfUpBound));
        mainPanel.add(new LabelledItem("Floor", tfLowBound));
        mainPanel.add(new LabelledItem("Mean", tfMean));
        mainPanel.add(new LabelledItem("Std. Dev.", tfStandardDeviation));
        tsClipping.addActionListener(this);
        tsClipping.setActionCommand("useBounds");
        mainPanel.add(new LabelledItem("Clipping", tsClipping));

        this.add(mainPanel);
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
    public void checkBounds() {
        tfLowBound.setEnabled(tsClipping.isSelected());
        tfUpBound.setEnabled(tsClipping.isSelected());
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
        tfLowBound.setValue(Double.toString(rand.getLowerBound()));
        tfUpBound.setValue(Double.toString(rand.getUpperBound()));
        tfStandardDeviation.setValue(Double.toString(rand
                .getStandardDeviation()));
        tfMean.setValue(Double.toString(rand.getMean()));

    }

    /**
     * Fills fields with values from a Random Source.
     *
     * @param rand
     */
    public void fillFieldValues(Randomizer rand) {
        cbDistribution.setSelectedIndex(rand.getDistributionIndex());
        tsClipping.setSelected(rand.getClipping());
        tfLowBound.setValue(Double.toString(rand.getLowerBound()));
        tfUpBound.setValue(Double.toString(rand.getUpperBound()));
        tfStandardDeviation.setValue(Double.toString(rand
                .getStandardDeviation()));
        tfMean.setValue(Double.toString(rand.getMean()));
    }

    /**
     * Fills fields with default values.
     */
    public void fillDefaultValues() {
        Randomizer rand = new Randomizer();
        cbDistribution.setSelectedIndex(rand.getDistributionIndex());
        tsClipping.setSelected(rand.getClipping());
        tfLowBound.setValue(Double.toString(rand.getLowerBound()));
        tfUpBound.setValue(Double.toString(rand.getUpperBound()));
        tfStandardDeviation.setValue(Double.toString(rand
                .getStandardDeviation()));
        tfMean.setValue(Double.toString(rand.getMean()));
    }

    /**
     * Called externally when dialog is being closed.
     *
     * @param rand Random source
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

    public void setEnabled(boolean enabled) {

        boolean gaussConditions = enabled
                && (cbDistribution.getSelectedIndex() == Randomizer.GAUSSIAN);
        boolean roundingConditions = (tsClipping.isSelected() || (cbDistribution
                .getSelectedIndex() == Randomizer.UNIFORM)) && enabled;

        cbDistribution.setEnabled(enabled);

        tfUpBound.setEnabled(roundingConditions);
        tfLowBound.setEnabled(roundingConditions);

        tfMean.setEnabled(gaussConditions);
        tfStandardDeviation.setEnabled(gaussConditions);
        tsClipping.setEnabled(gaussConditions);

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

    // /**
    // * @return Returns the isUseBoundsBox.
    // */
    // public TristateDropDown getTsClipping() {
    // return tsClipping;
    // }

    /**
     * @return Returns the isUseBoundsBox.
     */
    public JCheckBox getTsClipping() {
        return tsClipping;
    }

    // /**
    // * @param isUseBoundsBox The isUseBoundsBox to set.
    // */
    // public void setTsClipping(final TristateDropDown isUseBoundsBox) {
    // this.tsClipping = isUseBoundsBox;
    // }

    /**
     * @param isUseBoundsBox The isUseBoundsBox to set.
     */
    public void setTsClipping(final JCheckBox isUseBoundsBox) {
        this.tsClipping = isUseBoundsBox;
    }

    /**
     * @return Returns the tfLowBound.
     */
    public JFormattedTextField getTfLowBound() {
        return tfLowBound;
    }

    /**
     * @param tfLowBound The tfLowBound to set.
     */
    public void setTfLowBound(final JFormattedTextField tfLowBound) {
        this.tfLowBound = tfLowBound;
    }

    /**
     * @return Returns the tfMean.
     */
    public JFormattedTextField getTfMean() {
        return tfMean;
    }

    /**
     * @param tfMean The tfMean to set.
     */
    public void setTfMean(final JFormattedTextField tfMean) {
        this.tfMean = tfMean;
    }

    /**
     * @return Returns the tfStandardDeviation.
     */
    public JFormattedTextField getTfStandardDeviation() {
        return tfStandardDeviation;
    }

    /**
     * @param tfStandardDeviation The tfStandardDeviation to set.
     */
    public void setTfStandardDeviation(
            final JFormattedTextField tfStandardDeviation) {
        this.tfStandardDeviation = tfStandardDeviation;
    }

    /**
     * @return Returns the tfUpBound.
     */
    public JFormattedTextField getTfUpBound() {
        return tfUpBound;
    }

    /**
     * @param tfUpBound The tfUpBound to set.
     */
    public void setTfUpBound(final JFormattedTextField tfUpBound) {
        this.tfUpBound = tfUpBound;
    }

    public void addPropertyChangeListenerToFields(PropertyChangeListener pc) {
        cbDistribution.addPropertyChangeListener(pc);
        tfUpBound.addPropertyChangeListener(pc);
        tfLowBound.addPropertyChangeListener(pc);
        tfMean.addPropertyChangeListener(pc);
        tfStandardDeviation.addPropertyChangeListener(pc);
        tsClipping.addPropertyChangeListener(pc);
    }
}
