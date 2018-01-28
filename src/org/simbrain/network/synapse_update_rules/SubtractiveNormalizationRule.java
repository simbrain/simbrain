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
import org.simbrain.util.UserParameter;

/**
 * <b>SubtractiveNormalizationSynapse</b>.
 */
public class SubtractiveNormalizationRule extends SynapseUpdateRule {

    //TODO: check description
    /**
     * Momentum.
     */
    @UserParameter(label = "Learning rate", description = "Momentum", defaultValue = "1", order = 1)
    private double learningRate;

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getName() {
        return "Subtractive Normalization";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        SubtractiveNormalizationRule sns = new SubtractiveNormalizationRule();
        sns.setLearningRate(getLearningRate());
        return sns;
    }

    @Override
    public void update(Synapse synapse) {
        double input = synapse.getSource().getActivation();
        double output = synapse.getTarget().getActivation();
        double averageInput = synapse.getTarget().getAverageInput();
        double strength = synapse.getStrength() + ((learningRate * output * input) - (learningRate * output * averageInput));
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
}
