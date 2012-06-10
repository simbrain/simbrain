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

/**
 * <b>HebbianThresholdSynapse</b>.
 */
public class HebbianThresholdRule extends SynapseUpdateRule {

    /** Learning rate. */
    public static final double DEFAULT_LEARNING_RATE = .1;

    /** Output threshold momentum. */
    public static final double DEFAULT_OUTPUT_THRESHOLD_MOMENTUM = .1;

    /** Output threshold. */
    public static final double DEFAULT_OUTPUT_THRESHOLD = .5;

    /** Use sliding output threshold. */
    public static final boolean DEFAULT_USE_SLIDING_OUTPUT_THRESHOLD = false;

    /** Learning rate. */
    private double learningRate = .1;

    /** Output threshold momentum. */
    private double outputThresholdMomentum = .1;

    /** Output threshold. */
    private double outputThreshold = .5;

    /** Use sliding output threshold. */
    private boolean useSlidingOutputThreshold = false;

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getDescription() {
        return "Hebbian threshold";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        HebbianThresholdRule h = new HebbianThresholdRule();
        h.setLearningRate(getLearningRate());
        h.setOutputThreshold(this.getOutputThreshold());
        h.setOutputThresholdMomentum(this.getOutputThresholdMomentum());
        h.setUseSlidingOutputThreshold(this.getUseSlidingOutputThreshold());
        return h;
    }

    @Override
    public void update(Synapse synapse) {
        double input = synapse.getSource().getActivation();
        double output = synapse.getTarget().getActivation();

        if (useSlidingOutputThreshold) {
            outputThreshold += (outputThresholdMomentum * ((output * output) - outputThreshold));
        }
        double strength = synapse.getStrength()
                + (learningRate * input * output * (output - outputThreshold));
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
     * @return Returns the outputThreshold.
     */
    public double getOutputThreshold() {
        return outputThreshold;
    }

    /**
     * @param outputThreshold The outputThreshold to set.
     */
    public void setOutputThreshold(final double outputThreshold) {
        this.outputThreshold = outputThreshold;
    }

    /**
     * @return Returns the useSlidingOutputThreshold.
     */
    public boolean getUseSlidingOutputThreshold() {
        return useSlidingOutputThreshold;
    }

    /**
     * @param useSlidingOutputThreshold The useSlidingOutputThreshold to set.
     */
    public void setUseSlidingOutputThreshold(
            final boolean useSlidingOutputThreshold) {
        this.useSlidingOutputThreshold = useSlidingOutputThreshold;
    }

    /**
     * @return Returns the outputThresholdMomentum.
     */
    public double getOutputThresholdMomentum() {
        return outputThresholdMomentum;
    }

    /**
     * @param outputThresholdMomentum The outputThresholdMomentum to set.
     */
    public void setOutputThresholdMomentum(final double outputThresholdMomentum) {
        this.outputThresholdMomentum = outputThresholdMomentum;
    }
}
