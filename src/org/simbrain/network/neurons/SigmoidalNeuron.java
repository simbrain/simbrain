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
package org.simbrain.network.neurons;

import org.simbrain.network.interfaces.BiasedNeuron;
import org.simbrain.network.interfaces.Inverse;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.NeuronUpdateRule;
import org.simbrain.network.interfaces.RootNetwork.TimeType;
import org.simbrain.network.util.RandomSource;

/**
 * <b>SigmoidalNeuron</b> provides variuos implementations of a standard
 * sigmoidal neuron.
 */
public class SigmoidalNeuron extends NeuronUpdateRule implements BiasedNeuron, Inverse {

    /** Implementations of the Sigmoidal activation function. */
    public static enum SigmoidType {
        /** Arctangent. */
        ARCTAN {
            @Override
            public String toString() {
                return "Arctan";
            }
        },
        /** Logistic unscaled. */
        BARE {
            @Override
            public String toString() {
                return "Logistic (unscaled)";
            }
        },
        /** Logistic scaled. */
        LOGISTIC {
            @Override
            public String toString() {
                return "Logistic (scaled)";
            }
        },
        /** Tanh. */
        TANH {
            @Override
            public String toString() {
                return "Tanh";
            }
        }
    };

    /** Current implementation. */
    private SigmoidType type = SigmoidType.LOGISTIC;

    /** Bias. */
    private double bias = 0;

    /** Slope. */
    private double slope = 1;

    /** Noise dialog. */
    private RandomSource noiseGenerator = new RandomSource();

    /** Adds noise to neuron. */
    private boolean addNoise = false;

    /** Clipping. */
    private boolean clipping = false;

    /**
     * Default sigmoidal.
     */
    public SigmoidalNeuron() {
        super();
    }

    /**
     * Construct a sigmoid update with a specified implementaiton.
     *
     * @param type the implementation to use.
     */
    public SigmoidalNeuron(SigmoidType type) {
        super();
        this.type = type;
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
    public void init(Neuron neuron) {
        // No implementation
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {

        double val = neuron.getWeightedInputs() + bias;

        switch(type) {
        case TANH:
            val = tanh(val, neuron);
            break;
        case ARCTAN:
            val = atan(val, neuron);
            break;
        case LOGISTIC:
            val = sigm(val, neuron);
            break;
        default:
            val = 1 / 1 + Math.exp(-val);
            break;
        }

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        if (clipping) {
            val = neuron.clip(val);
        }

        neuron.setBuffer(val);
    }

    /**
     * {@inheritDoc}
     */
    public double inverse(double val, Neuron neuron) {
        switch(type) {
        case TANH:
            val = invTanh(val, neuron);
            break;
        case ARCTAN:
            val = invAtan(val, neuron);
            break;
        case LOGISTIC:
            val = invSigm(val, neuron);
            break;
        default:
            val = -Math.log(1 / val - 1);
            break;
        }
        return val;
    }

    /**
     * Returns the results of the hyperbolic tangent function.
     *
     * @param input argument
     * @param neuron undergoing update
     * @return results of tanh
     */
    private double tanh(final double input, Neuron neuron) {
        double upperBound = neuron.getUpperBound();
        double lowerBound = neuron.getLowerBound();
        return (((upperBound - lowerBound) * (0.5 * Math.tanh(input) + 0.5)) + (lowerBound));
    }

    /**
     * Returns the result of the arc hyperbolic tangent function
     *
     * @param input argument
     * @param neuron from which the value is being mapped
     * @return arctanh
     */
    private double invTanh(final double input, Neuron neuron) {
        double upperBound = neuron.getUpperBound();
        double lowerBound = neuron.getLowerBound();
        double z = 0.5 * (((input - lowerBound) / (upperBound - lowerBound)) - 0.5);
        return (Math.log((1 + z)) / (1 - z));
    }

    /**
     * Returns the results of the standard sigmoidal function.
     *
     * @param input argument
     * @param neuron undergoing update
     * @return results of sigm
     */
    private double sigm(final double input, Neuron neuron) {
        double upperBound = neuron.getUpperBound();
        double lowerBound = neuron.getLowerBound();
        double diff = upperBound - lowerBound;
        return diff * (1 / (1 + Math.exp(-(slope * input / diff))))
            + lowerBound;
    }

    /**
     * Returns the results of the inverse of the standard sigmoidal function
     *
     * @param input argument
     * @param neuron from which the value is being mapped
     * @return the inverse sigmoid
     */
    private double invSigm(final double input, Neuron neuron) {
        double upperBound = neuron.getUpperBound();
        double lowerBound = neuron.getLowerBound();
        double diff = upperBound - lowerBound;
        return diff * -Math.log(diff / (input - lowerBound) - 1) / slope;
    }

    /**
     * Returns the result of the arctangent function
     *
     * @param input argument
     * @param neuron undergoing update
     * @return results of atan
     */
    private double atan(final double input, Neuron neuron) {
        double upperBound = neuron.getUpperBound();
        double lowerBound = neuron.getLowerBound();
        double a = (Math.PI * slope) / (upperBound - lowerBound);
        return ((upperBound - lowerBound) / Math.PI) * Math.atan(a * input)
            + ((upperBound  + lowerBound) / 2);
    }

    /**
     * Returns the result of the inverse arctangent or tangent function
     *
     * @param input argument
     * @param neuron from which the value is being mapped
     * @return the inverse of the atan activation function
     */
    private double invAtan(final double input, Neuron neuron) {
        double upperBound = neuron.getUpperBound();
        double lowerBound = neuron.getLowerBound();
        double a = (Math.PI * slope) / (upperBound - lowerBound);
        double diff = upperBound - lowerBound;
        double z = ((input - ((upperBound + lowerBound) / 2))
                * (Math.PI / diff));
        return Math.tan(z) / a;
    }

    /**
     * {@inheritDoc}
     */
    public SigmoidalNeuron deepCopy() {
        SigmoidalNeuron sn = new SigmoidalNeuron();
        sn.setBias(getBias());
        sn.setClipping(getClipping());
        sn.setType(getType());
        sn.setSlope(getSlope());
        sn.setAddNoise(getAddNoise());
        sn.noiseGenerator =  new RandomSource(noiseGenerator);
        return sn;
    }

    /**
     * @return Returns the inflectionPoint.
     */
    public double getBias() {
        return bias;
    }

    /**
     * @param inflectionY The inflectionY to set.
     */
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
    public RandomSource getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noise The noise to set.
     */
    public void setNoiseGenerator(final RandomSource noise) {
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

    /**
     * @return Returns the clipping.
     */
    public boolean getClipping() {
        return clipping;
    }

    /**
     * @param clipping The clipping to set.
     */
    public void setClipping(final boolean clipping) {
        this.clipping = clipping;
    }

    @Override
    public String getDescription() {
        return "Sigmoidal";
    }

    /**
     * @return the type
     */
    public SigmoidType getType() {
        if (type == null) {
            type = SigmoidType.LOGISTIC; //TODO: Explain (backwards compat) 
        }
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(SigmoidType type) {
        this.type = type;
    }
}
