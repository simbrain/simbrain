/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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

import org.simnet.interfaces.ActivationRule;
import org.simnet.interfaces.Neuron;

public class StandardNeuron extends Neuron{
	
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public StandardNeuron() {
	}
	
	public void update() {
		activationFunction.apply(this);
		this.checkBounds();
	}

	public Neuron duplicate() {
		StandardNeuron sn = new StandardNeuron();
		return super.duplicate(sn);
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public StandardNeuron(Neuron n) {
		super(n);
	}
	
	/**
	 * Construct a neuron using an array of parameter values
	 * @param values
	 */
	public StandardNeuron(String[] values) {
		if (values.length < NUM_PARAMETERS) {
			System.out.println("Problem reading neuron parameters");
			return;
		}
		setParameters(values);
	}
	/**
	 * Set specified values of this neuron by passing a string of parameter values, with nulls for
	 * values not to change.
	 * 
	 * @param values a set of new values for this neuron
	 */
	public void setParameters(String[] values) {
		//System.out.println("set:" + java.util.Arrays.asList(values));
		if (values[0] != null)
			id = values[0];
		if (values[1] != null) {
			//For compatibility with older versions of Simbrain	
			if(values[1].equalsIgnoreCase("false") || values[1].equalsIgnoreCase("true")) {
				isInput = Boolean.valueOf(values[1]).booleanValue();
			}
			 else {
			 	String input = values[1]; 				
				if ((input.equals("not_input")) || (input.equals("0")) || (input.equals(""))) {
					isInput= false;
				} else {
					isInput = true; 
					inputLabel = values[1];
				}
			}
		}
		if (values[2] != null) {
			//For compatibility with older versions of Simbrain	
			if(values[2].equalsIgnoreCase("false") || values[2].equalsIgnoreCase("true")) {
				isOutput = Boolean.valueOf(values[2]).booleanValue();
			}
			else {
				outputLabel = values[2];				
				if ((outputLabel.equals("not_output")) || (outputLabel.equals("0")) || (outputLabel.equals(""))) {
					isOutput = false;
				} else {
					isOutput = true;
				}
			} //TODO This must change.  Must get rid of true /false
		}
		if (values[3] != null)
			activationFunction = ActivationRule.getActivationFunction(values[3]);
		if (values[5] != null)
			activation = Double.parseDouble(values[5]);
		if (values[6] != null)
			lowerBound = Double.parseDouble(values[6]);
		if (values[7] != null)
			upperBound = Double.parseDouble(values[7]);
		if (values[9] != null)
			;
		if (values[10] != null)
			;
		if (values[11] != null)
			increment = Double.parseDouble(values[11]);
		if (values[12] != null)
			decay = Double.parseDouble(values[12]);
		if (values[13] != null)
			bias = Double.parseDouble(values[13]);
	}

	/**
	 * Get an array of strings describing this neuron's state and paramter settings
	 * 
	 * @param values an array of Strings containing state and parameter settings for this neuron
	 * 
	 */	
	public String[] getParameters() {
		//TODO: Call in net serializer?
		String[] retString = {
			"",
			inputLabel,
			getOutputLabel(),
			getActivationFunction().getName(),
			"",
			Double.toString(getActivation()),
			Double.toString(getLowerBound()),
			Double.toString(getUpperBound()), "","","",
			//Double.toString(getOutputSignal()),
			//Double.toString(getOutputThreshold()),
			//Double.toString(getActivationThreshold()),
			Double.toString(getIncrement()),
			Double.toString(getDecay()),
			Double.toString(getBias())
		};
		//System.out.println("get:" + Arrays.asList(retString));
		return retString;
	}

	public static String getName() {return "Standard";}

}