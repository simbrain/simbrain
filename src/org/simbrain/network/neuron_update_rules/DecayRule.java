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

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ClippableUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;

/**
 * <b>DecayNeuron</b> implements various forms of standard decay.
 */
public class DecayRule extends NeuronUpdateRule implements BoundedUpdateRule, ClippableUpdateRule, NoisyUpdateRule {

    /**
     * The Default upper bound.
     */
    private static final double DEFAULT_CEILING = 1.0;

    /**
     * The Default lower bound.
     */
    private static final double DEFAULT_FLOOR = -1.0;

    /**
     * Relative.
     */
    public static final int RELATIVE = 0;

    /**
     * Absolute.
     */
    public static final int ABSOLUTE = 1;

    //TODO: add drop down back
    /**
     * Relative absolute.
     */
    private int relAbs = RELATIVE;

  //TODO: disable when RELATIVE
    /**
     * Decay amount.
     */
    @UserParameter(
            label = "Decay amount",
            description = "The amount by which the activation is changed each iteration if absolute decay is chosen.",
            defaultValue = "0.1", order = 3)
    private double decayAmount = .1;

    //TODO: disable when ABSOLUTE
    /**
     * Decay fraction.
     */
    @UserParameter(
            label = "Decay fraction",
            description = "The proportion of the distance between the current value and the base-line value, "
                    + "by which the activation is changed each iteration if relative decay is chosen.",
            defaultValue = "0.1", order = 4)
    private double decayFraction = .1;

    /**
     * Base line.
     */
    @UserParameter(
            label = "Base Line",
            description = "An option to add noise.",
            defaultValue = "0", order = 2)
    private double baseLine = 0;

    /**
     * Clipping.
     */
    private boolean clipping = true;

    /**
     * Noise generator.
     */
    private ProbabilityDistribution noiseGenerator = UniformDistribution.create();

    /**
     * Add noise to the neuron.
     */
    private boolean addNoise = false;

    /**
     * The upper bound of the activity if clipping is used.
     */
    private double ceiling = DEFAULT_CEILING;

    /**
     * The lower bound of the activity if clipping is used.
     */
    private double floor = DEFAULT_FLOOR;

    @Override
    public void update(Neuron neuron) {
        double val = neuron.getActivation() + neuron.getInput();
        double decayVal = 0;

        if (relAbs == RELATIVE) {
            decayVal = decayFraction * Math.abs(val - baseLine);
        } else if (relAbs == ABSOLUTE) {
            decayVal = decayAmount;
        }

        // Here's where the action happens
        if (val < baseLine) {
            val += decayVal;

            // in case of an overshoot
            if (val > baseLine) {
                val = baseLine;
            }
        } else if (val > baseLine) {
            val -= decayVal;

            // in case of an overshoot
            if (val < baseLine) {
                val = baseLine;
            }
        }

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        if (clipping) {
            val = clip(val);
        }

        neuron.setBuffer(val);
    }


    @Override
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    @Override
    public DecayRule deepCopy() {
        DecayRule dn = new DecayRule();
        dn.setRelAbs(getRelAbs());
        dn.setDecayAmount(getDecayAmount());
        dn.setDecayFraction(getDecayFraction());
        dn.setClipped(isClipped());
        dn.setUpperBound(getUpperBound());
        dn.setLowerBound(getLowerBound());
        dn.setAddNoise(getAddNoise());
        dn.noiseGenerator = noiseGenerator.deepCopy();
        return dn;
    }

    @Override
    public double clip(double val) {
        if (val > getUpperBound()) {
            return getUpperBound();
        } else if (val < getLowerBound()) {
            return getLowerBound();
        } else {
            return val;
        }
    }

    @Override
    public void contextualIncrement(Neuron n) {
        double act = n.getActivation();
        if (act >= getUpperBound() && isClipped()) {
            return;
        } else {
            if (isClipped()) {
                act = clip(act + n.getIncrement());
            } else {
                act = act + n.getIncrement();
            }
            n.setActivation(act);
            n.getNetwork().fireNeuronChanged(n);
        }
    }

    @Override
    public void contextualDecrement(Neuron n) {
        double act = n.getActivation();
        if (act <= getLowerBound() && isClipped()) {
            return;
        } else {
            if (isClipped()) {
                act = clip(act - n.getIncrement());
            } else {
                act = act - n.getIncrement();
            }
            n.setActivation(act);
            n.getNetwork().fireNeuronChanged(n);
        }
    }

    /**
     * @return Returns the decayAmount.
     */
    public double getDecayAmount() {
        return decayAmount;
    }

    /**
     * @param decayAmount The decayAmount to set.
     */
    public void setDecayAmount(final double decayAmount) {
        this.decayAmount = decayAmount;
    }

    /**
     * @return Returns the dedayPercentage.
     */
    public double getDecayFraction() {
        return decayFraction;
    }

    /**
     * @param decayFraction The decayFraction to set.
     */
    public void setDecayFraction(final double decayFraction) {
        this.decayFraction = decayFraction;
    }

    /**
     * @return Returns the relAbs.
     */
    public int getRelAbs() {
        return relAbs;
    }

    /**
     * @param relAbs The relAbs to set.
     */
    public void setRelAbs(final int relAbs) {
        this.relAbs = relAbs;
    }

    @Override
    public boolean getAddNoise() {
        return addNoise;
    }

    @Override
    public void setAddNoise(final boolean addNoise) {
        this.addNoise = addNoise;
    }

    @Override
    public ProbabilityDistribution getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(final ProbabilityDistribution noise) {
        this.noiseGenerator = noise;
    }

    /**
     * @return Returns the baseLine.
     */
    public double getBaseLine() {
        return baseLine;
    }

    /**
     * @param baseLine The baseLine to set.
     */
    public void setBaseLine(final double baseLine) {
        this.baseLine = baseLine;
    }

    @Override
    public String getName() {
        return "Decay";
    }

    @Override
    public double getUpperBound() {
        return ceiling;
    }

    @Override
    public double getLowerBound() {
        return floor;
    }

    @Override
    public void setUpperBound(double ceiling) {
        this.ceiling = ceiling;
    }

    @Override
    public void setLowerBound(double floor) {
        this.floor = floor;
    }

    @Override
    public boolean isClipped() {
        return clipping;
    }

    @Override
    public void setClipped(boolean clipping) {
        this.clipping = clipping;
    }

}
