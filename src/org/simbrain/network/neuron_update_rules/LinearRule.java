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

import java.util.Arrays;
import java.util.List;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ClippableUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.DifferentiableUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.ParameterEditor;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>LinearNeuron</b> is a standard linear neuron.
 */
public class LinearRule extends NeuronUpdateRule implements BiasedUpdateRule,
    DifferentiableUpdateRule, BoundedUpdateRule, ClippableUpdateRule,
    NoisyUpdateRule {

    /** The Default upper bound. */
    private static final double DEFAULT_UPPER_BOUND = 1.0;

    /** The Default lower bound. */
    private static final double DEFAULT_LOWER_BOUND = -1.0;

    /** Default clipping setting. */
    private static final boolean DEFAULT_CLIPPING = true;

    /** Slope. */
    public double slope = 1;

    /** Bias. */
    public double bias = 0;

    /** Noise generator. */
    private Randomizer noiseGenerator = new Randomizer();

    /** Add noise to the neuron. */
    private boolean addNoise = false;

    /** Clipping. */
    private boolean clipping = DEFAULT_CLIPPING;

    /** The upper bound of the activity if clipping is used. */
    private double upperBound = DEFAULT_UPPER_BOUND;

    /** The lower bound of the activity if clipping is used. */
    private double lowerBound = DEFAULT_LOWER_BOUND;

    /**
     * {@inheritDoc}
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    /**
     * {@inheritDoc}
     */
    public LinearRule deepCopy() {
        LinearRule ln = new LinearRule();
        ln.setBias(getBias());
        ln.setSlope(getSlope());
        ln.setClipped(isClipped());
        ln.setAddNoise(getAddNoise());
        ln.setUpperBound(getUpperBound());
        ln.setLowerBound(getLowerBound());
        ln.noiseGenerator = new Randomizer(noiseGenerator);
        return ln;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {
        double wtdInput = inputType.getInput(neuron);
        double val = (slope * wtdInput) + bias;

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        if (clipping) {
            val = clip(val);
        }

        neuron.setBuffer(val);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextualIncrement(Neuron n) {
        double act = n.getActivation();
        if (act >= getUpperBound() && isClipped()) {
            return;
        } else {
            if (isClipped()) {
                act = clip(act + increment);
            } else {
                act = act + increment;
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
        if (act <= getLowerBound() && isClipped()) {
            return;
        } else {
            if (isClipped()) {
                act = clip(act - increment);
            } else {
                act = act - increment;
            }
            n.setActivation(act);
            n.getNetwork().fireNeuronChanged(n);
        }
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * @return Returns the bias.
     */
    public double getBias() {
        return bias;
    }

    /**
     * @param bias The bias to set.
     */
    public void setBias(final double bias) {
        this.bias = bias;
    }

    /**
     * @return Returns the slope.
     */
    public double getSlope() {
        return slope;
    }

    /**
     * @param slope The slope to set.
     */
    public void setSlope(final double slope) {
        this.slope = slope;
    }

    /**
     * @return Returns the noise generator.
     */
    public Randomizer getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noise The noise generator to set.
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

    // TODO: This can now be removed.  Put this on the panel side
    /**
     * List of property editors for use by neuron property dialogs.
     */
    public static List<ParameterEditor> editorList = Arrays.asList(
            new ParameterEditor<NeuronUpdateRule, Double>(Double.class, "slope",
                    (r) -> ((LinearRule) r).getSlope(),
                    (r, val) -> ((LinearRule) r).setSlope((double) val)),
            new ParameterEditor<NeuronUpdateRule, Double>(Double.class, "bias",
                    (r) -> ((LinearRule) r).getBias(),
                    (r, val) -> ((LinearRule) r).setBias((double) val)));

}
