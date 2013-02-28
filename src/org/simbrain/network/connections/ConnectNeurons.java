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
package org.simbrain.network.connections;

import java.util.List;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.util.RandomSource;

/**
 * Subclasses create connections (collections of synapses) between groups of
 * neurons.
 *
 * @author jyoshimi
 */
public abstract class ConnectNeurons {

    /** The network whose neurons are to be connected. */
    protected Network network;

    /**
     * The source group of neurons, generally from which connections will be
     * made.
     */
    protected List<? extends Neuron> sourceNeurons;

    /**
     * The target group of neurons, generally to which connections will be made.
     */
    protected List<? extends Neuron> targetNeurons;

    /** The default ratio of excitatory to inhibitory weights. */
    protected static double DEFAULT_EXCITATORY_RATIO = 1;

    /** The default excitatory strength. */
    protected static double DEFAULT_EXCITATORY_STRENGTH = 1;

    /** The default inhibitory strength. */
    protected static double DEFAULT_INHIBITORY_STRENGTH = -1;

    /**
     * Ratio of excitatory to inhibitory neurons. 1 means all excitatory, 0
     * means all inhibitory.
     */
    protected double excitatoryRatio = DEFAULT_EXCITATORY_RATIO;

    /** Template synapse for excitatory synapses. */
    protected Synapse baseExcitatorySynapse = Synapse.getTemplateSynapse();

    /** Template synapse for inhibitory synapses. */
    protected Synapse baseInhibitorySynapse = Synapse.getTemplateSynapse();

    /** A source of random numbers for inhibitory connections. */
    protected RandomSource inhibitoryRandomizer = new RandomSource();

    /** A source of random numbers for excitatory connections. */
    protected RandomSource excitatoryRandomizer = new RandomSource();

    /** A switch for enabling randomized excitatory connections. */
    protected boolean enableExcitatoryRandomization;

    /** A switch for enabling randomized inhibitory connections. */
    protected boolean enableInhibitoryRandomization;
    
    /** 
     * A flag denoting whether or not the connection is between the same set
     * of neurons: i.e. is recurrent.
     */
    protected boolean recurrent;

    /**
     * Default constructor.
     *
     * @param network network to receive connections
     * @param neurons source neurons
     * @param neurons2 target neurons
     */
    public ConnectNeurons(final Network network,
            final List<? extends Neuron> neurons,
            final List<? extends Neuron> neurons2) {
        this.network = network;
        sourceNeurons = neurons;
        targetNeurons = neurons2;
        recurrent = testRecurrence();
    }

    /**
     * This parameter-free constructor is used in the desktop. User: - Picks a
     * connection style in the GUI - Selects source and target neurons - Invokes
     * connection.
     */
    public ConnectNeurons() {
    }

    /**
     * Apply connection using specified parameters.
     *
     * @param network reference to parent network
     * @param neurons source neurons
     * @param neurons2 target neurons
     */
    public List<Synapse> connectNeurons(final Network network,
            final List<Neuron> neurons, final List<Neuron> neurons2) {
        this.network = network;
        sourceNeurons = neurons;
        targetNeurons = neurons2;
        return connectNeurons();
        
    }

    /**
     * Connect the source to the target neurons using some method.
     */
    public abstract List<Synapse> connectNeurons();

    /**
     * An accessor for the parent network.
     *
     * @return the parent network.
     */
    public Network getNetwork() {
        return network;
    }

    /** 
     * Tests whether or not these connections are recurrent, that is,
     * whether or not the neurons in the source list are the same as those in
     * the target list. 
     * @return true or false: whether or not these connections are recurrent.
     */
    public boolean testRecurrence() {
    	if(sourceNeurons.size() != targetNeurons.size()) {
    		return false;
    	} else {
    		for(int i = 0; i < sourceNeurons.size(); i++) {
    			if(sourceNeurons.get(i) != targetNeurons.get(i)) {
    				return false;
    			}
    		}
    	}
    	return true;    	
    }
    
    /**
     * @return the baseExcitatorySynapse
     */
    public Synapse getBaseExcitatorySynapse() {
        return baseExcitatorySynapse;
    }

    /**
     * @param baseExcitatorySynapse the baseExcitatorySynapse to set
     */
    public void setBaseExcitatorySynapse(Synapse baseExcitatorySynapse) {
        this.baseExcitatorySynapse = baseExcitatorySynapse;
    }

    /**
     * @return the baseInhibitorySynapse
     */
    public Synapse getBaseInhibitorySynapse() {
        return baseInhibitorySynapse;
    }

    /**
     * @param baseInhibitorySynapse the baseInhibitorySynapse to set
     */
    public void setBaseInhibitorySynapse(Synapse baseInhibitorySynapse) {
        this.baseInhibitorySynapse = baseInhibitorySynapse;
    }

    /**
     * Sets the ratio of excitatory to inhibitory connections. The inhibitory
     * part of the ratio is assumed to be 1- excitatoryRatio. So, 1 is all
     * excitatory (1:0), 0 is all inhibitory (0:1), .7 is 70% excitatory
     * (.7:.3), etc.
     *
     * @param excitatoryRatio ratio of excitatory to inhibitory connections
     */
    public void setExcitatoryRatio(double excitatoryRatio) {
        if (excitatoryRatio < 0 || excitatoryRatio > 1.0) {
            throw new IllegalArgumentException("Invalid excitatory ratio value");
        }
        this.excitatoryRatio = excitatoryRatio;
    }


    /**
     * Helper method for setting excitatory ratio using an excitatory
     * percentage.
     *
     * @param percentExcitatory percentage of excitatory neurons to use.
     */
    public void setPercentExcitatory(double percentExcitatory) {
        setExcitatoryRatio(percentExcitatory / 100);
    }

    /**
     * Helper method for setting inhibitory ratio using an inhibitory
     * percentage.
     *
     * @param percentInhibitory percentage of inhibitory neurons to use.
     */
    public void setPercentInhibitory(double percentInhibitory) {
        setExcitatoryRatio((100 - percentInhibitory) / 100);
    }

    public static double getDefaultRatio() {
        return DEFAULT_EXCITATORY_RATIO;
    }

    public RandomSource getInhibitoryRandomizer() {
        return inhibitoryRandomizer;
    }

    public RandomSource getExcitatoryRandomizer() {
        return excitatoryRandomizer;
    }

    public void setEnableExcitatoryRandomization(boolean enableExRand) {
        this.enableExcitatoryRandomization = enableExRand;
    }

    public void setEnableInhibitoryRandomization(boolean enableInRand) {
        this.enableInhibitoryRandomization = enableInRand;
    }

	public List<? extends Neuron> getSourceNeurons() {
		return sourceNeurons;
	}

	public void setSourceNeurons(List<? extends Neuron> sourceNeurons) {
		this.sourceNeurons = sourceNeurons;
		recurrent = testRecurrence();
	}

	public List<? extends Neuron> getTargetNeurons() {
		return targetNeurons;
	}

	public void setTargetNeurons(List<? extends Neuron> targetNeurons) {
		this.targetNeurons = targetNeurons;
		recurrent = testRecurrence();
	}
	
	/**
	 * Sets all the major parameters for the connection: source, target, and
	 * network. This is used in place of a constructor for when other variables
	 * of the connection are being set before a specific network is defined.
	 * @param network the network where the connections are being made
	 * @param source the source neurons
	 * @param target the target neurons
	 */
	public void setParameters(final Network network, NeuronGroup source, NeuronGroup target){
		setParameters(network, source.getNeuronList(), target.getNeuronList());
	}

	/**
	 * Sets all the major parameters for the connection: source, target, and
	 * network. This is used in place of a constructor for when other variables
	 * of the connection are being set before a specific network is defined.
	 * @param network the network where the connections are being made
	 * @param source the source neurons
	 * @param target the target neurons
	 */
	public void setParameters(final Network network, List<Neuron> source, List<Neuron> target){
		this.network = network;
		this.sourceNeurons = source;
		this.targetNeurons = target;
	}
	
}
