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
package org.simbrain.network.neurons;

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.SpikingNeuronUpdateRule;

/**
 * A simple spiking neuron that fires when weighted inputs exceed a threshold.
 */
public class SpikingThresholdNeuron extends SpikingNeuronUpdateRule {

    /** Threshold. */
    private double threshold = .5;

//    /**
//     * @return duplicate ProbabilisticSpiking (used, e.g., in copy/paste).
//     */
//    public SpikingThresholdNeuron duplicate() {
//        SpikingThresholdNeuron ifn = new SpikingThresholdNeuron();
//        ifn = (SpikingThresholdNeuron) super.duplicate(ifn);
//        ifn.setThreshold(getThreshold());
//
//        return ifn;
//    }

    @Override
    public void init(Neuron neuron) {
        super.init(neuron);
        neuron.setLowerBound(0);
    }

    @Override
    public void update(Neuron neuron) {

        if (neuron.getWeightedInputs() >= threshold) {
            setHasSpiked(true);
            neuron.setBuffer(neuron.getUpperBound());
        } else {
            setHasSpiked(false);
            neuron.setBuffer(0); // Make this a separate variable?
        }

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
    public String getName() {
        return "Spiking threshold";
    }

}
