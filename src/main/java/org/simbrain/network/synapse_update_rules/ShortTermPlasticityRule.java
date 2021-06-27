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

import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.util.UserParameter;

/**
 * <b>ShortTermPlasticitySynapse</b>.
 */
public class ShortTermPlasticityRule extends SynapseUpdateRule {

    // TODO: Enum

    /**
     * STD.
     */
    private static final int STD = 0;

    /**
     * Plasticity type.
     */
    public static final int DEFAULT_PLASTICITY_TYPE = STD;

    /**
     * Pseudo spike threshold.
     */
    public static final double DEFAULT_FIRING_THRESHOLD = 0;

    /**
     * Base line strength.
     */
    public static final double DEFAULT_BASE_LINE_STRENGTH = 1;

    /**
     * Input threshold.
     */
    public static final double DEFAULT_INPUT_THRESHOLD = 0;

    /**
     * Bump rate.
     */
    public static final double DEFAULT_BUMP_RATE = .5;

    /**
     * Rate at which the synapse will decay.
     */
    public static final double DEFAULT_DECAY_RATE = .2;

    /**
     * Activated.
     */
    public static final boolean DEFAULT_ACTIVATED = false;

    /**
     * Plasticity type.
     */
    @UserParameter(label = "Plasticity Type", description = "Plasticity Type", increment = 1.0, order = 1)
    private int plasticityType = STD;

    /**
     * Pseudo spike threshold.
     */
    @UserParameter(label = "Spike Threshold", description = "Pseudo Spike Threshold", increment = .1, order = 2)
    private double firingThreshold = DEFAULT_FIRING_THRESHOLD;

    /**
     * Base line strength.
     */
    @UserParameter(label = "Line Strength", description = "Base line strength", increment = .1, order = 3)
    private double baseLineStrength = DEFAULT_BASE_LINE_STRENGTH;

    /**
     * Input threshold.
     */
    @UserParameter(label = "Input Threshold", description = "Input threshold", increment = .1, order = 4)
    private double inputThreshold = DEFAULT_INPUT_THRESHOLD;

    /**
     * Bump rate.
     */
    @UserParameter(label = "Bump rate", description = "Bump Rate", increment = .1, order = 5)
    private double bumpRate = DEFAULT_BUMP_RATE;

    /**
     * Rate at which the synapse will decay.
     */
    @UserParameter(label = "Decay Rate", description = "Rate at which the synapse will decay", increment = .1, order = 6)
    private double decayRate = DEFAULT_DECAY_RATE;

    /**
     * Activated.
     */
    @UserParameter(label = "Activated", description = "Activated", increment = .1, order = 7)
    private boolean activated = DEFAULT_ACTIVATED;

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getName() {
        return "Short Term Plasticity";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        ShortTermPlasticityRule stp = new ShortTermPlasticityRule();
        stp.setBaseLineStrength(getBaseLineStrength());
        stp.setBumpRate(getBumpRate());
        stp.setDecayRate(getDecayRate());
        stp.setInputThreshold(getInputThreshold());
        stp.setPlasticityType(getPlasticityType());
        return stp;
    }

    @Override
    public void apply(Synapse synapse, ScalarDataHolder data) {

        // Determine whether to activate short term dynamics
        if (synapse.getSource().getUpdateRule() instanceof SpikingNeuronUpdateRule) {
            if (synapse.getSource().isSpike()) {
                activated = true;
            } else {
                activated = false;
            }
        } else {
            if (synapse.getSource().getActivation() > firingThreshold) {
                activated = true;
            } else {
                activated = false;
            }
        }
        double strength = synapse.getStrength();
        if (activated) {
            if (plasticityType == STD) {
                strength -= (bumpRate * (strength - synapse.getLowerBound()));
            } else {
                strength += (bumpRate * (synapse.getUpperBound() - strength));
            }
        } else {
            strength -= (decayRate * (strength - baseLineStrength));
        }

        synapse.setStrength(synapse.clip(strength));
    }

    public double getBaseLineStrength() {
        return baseLineStrength;
    }

    public void setBaseLineStrength(final double baseLineStrength) {
        this.baseLineStrength = baseLineStrength;
    }

    public double getDecayRate() {
        return decayRate;
    }

    public void setDecayRate(final double decayRate) {
        this.decayRate = decayRate;
    }

    public double getBumpRate() {
        return bumpRate;
    }

    public void setBumpRate(final double growthRate) {
        this.bumpRate = growthRate;
    }

    public double getInputThreshold() {
        return inputThreshold;
    }

    public void setInputThreshold(final double inputThreshold) {
        this.inputThreshold = inputThreshold;
    }

    public int getPlasticityType() {
        return plasticityType;
    }

    public void setPlasticityType(final int plasticityType) {
        this.plasticityType = plasticityType;
    }

    public double getFiringThreshold() {
        return firingThreshold;
    }

    public void setFiringThreshold(final double firingThreshold) {
        this.firingThreshold = firingThreshold;
    }

}
