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
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.DifferentiableUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.InvertibleUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.math.SquashingFunction;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>SigmoidalNeuron</b> provides various implementations of a standard
 * sigmoidal neuron.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class SigmoidalRule extends NeuronUpdateRule implements
        BiasedUpdateRule, DifferentiableUpdateRule, InvertibleUpdateRule,
        BoundedUpdateRule, NoisyUpdateRule {

    /** The Default upper bound. */
    private static final double DEFAULT_UPPER_BOUND = 1.0;

    /** The Default lower bound. */
    private static final double DEFAULT_LOWER_BOUND = 0.0;

    /** Current implementation. */
    private SquashingFunction sFunction = SquashingFunction.LOGISTIC;

    /** Bias. */
    private double bias;

    /** Slope. */
    private double slope = 1;

    /** Noise dialog. */
    private Randomizer noiseGenerator = new Randomizer();

    /** Adds noise to neuron. */
    private boolean addNoise;

    /** The upper bound of the activity if clipping is used. */
    private double upperBound = DEFAULT_UPPER_BOUND;

    /** The lower bound of the activity if clipping is used. */
    private double lowerBound = DEFAULT_LOWER_BOUND;

    /**
     * Default sigmoidal.
     */
    public SigmoidalRule() {
        super();
    }

    /**
     * Construct a sigmoid update with a specified implementation.
     *
     * @param sFunction the squashing function implementation to use.
     */
    public SigmoidalRule(SquashingFunction sFunction) {
        super();
        this.sFunction = sFunction;
        setUpperBound(sFunction.getDefaultUpperBound());
        setLowerBound(sFunction.getDefaultLowerBound());
    }

    /**
     * {@inheritDoc}
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {

        double val = neuron.getWeightedInputs() + bias;

        val = sFunction.valueOf(val, getUpperBound(), getLowerBound(), getSlope());

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        neuron.setBuffer(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextualIncrement(Neuron n) {
        double act = n.getActivation();
        if (act < getUpperBound()) {
            act += getIncrement();
            if (act > getUpperBound()) {
                act = getUpperBound();
            }
            n.setActivation(act);
            n.getNetwork().fireNeuronChanged(n);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextualDecrement(Neuron n) {
        double act = n.getActivation();
        if (act > getLowerBound()) {
            act -= getIncrement();
            if (act < getLowerBound()) {
                act = getLowerBound();
            }
            n.setActivation(act);
            n.getNetwork().fireNeuronChanged(n);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDerivative(final double val) {
        double up = getUpperBound();
        double lw = getLowerBound();
        double diff = up - lw;
        return sFunction.derivVal(val, up, lw, diff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getInverse(double val) {
        double up = getUpperBound();
        double lw = getLowerBound();
        double diff = up - lw;
        return sFunction.inverseVal(val, up, lw, diff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SigmoidalRule deepCopy() {
        SigmoidalRule sn = new SigmoidalRule();
        sn.setBias(getBias());
        sn.setSquashFunctionType(getSquashFunctionType());
        sn.setSlope(getSlope());
        sn.setAddNoise(getAddNoise());
        sn.noiseGenerator = new Randomizer(noiseGenerator);
        return sn;
    }

    /**
     * @return Returns the inflectionPoint.
     */
    @Override
    public double getBias() {
        return bias;
    }

    /**
     * @param inflectionY The inflectionY to set.
     */
    @Override
    public void setBias(final double inflectionY) {
        this.bias = inflectionY;
    }

    /**
     * @return Returns the inflectionPointSlope.
     */
    public double getSlope() {
        return slope;
    }

    /**
     * @param inflectionPointSlope The inflectionPointSlope to set.
     */
    public void setSlope(final double inflectionPointSlope) {
        this.slope = inflectionPointSlope;
    }

    /**
     * @return Returns the noise.
     */
    public Randomizer getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noise The noise to set.
     */
    public void setNoiseGenerator(final Randomizer noise) {
        this.noiseGenerator = noise;
    }

    /**
     * @return Returns the addNoise.
     */
    public boolean getAddNoise() {
        return addNoise;
    }

    /**
     * @param addNoise The addNoise to set.
     */
    public void setAddNoise(final boolean addNoise) {
        this.addNoise = addNoise;
    }

    @Override
    public String getDescription() {
        return "Sigmoidal (Discrete)";
    }

    /**
     * @return the type
     */
    public SquashingFunction getSquashFunctionType() {
        if (sFunction == null) {
            sFunction = SquashingFunction.LOGISTIC; // TODO: Explain (backwards
            // compat)
        }
        return sFunction;
    }

    /**
     * @param type the type to set
     */
    public void setSquashFunctionType(SquashingFunction type) {
        this.sFunction = type;
        setUpperBound(type.getDefaultUpperBound());
        setLowerBound(type.getDefaultLowerBound());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getUpperBound() {
        return upperBound;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUpperBound(double ceiling) {
        this.upperBound = ceiling;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLowerBound(double floor) {
        this.lowerBound = floor;
    }

}
