/*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.interfaces;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class ActivationRule {


	private final transient String name = null;

	/**
	 * This is the main logic method for the activation function, which must be
	 * overridden by each specific function below. 
	 * 
	 * @param n Neuron to modify
	 */
	protected abstract void apply(Neuron n); // applyulate activation

	/**
	 * Returns a string describing how this activation function works.  Can be
	 * used in providing run-time help for the network designer.
	 * 
	 * @return a string describing how this activation function works
	 */
	public abstract String getHelp(); 
	
	
	public String getName() {
		return name;
	} 

	/**
	 * By default activation functions don't use a threshold value; output functions do
	 * 
	 * @return true if this function uses the threshold value
	 */
	public boolean usesThreshold() {
		return false;
	}
	
	/**
	 * Returns a list of available activation functions. Used, for example,
	 * by a GUI dialog box to populate a combo-box presenting possible activation
	 * functions to select.
	 * 
	 * @return a list of activation functions
	 */
	public static String[] getList() {
		String[] functionList =
			{ "Linear", "Threshold", "Sigmoidal", "Clamped", "Random", "Tanh"};
		return functionList;
	}
	
}
