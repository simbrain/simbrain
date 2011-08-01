/*
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
package org.simbrain.network.connections;

import java.util.List;
import java.util.Random;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Connect neurons sparsely with some probabilities.  
 * 
 * To be renamed...
 * 
 * @author ztosi
 *
 * TODO:    - Rename this class; it does not necessarily produce "sparse" 
 * 			  connectivity.
 *          - Set weights strengths.
 *          
 */
public class Sparse2 extends ConnectNeurons {

    /** Probability connection will be an excitatory weight. */
    private static double excitatoryProbability;

    /** Probability connection will be an inhibitory weight. */
    private static double inhibitoryProbability;

    /** base synapse is clamped for most sparse connections */
	private static Synapse baseSynapse = Synapse
			.getTemplateSynapse(new ClampedSynapse());
	
	private Random rGen;

	/**
	 * Min and Max weight values. As of now floor ought to be negative and
	 * ceiling positive. Defaults are -1 and 1 respectively TODO: Add warning or
	 * throw exception if above does not hold?
	 * */
	private double floor = -1.0;
	private double ceiling = 1.0;

    /**
     * See super class description.
     *
     * @param network network with neurons to be connected.
     * @param neurons source neurons.
     * @param neurons2 target neurons.
     */
	public Sparse2(final Network network, final List<? extends Neuron> neurons,
			final List<? extends Neuron> neurons2) {
        super(network, neurons, neurons2);
        rGen = new Random();
    }

	/** {@inheritDoc} */
	public Sparse2() {
	}

    @Override
    public String toString() {
        return "Sparse";
    }

    /** @inheritDoc */
    public void connectNeurons(double prob) {

        for (Neuron source : sourceNeurons) {

            for (Neuron target : targetNeurons) {

            	if(Math.random() < prob){
            		 Synapse synapse = baseSynapse
                     	.instantiateTemplateSynapse(source, target, network);
            		 if(rGen.nextBoolean())
            			 synapse.setStrength(excitatoryRand());
            		 else
            			 synapse.setStrength(inhibitoryRand());
            		 
            		 network.addSynapse(synapse);
            		
            	}
            }
        }
    }

	/**
	 * @return a random excitatory weight strength [0, ceiling), if ceiling. is
	 *         positive
	 */
    public double excitatoryRand() {
    	return Math.random() * ceiling;
    }

    /**
     * @return a random inhibitory weight strength (floor, 0], if floor is
     *  negative.
     */
    public double inhibitoryRand() {
    	return Math.random() * floor;
    }

    /**
     * @return the excitatoryProbability
     */
    public static double getExcitatoryProbability() {
        return excitatoryProbability;
    }

    /**
     * @param excitatoryProbability the excitatoryProbability to set
     */
    public static void setExcitatoryProbability
    	(double excitatoryProbability) {
        Sparse.excitatoryProbability = excitatoryProbability;
    }

    /**
     * @return the inhibitoryProbability
     */
    public static double getInhibitoryProbability() {
        return inhibitoryProbability;
    }

    /**
     * @param inhibitoryProbability the inhibitoryProbability to set
     */
    public static void setInhibitoryProbability
    	(double inhibitoryProbability) {
        Sparse.inhibitoryProbability = inhibitoryProbability;
    }

    /**
     * @param theBaseSynapse new synapse type
     */
	public static void setbaseSynapse(Synapse theBaseSynapse) {
		baseSynapse = theBaseSynapse;
	}

	/**
	 * @return the base synapse
	 */
	public static Synapse getbaseSynapse() {
		return baseSynapse;
	}

	public void setFloor(double floor) {
		this.floor = floor;
	}

	public double getFloor() {
		return floor;
	}

	public void setceiling(double ceiling) {
		this.ceiling = ceiling;
	}

	public double getceiling() {
		return ceiling;
	}

	@Override
	public void connectNeurons() {
		// TODO Auto-generated method stub
		
	}

}
