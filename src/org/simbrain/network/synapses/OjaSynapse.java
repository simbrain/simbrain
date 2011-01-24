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
package org.simbrain.network.synapses;

import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.interfaces.SynapseUpdateRule;

/**
 * <b>OjaSynapse</b> is a synapse which asymptotically normalizes the sum of
 * squares of the weights attaching to a neuron to a user-defined value.
 */
public class OjaSynapse extends SynapseUpdateRule {

    /** Learning rate. */
    public static final double DEFAULT_LEARNING_RATE = .1;
    /** Normalization factor. */

    public static final double DEFAULT_NORMALIZATION_FACTOR = 1;

    /** Learning rate. */
    private double learningRate = DEFAULT_LEARNING_RATE;

    /** Normalization factor. */
    private double normalizationFactor = DEFAULT_NORMALIZATION_FACTOR;

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getDescription() {
        return "Oja";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        OjaSynapse os = new OjaSynapse();
        os.setNormalizationFactor(this.getNormalizationFactor());
        os.setLearningRate(getLearningRate());
        return os;
    }

    @Override
    public void update(Synapse synapse) {
        double input = synapse.getSource().getActivation();
        double output = synapse.getTarget().getActivation();

        double strength = synapse.getStrength()
                + (learningRate * ((input * output) - ((output * output * synapse
                        .getStrength()) / normalizationFactor)));
        synapse.setStrength(synapse.clip(strength));
    }

    /**
     * @return Returns the momentum.
     */
    public double getLearningRate() {
        return learningRate;
    }

    /**
     * @param momentum The momentum to set.
     */
    public void setLearningRate(final double momentum) {
        this.learningRate = momentum;
    }

    /**
     * @return Returns the normalizationFactor.
     */
    public double getNormalizationFactor() {
        return normalizationFactor;
    }

    /**
     * @param normalizationFactor The normalizationFactor to set.
     */
    public void setNormalizationFactor(final double normalizationFactor) {
        this.normalizationFactor = normalizationFactor;
    }

}
