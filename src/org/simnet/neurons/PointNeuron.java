/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.neurons;

import java.util.ArrayList;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;


/**
 * <b>PointNeuron</b> from O'Reilley and Munakata, Computational Explorations in Cognitive Neuroscience, chapter 2.
 * All references to charts, etc. below are to chapter 2 of this book.
 */
public class PointNeuron extends Neuron {

	//TODO: Steal javadocs from PointNeuronPanel
	// TODO: Rename badly named variables below
	/** Excitatory Reversal field. */
	double excitatoryReversal = 55;

	/** Inhibitory Reversal field. */
	double inhibitoryReversal = -70;

	/** Leak Reversal field. */
	double leakReversal = -70;

	/** Leak Conductance field. */
	double leakConductance = 2.8;

	/** Threshold for output function. */
	double threshold = 1;

	/** Gain for output function. */
	double gain = 600;

	/** None for output function. */
	private int NONE = 0;

	/** Sigmoidal for output function. */
	private int SIGMOIDAL = 1;

	/** None option for output function. */
	private int outputFunction = NONE;

	/** A normalization factor for excitatory inputs. */
	private double normFactor = 1;

	/** Time averaging for excitatory inputs.  */
	private double timeAveraging = .7;

	/** Bias for excitatory inputs.   */
	private double bias = 0;
    
	/** Previous excitatory current.   */
	double previousExcitatoryCurrent = 0;
    
	/** Excitatory inputs for connected Synapses.   */
	private ArrayList<Synapse> excitatoryInputs = new ArrayList<Synapse>();
    
	/** Inhibitory inputs for connected Synapses.   */
	private ArrayList<Synapse> inhibitoryInputs = new ArrayList<Synapse>();

	/** Output functions    */
	double TimeStep, output, voltage, current;
	
	/** List of output functions */
	private static String[] functionList = {"None", "Sigmoidal" };
    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public PointNeuron() {
    }

    /**
     * TODO: Not really true...
     * @return time type.
     */
    public int getTimeType() {
        return org.simnet.interfaces.Network.DISCRETE;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to make the type
     */
    public PointNeuron(final Neuron n) {
        super(n);
    }
    
    /**
     * Returns the output function list (NONE, SIGMOIDAL).
     * @return Function List
     */
    public static String[] getFunctionList() {
        return functionList;
    }
    /**
     * Returns a duplicate PointNeuron (used, e.g., in copy/paste).
     * @return Duplicated neuron
     */
    public Neuron duplicate() {
        PointNeuron cn = new PointNeuron();
        cn = (PointNeuron) super.duplicate(cn);
        cn.setOutputFunction(getOutputFunction());
        //TODO
        return cn;
    }
    
    /**
     * Sets the output function.
     */
    public void setOutputFunction(final int index) {
        this.outputFunction = index;
    }

    /**
     * @return Returns the implementationIndex
     */
    public int getOutputFunction() {
        return outputFunction;
    }

    /**
     * Update neuron.  See Box 2.2.  Note that projections are not currently used.
     */
    public void update() {


		// Set currents
        setInputLists();
        current = leakConductance * (activation - leakReversal);
        current += getInhibitoryNetInput() * (activation - inhibitoryReversal);
        current +=  getExcitatoryNetInput() * (activation - excitatoryReversal);

        // Update voltage
		voltage = activation - this.getParentNetwork().getTimeStep() * current;

        // Apply output function
		if (outputFunction == NONE) {
	        output = voltage;
		} else if (outputFunction == SIGMOIDAL) {
	        output = sigmoidal(voltage);
		}

	setBuffer(output);
	
    }
        
    /**
     * Update the lists of excitatory and inhibitory currents based on synapse values.
     * TODO: This is majorly slow!  only do it when synapses change!
     */
    private void setInputLists() {
        excitatoryInputs.clear();
        inhibitoryInputs.clear();
        if (fanIn.size() > 0) {
            for (int j = 0; j < fanIn.size(); j++) {
                Synapse synapse = (Synapse) fanIn.get(j);
                if (synapse.getStrength() > 0) {
                    excitatoryInputs.add(synapse);
                } else {
                   inhibitoryInputs.add(synapse);
                }
            }
        }
    }
    
    /**
     * Returns the inhibitory currents.
     *
     * @return inhibitory current
     */
    private double getInhibitoryNetInput() {
        double ret = 0;
        if (inhibitoryInputs.size() > 0) {
            for (int j = 0; j < inhibitoryInputs.size(); j++) {
                Synapse synapse = inhibitoryInputs.get(j);
                Neuron source = synapse.getSource();
                ret += (source.getActivation() * synapse.getStrength());
             }  
             System.out.println("inhibitory = " + ret);            
        }
        return ret;
    }
    
    /**
     * Returns the excitatory net input.  See equation 2.16
     *
     * @return excitatory net input
     */
    private double getExcitatoryNetInput() {
        double ret = 0;
        if (excitatoryInputs.size() > 0) {
            for (int j = 0; j < excitatoryInputs.size(); j++) {
                Synapse synapse = excitatoryInputs.get(j);
                Neuron source = synapse.getSource();
                ret += (source.getActivation() * synapse.getStrength());
             }  
             // TODO: Ask David if N (fan_in.size()) is the same as total inputs or just excitatory inputs
             ret = (1 - timeAveraging) * previousExcitatoryCurrent + timeAveraging *
                     (1 / (normFactor * excitatoryInputs.size()) * ret + (bias / fanIn.size()));  
             previousExcitatoryCurrent = ret;
             System.out.println("excitatory = " + ret);            
        }
        return ret;
    }
    
    /**
     * Equation 2.20 
     *
     * @param input current voltage
     * @return result of sigmoidal output function
     */
	private double sigmoidal(double input) {
		return 1 /(1 + 1/(gain * Math.max(0, input - threshold)));
	}

	/**
	 * @return Name of neuron type.
	 */
	public static String getName() {
		return "Point";
	}

    /**
     * @return Returns the excitatoryReversal.
     */
    public double getExcitatoryReversal() {
        return excitatoryReversal;
    }
		

    /**
     * @param excitatoryReversal The excitatoryReversal to set.
     */
    public void setExcitatoryReversal(double excitatoryReversal) {
        this.excitatoryReversal = excitatoryReversal;
    }

    /**
     * @return Returns the inhibitoryReversal.
     */
    public double getInhibitoryReversal() {
        return inhibitoryReversal;
    }

    /**
     * @param InhibitoryReversal The inhibitoryReversal to set.
     */
    public void setInhibitoryReversal(double inhibitoryReversal) {
        this.inhibitoryReversal = inhibitoryReversal;
    }

    /**
     * @return Returns the leakConductance.
     */
    public double getLeakConductance() {
        return leakConductance;
    }

    /**
     * @param leakConductance The leakConductance to set.
     */
    public void setLeakConductance(double leakConductance) {
        this.leakConductance = leakConductance;
    }

    /**
     * @return Returns the leakReversal.
     */
    public double getLeakReversal() {
        return leakReversal;
    }

    /**
     * @param leakReveral The leak reversal.
     */
    public void setLeakReversal(double leakReversal) {
        this.leakReversal = leakReversal;
    }

    /**
     * @return Returns the gain.
     */
    public double getGain() {
        return gain;
    }


    /**
     * Set the gain.
     *
     * @param gamma gamma to set.
     */
    public void setGain(final double gamma) {
        this.gain = gamma;
    }

    /**
     * @return Returns the threshold.
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * @param threshold The threshold to set.
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    /**
     * @return Returns the bias.
     */
    public double getBias() {
        return bias;
    }

    /**
     * @param bias The bias to set.
     */
    public void setBias(double bias) {
        this.bias = bias;
    }

    /**
     * @return Returns the norm_factor.
     */
    public double getNorm_factor() {
        return normFactor;
    }

    /**
     * @param norm_factor The norm_factor to set.
     */
    public void setNorm_factor(double normFactor) {
        this.normFactor = normFactor;
    }

    /**
     * @return Returns the time_averaging.
     */
    public double getTime_averaging() {
        return timeAveraging;
    }

    /**
     * @param time_averaging The time_averaging to set.
     */
    public void setTime_averaging(double timeAveraging) {
        this.timeAveraging = timeAveraging;
    }



    
}
