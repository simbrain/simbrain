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
package org.simnet.interfaces;

import org.simnet.neurons.rules.Clamped;
import org.simnet.neurons.rules.Identity;
import org.simnet.neurons.rules.Linear;
import org.simnet.neurons.rules.Random;
import org.simnet.neurons.rules.Sigmoidal;
import org.simnet.neurons.rules.Tanh;
import org.simnet.neurons.rules.Threshold;


public abstract class ActivationRule {

	private static String[] functionList = { "Linear", "Threshold", "Sigmoidal", "Clamped", "Random", "Tanh", "Identity"};

	public abstract String getName();
	
	/**
	 * This is the main logic method for the activation function, which must be
	 * overridden by each specific function below. 
	 * 
	 * @param n Neuron to modify
	 */
	public abstract void apply(Neuron n); // applyulate activation

	/**
	 * Returns a string describing how this activation function works.  Can be
	 * used in providing run-time help for the network designer.
	 * 
	 * @return a string describing how this activation function works
	 */
	public abstract String getHelp(); 

	
	/**
	 * Returns a list of available activation functions. Used, for example,
	 * by a GUI dialog box to populate a combo-box presenting possible activation
	 * functions to select.
	 * 
	 * @return a list of activation functions
	 */
	public static String[] getList() {
		return functionList;
	}
	
	/**
	 * Helper function for combo boxes.  Associates strings with indices.
	 */	
	public static int getActivationFunctionIndex(String af) {
		for (int i = 0; i < functionList.length; i++) {
			if (af.equals(functionList[i])) {
				return i;
			}
		}
		return 0;
	}
	
	public static ActivationRule getActivationFunction(String functionName) {
		if (functionName.equalsIgnoreCase("Linear")) {
			return new Linear();
		} else if (functionName.equalsIgnoreCase("Clamped")) {
			return new Clamped();
		} else if (functionName.equalsIgnoreCase("Identity")) {
			return new Identity();
		} else if (functionName.equalsIgnoreCase("Sigmoidal")) {
			return new Sigmoidal();
		} else if (functionName.equalsIgnoreCase("Tanh")) {
			return new Tanh();
		} else if (functionName.equalsIgnoreCase("Threshold")) {
			return new Threshold();
		} else if (functionName.equalsIgnoreCase("Random")) {
			return new Random();
		} else if (functionName.equalsIgnoreCase("Add-Linear")) {
			return new Identity();
		} 
		//Last items are temporary for reading in old files
		
		System.out.println("Error: selected function not in internal list");
		return null;
	}

	
}
