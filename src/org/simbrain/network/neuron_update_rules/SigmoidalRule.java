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

import org.jblas.DoubleMatrix;
import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.util.math.SquashingFunctionEnum;

/**
 * <b>SigmoidalRule</b> provides various implementations of a standard sigmoidal
 * neuron.
 * <p>
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public class SigmoidalRule extends AbstractSigmoidalRule implements TransferFunction {

    // TODO: Possibly rename to "DiscreteSigmoidalRule"

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
    public SigmoidalRule(SquashingFunctionEnum sFunction) {
        super(sFunction);
    }

    @Override
    public final TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    @Override
    public final void update(Neuron neuron) {

        double val = neuron.getInput() + bias;

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        val = sFunction.valueOf(val, getUpperBound(), getLowerBound(), getSlope());

        neuron.setBuffer(val);
    }

    @Override
    public final SigmoidalRule deepCopy() {
        SigmoidalRule sr = new SigmoidalRule();
        sr = (SigmoidalRule) super.baseDeepCopy(sr);
        return sr;
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
    public void contextualDecrement(Neuron n) {
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

    @Override
    public double getDerivative(final double val) {
        double up = getUpperBound();
        double lw = getLowerBound();
        double diff = up - lw;
        return sFunction.derivVal(val, up, lw, diff);
    }

    @Override
    public final String getName() {
        return "Sigmoidal (Discrete)";
    }

    @Override
    public void applyFunctionInPlace(DoubleMatrix input) {
        applyFunction(input, input);
    }

    @Override
    public void applyFunction(DoubleMatrix input, DoubleMatrix output) {
        sFunction.valueOf(input, output, getUpperBound(), getLowerBound(), slope);
    }

    @Override
    public void getDerivative(DoubleMatrix input, DoubleMatrix output) {
        sFunction.derivVal(input, output, getUpperBound(), getLowerBound(), slope);
    }

    @Override
    public void applyFunctionAndDerivative(DoubleMatrix input, DoubleMatrix output, DoubleMatrix derivative) {
        sFunction.valueAndDeriv(input, output, derivative, getUpperBound(), getLowerBound(), slope);
    }

}
