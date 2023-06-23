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

import org.simbrain.network.core.Layer;
import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.DifferentiableUpdateRule;
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule;
import org.simbrain.network.updaterules.interfaces.ClippableUpdateRule;
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule;
import org.simbrain.network.util.BiasedMatrixData;
import org.simbrain.network.util.BiasedScalarData;
import org.simbrain.util.UserParameter;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.UniformRealDistribution;

/**
 * <b>LinearNeuron</b> is a standard linear neuron.
 */
public class LinearRule extends NeuronUpdateRule<BiasedScalarData, BiasedMatrixData> implements DifferentiableUpdateRule,
        BoundedUpdateRule, ClippableUpdateRule, NoisyUpdateRule {

    /**
     * The Default upper bound.
     */
    private static final double DEFAULT_UPPER_BOUND = 10.0;

    /**
     * The Default lower bound.
     */
    private static final double DEFAULT_LOWER_BOUND = -10.0;

    /**
     * Default clipping setting.
     */
    private static final boolean DEFAULT_CLIPPING = true;

    /**
     * Slope.
     */
    @UserParameter(
            label = "Slope",
            description = "Slope of linear rule",
            increment = .1)
    private double slope = 1;

    /**
     * Noise generator.
     */
    private ProbabilityDistribution noiseGenerator = new UniformRealDistribution();

    /**
     * Add noise to the neuron.
     */
    private boolean addNoise = false;

    /**
     * Clipping.
     */
    private boolean clipping = DEFAULT_CLIPPING;

    /**
     * The upper bound of the activity if clipping is used.
     */
    private double upperBound = DEFAULT_UPPER_BOUND;

    /**
     * The lower bound of the activity if clipping is used.
     */
    private double lowerBound = DEFAULT_LOWER_BOUND;

    @Override
    public void apply(Layer array, BiasedMatrixData data) {
        for (int i = 0; i < array.getOutputs().nrow() ; i++) {
            array.getOutputs().set(i, 0, linearRule(array.getInputs().get(i, 0), data.getBiases().get(i, 0)));
        }
    }

    @Override
    public void apply(Neuron neuron, BiasedScalarData data) {
        neuron.setActivation(linearRule(neuron.getInput(), data.getBias()));
    }

    public double linearRule(double input, double bias) {
        double ret = input * slope + bias;
        if (addNoise) {
            ret  += noiseGenerator.sampleDouble();
        }
        if (clipping) {
            ret  = clip(ret);
        }
        return ret;
    }

    @Override
    public BiasedMatrixData createMatrixData(int size) {
        return new BiasedMatrixData(size);
    }

    @Override
    public BiasedScalarData createScalarData() {
        return new BiasedScalarData();
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
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    @Override
    public LinearRule deepCopy() {
        LinearRule ln = new LinearRule();
        ln.setSlope(getSlope());
        ln.setClipped(isClipped());
        ln.setAddNoise(getAddNoise());
        ln.setUpperBound(getUpperBound());
        ln.setLowerBound(getLowerBound());
        ln.noiseGenerator = noiseGenerator.deepCopy();
        return ln;
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
            n.forceSetActivation(act);
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
            n.forceSetActivation(act);
        }
    }

    @Override
    public double getDerivative(double val) {
        if (val >= getUpperBound()) {
            return 0;
        } else if (val <= getLowerBound()) {
            return 0;
        } else {
            return slope;
        }
    }

    public void setSlope(final double slope) {
        this.slope = slope;
    }

    public double getSlope() {
        return slope;
    }

    @Override
    public ProbabilityDistribution getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(final ProbabilityDistribution noise) {
        this.noiseGenerator = noise;
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
    public String getName() {
        return "Linear";
    }

    @Override
    public double getUpperBound() {
        return upperBound;
    }

    @Override
    public double getLowerBound() {
        return lowerBound;
    }

    @Override
    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    @Override
    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
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
