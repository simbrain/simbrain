package org.simbrain.world.odorworld.entities;

import java.util.SortedMap;
import java.util.TreeMap;

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

    public RotatingEntity(OdorWorld world, TreeMap<Double, Animation>  map) {
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
    public void setOrientation(final double d) {
        System.out.println("setOrientation:" + d);
        heading = d;
    }
    
    /**
     * Updates this OdorWorldEntity's Animation and its position based on the velocity.
     */
    public void update(long elapsedTime) {
        
        heading = heading % 360;
        SortedMap<Double, Animation> headMap = map.headMap(heading);
        
        if (headMap.size() > 0) {
            anim = map.get(headMap.lastKey());
        } else {
            anim = map.get(map.firstKey());
        }
        anim.update(elapsedTime);
    }
    
    /**
     * Called before update() if the creature collided with a tile horizontally.
     */
    public void collideHorizontal() {
        // No implementation
    }

    /**
     * Called before update() if the creature collided with a tile vertically.
     */
    public void collideVertical() {
        // No implementation
    }

    public double getHeading() {
        return heading;
    }


}
