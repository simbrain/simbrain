/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;

/**
 * Connect every source neuron to every target neuron.
 *
 * @author jyoshimi
 * @author ztosi
 */
public class AllToAll extends ConnectNeurons {

    /** Allows neurons to have a self connection. */
    private boolean allowSelfConnection = true;

    /**
     * Construct all to all connection object.
     * 
     * @param network parent network
     * @param neurons base neurons
     * @param neurons2 target neurons
     */
    public AllToAll(final Network network,
            final List<? extends Neuron> neurons,
            final List<? extends Neuron> neurons2) {
        super(network, neurons, neurons2);
    }

    /**
     * Construct all to all connection object specifying only
     * the parent network
     * 
     * @param network parent network
     */
    public AllToAll(final Network network) {
        this.network = network;
    }

    /** {@inheritDoc} */
    public AllToAll() {
    }

    @Override
    public String toString() {
        return "All to all";
    }

    /** {@inheritDoc} */
    public List<Synapse> connectNeurons() {
    	ArrayList<Synapse> syns = new ArrayList<Synapse>();
    	Random rGen = new Random();
    	int numConnects = 0;
 
    	numConnects = sourceNeurons.size() * targetNeurons.size();
    	
        int numEx = (int) (percentExcitatory * numConnects);
        int numIn = numConnects - numEx;
    	
        //TODO: percent excititory currently not guaranteed for recurrent
        //connections (source list == target list) when self connection is
        //not allowed
        
        for(Neuron source : sourceNeurons) {
        	for(Neuron target : targetNeurons) {
        		if(!(!(Network.getSynapse(source, target) == null) ||
        				(!allowSelfConnection && (source == target)))) {
        			Synapse synapse = null;
        			int ex = rGen.nextInt(numEx + numIn);
        			if(ex < numEx) {
        				numEx--;
        				synapse = baseExcitatorySynapse.
        					instantiateTemplateSynapse(source, target, network);
        				if(enableExRand){
    	    				synapse.setStrength(excitatoryRand.getRandom());
    	    			} else {
    	    				synapse.setStrength(DEFAULT_EXCITATORY_STRENGTH);
    	    			}
        			} else {
        				numIn--;
        				synapse = baseInhibitorySynapse.
    						instantiateTemplateSynapse(source, target, network);
        				if(enableInRand){
        					synapse.setStrength(inhibitoryRand.getRandom());
        				} else {
        					synapse.setStrength(DEFAULT_INHIBITORY_STRENGTH);
        				}
        			}
        			network.addSynapse(synapse);
        			syns.add(synapse);
        		}
        	}
        }
        
        return syns;

    }

    /**
     * @return the allowSelfConnection
     */
    public boolean isAllowSelfConnection() {
        return allowSelfConnection;
    }

    /**
     * @param allowSelfConnection the allowSelfConnection to set
     */
    public void setAllowSelfConnection(boolean allowSelfConnection) {
        this.allowSelfConnection = allowSelfConnection;
    }
}
