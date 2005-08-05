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


public class SigmoidalNeuron extends Neuron {
    
    private static String[] functionList = {"Tanh", "Arctan"};
    private int implementationIndex = 1;
    public static int TANH = 0;
    public static int ARCTAN = 1;
    
    private double inflectionPoint = 0;
    private double inflectionPointSlope = 1;
    
    
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public SigmoidalNeuron() {
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public SigmoidalNeuron(Neuron n) {
		super(n);
	}
	
	public void update() {
		
		double val = this.weightedInputs();
		
		if(implementationIndex == TANH ) {
			double A = (4 * inflectionPointSlope) / (upperBound - lowerBound);
			val = (upperBound - lowerBound) * sigmoidal(A * (val - inflectionPoint)) + lowerBound;
			setBuffer(val);
		} else if (implementationIndex == ARCTAN) {
			double A = (Math.PI * inflectionPointSlope) / (upperBound - lowerBound);
			val = ((upperBound - lowerBound) / Math.PI) * (Math.atan(A * (val - inflectionPoint)) + Math.PI / 2) + lowerBound;
			setBuffer(val);
		}
			
	}

	private double sigmoidal(double input) {
		return (1 / (1 + Math.exp(-input)));
	}
	
	/**
	 * Returns a duplicate StandardNeuron (used, e.g., in copy/paste)
	 */
	public Neuron duplicate() {
//		StandardNeuron sn = new StandardNeuron();
//		return super.duplicate(sn);
	    return null;
	}

    /**
     * @return Returns the inflectionPoint.
     */
    public double getInflectionPoint() {
        return inflectionPoint;
    }
    /**
     * @param inflectionPoint The inflectionPoint to set.
     */
    public void setInflectionPoint(double inflectionPoint) {
        this.inflectionPoint = inflectionPoint;
    }
    /**
     * @return Returns the inflectionPointSlope.
     */
    public double getInflectionPointSlope() {
        return inflectionPointSlope;
    }
    /**
     * @param inflectionPointSlope The inflectionPointSlope to set.
     */
    public void setInflectionPointSlope(double inflectionPointSlope) {
        this.inflectionPointSlope = inflectionPointSlope;
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
    
	public static String getName() {return "Sigmoidal";}
	
}
