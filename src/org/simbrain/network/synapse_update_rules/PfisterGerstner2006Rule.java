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
import org.simbrain.network.gui.UserParameter;


/**
 * Implementation of the model described by Pfister, J-P, Gerstner, W: 
 * Triplets of Spikes in a Model of Spike Timing-Dependent Plasticity. 
 * J. Neurosci. 26, 9673–9682 (2006).
 * 
 * Only works if source and target neurons are spiking neurons.
 * 
 * @author Oliver J. Coleman
 */
public class PfisterGerstner2006Rule extends SynapseUpdateRule implements Cloneable {
	@UserParameter(label="Tau+", description="Decay rate for r1 trace", minimumValue=1, maximumValue=100, defaultValue="16.8", order=0)
    protected double tauPlus = 16.8;

	@UserParameter(label="Tau x", description="Decay rate for r2 trace", minimumValue=1, maximumValue=100, defaultValue="1.0", order=1)
    protected double tauX = 1.0;

	@UserParameter(label="Tau-", description="Decay rate for o1 trace", minimumValue=1, maximumValue=100, defaultValue="33.7", order=2)
    protected double tauNeg = 33.7;

	@UserParameter(label="Tau y", description="Decay rate for o2 trace", minimumValue=1, maximumValue=100, defaultValue="48.0", order=3)
    protected double tauY = 48.0;

	@UserParameter(label="A2+", description="Amplitude of the weight change for a pre-post spike pair.", minimumValue=0, maximumValue=0.1, defaultValue="0.003", order=4)
    protected double a2P = 0.0046;

	@UserParameter(label="A2-", description="Amplitude of the weight change for a post-pre spike pair.", minimumValue=0, maximumValue=0.1, defaultValue="0.003", order=5)
    protected double a2N = 0.003;

	@UserParameter(label="A3+", description="Amplitude of the triplet term for potentiation.", minimumValue=0, maximumValue=0.1, defaultValue="0.0091", order=6)
    protected double a3P = 0.0091;
    
	@UserParameter(label="A3-", description="Amplitude of the triplet term for depression.", minimumValue=0, maximumValue=0.1, defaultValue="0.0", order=7)
    protected double a3N = 0;
	
	
    // Spike traces.
	private double r1, r2, o1, o2;
	// Cached multipliers for trace decays.
	private double tauPlusMult, tauXMult, tauNegMult, tauYMult;
    
	
    @Override
    public void init(Synapse synapse) {
    	tauPlusMult = 1 / tauPlus;
        tauXMult = 1 / tauX;
        tauNegMult = 1 / tauNeg;
        tauYMult = 1 / tauY;
    }

    
    @Override
    public String getName() {
        return "Pfister and Gerstner, 2006";
    }

    
    @Override
    public SynapseUpdateRule deepCopy() {
    	// We're only using primitive fields so clone() works.
    	try {
			return (PfisterGerstner2006Rule) this.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
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
			r1 -= r1 * tauPlusMult * timeStep;
			r2 -= r2 * tauXMult * timeStep;
		}
		if (postSpiked) {
			o1 = 1;
			o2 = 1;
		}
		else {
			o1 -= o1 * tauNegMult * timeStep;
			o2 -= o2 * tauYMult * timeStep;
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
		return 1 / tauPlusMult;
	}


	/**
	 * @param tauPlus Decay rate for r1 trace.
	 */
	public void setTauPlusDecay(double tauPlus) {
		this.tauPlusMult = 1 / tauPlus;
	}


	/**
	 * @return Decay rate for r2 trace.
	 */
	public double getTauXDecay() {
		return 1 / tauXMult;
	}


	/**
	 * @param tauX Decay rate for r2 trace.
	 */
	public void setTauXDecay(double tauX) {
		this.tauXMult = 1 / tauX;
	}


	/**
	 * @return Decay rate for o1 trace.
	 */
	public double getTauNegDecay() {
		return 1 / tauNegMult;
	}


	/**
	 * @param tauNeg Decay rate for o1 trace.
	 */
	public void setTauNegDecay(double tauNeg) {
		this.tauNegMult = 1 / tauNeg;
	}


	/**
	 * @return Decay rate for o2 trace.
	 */
	public double getTauYDecay() {
		return 1 / tauYMult;
	}


	/**
	 * @param tauY Decay rate for o2 trace.
	 */
	public void setTauYDecay(double tauY) {
		this.tauYMult = 1 / tauY;
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
