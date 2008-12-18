package org.simbrain.util.environment.effectors;

import org.simbrain.util.environment.Agent;

/**
 * Move agent in a straight line scaled by value.
 */
public class StraightMovementEffector extends Effector {

	/** Translation. */
	private double movementAmount;

	/** ScaleFactor. */
	private double scaleFactor;

	/**
	 * Initialize a straight movement effector.
	 *
	 * @param agent
	 * @param name
	 * @param movementAmount
	 * @param scaleFactor
	 */
	public StraightMovementEffector(Agent agent, String name, double movementAmount, double scaleFactor) {
		super(agent, name);
		this.movementAmount = movementAmount;
		this.scaleFactor = scaleFactor;
	}
	
	/**
     * {@inheritDoc}
     */
    public void setValue(Double value) {
        if (value == 0) {
            return;
        }
        double offset = (value * movementAmount) * scaleFactor;
        double heading = getParent().getHeadingRadians();
        double x = getParent().getSuggestedLocation()[0] + (offset * Math.cos(heading));
        double y = getParent().getSuggestedLocation()[1] - (offset * Math.sin(heading));
        getParent().setSuggestedLocation(new double[]{x,y}); // TODO: Is this inefficient? 
	}

}
