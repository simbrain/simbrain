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
import org.simbrain.network.neuron_update_rules.interfaces.ClippableUpdateRule;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>DecayNeuron</b> implements various forms of standard decay.
 */
public class DecayRule extends NeuronUpdateRule implements BoundedUpdateRule,
	ClippableUpdateRule {

	/** The Default upper bound. */
	private static final double DEFAULT_CEILING = 1.0;
	
	/** The Default lower bound. */
	private static final double DEFAULT_FLOOR = -1.0;
	
	/** Relative. */
	private static final int RELATIVE = 0;

	/** Absolute. */
	private static final int ABSOLUTE = 1;

	/** Relative absolute. */
	private int relAbs = RELATIVE;

	/** Decay amount. */
	private double decayAmount = .1;

	/** Decay fraction. */
	private double decayFraction = .1;

	/** Base line. */
	private double baseLine = 0;

	/** Clipping. */
	private boolean clipping = true;

	/** Noise dialog. */
	private Randomizer noiseGenerator = new Randomizer();

	/** Add noise to the neuron. */
	private boolean addNoise = false;
	
	/** The upper bound of the activity if clipping is used. */
	private double ceiling = DEFAULT_CEILING;
	
	/** The lower bound of the activity if clipping is used. */
	private double floor = DEFAULT_FLOOR;

	/**
	 * @return Time type.
	 */
	public TimeType getTimeType() {
		return TimeType.DISCRETE;
	}

	/**
	 * @{inheritDoc
	 */
	public DecayRule deepCopy() {
		DecayRule dn = new DecayRule();
		dn.setRelAbs(getRelAbs());
		dn.setDecayAmount(getDecayAmount());
		dn.setDecayFraction(getDecayFraction());
		dn.setClipped(isClipped());
		dn.setCeiling(getCeiling());
		dn.setFloor(getFloor());
		dn.setIncrement(getIncrement());
		dn.setAddNoise(getAddNoise());
		dn.noiseGenerator = new Randomizer(noiseGenerator);
		return dn;
	}

	/**
	 * @{inheritDoc
	 */
	public void update(Neuron neuron) {
		double val = neuron.getActivation() + neuron.getWeightedInputs();
		double decayVal = 0;

		if (relAbs == RELATIVE) {
			decayVal = decayFraction * Math.abs(val - baseLine);
		} else if (relAbs == ABSOLUTE) {
			decayVal = decayAmount;
		}

		// Here's where the action happens
		if (val < baseLine) {
			val += decayVal;

			// in case of an overshoot
			if (val > baseLine) {
				val = baseLine;
			}
		} else if (val > baseLine) {
			val -= decayVal;

			// in case of an overshoot
			if (val < baseLine) {
				val = baseLine;
			}
		}

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
		if(val > getCeiling()) {
			return getCeiling();
		} else if (val < getFloor()) {
			return getFloor();
		} else {
			return val;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void incrementActivation(Neuron n) {
		double act = n.getActivation();
		if (act >= getCeiling() && isClipped()) {
			return;
		} else {
			if(isClipped()) {
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
	public void decrementActivation(Neuron n) {
		double act = n.getActivation();
		if (act <= getFloor() && isClipped()) {
			return;
		} else {
			if(isClipped()) {
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
	public double getRandomValue() {
		return (getCeiling() - getFloor()) * Math.random()
				- getFloor();
	}
	
	/**
	 * @return Returns the decayAmount.
	 */
	public double getDecayAmount() {
		return decayAmount;
	}

	/**
	 * @param decayAmount
	 *            The decayAmount to set.
	 */
	public void setDecayAmount(final double decayAmount) {
		this.decayAmount = decayAmount;
	}

	/**
	 * @return Returns the dedayPercentage.
	 */
	public double getDecayFraction() {
		return decayFraction;
	}

	/**
	 * @param decayFraction
	 *            The decayFraction to set.
	 */
	public void setDecayFraction(final double decayFraction) {
		this.decayFraction = decayFraction;
	}

	/**
	 * @return Returns the relAbs.
	 */
	public int getRelAbs() {
		return relAbs;
	}

	/**
	 * @param relAbs
	 *            The relAbs to set.
	 */
	public void setRelAbs(final int relAbs) {
		this.relAbs = relAbs;
	}

	/**
	 * @return Returns the addNoise.
	 */
	public boolean getAddNoise() {
		return addNoise;
	}

	/**
	 * @param addNoise
	 *            The addNoise to set.
	 */
	public void setAddNoise(final boolean addNoise) {
		this.addNoise = addNoise;
	}

	/**
	 * @return Returns the noiseGenerator.
	 */
	public Randomizer getNoiseGenerator() {
		return noiseGenerator;
	}

	/**
	 * @param noiseGenerator
	 *            The noiseGenerator to set.
	 */
	public void setNoiseGenerator(final Randomizer noiseGenerator) {
		this.noiseGenerator = noiseGenerator;
	}

	/**
	 * @return Returns the baseLine.
	 */
	public double getBaseLine() {
		return baseLine;
	}

	/**
	 * @param baseLine
	 *            The baseLine to set.
	 */
	public void setBaseLine(final double baseLine) {
		this.baseLine = baseLine;
	}

	@Override
	public String getDescription() {
		return "Decay";
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

	@Override
	public boolean isClipped() {
		return clipping;
	}

	@Override
	public void setClipped(boolean clipping) {
		this.clipping = clipping;
	}

}
