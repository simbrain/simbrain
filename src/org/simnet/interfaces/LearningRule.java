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
public abstract class LearningRule {

	private final transient String name = null;

	/**
	 * This is the main logic method for the learning rule, which must be
	 * overridden by each specific function below. 
	 * 
	 * @param w Weight to modify
	 */
	protected abstract void apply(Synapse w); 

	/**
	 * Returns a string describing how this learning rule works.  Can be
	 * used in providing run-time help for the network designer.
	 * 
	 * @return a string describing how this learning rule works
	 */
	public abstract String getHelp();

	public String getName() {
		return name;
	}

	public static String[] getList() {
		return new String[] {"None", "Hebbian", "AntiHebbian", "Random", "HebbAutoscale"};	
	}

}
