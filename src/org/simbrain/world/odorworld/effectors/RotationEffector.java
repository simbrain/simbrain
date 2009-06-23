package org.simbrain.world.odorworld.effectors;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Effector which rotates agent by a specified amount.
 */
public class RotationEffector implements Effector {

    /** Reference to parent object. */
    private RotatingEntity parentObject;

    /** Translation. */
    private double turnIncrement = 1;

    /** Makes the difference between Right and Left and how much. */
    private double scaleFactor = 0;

    /** Obvious... */
    private final static double DEGREES_IN_A_CIRCLE = 360;
    
    /**
     * Constructor.
     */
    public RotationEffector(final RotatingEntity agent) {
        parentObject = agent;
    }

    /**
     * Ensures that value lies between 0 and 360.
     * 
     * @param value the value to compute
     * @return value's "absolute angle"
     */
    private double computeAngle(final double val) {
        double retVal = val; 
        while (val >= DEGREES_IN_A_CIRCLE) {
            retVal -= DEGREES_IN_A_CIRCLE;
        }
        while (val < 0) {
            retVal += DEGREES_IN_A_CIRCLE;
        }

        return retVal;
    }

    /**
     * {@inheritDoc}
     */
    public void activate() {
        if (scaleFactor == 0) {
            return;
        }
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
