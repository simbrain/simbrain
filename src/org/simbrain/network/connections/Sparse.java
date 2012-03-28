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

/**
 * Connect neurons sparsely with some probabilities.
 *
 * @author ztosi
 * @author jyoshimi
 */
public class Sparse extends ConnectNeurons {

	/** The default sparsity. */
	private static double DEFAULT_SPARSITY = 0.1;

	/** The overall sparsity of the connections. */
    private double sparsity = DEFAULT_SPARSITY;
    
    /**  Whether or not sparsity applies a constant number of synapses to each
     * source neuron. */
    private boolean sparseSpecific;

    /** A switch determining if self connections are possible. */
    private boolean allowSelfConnection;
    
    /**
     * See super class description.
     *
     * @param network network with neurons to be connected.
     * @param neurons source neurons.
     * @param neurons2 target neurons.
     */
    public Sparse(final Network network, final List<? extends Neuron> neurons, final List<? extends Neuron> neurons2) {
        super(network, neurons, neurons2);
    }

    /** {@inheritDoc} */
    public Sparse() {}

    @Override
    public String toString() {
        return "Sparse";
    }

    /** @inheritDoc */
    public void connectNeurons() {
    	
    	int possibleConnects = sourceNeurons.size() * targetNeurons.size();
    	
    	//TODO: Not entirely stable if target neurons are only a partial subset of source or vice versa
    	if(sourceNeurons.containsAll(targetNeurons) && !allowSelfConnection){
    		possibleConnects = possibleConnects - sourceNeurons.size();
    	}

    	int numSyns = (int) (sparsity * possibleConnects);
    	
    	int numExcite = (int) ((percentExcitatory / 100) * numSyns);
    	Neuron source;
    	Neuron target;
    	Synapse synapse;
    	Random randGen = new Random();
    	
    	if (!sparseSpecific) {
    		
	    	for (int i = 0; i < numSyns; i++) {
	    		do {
	    		source = sourceNeurons
	    			.get(randGen.nextInt(sourceNeurons.size()));
	    		target = targetNeurons
	    			.get(randGen.nextInt(targetNeurons.size()));
	    		} while (Network.getSynapse(source, target) != null ||
	    				(!allowSelfConnection && (source == target)));
	    		
	    		
	    		
	    		if(i < numExcite){
	    			synapse = baseExcitatorySynapse
	                	.instantiateTemplateSynapse(source, target, network);
	    			if(enableExRand){
	    				synapse.setStrength(excitatoryRand.getRandom());
	    			} else {
	    				synapse.setStrength(DEFAULT_EXCITATORY_STRENGTH);
	    			}
	    		} else {
	    			synapse = baseInhibitorySynapse
	                	.instantiateTemplateSynapse(source, target, network);
	    			if(enableInRand) {
	    				synapse.setStrength(inhibitoryRand.getRandom());
	    			} else {
	    				synapse.setStrength(DEFAULT_INHIBITORY_STRENGTH);
	    			}
	    		}
	    		
	    		network.addSynapse(synapse);
	    		
	    	}

    	} else {
    		int synsPerSource = numSyns / sourceNeurons.size();
    		Random rGen = new Random();
    		int numEx = (int) ((percentExcitatory / 100) * numSyns);
    		int numIn = numSyns - numEx;
    		
    		for (int i = 0; i < sourceNeurons.size(); i++) {
    			source = sourceNeurons.get(i);
    			for (int j = 0; j < synsPerSource; j++) {
    				
    				do { 
    					target = targetNeurons.get(randGen.nextInt(targetNeurons.size()));
    				} while (Network.getSynapse(source, target) != null || 
    	    				(!allowSelfConnection && (source == target)));
    				int ex = rGen.nextInt(numEx + numIn);
    				if(ex < numEx){
    					numEx--;
    					synapse = baseExcitatorySynapse
	                		.instantiateTemplateSynapse(source, target, network);
    					if(enableExRand) {
    						synapse.setStrength(excitatoryRand.getRandom());
    					} else {
    						synapse.setStrength(DEFAULT_EXCITATORY_STRENGTH);
    					}
    					
    				} else {
    					numIn--;
    					synapse = baseInhibitorySynapse
	                		.instantiateTemplateSynapse(source, target, network);
    					if(enableInRand) {
    						synapse.setStrength(inhibitoryRand.getRandom());
    					} else {
    						synapse.setStrength(DEFAULT_INHIBITORY_STRENGTH);
    					}
    				}
    				network.addSynapse(synapse);
    			}
    			
    		}
    		
    	}
    }
    
    /** 
     * A method for determining if the target neurons are a subset of the
     * source neurons.
     * @return if the source list contains the members of the target list
     */
    public boolean sourceContainsTarget(){
    	if(sourceNeurons.contains(targetNeurons)) {
    		return true;
    	} else {
    		return false;
    	}
    }

	public static double getDEFAULT_SPARSITY() {
		return DEFAULT_SPARSITY;
	}

	public static void setDEFAULT_SPARSITY(double dEFAULTSPARSITY) {
		DEFAULT_SPARSITY = dEFAULTSPARSITY;
	}

	public double getSparsity() {
		return sparsity;
	}

	public void setSparsity(double sparsity) {
		this.sparsity = sparsity;
	}

	public boolean isSparseSpecific() {
		return sparseSpecific;
	}

	public void setSparseSpecific(boolean sparseSpecific) {
		this.sparseSpecific = sparseSpecific;
	}

	public boolean isAllowSelfConnection() {
		return allowSelfConnection;
	}

	public void setAllowSelfConnection(boolean allowSelfConnection) {
		this.allowSelfConnection = allowSelfConnection;
	}

}
