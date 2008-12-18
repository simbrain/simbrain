package org.simbrain.util.environment.effectors;

import org.simbrain.util.environment.Agent;

/**
 * Move agent in specified direction scaled by the provided value
 * Can be used to create "North," "South," etc.
 * 
 * TODO: Not tested yet...
 */
public class TranslationEffector extends Effector {

	/** Translation. */
	private double[] translation;

	/**
	 * Initialize translation effector.
	 *
	 * @param agent
	 * @param name
	 * @param translation
	 */
	public TranslationEffector(Agent agent, String name, double[] translation) {
		super(agent, name);
		this.translation = translation;
	}
	
	/**
     * {@inheritDoc}
     */
	public void setValue(Double value) {
		double[] oldLocation = this.getParent().getSuggestedLocation();
		this.getParent().setSuggestedLocation(new double[] {
				oldLocation[0] + value * translation[0], 
				oldLocation[1] + value * translation[1]});
	}

}
