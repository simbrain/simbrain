package org.simbrain.world.odorworld.effectors;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Rotate agent.
 */
public class RotationEffector implements Effector {

    private RotatingEntity parentObject;

    /** Translation. */
    private double turnIncrement = 1;

    /** Makes the difference between Right and Left and how much. */
    private double scaleFactor = 1;

    /** Obvious... */
    private final static double DEGREES_IN_A_CIRCLE = 360;
    
    /**
     * Constructor.
     */
    public RotationEffector(RotatingEntity agent) {
        parentObject = agent;
    }

    /**
     * Ensures that value lies between 0 and 360.
     * 
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
    public void activate() {
//        if (scaleFactor == 0) {
//            return;
//        }
        double offset =  turnIncrement * scaleFactor;
        parentObject.setOrientation(parentObject.getHeading()
                + offset);
    }

    public List<Class> getApplicableTypes() {
        //TODO: Why can't I use Collections.singleton here?
        ArrayList<Class> list = new ArrayList<Class>();
        list.add(RotatingEntity.class);
        return list;
    }

    /**
     * @return the parentObject
     */
    public RotatingEntity getParent() {
        return parentObject;
    }

    /**
     * @return the scaleFactor
     */
    public double getScaleFactor() {
        return scaleFactor;
    }

    /**
     * @param scaleFactor the scaleFactor to set
     */
    public void setScaleFactor(final double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }
}
