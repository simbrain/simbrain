/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.odorworld.entities;

import org.simbrain.util.UserParameter;
import org.simbrain.world.odorworld.OdorWorld;

/**
 * Represents an entity that can rotate.
 * <p>
 * TODO: Currently rotating entities are "agents" that can accept sensors and
 * effectors. The concept of an "Agent" needs to be separated from that of a
 * rotating entity. In principle non-rotating entities should be able to have
 * sensors, for example.
 */
public class RotatingEntity extends OdorWorldEntity {

    /**
     * Current heading / orientation.
     */
    private double heading = DEFAULT_HEADING;

    /**
     * Initial heading of agent.
     */
    private final static double DEFAULT_HEADING = 0;

    /**
     * Default location for sensors relative to agent.
     */
    private static double WHISKER_ANGLE = Math.PI / 4;

    /**
     * Amount to manually rotate.
     */
    @UserParameter(label = "Turn amount", order = 10)
    private double manualMotionTurnIncrement = 14;

    /**
     * Create a rotating entity using default map.
     *
     * @param world parent world
     */
    public RotatingEntity(final OdorWorld world) {
        super(world);
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

        double newHeading = d;
        if (newHeading >= 360) {
            newHeading -= 360;
        }
        if (newHeading < 0) {
            newHeading += 360;
        }
        heading = newHeading;
        changeSupport.firePropertyChange("moved", null, null);
    }

    /**
     * Returns the current heading, in degrees.
     *
     * @return current heading.
     */
    public double getHeading() {
        return heading;
    }

    @Override
    public void update() {
        super.update();
    }


    /**
     * Initialize map animations using image location information.
     */
    public void postSerializationInit() {
        super.postSerializationInit();
//        initTreeMap();
//        Iterator<Double> i = imageMap.keySet().iterator();
//        while (i.hasNext()) {
//            Double key = i.next();
//            imageMap.get(key).initializeImages();
//        }
    }


    /**
     * Rotate left by the specified amount.
     *
     * @param amount amount to turn left. Assumes a positive number.
     */
    //@Consumible(customDescriptionMethod="getId")
    public void turnLeft(double amount) {
        turn(amount);
    }

    /**
     * Turn by the specified amount, positive or negative.
     *
     * @param amount
     */
    //@Consumible(customDescriptionMethod="getId")
    public void turn(double amount) {
        if (amount == 0) {
            return;
        }
        if (!isBlocked()) {
            setHeading(heading + amount);
        }
        changeSupport.firePropertyChange("moved", null, null);

    }

    /**
     * Rotate right by the specified amount.
     *
     * @param amount amount to turn right. Assumes a positive number.
     */
    //@Consumible(customDescriptionMethod="getId")
    public void turnRight(double amount) {
        turn(-amount);
    }

    /**
     * Move the entity in a straight line relative to its current heading.
     *
     * @param amount
     */
    //@Consumible(customDescriptionMethod="getId")
    public void goStraight(double amount) {
        if (amount == 0) {
            return;
        }
        if (!isBlocked()) {
            double radians = getHeadingRadians();
            setX(getX() + (float) (amount * Math.cos(radians)));
            setY(getY() - (float) (amount * Math.sin(radians)));
        }
        changeSupport.firePropertyChange("moved", null, null);
    }

    public void goStraight() {
        goStraight(manualStraightMovementIncrement);
    }

    public void turnLeft() {
        turnLeft(manualMotionTurnIncrement);
    }
    public void turnRight() {
        turnRight(manualMotionTurnIncrement);
    }
}