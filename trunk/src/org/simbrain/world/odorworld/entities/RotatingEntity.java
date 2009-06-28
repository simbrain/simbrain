package org.simbrain.world.odorworld.entities;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.odorworld.OdorWorld;

/**
 * Represents an entity that can rotate.
 */
public class RotatingEntity extends OdorWorldEntity {

    /** Images for various angles. */
    TreeMap<Double, Animation>  map;
    
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
    private static final TreeMap<Double, Animation>  DEFAULT_MAP;
    
     static {
        DEFAULT_MAP = new TreeMap<Double, Animation>();
        DEFAULT_MAP.put(7.5, new Animation("Mouse_0.gif"));
        DEFAULT_MAP.put(22.5, new Animation("Mouse_15.gif"));
        DEFAULT_MAP.put(37.5, new Animation("Mouse_30.gif"));
        DEFAULT_MAP.put(52.5, new Animation("Mouse_45.gif"));
        DEFAULT_MAP.put(67.5, new Animation("Mouse_60.gif"));
        DEFAULT_MAP.put(82.5, new Animation("Mouse_75.gif"));
        DEFAULT_MAP.put(97.5, new Animation("Mouse_90.gif"));
        DEFAULT_MAP.put(112.5, new Animation("Mouse_105.gif"));
        DEFAULT_MAP.put(127.5, new Animation("Mouse_120.gif"));
        DEFAULT_MAP.put(142.5, new Animation("Mouse_135.gif"));
        DEFAULT_MAP.put(157.5, new Animation("Mouse_150.gif"));
        DEFAULT_MAP.put(172.5, new Animation("Mouse_165.gif"));
        DEFAULT_MAP.put(187.5, new Animation("Mouse_180.gif"));
        DEFAULT_MAP.put(202.5, new Animation("Mouse_195.gif"));
        DEFAULT_MAP.put(217.5, new Animation("Mouse_210.gif"));
        DEFAULT_MAP.put(232.5, new Animation("Mouse_225.gif"));
        DEFAULT_MAP.put(247.5, new Animation("Mouse_240.gif"));
        DEFAULT_MAP.put(262.5, new Animation("Mouse_255.gif"));
        DEFAULT_MAP.put(277.5, new Animation("Mouse_270.gif"));
        DEFAULT_MAP.put(292.5, new Animation("Mouse_285.gif"));
        DEFAULT_MAP.put(307.5, new Animation("Mouse_300.gif"));
        DEFAULT_MAP.put(322.5, new Animation("Mouse_315.gif"));
        DEFAULT_MAP.put(337.5, new Animation("Mouse_330.gif"));
        DEFAULT_MAP.put(352.5, new Animation("Mouse_345.gif"));
    }
    
     /**
      * Create a rotating entity using default map.
      *
      * @param world parent world
      */
    public RotatingEntity(OdorWorld world) {
        super(world, DEFAULT_MAP.get(DEFAULT_MAP.firstKey()));
    }

    /**
     * Create a rotating entity using specified map.
     * 
     * @param world parent world
     * @param map the map to use
     */
    public RotatingEntity(final OdorWorld world, final TreeMap<Double, Animation>  map) {
        super(world, map.get(map.firstKey()));  // Default to animation for 0 degrees
        this.map = map;
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
     * Updates this OdorWorldEntity's Animation and its position based on the velocity.
     */
    public void update(final long elapsedTime) {
        
        behavior.apply(elapsedTime);
        
        heading = heading % 360;
        SortedMap<Double, Animation> headMap = map.headMap(heading);
        
        if (headMap.size() > 0) {
            setAnimation(map.get(headMap.lastKey()));
        } else {
            setAnimation(map.get(map.firstKey()));
        }
        getAnimation().update(elapsedTime);
    }
    
    /**
     * Initialize map animations using image location information.
     */
    public void postSerializationInit() {
        super.postSerializationInit();
        Iterator<Double> i = map.keySet().iterator();
        while (i.hasNext()) {
            Double key = i.next();
            map.get(key).initializeImages();
        }
        
    }


}
