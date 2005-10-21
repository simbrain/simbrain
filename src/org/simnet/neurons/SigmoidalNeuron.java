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

import org.simnet.interfaces.Neuron;
import org.simnet.util.RandomSource;

/**
 * 
 * <b>SigmoidalNeuron</b>
 */
public class SigmoidalNeuron extends Neuron {
    
    private static String[] functionList = {"Tanh", "Arctan"};
    private int implementationIndex = 1;
    public static int TANH = 0;
    public static int ARCTAN = 1;
    
    private double bias = 0;
    private double slope = 1;
	private RandomSource noiseGenerator = new RandomSource();
	private boolean addNoise = false;
	private boolean clipping = false;
    
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public SigmoidalNeuron() {
	}
	
	public int getTimeType() {
		return org.simnet.interfaces.Network.DISCRETE;
	}

	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public SigmoidalNeuron(Neuron n) {
		super(n);
	}
	
	public void update() {
		
		double val = this.weightedInputs() + bias;
		
		if(implementationIndex == TANH ) {
			double A = (4 * slope) / (upperBound - lowerBound);
			val = (upperBound - lowerBound) * sigmoidal(A * val) + lowerBound;
		} else if (implementationIndex == ARCTAN) {
			double A = (Math.PI * slope) / (upperBound - lowerBound);
			val = ((upperBound - lowerBound) / Math.PI) * (Math.atan(A * val) + Math.PI / 2) + lowerBound;
		}
		
		if(addNoise == true) {
			val += noiseGenerator.getRandom();
		}
		if (clipping == true) {
			val = clip(val);
		}
		
		setBuffer(val);

		
	}

	private double sigmoidal(double input) {
		return (1 / (1 + Math.exp(-input)));
	}
	
	/**
	 * Returns a duplicate StandardNeuron (used, e.g., in copy/paste)
	 */
	public Neuron duplicate() {
        SigmoidalNeuron sn = new SigmoidalNeuron();
        sn = (SigmoidalNeuron)super.duplicate(sn);
        sn.setBias(getBias());
        sn.setClipping(getClipping());
        sn.setImplementationIndex(getImplementationIndex());
        sn.setSlope(getSlope());
        sn.setAddNoise(getAddNoise());
        sn.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);
        return sn;
	}

    /**
     * @return Returns the inflectionPoint.
     */
    public double getBias() {
        return bias;
    }
    /**
     * @param inflectionPoint The inflectionPoint to set.
     */
    public void setBias(double inflection_y) {
        this.bias = inflection_y;
    }
    /**
     * @return Returns the inflectionPointSlope.
     */
    public double getSlope() {
        return slope;
    }
    /**
     * @param inflectionPointSlope The inflectionPointSlope to set.
     */
    public void setSlope(double inflectionPointSlope) {
        this.slope = inflectionPointSlope;
    }
    /**
     * @return Returns the functionList.
     */
    public static String[] getFunctionList() {
        return functionList;
    }
    /**
     * @param index The impementatinIndex to set
     */
    public void setImplementationIndex(int index){
        this.implementationIndex = index;
    }
    /**
     * @return Returns the implementationIndex
     */
    public int getImplementationIndex(){
        return implementationIndex;
    }
	/**
	 * @return Returns the noise.
	 */
	public RandomSource getNoiseGenerator() {
		return noiseGenerator;
	}
	/**
	 * @param noise The noise to set.
	 */
	public void setNoiseGenerator(RandomSource noise) {
		this.noiseGenerator = noise;
	}
    /**
     * @return Returns the addNoise.
     */
    public boolean getAddNoise() {
        return addNoise;
    }
    /**
     * @param addNoise The addNoise to set.
     */
    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }
    
	public static String getName() {return "Sigmoidal";}
	
	/**
	 * @return Returns the clipping.
	 */
	public boolean getClipping() {
		return clipping;
	}
	/**
	 * @param clipping The clipping to set.
	 */
	public void setClipping(boolean clipping) {
		this.clipping = clipping;
	}

}
