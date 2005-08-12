/*
 * Created on Aug 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.simnet.synapses;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;

/**
 * @author Kyle Baron
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class NormalizedHebbianSynapse extends Synapse {
	private double momentum = 1;
	private double overallWeight = 0;
	private int summation = 0;
	
	public NormalizedHebbianSynapse() {
	}
	
	public NormalizedHebbianSynapse(Synapse s) {
		super(s);
	}
	
	public static String getName() {return "Normalized Hebbian";}

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
	public NormalizedHebbianSynapse(Neuron source, Neuron target) {
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
	public double getMomentum() {
		return momentum;
	}
	/**
	 * @param momentum The momentum to set.
	 */
	public void setMomentum(double momentum) {
		this.momentum = momentum;
	}
    /**
     * @return Returns the overallWeight.
     */
    public double getOverallWeight() {
        return overallWeight;
    }
    /**
     * @param overallWeight The overallWeight to set.
     */
    public void setOverallWeight(double overallWeight) {
        this.overallWeight = overallWeight;
    }
    /**
     * @return Returns the summation.
     */
    public int getSummation() {
        return summation;
    }
    /**
     * @param summation The summation to set.
     */
    public void setSummation(int summation) {
        this.summation = summation;
    }
}
