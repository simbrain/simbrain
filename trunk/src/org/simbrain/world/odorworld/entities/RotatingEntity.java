package org.simbrain.world.odorworld.entities;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.effectors.RotationEffector;
import org.simbrain.world.odorworld.effectors.StraightMovementEffector;

/**
 * Represents an entity that can rotate.
 */
public class RotatingEntity extends OdorWorldEntity {

    /** Images for various angles. */
    private TreeMap<Double, Animation>  imageMap = DEFAULT_MAP;
    
    /** Current heading / orientation. */
    private double heading = DEFAULT_HEADING;

    /** Initial heading of agent. */
    private final static double DEFAULT_HEADING = 0;

    /** Default location for sensors relative to agent. */
    private static double WHISKER_ANGLE = Math.PI / 4;

    /** Amount to manually rotate. */
    private final double manualMotionTurnIncrement = 4;

    /** Amount to manually rotate. */
    private final double manualStraightMovementIncrement = 4;

    /** Default tree map; of a mouse. */
    private static final TreeMap<Double, Animation>  DEFAULT_MAP = RotatingEntityManager.getMouse();

    /** Obvious... */
    private final static double DEGREES_IN_A_CIRCLE = 360;

     /**
      * Create a rotating entity using default map.
      *
      * @param world parent world
      */
    public RotatingEntity(final OdorWorld world) {
        super(DEFAULT_MAP.get(DEFAULT_MAP.firstKey()), world);
    }

    /**
     * Create a rotating entity using specified map.
     *
     * @param map the map to use
     */
    public RotatingEntity(final TreeMap<Double, Animation>  map, final OdorWorld world) {
        super(map.get(map.firstKey()), world);  // Default to animation for 0 degrees
        this.imageMap = map;
    }

    /**
     * Returns the heading in radians.
     *
     * @return orientation in degrees
     */
    public double getHeadingRadians() {
        return (heading * Math.PI) / 180;
    }

    /**
     * Set the orientation of the creature.
     *
     * @param d the orientation, in degrees
     */
    public void setHeading(final double d) {
        //System.out.println("setOrientation:" + d);
        heading = d;
    }

    /**
     * Returns the current heading, in degrees.
     *
     * @return current heading.
     */
    public double getHeading() {
        return heading;
    }

    /**
     * Ensures that value lies between 0 and 360.
     *
     * @param value the value to compute
     * @return value's "absolute angle"
     */
    private double computeAngle(final double val) {

        //TODO: This will not work for vals greater or less than 360
        double retVal = val; 
        if (val >= DEGREES_IN_A_CIRCLE) {
            retVal -= DEGREES_IN_A_CIRCLE;
        }
        if (val < 0) {
            retVal += DEGREES_IN_A_CIRCLE;
        }
        return retVal;
    }

    /**
     * Updates this OdorWorldEntity's Animation and its position based on the velocity.
     */
    public void update(final long elapsedTime) {

        behavior.apply(elapsedTime);

        //System.out.println("heading:" + heading);

        heading =  computeAngle(heading);

        //TODO: only do this if heading has changed
        for (Entry<Double, Animation> entry : imageMap.entrySet()) {
            //System.out.println("" + heading + "-" + entry.getKey());
            if (heading < entry.getKey()) {
                setAnimation(entry.getValue());
                break;
            }
        }

        getAnimation().update(elapsedTime);
    }

    /**
     * Initialize map animations using image location information.
     */
    public void postSerializationInit() {
        super.postSerializationInit();
        Iterator<Double> i = imageMap.keySet().iterator();
        while (i.hasNext()) {
            Double key = i.next();
            imageMap.get(key).initializeImages();
        }

        //TODO: Check this against overall attribute policy
        addEffector(new RotationEffector(this));
        addEffector(new StraightMovementEffector(this));

    }

    /**
     * @return the imageMap
     */
    public TreeMap<Double, Animation> getImageMap() {
        return imageMap;
    }

    /**
     * @param imageMap the imageMap to set
     */
    public void setImageMap(TreeMap<Double, Animation> imageMap) {
        this.imageMap = imageMap;
    }


}
