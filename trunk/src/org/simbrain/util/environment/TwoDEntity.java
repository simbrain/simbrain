package org.simbrain.util.environment;

/**
 * An entity in a 2d environment.  
 */
public interface TwoDEntity {

	/** Parent environment. */
	public TwoDEnvironment getEnvironment();
	
	/** 
	 * Suggested Location.  It is up to user of this package to determine whether 
	 * to make the suggested location the actual location.
	 * 
	 * For collision handling.
	 * 
	 * Implementing classes should also have a location parameter.
	 * 
	 * TODO: Not sure if this is the best method.
	 */
	public double[] getSuggestedLocation();
	
	/** Set location. */
	public void setSuggestedLocation(double[] location);
	
	/** Update entity. */
	public void update();

}
