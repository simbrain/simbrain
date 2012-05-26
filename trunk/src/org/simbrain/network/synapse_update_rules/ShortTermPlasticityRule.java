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


/**
 * <b>ShortTermPlasticitySynapse</b>.
 */
public class ShortTermPlasticityRule extends SynapseUpdateRule {

    //TODO: Enum

    /** STD. */
    private static final int STD = 0;

    /** Plasticity type. */
    public static final int DEFAULT_PLASTICITY_TYPE = STD;

    /** Pseudo spike threshold. */
    public static final double DEFAULT_FIRING_THRESHOLD = 0;

    /** Base line strength. */
    public static final double DEFAULT_BASE_LINE_STRENGTH = 1;

    /** Input threshold. */
    public static final double DEFAULT_INPUT_THRESHOLD = 0;

    /** Bump rate. */
    public static final double DEFAULT_BUMP_RATE = .5;

    /** Rate at which the synapse will decay. */
    public static final double DEFAULT_DECAY_RATE = .2;

    /** Activated. */
    public static final boolean DEFAULT_ACTIVATED = false;

    /** Plasticity type. */
    private int plasticityType = STD;

    /** Pseudo spike threshold. */
    private double firingThreshold = DEFAULT_FIRING_THRESHOLD;

    /** Base line strength. */
    private double baseLineStrength = DEFAULT_BASE_LINE_STRENGTH;

    /** Input threshold. */
    private double inputThreshold = DEFAULT_INPUT_THRESHOLD;

    /** Bump rate. */
    private double bumpRate = DEFAULT_BUMP_RATE;

    /** Rate at which the synapse will decay. */
    private double decayRate = DEFAULT_DECAY_RATE;

    /** Activated. */
    private boolean activated = DEFAULT_ACTIVATED;

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getDescription() {
        return "Short term plasticity";
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
    public void update(Synapse synapse) {
        // Determine whether to activate short term dynamics
        if (synapse.getSource().getUpdateRule() instanceof SpikingNeuronUpdateRule) {
            if (((SpikingNeuronUpdateRule) synapse.getSource().getUpdateRule()).hasSpiked()) {
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

    /**
     * @return Returns the baseLineStrength.
     */
    public double getBaseLineStrength() {
        return baseLineStrength;
    }

    /**
     * @param baseLineStrength The baseLineStrength to set.
     */
    public void setBaseLineStrength(final double baseLineStrength) {
        this.baseLineStrength = baseLineStrength;
    }

    /**
     * @return Returns the decayRate.
     */
    public double getDecayRate() {
        return decayRate;
    }

    /**
     * @param decayRate The decayRate to set.
     */
    public void setDecayRate(final double decayRate) {
        this.decayRate = decayRate;
    }

    /**
     * @return Returns the growthRate.
     */
    public double getBumpRate() {
        return bumpRate;
    }

    /**
     * @param growthRate The growthRate to set.
     */
    public void setBumpRate(final double growthRate) {
        this.bumpRate = growthRate;
    }

    /**
     * @return Returns the inputThreshold.
     */
    public double getInputThreshold() {
        return inputThreshold;
    }

    /**
     * @param inputThreshold The inputThreshold to set.
     */
    public void setInputThreshold(final double inputThreshold) {
        this.inputThreshold = inputThreshold;
    }

    /**
     * @return Returns the plasticityType.
     */
    public int getPlasticityType() {
        return plasticityType;
    }

    /**
     * @param plasticityType The plasticityType to set.
     */
    public void setPlasticityType(final int plasticityType) {
        this.plasticityType = plasticityType;
    }

    /**
     * @return the firing threshold.
     */
    public double getFiringThreshold() {
        return firingThreshold;
    }

    /**
     * @param firingThreshold The firingThreshold to set.
     */
    public void setFiringThreshold(final double firingThreshold) {
        this.firingThreshold = firingThreshold;
    }
}
