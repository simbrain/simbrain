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
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>SigmoidalNeuron</b> provides various implementations of a standard
 * sigmoidal neuron.
 *
 * @author Scott Hotton
 * @author Zach Tosi
 * @author Jeff Yoshimi
 *
 */
public class SigmoidalRule extends NeuronUpdateRule implements
        BiasedUpdateRule, DifferentiableUpdateRule, InvertibleUpdateRule,
        BoundedUpdateRule {

    /** Implementations of the Sigmoidal activation function. */
    public static enum SigmoidType {
        /** Arctangent. */
        ARCTAN {
            @Override
            public String toString() {
                return "Arctan";
            }
        },
        /** Logistic. */
        LOGISTIC {
            @Override
            public String toString() {
                return "Logistic";
            }
        },
        /** Tanh. */
        TANH {
            @Override
            public String toString() {
                return "Tanh";
            }
        },
        NULL_STRING{
        	@Override
        	public String toString() {
        		return "...";
        	}
        }
    };

	/** The Default upper bound. */
	private static final double DEFAULT_CEILING = 1.0;
	
	/** The Default lower bound. */
	private static final double DEFAULT_FLOOR = 0.0;
    
    /** Current implementation. */
    private SigmoidType type = SigmoidType.LOGISTIC;

    /** Bias. */
    private double bias = 0;

    /** Slope. */
    private double slope = 1;

    /** Noise dialog. */
    private Randomizer noiseGenerator = new Randomizer();

    /** Adds noise to neuron. */
    private boolean addNoise = false;

	/** The upper bound of the activity if clipping is used. */
	private double ceiling = DEFAULT_CEILING;
	
	/** The lower bound of the activity if clipping is used. */
	private double floor = DEFAULT_FLOOR;
    
    /**
     * Default sigmoidal.
     */
    public SigmoidalRule() {
        super();
    }

    /**
     * Construct a sigmoid update with a specified implementation.
     *
     * @param type the implementation to use.
     */
    public SigmoidalRule(SigmoidType type) {
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

        double val = neuron.getWeightedInputs();

        switch (type) {
        case TANH:
            val = tanh(val, neuron);
            break;
        case ARCTAN:
            val = atan(val, neuron);
            break;
        case LOGISTIC:
            val = logistic(val, neuron);
            break;
        default:
            val = logistic(val, neuron);
            break;
        }

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        neuron.setBuffer(val);
    }
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void incrementActivation(Neuron n) {
		double act = n.getActivation();
		if (act < getCeiling()){
			act += getIncrement();
			if (act > getCeiling())
				act = getCeiling();
			n.setActivation(act);
			n.getNetwork().fireNeuronChanged(n);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void decrementActivation(Neuron n) {
		double act = n.getActivation();
		if (act > getFloor()){
			act -= getIncrement();
			if (act < getFloor())
				act = getFloor();
			n.setActivation(act);
			n.getNetwork().fireNeuronChanged(n);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getRandomValue() {
		return (getCeiling() - getFloor()) * Math.random()
				- getFloor();
	}

    /**
     * Return the derivative of the activation function.
     *
     * @param x value to be passed to the derivative.
     * @param neuron neuron on which to compute the derivative
     * @return derivative
     */
    public double getDerivative(final double x, final Neuron neuron) {
        double retVal = 0;
        double up = getCeiling();
        double lw = getFloor();
        double diff = up - lw;

        switch (type) {
        case TANH:
            double a = (2 * slope) / diff;
            retVal = diff / 2 * a * Math.pow(1 / Math.cosh(a * x), 2);
            break;
        case ARCTAN:
            a = (Math.PI * slope) / diff;
            retVal = a * (diff / Math.PI) * (1 / (1 + Math.pow(a * x, 2)));
            break;
        case LOGISTIC:
            retVal = slope
                    / (Math.exp(slope * x / diff) * Math.pow(
                            1 / Math.exp(slope * x / diff) + 1, 2));
            break;
        default:
            break;
        }
        return retVal;
    }

    /**
     * {@inheritDoc}
     */
    public double getInverse(double val, Neuron neuron) {
        switch (type) {
        case TANH:
            val = invTanh(val, neuron);
            break;
        case ARCTAN:
            val = invAtan(val, neuron);
            break;
        case LOGISTIC:
            val = invLogistic(val, neuron);
            break;
        default:
            val = invLogistic(val, neuron);
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
        double up = getCeiling();
        double lw = getFloor();
        double diff = up - lw;
        double a = (2 * slope) / diff;
        return (diff / 2) * Math.tanh(a * input) + ((up + lw) / 2);
    }

    /**
     * Returns the result of the arc hyperbolic tangent function
     *
     * @param input argument
     * @param neuron from which the value is being mapped
     * @return arctanh
     */
    private double invTanh(final double input, Neuron neuron) {
        double up = getCeiling();
        double lw = getFloor();
        double z = 0.5 * (((input - lw) / (up - lw)) - 0.5);
        return (Math.log((1 + z)) / (1 - z));
    }

    /**
     * Returns the results of the standard sigmoidal function.
     *
     * @param input argument
     * @param neuron undergoing update
     * @return results of sigm
     */
    private double logistic(final double input, Neuron neuron) {
        double up = getCeiling();
        double lw = getFloor();
        double diff = up - lw;
        return diff * logistic(slope * input / diff) + lw;
    }

    /**
     * Returns the standard logistic.
     *
     * @param x input argument
     * @return result of logistic
     */
    private double logistic(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    /**
     * Returns the results of the inverse of the standard sigmoidal function
     *
     * @param input argument
     * @param neuron from which the value is being mapped
     * @return the inverse sigmoid
     */
    private double invLogistic(final double input, Neuron neuron) {
        double up = getCeiling();
        double lw = getFloor();
        double diff = up - lw;
        return diff * -Math.log(diff / (input - lw) - 1) / slope;
    }

    /**
     * Returns the result of the arctangent function
     *
     * @param input argument
     * @param neuron undergoing update
     * @return results of atan
     */
    private double atan(final double input, Neuron neuron) {
        double up = getCeiling();
        double lw = getFloor();
        double diff = up - lw;
        double a = (Math.PI * slope) / diff;
        return (diff / Math.PI) * Math.atan(a * input) + ((up + lw) / 2);
    }

    /**
     * Returns the result of the inverse arctangent or tangent function
     *
     * @param input argument
     * @param neuron from which the value is being mapped
     * @return the inverse of the atan activation function
     */
    private double invAtan(final double input, Neuron neuron) {
        double up = getCeiling();
        double lw = getFloor();
        double a = (Math.PI * slope) / (up - lw);
        double diff = up - lw;
        double z = ((input - ((up + lw) / 2)) * (Math.PI / diff));
        return Math.tan(z) / a;
    }

    /**
     * {@inheritDoc}
     */
    public SigmoidalRule deepCopy() {
        SigmoidalRule sn = new SigmoidalRule();
        sn.setBias(getBias());
        sn.setType(getType());
        sn.setSlope(getSlope());
        sn.setAddNoise(getAddNoise());
        sn.noiseGenerator = new Randomizer(noiseGenerator);
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
        return "Sigmoidal";
    }

    /**
     * @return the type
     */
    public SigmoidType getType() {
        if (type == null) {
            type = SigmoidType.LOGISTIC; // TODO: Explain (backwards compat)
        }
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(SigmoidType type) {
        this.type = type;
    }

    @Override
	public double getCeiling() {
		return ceiling;
	}

	@Override
	public double getFloor() {
		return floor;
	}

	@Override
	public void setCeiling(double ceiling) {
		this.ceiling = ceiling;	
	}

	@Override
	public void setFloor(double floor) {
		this.floor = floor;
	}

}
