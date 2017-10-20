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
package org.simbrain.network.synapse_update_rules;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;


/**
 * Implementation of the model described by Pfister, J-P, Gerstner, W: 
 * Triplets of Spikes in a Model of Spike Timing-Dependent Plasticity. 
 * J. Neurosci. 26, 9673–9682 (2006).
 * 
 * Only works if source and target neurons are spiking neurons.
 * 
 * @author Oliver J. Coleman
 */
public class PfisterGerstner2006Rule extends SynapseUpdateRule {
	/**
	 * Decay rate for r1 trace, as a multiplier.
	 */
    protected double tauPlusDecayMult = 1/16.8;

	/**
	 * Decay rate for r2 trace, as a multiplier.
	 */
    protected double tauXDecayMult = 1/1.0;

	/**
	 * Decay rate for o1 trace, as a multiplier.
	 */
    protected double tauNegDecayMult = 1/33.7;

	/**
	 * Decay rate for o2 trace, as a multiplier.
	 */
    protected double tauYDecayMult = 1/48.0;

	/**
	 * Amplitude of the weight change for a pre-post spike pair.
	 */
    protected double a2N = 0.003;

	/**
	 * Amplitude of the weight change for a post-pre spike pair.
	 */
    protected double a2P = 0.0046;

	/**
	 * Amplitude of the triplet term for potentiation.
	 */
    protected double a3N = 0;

	/**
	 * Amplitude of the triplet term for depression.
	 */
    protected double a3P = 0.0091;
    
    
    // Spike traces.
	private double r1, r2, o1, o2;

	
    @Override
    public void init(Synapse synapse) {
    }

    
    @Override
    public String getName() {
        return "Pfister and Gerstner, 2006";
    }

    
    @Override
    public SynapseUpdateRule deepCopy() {
        PfisterGerstner2006Rule duplicateSynapse = new PfisterGerstner2006Rule();
        duplicateSynapse.tauPlusDecayMult = this.tauPlusDecayMult;
        duplicateSynapse.tauXDecayMult = this.tauXDecayMult;
        duplicateSynapse.tauNegDecayMult = this.tauNegDecayMult;
        duplicateSynapse.tauYDecayMult = this.tauYDecayMult;
        duplicateSynapse.a2N = this.a2N;
        duplicateSynapse.a2P = this.a2P;
        duplicateSynapse.a3N = this.a3N;
        duplicateSynapse.a3P = this.a3P;
        return duplicateSynapse;
    }
    
    
    @Override
    public void update(Synapse synapse) {
    	// Time step in ms.
    	final double timeStep = synapse.getNetwork().getTimeStep();
    	final boolean preSpiked = synapse.getSource().isSpike();
    	final boolean postSpiked = synapse.getTarget().isSpike();
    		
		// Need current values for these traces for strength update equations below.
		final double r2p = r2;
		final double o2p = o2;

		// Update trace values.
		if (preSpiked) {
			r1 = 1;
			r2 = 1;
		}
		else {
			r1 -= r1 * tauPlusDecayMult * timeStep;
			r2 -= r2 * tauXDecayMult * timeStep;
		}
		if (postSpiked) {
			o1 = 1;
			o2 = 1;
		}
		else {
			o1 -= o1 * tauNegDecayMult * timeStep;
			o2 -= o2 * tauYDecayMult * timeStep;
		}
		
		// Update efficacy if a pre or post spike occurred.
		if (preSpiked) {
			synapse.setStrength(synapse.getStrength() - o1 * (a2N + a3N * r2p));
		}
		
		if (postSpiked) {
			synapse.setStrength(synapse.getStrength() + r1 * (a2P + a3P * o2p));
		}
    }


	/**
	 * @return Decay rate for r1 trace.
	 */
	public double getTauPlusDecay() {
		return 1 / tauPlusDecayMult;
	}


	/**
	 * @param tauPlusDecay Decay rate for r1 trace.
	 */
	public void setTauPlusDecay(double tauPlusDecay) {
		this.tauPlusDecayMult = 1 / tauPlusDecay;
	}


	/**
	 * @return Decay rate for r2 trace.
	 */
	public double getTauXDecay() {
		return 1 / tauXDecayMult;
	}


	/**
	 * @param tauXDecay Decay rate for r2 trace.
	 */
	public void setTauXDecay(double tauXDecay) {
		this.tauXDecayMult = 1 / tauXDecay;
	}


	/**
	 * @return Decay rate for o1 trace.
	 */
	public double getTauNegDecay() {
		return 1 / tauNegDecayMult;
	}


	/**
	 * @param tauNegDecay Decay rate for o1 trace.
	 */
	public void setTauNegDecay(double tauNegDecay) {
		this.tauNegDecayMult = 1 / tauNegDecay;
	}


	/**
	 * @return Decay rate for o2 trace.
	 */
	public double getTauYDecay() {
		return 1 / tauYDecayMult;
	}


	/**
	 * @param tauYDecay Decay rate for o2 trace.
	 */
	public void setTauYDecay(double tauYDecay) {
		this.tauYDecayMult = 1 / tauYDecay;
	}


	/**
	 * @return Amplitude of the weight change for a pre-post spike pair.
	 */
	public double getA2P() {
		return a2P;
	}


	/**
	 * @param a2p Amplitude of the weight change for a pre-post spike pair.
	 */
	public void setA2P(double a2p) {
		a2P = a2p;
	}


	/**
	 * @return Amplitude of the weight change for a post-pre spike pair.
	 */
	public double getA2N() {
		return a2N;
	}


	/**
	 * @param Amplitude of the weight change for a post-pre spike pair.
	 */
	public void setA2N(double a2n) {
		a2N = a2n;
	}


	/**
	 * @return Amplitude of the triplet term for potentiation.
	 */
	public double getA3P() {
		return a3P;
	}


	/**
	 * @param Amplitude of the triplet term for potentiation.
	 */
	public void setA3P(double a3p) {
		a3P = a3p;
	}


    /**
	 * @return Amplitude of the triplet term for depression.
	 */
	public double getA3N() {
		return a3N;
	}


	/**
	 * @param a3n Amplitude of the triplet term for depression.
	 */
	public void setA3N(double a3n) {
		a3N = a3n;
	}
}
