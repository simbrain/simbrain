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

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>IntegrateAndFireNeuron</b> implements an integrate and fire neuron.
 * Parameters taken from recordings of rat cortex from: Maass (2002) Real Time
 * Computing Without Stable States: A new framework for neural computations
 * based on perturbations.
 * 
 * TODO: Add custom tooltip
 */
public class IntegrateAndFireRule extends SpikingNeuronUpdateRule {

	/** Resistance (MΩ). */
	private double resistance = 1;

	/** Time constant (ms) */
	private double timeConstant = 30;

	/** Threshold (mV) */
	private double threshold = 15;

	/** Reset potential (mV) */
	private double resetPotential = 13.5;

	/** Resting potential (mV) */
	private double restingPotential = 0.0;

	/** Background Current (nA) . */
	private double backgroundCurrent = 13.5;

	/** Noise dialog. */
	private Randomizer noiseGenerator = new Randomizer();

	/** Add noise to neuron. */
	private boolean addNoise = false;

	/** Clipping. */
	private boolean clipping = false;

	/**
	 * {@inheritDoc}
	 */
	public IntegrateAndFireRule deepCopy() {
		IntegrateAndFireRule ifn = new IntegrateAndFireRule();
		ifn.setRestingPotential(getRestingPotential());
		ifn.setResetPotential(getResetPotential());
		ifn.setThreshold(getThreshold());
		ifn.setTimeConstant(getTimeConstant());
		ifn.setResistance(getResistance());
		ifn.setClipping(getClipping());
		ifn.setAddNoise(getAddNoise());
		ifn.noiseGenerator = new Randomizer(noiseGenerator);

		return ifn;
	}

	/**
	 * {@inheritDoc}
	 */
	public void update(Neuron neuron) {
		double Isyn = neuron.getWeightedInputs();

		if (addNoise) {
			Isyn += noiseGenerator.getRandom();
		}

		double timeStep = neuron.getNetwork().getTimeStep();

		double memPotential = neuron.getActivation();

		/*
		 * Formula:
		 * 
		 * dV/dt = ( -(Vm - Vr) + Rm * (Isyn + Ibg) ) / tau
		 * 
		 * Vm > theta ? Vm <- Vreset ; spike
		 * 
		 * Vm: membrane potential Vr: resting potential* Rm: membrane resistance
		 * Isyn: synaptic input current Ibg: background input current tau: time
		 * constant Vreset: reset potential theta: threshold
		 */

		double dVm =
				timeStep
						* (-(memPotential - restingPotential) + resistance
								* (Isyn + backgroundCurrent))
						/ timeConstant;

		memPotential += dVm;

		if (memPotential > threshold) {
			setHasSpiked(true, neuron);
			memPotential = resetPotential;
		} else {
			setHasSpiked(false, neuron);
		}

		if (clipping) {
			memPotential = neuron.clip(memPotential);
		}

		neuron.setBuffer(memPotential);
	}

	/**
	 * @return Returns the lowerValue.
	 */
	public double getRestingPotential() {
		return restingPotential;
	}

	/**
	 * @param restingPotential
	 *            The restingPotential to set.
	 */
	public void setRestingPotential(final double restingPotential) {
		this.restingPotential = restingPotential;
	}

	/**
	 * @return Returns the upperValue.
	 */
	public double getResistance() {
		return resistance;
	}

	/**
	 * @param resistance
	 *            The resistance to set.
	 */
	public void setResistance(final double resistance) {
		this.resistance = resistance;
	}

	/**
	 * @return Returns the lowerValue.
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
	 * @param noise
	 *            The noise to set.
	 */
	public void setAddNoise(final Randomizer noise) {
		this.noiseGenerator = noise;
	}

	/**
	 * @return Returns the clipping.
	 */
	public boolean getClipping() {
		return clipping;
	}

	/**
	 * @param clipping
	 *            The clipping to set.
	 */
	public void setClipping(final boolean clipping) {
		this.clipping = clipping;
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
	 * @return Returns the resetPotential.
	 */
	public double getResetPotential() {
		return resetPotential;
	}

	/**
	 * @param resetPotential
	 *            The resetPotential to set.
	 */
	public void setResetPotential(final double resetPotential) {
		this.resetPotential = resetPotential;
	}

	/**
	 * @return Returns the background current
	 */
	public double getBackgroundCurrent() {
		return backgroundCurrent;
	}

	/**
	 * @param backgroundCurrent
	 *            The background current to set
	 */
	public void setBackgroundCurrent(double backgroundCurrent) {
		this.backgroundCurrent = backgroundCurrent;
	}

	/**
	 * @return Returns the threshold.
	 */
	public double getThreshold() {
		return threshold;
	}

	/**
	 * @param threshold
	 *            The threshold to set.
	 */
	public void setThreshold(final double threshold) {
		this.threshold = threshold;
	}

	/**
	 * @return Returns the timeConstant.
	 */
	public double getTimeConstant() {
		return timeConstant;
	}

	/**
	 * @param timeConstant
	 *            The timeConstant to set.
	 */
	public void setTimeConstant(final double timeConstant) {
		this.timeConstant = timeConstant;
	}

	@Override
	public String getDescription() {
		return "Integrate and Fire";
	}

	/**
	 * Default floor for I&F neurons is 0 {@inheritDoc}
	 */
	public void setDefaultParameters(Neuron n) {
		n.setUpperBound(DEFAULT_CEILING);
		n.setLowerBound(DEFAULT_FLOOR);
		n.setIncrement(DEFAULT_INCREMENT);
	}

	public double getDefaultCeiling() {
		return threshold;
	}

	public double getDefaultFloor() {
		return Double.NEGATIVE_INFINITY;
	}

}
