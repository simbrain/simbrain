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
import org.simbrain.network.neuron_update_rules.interfaces.DifferentiableUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.InvertibleUpdateRule;
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule;
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule;
import org.simbrain.network.util.BiasedMatrixData;
import org.simbrain.network.util.BiasedScalarData;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.SigmoidFunctionEnum;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.UniformRealDistribution;

/**
 * An abstract superclass for discrete and continuous time sigmodial squashing
 * function based update rules containing methods and variables common to both.
 *
 * @author Zoë Tosi
 */
public abstract class AbstractSigmoidalRule extends NeuronUpdateRule<BiasedScalarData, BiasedMatrixData> implements DifferentiableUpdateRule, InvertibleUpdateRule, BoundedUpdateRule, NoisyUpdateRule {

    /**
     * The default squashing function, informs the default upper and lower bounds.
     */
    public static final SigmoidFunctionEnum DEFAULT_SIGMOID_TYPE = SigmoidFunctionEnum.LOGISTIC;

    @UserParameter(label = "Implementation", order = -10)
    protected SigmoidFunctionEnum sFunction = DEFAULT_SIGMOID_TYPE;

    @UserParameter(
            label = "Slope",
            description = "This represents how steep the sigmoidal is.",
            increment=.1, order = 3)
    protected double slope = 1;

    /**
     * Noise generator.
     */
    protected ProbabilityDistribution noiseGenerator = new UniformRealDistribution();

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
    protected double upperBound = sFunction.getDefaultUpperBound();

    /**
     * The lower bound of the activity if clipping is used.
     */
    protected double lowerBound = sFunction.getDefaultLowerBound();

    /**
     * Bounded update rule is automatically clippable.  It is not needed here since sigmoids automatically respect
     * upper and lower bounds but can still be turned on to constrain contextual increment and decrement.
     */
    private boolean isClipped = false;

    public AbstractSigmoidalRule() {
    }

    @Override
    public BiasedScalarData createScalarData() {
        return new BiasedScalarData();
    }

    @Override
    public BiasedMatrixData createMatrixData(int size) {
        return new BiasedMatrixData(size);
    }

    public SigmoidFunctionEnum getType() {
        return sFunction;
    }

    public final void setType(SigmoidFunctionEnum type) {
        this.sFunction = type;
        setUpperBound(type.getDefaultUpperBound());
        setLowerBound(type.getDefaultLowerBound());
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
        sr.setType(getType());
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
    public boolean isClipped() {
        return isClipped;
    }

    @Override
    public void setClipped(boolean b) {
        isClipped = b;
    }
}
