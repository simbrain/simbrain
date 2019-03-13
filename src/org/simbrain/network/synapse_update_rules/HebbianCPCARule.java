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
 * <b>HebbianCPCA</b>. TODO: No Doc.
 */
public class HebbianCPCARule extends SynapseUpdateRule {

    /**
     * Default Maximum weight value (see equation 4.19 in O'Reilly and
     * Munakata).
     */
    public static final double DEFAULT_M = .5 / .15;

    /**
     * Learning rate.
     */
    @UserParameter(label = "Learning rate", description = "Learning rate for Hebb CPCA", minimumValue = 0, maximumValue = DEFAULT_M, increment = .1, order = 1)
    private double learningRate;

    /**
     * Max Weight Value.
     */
    @UserParameter(label = "m", description = "Max Weight", minimumValue = -10, maximumValue = 10, increment = .1, order = 1)
    private double m;

    /**
     * Weight offset.
     */
    @UserParameter(label = "Theta", description = "Weight Offset value", minimumValue = -10, maximumValue = 10, increment = .1, order = 1)
    private double theta;

    /**
     * Lambda.
     */
    @UserParameter(label = "Lambda", description = "Sigmomid Function", minimumValue = -1, maximumValue = 10, increment = .1, order = 1)
    private double lambda;

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getName() {
        return "Hebbian CPCA";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        HebbianCPCARule learningRule = new HebbianCPCARule();
        learningRule.setLearningRate(getLearningRate());
        learningRule.setM(getM());
        learningRule.setTheta(getTheta());
        learningRule.setLambda(getLambda());
        return learningRule;
    }

    @Override
    public void update(Synapse synapse) {
        // Updates the synapse (see equation 4.18 in O'Reilly and Munakata).

        double input = synapse.getSource().getActivation();
        double output = synapse.getTarget().getActivation();

        double deltaW = learningRate * ((output * input) - (output * synapse.getStrength())); // Equation
        // 4.12
        // deltaW = learningRate * (output * input * (m - strength) + output *
        // (1 - input) * (-strength));
        // strength = sigmoidal(strength);
        // strength = clip(strength + deltaW);
        synapse.setStrength(synapse.getStrength() + deltaW);
    }

    /**
     * Sigmoidal Function (see equation 4.23 in O'Reilly and Munakata).
     *
     * @param arg value to send to sigmoidal
     * @return value of sigmoidal
     */
    private double sigmoidal(final double arg) {
        return 1 / (1 + Math.pow(theta * (arg / (1 - arg)), -lambda));
    }

    public double getLearningRate() {
        return learningRate;
    }

    public double getM() {
        return m;
    }

    public double getTheta() {
        return theta;
    }

    public double getLambda() {
        return lambda;
    }

    public void setLearningRate(final double momentum) {
        this.learningRate = momentum;
    }

    public void setM(final double m) {
        this.m = m;
    }

    public void setTheta(final double theta) {
        this.theta = theta;
    }

    public void setLambda(final double lambda) {
        this.lambda = lambda;
    }

}
