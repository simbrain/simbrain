/*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.interfaces;

import org.simnet.neurons.rules.Clamped;
import org.simnet.neurons.rules.Identity;
import org.simnet.neurons.rules.Linear;
import org.simnet.neurons.rules.Random;
import org.simnet.neurons.rules.Sigmoidal;
import org.simnet.neurons.rules.Tanh;
import org.simnet.neurons.rules.Threshold;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
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
		} 
		System.out.println("Error: selected function not in internal list");
		return null;
	}

	
}
