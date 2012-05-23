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

    /** The default percent excitatory/inhibitory. */
	protected static double DEFAULT_RATIO = 0.5;
	
	/** The default excitatory strength. */
	protected static double DEFAULT_EXCITATORY_STRENGTH = 1;
	
	/** The default inhibitory strength. */
	protected static double DEFAULT_INHIBITORY_STRENGTH = -1;
    
    /** Percent of connections which are excitatory. */
	protected double percentExcitatory = DEFAULT_RATIO;
	
	/** Percent of connections which are inhibitory */
	protected double percentInhibitory = 1-DEFAULT_RATIO;
    
    /** Template synapse for excitatory synapses. */
    protected Synapse baseExcitatorySynapse = Synapse.getTemplateSynapse();

    /** Template synapse for inhibitory synapses. */
    protected Synapse baseInhibitorySynapse = Synapse.getTemplateSynapse();

    /** A source of random numbers for inhibitory connections. */
    protected RandomSource inhibitoryRand = new RandomSource();
    
    /** A source of random numbers for excitatory connections. */
    protected RandomSource excitatoryRand = new RandomSource();
    
    /** A switch for enabling randomized excitatory connections. */
    protected boolean enableExRand;
    
    /** A switch for enabling randomized inhibitory connections. */
    protected boolean enableInRand;
    
    /**
     * Default constructor.
     *
     * @param network network to receive  connections
     * @param neurons source neurons
     * @param neurons2 target neurons
     */
    public ConnectNeurons(final Network network,
    		final List<? extends Neuron> neurons,
    		final List<? extends Neuron> neurons2) {
        this.network = network;
        sourceNeurons = neurons;
        targetNeurons = neurons2;
    }

    /**
     * This parameter-free constructor is used in the desktop.  User:
     *  - Picks a connection style in the GUI
     *  - Selects source and target neurons
     *  - Invokes connection.
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
     * @return the parent network.
     */
    public Network getNetwork(){
    	return network;
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
     * Returns the percent of connections which are excitatory.
     * @return the percent of excitatory connections.
     */
    public double getPercentExcitatory() {
		return percentExcitatory;
	}

    /**
     * Sets the percent of connections which are excitatory and 
     * simultaneously sets the percent of connections which are
     * inhibitory (1-percentExcitatory)
     * @param percentExcitatory percent of connections to be made excitatory
     */
	public void setPercentExcitatory(double percentExcitatory) {
		if(percentExcitatory < 0 || percentExcitatory > 1.0){
			throw new IllegalArgumentException("Invalid excitatory percent value");
		}
		this.percentExcitatory = percentExcitatory;
		percentInhibitory = 1 - percentExcitatory;
	}

	/**
	 * Sets the percent of connections which are inhibitory and 
     * simultaneously sets the percent of connections which are
     * excitatory (1-percentInhibitory)
	 * @param percentInhibitory percent of connections to be made inhibitory
	 */
	public void setPercentInhibitory(double percentInhibitory) {
		if(percentInhibitory < 0 || percentExcitatory > 1.0){
			throw new IllegalArgumentException("Invalid Inhibitory percent value");
		}
		this.percentInhibitory = percentInhibitory;
		percentExcitatory = 1 - percentInhibitory;
	}
	
	public double getPercentInhibitory() {
		return percentInhibitory;
	}

	public static double getDefaultRatio() {
		return DEFAULT_RATIO;
	}

	public RandomSource getInhibitoryRand() {
		return inhibitoryRand;
	}

	public void setInhibitoryRand(RandomSource inhibitoryRand) {
		this.inhibitoryRand = inhibitoryRand;
	}

	public RandomSource getExcitatoryRand() {
		return excitatoryRand;
	}

	public void setExcitatoryRand(RandomSource excitatoryRand) {
		this.excitatoryRand = excitatoryRand;
	}

	public List<? extends Neuron> getSourceNeurons() {
		return sourceNeurons;
	}

	public void setSourceNeurons(List<? extends Neuron> sourceNeurons) {
		this.sourceNeurons = sourceNeurons;
	}

	public List<? extends Neuron> getTargetNeurons() {
		return targetNeurons;
	}

	public void setTargetNeurons(List<? extends Neuron> targetNeurons) {
		this.targetNeurons = targetNeurons;
	}

	public static double getDEFAULT_RATIO() {
		return DEFAULT_RATIO;
	}

	public static void setDEFAULT_RATIO(double dEFAULTRATIO) {
		DEFAULT_RATIO = dEFAULTRATIO;
	}

	public static double getDEFAULT_EXCITATORY_STRENGTH() {
		return DEFAULT_EXCITATORY_STRENGTH;
	}

	public static void setDEFAULT_EXCITATORY_STRENGTH(
			double dEFAULTEXCITATORYSTRENGTH) {
		DEFAULT_EXCITATORY_STRENGTH = dEFAULTEXCITATORYSTRENGTH;
	}

	public static double getDEFAULT_INHIBITORY_STRENGTH() {
		return DEFAULT_INHIBITORY_STRENGTH;
	}

	public static void setDEFAULT_INHIBITORY_STRENGTH(
			double dEFAULTINHIBITORYSTRENGTH) {
		DEFAULT_INHIBITORY_STRENGTH = dEFAULTINHIBITORYSTRENGTH;
	}

	public boolean isEnableExRand() {
		return enableExRand;
	}

	public void setEnableExRand(boolean enableExRand) {
		this.enableExRand = enableExRand;
	}

	public boolean isEnableInRand() {
		return enableInRand;
	}

	public void setEnableInRand(boolean enableInRand) {
		this.enableInRand = enableInRand;
	}

}
