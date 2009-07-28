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

import java.util.ArrayList;

import javax.swing.ComboBoxModel;

import org.simbrain.network.interfaces.BiasedNeuron;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.util.SimbrainMath;

/**
 * <b>PointNeuron</b> from O'Reilley and Munakata, Computational Explorations
 * in Cognitive Neuroscience, chapter 2.  All page references below are are to this book.
 */
public class PointNeuron extends Neuron implements BiasedNeuron {

	
	/** AUTO GENEATED FIELD GETTERS AND SETTERS*/	

	
    public ArrayList<Synapse> getInhibitoryInputs() {
		return inhibitoryInputs;
	}

	public void setInhibitoryInputs(ArrayList<Synapse> inhibitoryInputs) {
		this.inhibitoryInputs = inhibitoryInputs;
	}

	public double getPreviousExcitatoryConductance() {
		return previousExcitatoryConductance;
	}

	public void setPreviousExcitatoryConductance(
			double previousExcitatoryConductance) {
		this.previousExcitatoryConductance = previousExcitatoryConductance;
	}

	public double getNetTimeConstant() {
		return netTimeConstant;
	}

	public void setNetTimeConstant(double netTimeConstant) {
		this.netTimeConstant = netTimeConstant;
	}

	public double getExcitatoryMaxConductance() {
		return excitatoryMaxConductance;
	}

	public void setExcitatoryMaxConductance(double excitatoryMaxConductance) {
		this.excitatoryMaxConductance = excitatoryMaxConductance;
	}

	public double getExcitatoryConductance() {
		return excitatoryConductance;
	}

	public void setExcitatoryConductance(double excitatoryConductance) {
		this.excitatoryConductance = excitatoryConductance;
	}

	public double getMembranePotential() {
		return membranePotential;
	}

	public void setMembranePotential(double membranePotential) {
		this.membranePotential = membranePotential;
	}

	public double getExcitatoryReversal() {
		return excitatoryReversal;
	}

	public void setExcitatoryReversal(double excitatoryReversal) {
		this.excitatoryReversal = excitatoryReversal;
	}

	public double getLeakReversal() {
		return leakReversal;
	}

	public void setLeakReversal(double leakReversal) {
		this.leakReversal = leakReversal;
	}

	public double getLeakMaxConductance() {
		return leakMaxConductance;
	}

	public void setLeakMaxConductance(double leakMaxConductance) {
		this.leakMaxConductance = leakMaxConductance;
	}

	public double getLeakConductance() {
		return leakConductance;
	}

	public void setLeakConductance(double leakConductance) {
		this.leakConductance = leakConductance;
	}

	public double getNetCurrent() {
		return netCurrent;
	}

	public void setNetCurrent(double netCurrent) {
		this.netCurrent = netCurrent;
	}

	public double getPotentialTimeConstant() {
		return potentialTimeConstant;
	}

	public void setPotentialTimeConstant(double potentialTimeConstant) {
		this.potentialTimeConstant = potentialTimeConstant;
	}

	public double getExcitatoryCurrent() {
		return excitatoryCurrent;
	}

	public void setExcitatoryCurrent(double excitatoryCurrent) {
		this.excitatoryCurrent = excitatoryCurrent;
	}

	public double getLeakCurrent() {
		return leakCurrent;
	}

	public void setLeakCurrent(double leakCurrent) {
		this.leakCurrent = leakCurrent;
	}

	public OutputFunction getCurrentOutputFunction() {
		return currentOutputFunction;
	}

	public void setCurrentOutputFunction(OutputFunction currentOutputFunction) {
		this.currentOutputFunction = currentOutputFunction;
	}

	public double getOutput() {
		return output;
	}

	public void setOutput(double output) {
		this.output = output;
	}

	public int getGain() {
		return gain;
	}

	public void setGain(int gain) {
		this.gain = gain;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public double getDur() {
		return dur;
	}

	public void setDur(double dur) {
		this.dur = dur;
	}

	public double getRefractoryPotential() {
		return refractoryPotential;
	}

	public void setRefractoryPotential(double refractoryPotential) {
		this.refractoryPotential = refractoryPotential;
	}

	public void setExcitatoryInputs(ArrayList<Synapse> excitatoryInputs) {
		this.excitatoryInputs = excitatoryInputs;
	}

	public void setInhibitoryCurrent(double inhibitoryCurrent) {
		this.inhibitoryCurrent = inhibitoryCurrent;
	}
	
	
	/** POINT NEURON PRIVATE FIELDS*/
	

	/** Excitatory inputs for connected Synapses. */
    private ArrayList<Synapse> excitatoryInputs = new ArrayList<Synapse>();

    /** Inhibitory inputs for connected Synapses. */
    private ArrayList<Synapse> inhibitoryInputs = new ArrayList<Synapse>();

    /** Previous excitatory conductance field. */
    private double previousExcitatoryConductance = 0;
    
	/** Time average constant for updating the net current field. (p. 43-44)*/
	private double netTimeConstant = 0.7;

	/** Max excitatory conductance field. Conductance if all channels are open. (p. 49)*/ 
	private double excitatoryMaxConductance = 0.4;
	
	/** Excitatory conductance field. Proportion of channels open. */
	private double excitatoryConductance = 0;

	/** Membrane potential field. (p. 45)*/
	private double membranePotential = 0.15;

	/** Excitatory reversal potential field. (p. 45)*/
	private double excitatoryReversal = 1;

	/** Leak reversal potential field. (p. 45)*/
	private double leakReversal = 0.15;

	/** Max leak conductance field. Conductance if all channels are open. (p. 49)*/
	private double leakMaxConductance = 2.8;

	/** Leak Conductance field. Proportion of channels open. (p. 49)*/
	private double leakConductance = 1;
	
	/** Net current field. Sum of all currents. */
	private double netCurrent = 0;

	/** Time averaging constant for updating the membrane potential field. (p. 37, Equation 2.7)*/
	private double potentialTimeConstant = 0.1;

	/** Excitatory current field. */
	private double excitatoryCurrent = 0;

	/** Leak current field. */
	private double leakCurrent = 0;

	/** Inhibitory current field. */
	private double inhibitoryCurrent = 0;
	
	/** Availalbe output functions. (p. 45-48) */
	private enum OutputFunction {DISCRETE_SPIKING, RATE_CODE, NOISY_RATE_CODE};

	/** Sets output function. */
	private OutputFunction currentOutputFunction = OutputFunction.DISCRETE_SPIKING;
	
	// Temporary variable for testing
	private double output = 0;

	/** Gain factor for output function. (p. 46)*/
	private int gain = 600;

	/** Threshold of excitation field. (p. 45)*/
	private double threshold = 0.25;
	
	/** Duration of spike for DISCRETE_SPIKING output function. 
	 * Used to extend spike across multiple cycles. (p. 46) */
	private double dur = 1; //TODO: Implement and verify against Emergent
	
	/** Membrane potential after spike for DISCRETE_SPIKING output function. (p. 46) */
	private double refractoryPotential = 0;
    
    /**
     * Default constructor needed for external calls which create neurons then
     * set their parameters.
     */
	
	
	
    public PointNeuron() {
    }

    /**
     * Init.
     */
    public void postUnmarshallingInit() {
        super.postUnmarshallingInit();
        this.setInputLists();
    }
    
    /**
     * Update the lists of excitatory and inhibitory currents based on synapse
     * values.
     */
    private void setInputLists() {
        excitatoryInputs.clear();
        inhibitoryInputs.clear();
        
        for (Synapse synapse : getFanIn()) {
            if (synapse.getStrength() > 0) {
                excitatoryInputs.add(synapse);
            } else {
                inhibitoryInputs.add(synapse);
            }
        }
    }


    /**
     * This constructor is used when creating a neuron of one type from another
     * neuron of another type Only values common to different types of neuron
     * are copied.
     *
     * @param n Neuron to make the type
     */
    public PointNeuron(final Neuron n) {
        super(n);
        postUnmarshallingInit();
    }

    /**
     * @inheritDoc Network.
     */
    public int getTimeType() {
        return org.simbrain.network.interfaces.RootNetwork.DISCRETE;
    }

    /**
     * Returns a duplicate PointNeuron (used, e.g., in copy/paste).
     *
     * @return Duplicated neuron
     */
    public PointNeuron duplicate() {
        PointNeuron cn = new PointNeuron();
        cn = (PointNeuron) super.duplicate(cn);
        return cn;
    }
    

    /**
     * @inheritDoc org.simnet.interfaces.Neuron
     */
    public void clear() {
        activation = 0;
        setInputLists();
    }

	@Override
	public void update() {
	
	/** Calculate the excitatory conductance using time averaging constant. (p. 44 Eq. 2.16) */	
	excitatoryConductance = (1-netTimeConstant) * excitatoryConductance +  netTimeConstant * (getExcitatoryInputs());
		//TODO: Add scaling parameters and bias term.
	
	/** Calculate the excitatory current given the time averaged excitatory conductance. (p. 37 eq. 2.5) */
	excitatoryCurrent  = excitatoryConductance * excitatoryMaxConductance * (membranePotential - excitatoryReversal);
	
	/** Calculate the leak current.(p. 37 eq. 2.5) */
	leakCurrent = leakConductance * leakMaxConductance * (membranePotential - leakReversal);
	
	/** Calculate the net current. (p. 37 eq. 2.6) */
	netCurrent = leakCurrent + excitatoryCurrent + inhibitoryCurrent;
	
	/** Calculate the time averaged membrane potential given net current. (p. 37 eq. 2.7) */
	membranePotential+= - potentialTimeConstant * netCurrent ;		
		
	/** Apply output function. (p. 45-48) */
		if (currentOutputFunction == OutputFunction.DISCRETE_SPIKING) {
			if (membranePotential > threshold) {
				output = 1;
				membranePotential = refractoryPotential;
			} else {
				output = 0;
			}
		} else if (currentOutputFunction == OutputFunction.RATE_CODE) {
			output = (gain * getPositiveComponent(membranePotential - threshold))
					/ (gain	* getPositiveComponent(membranePotential - threshold) + 1);
		} else if (currentOutputFunction == OutputFunction.NOISY_RATE_CODE) {
			output = 1;
		}
	/** Display current values of variables for diagnostics. */
	printState();
	}
	
	
	private void printState() {
//		System.out.println("getExcitatoryInputs:" + getExcitatoryInputs());
//		System.out.println("excitatoryConductance:" + excitatoryConductance);
//		System.out.println("excitatoryMaxConductance:" + excitatoryMaxConductance);
//		System.out.println("excitatoryReversal:" + excitatoryReversal);
//	
//		System.out.println("leakConductance:" + leakConductance);
//		System.out.println("leakMaxConductance:" + leakMaxConductance);
//		System.out.println("leakReversal" + leakReversal);
		
		System.out.println("excitatoryCurrent:" + excitatoryCurrent);	
		System.out.println("leakCurrent:" + leakCurrent);
		
		System.out.println("netCurrent:" + netCurrent);
		
		System.out.println("membranPotential:" + membranePotential);
		
		System.out.println("output:" + output);
	}

	
	/** PIONT NUERON PRIVATE METHODS */
	
	
	/** Input times weight */ 
	//TODO: Expand for projection level scaling.
	private double getExcitatoryInputs() {

		double retVal =0;
		if (excitatoryInputs.size() > 0) {
	        	for (Synapse synapse : excitatoryInputs) {
		            Neuron source = synapse.getSource();
		            retVal += source.getActivation() * synapse.getStrength();
	        	}
		}
         	return retVal;
	}
	
	private double getPositiveComponent(double val) {

		if (val > 0) {
			return val;
		} else {
			return 0;
		}
	}	
	
	
	/** AUTO GENERATED METHOD GETTERS AND SETTERS */
	
	
	private double getInhibitoryCurrent() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void setExcitatoryConductance() {

		excitatoryConductance = (1-netTimeConstant) * excitatoryConductance +  
		netTimeConstant * (getExcitatoryInputs());

	}

	public double getInhibitoryReversal() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getOutputFunction() {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getTimeAveraging() {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getNormFactor() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setOutputFunction(int selectedIndex) {
		// TODO Auto-generated method stub
		
	}

	public void setInhibitoryReversal(double parseDouble) {
		// TODO Auto-generated method stub
		
	}

	public void setGain(double parseDouble) {
		// TODO Auto-generated method stub
		
	}

	public void setTimeAveraging(double parseDouble) {
		// TODO Auto-generated method stub
		
	}

	public void setNormFactor(double parseDouble) {
		// TODO Auto-generated method stub
		
	}

	public double getBias() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setBias(double bias) {
		// TODO Auto-generated method stub
		
	}

	public double getThresholdInhibitoryConductance() {
		// TODO Auto-generated method stub
		return 0;
	}

}
