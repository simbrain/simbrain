package org.simbrain.util.environment.effectors;

import org.simbrain.util.environment.Agent;

/**
 * Rotate agent.
 */
public class RotationEffector extends Effector {

	/** Translation. */
	private double turnIncrement;

	/** Makes the difference between Right and Left. */
	private double scaleFactor;

	/** Obvious... */
	private final static double DEGREES_IN_A_CIRCLE = 360;

	/**
	 * Constructor.
	 *
	 * @param agent
	 * @param name
	 * @param turnIncrement
	 * @param scaleFactor
	 */
	public RotationEffector(Agent agent, String name, double turnIncrement, double scaleFactor) {
		super(agent, name);
		this.turnIncrement = turnIncrement;
		this.scaleFactor = scaleFactor;
	}
	
    /**
     * Ensures that value lies between 0 and 360.
     * @param value the value to compute
     * @return value's "absolute angle"
     */
    private double computeAngle(final double value) {
        double val = value;
        while (val >= DEGREES_IN_A_CIRCLE) {
            val -= DEGREES_IN_A_CIRCLE;
        }

        while (val < 0) {
            val += DEGREES_IN_A_CIRCLE;
        }

        return val;
    }
	/**
	 * {@inheritDoc}
	 */
	public void setValue(Double value) {
        if (value == 0) {
            return;
        }
        double offset = value * turnIncrement * scaleFactor;
        getParent().setHeading(computeAngle(getParent().getHeading() + offset));
	}
}
