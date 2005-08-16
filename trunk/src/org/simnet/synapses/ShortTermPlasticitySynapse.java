/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.synapses;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.SpikingNeuron;
import org.simnet.interfaces.Synapse;

public class ShortTermPlasticitySynapse extends Synapse {
	
	private double baseLineStrength = 1;
	private double timeConstant = 0;
	private double growthRate = 0;
	private double decayRate = 0;
	
	public ShortTermPlasticitySynapse(Neuron src, Neuron tar, double val, String the_id) {
		source = src;
		target = tar;
		strength = val;
		id = the_id;
	}
	
	public ShortTermPlasticitySynapse() {
	}
	
	public ShortTermPlasticitySynapse(Synapse s) {
		super(s);
	}
	
	public static String getName() {return "Short term plasticity";}

	public Synapse duplicate() {
//		Hebbian h = new Hebbian();
		return null;
	}
	
	/**
	 * Creates a weight connecting source and target neurons
	 * 
	 * @param source source neuron
	 * @param target target neuron
	 */
	public ShortTermPlasticitySynapse(Neuron source, Neuron target) {
		this.source = source;
		this.target = target;
	}

	public void update() {
		
		if (!(this.getSource() instanceof SpikingNeuron)) return;
		
		SpikingNeuron source = (SpikingNeuron)this.getSource();
				
		if (source.hasSpiked()) {
			strength -= (timeConstant * growthRate * (strength - upperBound));			 
			System.out.println("Spike: " + strength);
		} else {
			strength -= (timeConstant * decayRate * (strength - baseLineStrength));
			System.out.println("No spike: " + strength);
		}
		
		strength = clip(strength);
		
	}
	
    /**
     * @return Returns the baseLineStrength.
     */
    public double getBaseLineStrength() {
        return baseLineStrength;
    }
    /**
     * @param baseLineStrength The baseLineStrength to set.
     */
    public void setBaseLineStrength(double baseLineStrength) {
        this.baseLineStrength = baseLineStrength;
    }
    /**
     * @return Returns the decayRate.
     */
    public double getDecayRate() {
        return decayRate;
    }
    /**
     * @param decayRate The decayRate to set.
     */
    public void setDecayRate(double decayRate) {
        this.decayRate = decayRate;
    }
    /**
     * @return Returns the growthRate.
     */
    public double getGrowthRate() {
        return growthRate;
    }
    /**
     * @param growthRate The growthRate to set.
     */
    public void setGrowthRate(double growthRate) {
        this.growthRate = growthRate;
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
    public void setTimeConstant(double timeConstant) {
        this.timeConstant = timeConstant;
    }
}
