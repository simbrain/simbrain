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
package org.simbrain.network.synapse_update_rules;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.util.UserParameter;

/**
 * <b>OjaSynapse</b> is a synapse which asymptotically normalizes the sum of
 * squares of the weights attaching to a neuron to a user-defined value.
 */
public class OjaRule extends SynapseUpdateRule {

    /**
     * Learning rate.
     */
    @UserParameter(label = "Learning rate", description = "Learning rate for Oja rule", increment = .1, order = 1)
    private double learningRate;

    // TODO: check description
    /**
     * Normalization factor.
     */
    @UserParameter(label = "Normalize to", description = "Normalization factor for Oja rule", increment = .1, order = 1)
    private double normalizationFactor = 1;

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getName() {
        return "Oja";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        OjaRule os = new OjaRule();
        os.setNormalizationFactor(this.getNormalizationFactor());
        os.setLearningRate(getLearningRate());
        return os;
    }

    @Override
    public void apply(Synapse synapse, ScalarDataHolder data) {

        double input = synapse.getSource().getActivation();
        double output = synapse.getTarget().getActivation();

        double strength = synapse.getStrength() + (learningRate * ((input * output) - ((output * output * synapse.getStrength()) / normalizationFactor)));
        synapse.setStrength(synapse.clip(strength));
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(final double momentum) {
        this.learningRate = momentum;
    }

    public double getNormalizationFactor() {
        return normalizationFactor;
    }

    public void setNormalizationFactor(final double normalizationFactor) {
        this.normalizationFactor = normalizationFactor;
    }

}
