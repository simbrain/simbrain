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

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.SpikingNeuron;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.util.RandomSource;


/**
 * <b>ProbabilisticSpikingNeuron</b>.
 */
public class ProbabilisticSpikingNeuron extends SpikingNeuron {
	
	/** Fire Probability. */
	private double fireProbability = .8;

    /**
     * Default constructor needed for external calls which create neurons then
     * set their parameters.
     */
    public ProbabilisticSpikingNeuron() {
    }

    /**
     * This constructor is used when creating a neuron of one type from another
     * neuron of another type Only values common to different types of neuron
     * are copied.
     *
     * @param n Neuron to be made type integrate and fire
     */
    public ProbabilisticSpikingNeuron(final Neuron n) {
        super(n);
    }

    /**
     * @return duplicate ProbabilisticSpiking (used, e.g., in copy/paste).
     */
    public ProbabilisticSpikingNeuron duplicate() {
        ProbabilisticSpikingNeuron ifn = new ProbabilisticSpikingNeuron();
        ifn = (ProbabilisticSpikingNeuron) super.duplicate(ifn);
        ifn.setFireProbability(getFireProbability());
        
        return ifn;
    }
    /**
     * Update neuron.
     */
    public void update() {
    	
        if (getActivation() >= 1){
        	setHasSpiked(true);
            this.setActivation(0);
            setBuffer(0); //TODO: What's going on here?
        } else{
        	setHasSpiked(false);
        }
    	
        if (this.hasSpiked()) {
        	for (Synapse synapse : this.getFanOut()) {
        		ProbabilisticSpikingNeuron neuron = (ProbabilisticSpikingNeuron) synapse.getTarget();
        		if (Math.random() > fireProbability) {
        			neuron.setBuffer(1);
        			System.out.println("in neuron " + this.getId());
        		}
        	}
        }
    }
    
    public double getFireProbability() {
        return fireProbability;
    }
    
    public void setFireProbability(final double fireProbability) {
        this.fireProbability = fireProbability;
    }
}
