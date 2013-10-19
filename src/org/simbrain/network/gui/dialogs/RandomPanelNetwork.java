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

import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.AbstractNeuronPanel;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.randomizer.RandomizerPanel;

/**
 * <b>RandomPanelNetwork</b> extends RandomizerPanel with facilities for
 * checking consistency across multiple neurons or synapses.
 */
public class RandomPanelNetwork extends RandomizerPanel implements
        ActionListener {

    /** Null string. */
    private String nullString = AbstractNeuronPanel.NULL_STRING;

    /**
     * This method is the default constructor.
     *
     * @param useLocalBounds Should local bounds be used
     */
    public RandomPanelNetwork(final boolean useLocalBounds) {
        super(useLocalBounds);
    }

    @Override
    public void fillFieldValues(final ArrayList randomizers) {
        super.fillFieldValues(randomizers);

//        if (!NetworkUtils.isConsistent(randomizers, Randomizer.class,
//                "getClipping")) {
//            this.getTsClipping().setNull();
//        }

        if (!NetworkUtils.isConsistent(randomizers, Randomizer.class,
                "getLowerBound")) {
            this.getTfLowBound().setText(nullString);
        }

        if (!NetworkUtils.isConsistent(randomizers, Randomizer.class,
                "getUpperBound")) {
            this.getTfUpBound().setText(nullString);
        }

        if (!NetworkUtils.isConsistent(randomizers, Randomizer.class,
                "getStandardDeviation")) {
            this.getTfStandardDeviation().setText(nullString);
        }

        if (!NetworkUtils.isConsistent(randomizers, Randomizer.class,
                "getMean")) {
            this.getTfMean().setText(nullString);
        }
    }

    /**
     * Called externally when dialog is being closed.
     *
     * @param rand Random soruce
     */
    public void commitRandom(final Randomizer rand) {
        if (!this.getCbDistribution().getSelectedItem().equals(nullString)) {
            rand.setDistributionIndex(getCbDistribution().getSelectedIndex());
        }

        if (!this.getTfLowBound().getText().equals(nullString)) {
            rand.setLowerBound(Double.parseDouble(getTfLowBound().getText()));
        }

        if (!this.getTfUpBound().getText().equals(nullString)) {
            rand.setUpperBound(Double.parseDouble(getTfUpBound().getText()));
        }

        if (!this.getTfStandardDeviation().getText().equals(nullString)
                && this.getTfStandardDeviation().isEnabled()) {
            rand.setStandardDeviation(Double
                    .parseDouble(getTfStandardDeviation().getText()));
        }

        if (!this.getTfMean().getText().equals(nullString)
                && getTfMean().isEnabled()) {
            rand.setMean(Double.parseDouble(getTfMean().getText()));
        }

//        if (!(this.getTsClipping().getSelectedIndex() == TristateDropDown
//                .getNULL())) {
//            rand.setClipping(getTsClipping().isSelected());
//        }
        rand.setClipping(getTsClipping().isSelected());
        
    }

}
