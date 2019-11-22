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

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.*;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.math.SquashingFunctionEnum;

/**
 * An abstract superclass for discrete and continuous time sigmodial squashing
 * function based update rules containing methods and variables common to both.
 *
 * @author Zoë Tosi
 */
public abstract class AbstractSigmoidalRule extends NeuronUpdateRule implements BiasedUpdateRule, DifferentiableUpdateRule, InvertibleUpdateRule, BoundedUpdateRule, NoisyUpdateRule {

    /**
     * The default squashing function, informs the default upper and lower bounds.
     */
    public static final SquashingFunctionEnum DEFAULT_SQUASHING_FUNCTION = SquashingFunctionEnum.ARCTAN;

    /**
     * The Default upper bound.
     */
    public static final double DEFAULT_UPPER_BOUND = DEFAULT_SQUASHING_FUNCTION.getDefaultUpperBound();

    /**
     * The Default lower bound.
     */
    public static final double DEFAULT_LOWER_BOUND = DEFAULT_SQUASHING_FUNCTION.getDefaultLowerBound();

    /**
     * Current implementation.
     */
    protected SquashingFunctionEnum sFunction;

    /**
     * See {@link BiasedUpdateRule}. In a sigmoidal node, shifts the inflection point to the left or right of the origin.
     */
    protected double bias;

    /**
     * Slope.
     */
    @UserParameter(
            label = "Slope",
            description = "This represents how steep the sigmoidal is.",
            increment=.1, order = 3)
    protected double slope = 1;

    /**
     * Noise generator.
     */
    protected ProbabilityDistribution noiseGenerator = UniformDistribution.create();

    /**
     * Adds noise to neuron.
     */
    @UserParameter(
            label = "Add noise",
            description = "If this is set to true, random values are added to the activation via "
                    + "a noise generator.",
             order = 4)
    protected boolean addNoise;

    /**
     * The upper bound of the activity if clipping is used.
     */
    protected double upperBound = DEFAULT_UPPER_BOUND;

    /**
     * The lower bound of the activity if clipping is used.
     */
    protected double lowerBound = DEFAULT_LOWER_BOUND;

    public AbstractSigmoidalRule() {
        super();
        sFunction = DEFAULT_SQUASHING_FUNCTION;
    }

    /**
     * @param sFunction
     */
    public AbstractSigmoidalRule(SquashingFunctionEnum sFunction) {
        super();
        this.sFunction = sFunction;
        setUpperBound(sFunction.getDefaultUpperBound());
        setLowerBound(sFunction.getDefaultLowerBound());
    }

    public SquashingFunctionEnum getSquashFunctionType() {
        if (sFunction == null) {
            sFunction = SquashingFunctionEnum.LOGISTIC; // TODO: Explain (backwards compat)
        }
        return sFunction;
    }

    public final void setSquashFunctionType(SquashingFunctionEnum type) {
        this.sFunction = type;
        setUpperBound(type.getDefaultUpperBound());
        setLowerBound(type.getDefaultLowerBound());
    }

    public final int getSquashFunctionInt() {
        return sFunction.ordinal();
    }

    public void setSquashFunctionInt(Integer typeIndex) {
        this.sFunction = SquashingFunctionEnum.values()[typeIndex];
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(final double inflectionPointSlope) {
        this.slope = inflectionPointSlope;
    }

    @Override
    public ProbabilityDistribution getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(final ProbabilityDistribution noise) {
        this.noiseGenerator = noise;
    }

    public boolean getAddNoise() {
        return addNoise;
    }

    public void setAddNoise(final boolean addNoise) {
        this.addNoise = addNoise;
    }

    /**
     * Copy the overlapping bits of the rule for subclasses.
     *
     * @param sr the sigmoid rule to copy
     * @return the copy.
     */
    protected AbstractSigmoidalRule baseDeepCopy(AbstractSigmoidalRule sr) {
        sr.setBias(getBias());
        sr.setSquashFunctionType(getSquashFunctionType());
        sr.setSlope(getSlope());
        sr.setAddNoise(getAddNoise());
        sr.setLowerBound(getLowerBound());
        sr.setUpperBound(getUpperBound());
        sr.noiseGenerator = noiseGenerator.deepCopy();
        return sr;
    }

    @Override
    public final double getUpperBound() {
        return upperBound;
    }

    @Override
    public final double getLowerBound() {
        return lowerBound;
    }

    @Override
    public final void setUpperBound(final double ceiling) {
        this.upperBound = ceiling;
    }

    @Override
    public final void setLowerBound(final double floor) {
        this.lowerBound = floor;
    }

    @Override
    public final double getInverse(final double val) {
        double up = getUpperBound();
        double lw = getLowerBound();
        double diff = up - lw;
        return sFunction.inverseVal(val, up, lw, diff);
    }

    @Override
    public final double getBias() {
        return bias;
    }

    @Override
    public final void setBias(final double inflectionY) {
        this.bias = inflectionY;
    }

}
