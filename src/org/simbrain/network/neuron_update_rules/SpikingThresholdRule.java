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
package org.simbrain.network.neuron_update_rules;

import java.util.Random;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.SpikingNeuronUpdateRule;

/**
 * A simple spiking neuron that fires when weighted inputs exceed a threshold.
 * TODO: Has no documentation.
 */
public class SpikingThresholdRule extends SpikingNeuronUpdateRule {

    /** Threshold. */
    private double threshold = .5;

    @Override
    public SpikingThresholdRule deepCopy() {
        SpikingThresholdRule neuron = new SpikingThresholdRule();
        neuron.setThreshold(getThreshold());
        return neuron;
    }

    @Override
    public void update(Neuron neuron) {

        if (inputType.getInput(neuron) >= threshold) {
            setHasSpiked(true, neuron);
            neuron.setBuffer(1);
        } else {
            setHasSpiked(false, neuron);
            neuron.setBuffer(0); // Make this a separate variable?
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRandomValue() {
        Random rand = new Random();
        return rand.nextBoolean() ? 1 : 0;
    }

    /**
     * @return the threshold
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * @param threshold the threshold to set
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public String getDescription() {
        return "Spiking Threshold";
    }

}
