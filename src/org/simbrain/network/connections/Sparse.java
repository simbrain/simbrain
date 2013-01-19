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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

/**
 * Connect neurons sparsely.
 *
 * @author ztosi
 * @author jyoshimi
 */
public class Sparse extends ConnectNeurons {

    /** The default sparsity (between 0 and 1). */
    private static double DEFAULT_SPARSITY = 0.1;

    /** The overall sparsity of the connections. */
    private double sparsity = DEFAULT_SPARSITY;

    /**
     * Whether or not sparsity applies a constant number of synapses to each
     * source neuron.
     */
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
    public Sparse(final Network network, final List<? extends Neuron> neurons,
            final List<? extends Neuron> neurons2) {
        super(network, neurons, neurons2);
    }

    /** {@inheritDoc} */
    public Sparse() {
    }

    @Override
    public String toString() {
        return "Sparse";
    }

    /** @inheritDoc */
    public List<Synapse> connectNeurons() {

        ArrayList<Synapse> syns = new ArrayList<Synapse>();

        int numSyns;
        if(!allowSelfConnection && sourceNeurons == targetNeurons){
        	numSyns = (int) (sparsity * sourceNeurons.size() * (targetNeurons.size() - 1));
        } else {
        	numSyns = (int) (sparsity * sourceNeurons.size() * targetNeurons.size() );
        }          
        Neuron source;
        Neuron target;
        Synapse synapse;
        
        if(sparseSpecific){
            ArrayList<Integer> targetList = new ArrayList<Integer>(); 
            for(int i = 0; i < targetNeurons.size(); i++){
            	targetList.add(i);
            }
            Collections.shuffle(targetList);
        	int synsPerSource = numSyns / sourceNeurons.size();
        	for(int i = 0; i < sourceNeurons.size(); i++){
        		source = sourceNeurons.get(i);   	
        		for(int j = 0; j < synsPerSource; j++){
        			if(!allowSelfConnection && sourceNeurons == targetNeurons &&
        					targetList.get(j) == i) {
        				targetList.add(targetList.remove(j));
        			}
        			target = targetNeurons.get(targetList.get(j));
        			if (Math.random() < excitatoryRatio) {
                        synapse = baseExcitatorySynapse.instantiateTemplateSynapse(
                                source, target, network);
                        if (enableExcitatoryRandomization) {
                            synapse.setStrength(excitatoryRandomizer.getRandom());
                        } else {
                            synapse.setStrength(DEFAULT_EXCITATORY_STRENGTH);
                        }
                    } else {
                        synapse = baseInhibitorySynapse.instantiateTemplateSynapse(
                                source, target, network);
                        if (enableInhibitoryRandomization) {
                            synapse.setStrength(inhibitoryRandomizer.getRandom());
                        } else {
                            synapse.setStrength(DEFAULT_INHIBITORY_STRENGTH);
                        }
                    }
        			network.addSynapse(synapse);
                    syns.add(synapse);
        		}
        		Collections.shuffle(targetList);
        	}
        } else {
        	ArrayList<Integer> indices = new ArrayList<Integer>();
        	for(int i = 0; i < (sourceNeurons.size() * targetNeurons.size());
        			i++){
        		int dum = 0;
        		if(!allowSelfConnection && sourceNeurons == targetNeurons && i == dum){
        			dum = dum + targetNeurons.size() + 1;
        		} else {
        			indices.add(i);
        		}
        	}
        	Collections.shuffle(indices);
        	
        	for(int i = 0; i < numSyns; i++){
        		int s = indices.get(i) / targetNeurons.size();
        		int t = indices.get(i) % targetNeurons.size();
        		source = sourceNeurons.get(s);
        		target = targetNeurons.get(t);
        		if (Math.random() < excitatoryRatio) {
                    synapse = baseExcitatorySynapse.instantiateTemplateSynapse(
                            source, target, network);
                    if (enableExcitatoryRandomization) {
                        synapse.setStrength(excitatoryRandomizer.getRandom());
                    } else {
                        synapse.setStrength(DEFAULT_EXCITATORY_STRENGTH);
                    }
                } else {
                    synapse = baseInhibitorySynapse.instantiateTemplateSynapse(
                            source, target, network);
                    if (enableInhibitoryRandomization) {
                        synapse.setStrength(inhibitoryRandomizer.getRandom());
                    } else {
                        synapse.setStrength(DEFAULT_INHIBITORY_STRENGTH);
                    }
                }
    			network.addSynapse(synapse);
                syns.add(synapse);
        	}
        	
        }                   
        return syns;
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
