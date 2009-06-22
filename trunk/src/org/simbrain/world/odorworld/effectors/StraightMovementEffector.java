package org.simbrain.world.odorworld.effectors;

import java.util.List;

/**
 * Move agent in a straight line scaled by value.
 */
public class StraightMovementEffector implements Effector {

    public void activate() {
        // TODO Auto-generated method stub
        
    }

    public List<Class> getApplicableTypes() {
        // TODO Auto-generated method stub
        return null;
    }

//	/** Translation. */
//	private double movementAmount;
//
//	/** ScaleFactor. */
//	private double scaleFactor;
//
//	/**
//	 * Initialize a straight movement effector.
//	 *
//	 * @param agent
//	 * @param name
//	 * @param movementAmount
//	 * @param scaleFactor
//	 */
//	public StraightMovementEffector(Agent agent, String name, double movementAmount, double scaleFactor) {
//		super(agent, name);
//		this.movementAmount = movementAmount;
//		this.scaleFactor = scaleFactor;
//	}
//	
//	/**
//     * {@inheritDoc}
//     */
//    public void setValue(Double value) {
//        if (value == 0) {
//            return;
//        }
//        double offset = (value * movementAmount) * scaleFactor;
//        double heading = getParent().getHeadingRadians();
//        double x = getParent().getSuggestedLocation()[0] + (offset * Math.cos(heading));
//        double y = getParent().getSuggestedLocation()[1] - (offset * Math.sin(heading));
//        getParent().setSuggestedLocation(new double[]{x,y}); // TODO: Is this inefficient? 
//	}

}
