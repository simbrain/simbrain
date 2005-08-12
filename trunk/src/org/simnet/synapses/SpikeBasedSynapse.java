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
import org.simnet.interfaces.Synapse;


public class SpikeBasedSynapse extends Synapse {
	
	private double timeConstant = 1;
	private double openRate = 0;
	private double closeRate = 0;
	
	public SpikeBasedSynapse(Neuron src, Neuron tar, double val, String the_id) {
		source = src;
		target = tar;
		strength = val;
		id = the_id;
	}
	
	public SpikeBasedSynapse() {
	}
	
	public SpikeBasedSynapse(Synapse s) {
		super(s);
	}
	
	public static String getName() {return "Spike Based";}

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
	public SpikeBasedSynapse(Neuron source, Neuron target) {
		this.source = source;
		this.target = target;
	}

	public void update() {
		
//		setStrength(getStrength() + momentum * ((getSource().getActivation())
//				* getTarget().getActivation()));
//	
//		checkBounds();
	}
	
	/**
	 * @return Returns the momentum.
	 */
	public double getTimeConstant() {
		return timeConstant;
	}
	/**
	 * @param momentum The momentum to set.
	 */
	public void setTimeConstant(double momentum) {
		this.timeConstant = momentum;
	}
    /**
     * @return Returns the closeRate.
     */
    public double getCloseRate() {
        return closeRate;
    }
    /**
     * @param closeRate The closeRate to set.
     */
    public void setCloseRate(double closeRate) {
        this.closeRate = closeRate;
    }
    /**
     * @return Returns the openRate.
     */
    public double getOpenRate() {
        return openRate;
    }
    /**
     * @param openRate The openRate to set.
     */
    public void setOpenRate(double openRate) {
        this.openRate = openRate;
    }
}
