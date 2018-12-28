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

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.math.SquashingFunctionEnum;

/**
 * <b>Continuous Sigmoidal Rule</b> provides various squashing function
 * ouputs for a neuron whose activation is numerically integrated continuously
 * over time.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public class ContinuousSigmoidalRule extends AbstractSigmoidalRule {

    /**
     * Default time constant (ms).
     */
    public static final double DEFAULT_TIME_CONSTANT = 10.0;

    /**
     * Default leak constant {@link #leak}.
     */
    public static final double DEFAULT_LEAK_CONSTANT = 1.0;

    /**
     * The <b>time constant</b> of these neurons. If <b>timeConstant *
     * leakConstant == network time-step</b> (or vice versa), behavior is
     * equivalent to discrete sigmoid. The larger the time constant relative to
     * the time-step, the more slowly inputs will be integrated.
     */
    @UserParameter(
            label = "Time Constant",
            description = "The time constant controls how quickly the numerical integration occurs.",
            defaultValue = "" + DEFAULT_TIME_CONSTANT, order = 1)
    private double tau = DEFAULT_TIME_CONSTANT;

    /**
     * The leak constant: how strongly the neuron will be attracted to its base
     * activation. If <b>timeConstant * leakConstant == network time-step</b>
     * (or vice versa), behavior is equivalent to discrete sigmoid.
     */
    @UserParameter(
            label = "Leak Constant",
            description = "An option to add noise.",
            defaultValue = "" + DEFAULT_LEAK_CONSTANT, order = 2)
    private double leak = DEFAULT_LEAK_CONSTANT;

    /**
     * The net value of this neuron. This is the value that is integrated over
     * time and then passed to the squashing function. NOTE: the net inputs are
     * integrated and that value is passed through a squashing function to give
     * the neurons activation. The activation post-squashing is NOT what is
     * being numerically integrated.
     */
    private double netActivation;

    private double inputTerm;

    /**
     * Default sigmoidal.
     */
    public ContinuousSigmoidalRule() {
        super();
    }

    /**
     * Construct a sigmoid update with a specified implementation.
     *
     * @param sFunction the implementation to use.
     */
    public ContinuousSigmoidalRule(final SquashingFunctionEnum sFunction) {
        super();
        this.sFunction = sFunction;
    }

    @Override
    public final ContinuousSigmoidalRule deepCopy() {
        ContinuousSigmoidalRule sn = new ContinuousSigmoidalRule();
        sn = (ContinuousSigmoidalRule) super.baseDeepCopy(sn);
        sn.setTimeConstant(getTimeConstant());
        sn.setLeakConstant(getLeakConstant());
        return sn;
    }

    /**
     * Where x_i(t) is the net activation of neuron i at time t, r(t) is the
     * output activation after being put through a sigmoid squashing function at
     * time t, a is the leak constant, and c is the time constant:
     * <p>
     * <b>c * dx_i/dt = -ax_i(t) + sum(w_ij * r_j(t)</b>
     * <p>
     * Discretizing using euler integration:
     * <p>
     * <b>x_i(t + dt) = x_i(t) - (ax_i(t) * dt/c) + (dt/c)*sum(w_ij * r_j(t))</b>
     * <p>
     * Factorting out x_i(t):
     * <p>
     * <b>x_i(t + dt) = x_i(t) * (1 - a*dt/c) + (dt/c) * sum(w_ij * r_j(t))</b>
     */
    public void update(Neuron neuron) {

        double dt = neuron.getNetwork().getTimeStep();

        if (addNoise) {
            inputTerm = (dt / tau) * (neuron.getInput() + bias + noiseGenerator.getRandom());
        } else {
            inputTerm = (dt / tau) * (neuron.getInput() + bias);
        }

        netActivation = netActivation * (1 - (leak * dt / tau)) + inputTerm;

        double output = sFunction.valueOf(netActivation, getUpperBound(), getLowerBound(), getSlope());

        neuron.setBuffer(output);

    }

    public int getNoBytes() { // bump to interface...
        // [ buff | netInp | netAct | leak | tau | UB | LB | slope ]
        return 56 + 8; // Do some reflection here... 8 is for buffer
    }

    private int offset;

    public void update(int offset, final double[] arr) {
        arr[offset] = arr[offset + 2] * (1 - arr[offset + 3] * arr[offset + 4]) + arr[offset + 4] * arr[offset + 1];
        arr[offset] = sFunction.valueOf(arr[offset], arr[offset + 5], arr[offset + 6], arr[offset + 7]);
    }

    public int writeToArr(Network net, final double[] arr, int _offset) {
        offset = _offset;
        arr[_offset + 1] = inputTerm;
        arr[_offset + 2] = netActivation;
        arr[_offset + 3] = leak;
        arr[_offset + 4] = net.getTimeStep() / tau;
        arr[_offset + 5] = getUpperBound();
        arr[_offset + 6] = getLowerBound();
        arr[_offset + 7] = getSlope();
        return _offset + 8; // padding
    }

    public void writeFromArr(Neuron neu, final double[] arr) {
        neu.setBuffer(arr[offset]);
        netActivation = arr[offset + 2];
        leak = arr[offset + 3];
        tau = 1 / arr[offset + 4] * neu.getNetwork().getTimeStep();
        inputTerm = arr[offset + 1];
    }

    @Override
    public final void contextualIncrement(final Neuron n) {
        double act = n.getActivation();
        if (act < getUpperBound()) {
            act += n.getIncrement();
            if (act > getUpperBound()) {
                act = getUpperBound();
            }
            n.setActivation(act);
            n.getNetwork().fireNeuronChanged(n);
        }
    }


    @Override
    public final void contextualDecrement(final Neuron n) {
        double act = n.getActivation();
        if (act > getLowerBound()) {
            act -= n.getIncrement();
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
    public TimeType getTimeType() {
        return TimeType.CONTINUOUS;
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

    @Override
    public ProbabilityDistribution getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(final ProbabilityDistribution noise) {
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
    public String getName() {
        return "Sigmoidal (Continuous)";
    }

    /**
     * @return the time constant
     */
    public double getTimeConstant() {
        return tau;
    }

    public void setTimeConstant(double timeConstant) {
        this.tau = timeConstant;
    }

    /**
     * {@link #leak}
     *
     * @return the leak constant for the neuron.
     */
    public double getLeakConstant() {
        return leak;
    }

    /**
     * {@link #leak}
     *
     * @param leakConstant the leak constant for the neuron.
     */
    public void setLeakConstant(double leakConstant) {
        this.leak = leakConstant;
    }

    @Override
    public final void clear(Neuron neuron) {
        super.clear(neuron);
        netActivation = 0;
    }

}
