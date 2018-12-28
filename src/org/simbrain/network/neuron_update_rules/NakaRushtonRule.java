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
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;

/**
 * <b>NakaRushtonNeuron</b> is a firing-rate based neuron which is intended to
 * model spike rates of real neurons. It is used extensively in Hugh Wilson's
 * Spikes, Decisions, and Action.
 */
public class NakaRushtonRule extends NeuronUpdateRule implements BoundedUpdateRule, NoisyUpdateRule {

    /**
     * The default activation ceiling.
     */
    public static final int DEFAULT_UPPER_BOUND = 100;

    /**
     * Steepness.
     */
    @UserParameter(
            label = "Steepness",
            description = "This value controls the steepness of the sigmoidal-like function S(W).",
            defaultValue = "2", order = 1)
    private double steepness = 2;

    /**
     * Semi saturation constant.
     */
    @UserParameter(
            label = "Semi-saturation constant",
            description = "This value is the point at which S(W) reaches half of its maximum value.",
            defaultValue = "120", order = 2)
    private double semiSaturationConstant = 120;

    /**
     * Time constant of spike rate adaptation.
     */
    @UserParameter(
            label = "Adaptation Time Constant",
            description = "This value controls the rate at which the adaptation variable tends to "
                    + "its minimum value.",
            defaultValue = "1", order = 7)
    private double adaptationTimeConstant = 1;

    /**
     * Parameter of spike rate adaptation.
     */
    @UserParameter(
            label = "Adaptation Parameter",
            description = "The parameter of spike rate adaptation.",
            defaultValue = "0.7", order = 6)
    private double adaptationParameter = .7;

    /**
     * Whether to use spike rate adaptation or not.
     */
    @UserParameter(
            label = "Use Adaptation",
            description = "If this is set to true, spike rate adaptation is utilized.",
            defaultValue = "false", order = 5)
    private boolean useAdaptation = false;

    /**
     * Time constant.
     */
    @UserParameter(
            label = "Time constant",
            description = "This valu controls the rate at which the activation tends to the fixed "
                    + "point S(W).",
            defaultValue = "1", order = 3)
    private double timeConstant = 1;

    /**
     * Noise generator.
     */
    private ProbabilityDistribution noiseGenerator = UniformDistribution.create();

    /**
     * Add noise to neuron.
     */
    private boolean addNoise = false;

    /**
     * Local variable.
     */
    private double s = 0;

    /**
     * Local variable.
     */
    private double a = 0;

    /**
     * The upper bound of the activity.
     */
    private double upperBound = DEFAULT_UPPER_BOUND;

    /**
     * The lower bound of the activity.
     */
    private double lowerBound = 0;

    /**
     * Default constructor.
     */
    public NakaRushtonRule() {
    }

    /**
     * {@inheritDoc}
     */
    public TimeType getTimeType() {
        return TimeType.CONTINUOUS;
    }

    /**
     * {@inheritDoc}
     */
    public NakaRushtonRule deepCopy() {
        NakaRushtonRule rn = new NakaRushtonRule();
        rn.setSteepness(getSteepness());
        rn.setSemiSaturationConstant(getSemiSaturationConstant());
        rn.setUpperBound(getUpperBound());
        rn.setAddNoise(getAddNoise());
        rn.setUseAdaptation(getUseAdaptation());
        rn.setAdaptationParameter(getAdaptationParameter());
        rn.setAdaptationTimeConstant(getAdaptationTimeConstant());
        rn.noiseGenerator = noiseGenerator.deepCopy();
        return rn;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {

        // See Spikes (Hugh Wilson), pp. 20-21

        double p = neuron.getInput();
        double val = neuron.getActivation();

        // Update adaptation term; see Spike, p. 81
        if (useAdaptation) {
            a += (neuron.getNetwork().getTimeStep() / adaptationTimeConstant) * (adaptationParameter * val - a);
        } else {
            a = 0;
        }

        if (p > 0) {
            s = (getUpperBound() * Math.pow(p, steepness)) / (Math.pow(semiSaturationConstant + a, steepness) + Math.pow(p, steepness));
        } else {
            s = 0;
        }

        if (addNoise) {
            val += (neuron.getNetwork().getTimeStep() * (((1 / timeConstant) * (-val + s)) + noiseGenerator.getRandom()));
        } else {
            val += (neuron.getNetwork().getTimeStep() * ((1 / timeConstant) * (-val + s)));
        }

        neuron.setBuffer(val);
    }

    @Override
    public void contextualIncrement(Neuron n) {
        double act = n.getActivation();
        if (act <= getUpperBound()) {
            if (act + n.getIncrement()> getUpperBound()) {
                act = getUpperBound();
            }
            n.setActivation(act);
            n.getNetwork().fireNeuronChanged(n);
        }
    }

    /**
     * @return Returns the semiSaturationConstant.
     */
    public double getSemiSaturationConstant() {
        return semiSaturationConstant;
    }

    /**
     * @param semiSaturationConstant The semiSaturationConstant to set.
     */
    public void setSemiSaturationConstant(final double semiSaturationConstant) {
        this.semiSaturationConstant = semiSaturationConstant;
    }

    /**
     * @return Returns the steepness.
     */
    public double getSteepness() {
        return steepness;
    }

    /**
     * @param steepness The steepness to set.
     */
    public void setSteepness(final double steepness) {
        this.steepness = steepness;
    }

    /**
     * @return Returns the timeConstant.
     */
    public double getTimeConstant() {
        return timeConstant;
    }

    /**
     * @param timeConstant The timeConstant to set.
     */
    public void setTimeConstant(final double timeConstant) {
        this.timeConstant = timeConstant;
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
    public ProbabilityDistribution getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(final ProbabilityDistribution noise) {
        this.noiseGenerator = noise;
    }

    /**
     * @return the boolean value.
     */
    public boolean getUseAdaptation() {
        return useAdaptation;
    }

    /**
     * Sets the boolean use adaptation value.
     *
     * @param useAdaptation Value to set use adaptation to
     */
    public void setUseAdaptation(final boolean useAdaptation) {
        this.useAdaptation = useAdaptation;
    }

    /**
     * @return the adaptation time constant.
     */
    public double getAdaptationTimeConstant() {
        return adaptationTimeConstant;
    }

    /**
     * Sets the adaptation time constant.
     *
     * @param adaptationTimeConstant Value to set adaptation time constant
     */
    public void setAdaptationTimeConstant(final double adaptationTimeConstant) {
        this.adaptationTimeConstant = adaptationTimeConstant;
    }

    @Override
    public void clear(Neuron neuron) {
        super.clear(neuron);
        a = 0;
        s = 0;
    }

    @Override
    public String getToolTipText(Neuron neuron) {
        if (useAdaptation) {
            return "" + neuron.getActivation() + " A = " + a;
        } else {
            return super.getToolTipText(neuron);
        }
    }

    /**
     * Return the adaptation parameter.
     *
     * @return the adaptation parameter
     */
    public double getAdaptationParameter() {
        return adaptationParameter;
    }

    /**
     * Sets the adaptation parameter.
     *
     * @param adaptationParameter value to set
     */
    public void setAdaptationParameter(final double adaptationParameter) {
        this.adaptationParameter = adaptationParameter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Naka-Rushton";
    }

    @Override
    public double getUpperBound() {
        return upperBound;
    }

    @Override
    public void setUpperBound(double ceiling) {
        this.upperBound = ceiling;
    }

    @Override
    public double getLowerBound() {
        return lowerBound;
    }

    @Override
    public void setLowerBound(double floor) {
        this.lowerBound = floor;
    }

}
