/*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.interfaces;

import org.simnet.synapses.rules.*;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class LearningRule {

	private static String[] functionList = { "Hebbian", "Random", "AntiHebbian", "None", "Random"};

	public abstract String getName();
	
	/**
	 * This is the main logic method for the learning rule, which must be
	 * overridden by each specific function below. 
	 * 
	 * @param w Weight to modify
	 */
	public abstract void apply(Synapse w); 

	/**
	 * Returns a string describing how this learning rule works.  Can be
	 * used in providing run-time help for the network designer.
	 * 
	 * @return a string describing how this learning rule works
	 */
	public abstract String getHelp();


	public static String[] getList() {
		return functionList;
	}
	
	
	/**
	 * Helper function for combo boxes.  Associates strings with indices.
	 */	
	public static int getLearningRuleIndex(String lr) {
		for (int i = 0; i < functionList.length; i++) {
			if (lr.equals(functionList[i])) {
				return i;
			}
		}
		return 0;
	}
	
	public static LearningRule getLearningRule(String ruleName) {
		if (ruleName.equalsIgnoreCase("Hebbian")) {
			return new Hebbian();
		} else if (ruleName.equalsIgnoreCase("Random")) {
			return new Random();
		} else if (ruleName.equalsIgnoreCase("None")) {
			return new NoLearning();
		} else if (ruleName.equalsIgnoreCase("AntiHebbian") || ruleName.equalsIgnoreCase("Anti-Hebbian")) {
			return new AntiHebbian();
		}
		System.out.println("Error: selected function not in internal list");
		return null;
	}


}
