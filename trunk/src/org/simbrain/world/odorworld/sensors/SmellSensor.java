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
package org.simbrain.world.odorworld.sensors;

import java.util.List;

import org.simbrain.util.SimbrainMath;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * A sensor which is updated based on the presence of SmellSources near it.
 *
 * @see org.simbrain.util.environment.SmellSource
 */
public class SmellSensor implements Sensor {

    /** Angle of whisker in radians. */
    public static double DEFAULT_THETA = Math.PI / 4;

    /** Initial length of mouse whisker. */
    private final double DEFAULT_RADIUS = 23;

    /** Relative location of the sensor in polar coordinates. */
    private double theta = DEFAULT_THETA;

    /** Relative location of the sensor in polar coordinates. */
    private double radius = DEFAULT_RADIUS;

    /** Reference to parent entity. */
    private OdorWorldEntity parent;

    /** Current value of this sensor, as an array of doubles. */
    private double[] currentValue = new double[5];

    /** The name of this smell sensor.. */
    private String name;

    /**
     * Construct a sensor.
     *
     * @param parent reference
     * @param sensorName name
     * @param dim stimulus dimension
     */
    public SmellSensor(final OdorWorldEntity parent, final String name, double theta, double radius) {
        this.parent = parent;
        this.theta = theta;
        this.name = name;
        this.radius = radius;
    }

    /**
     * @return the location
     */
    public double[] getLocation() {
        //TODO: Formnalize rule that this sensor applies to rotating entity only,
        //      or relax the code so that it will work for non-rotating entities
        RotatingEntity parent = (RotatingEntity) this.getParent();
        double x = parent.getCenterLocation()[0]
                + (radius * Math.cos(parent.getHeadingRadians() + theta));
        double y = parent.getCenterLocation()[1]
                - (radius * Math.sin(parent.getHeadingRadians() + theta));
        return new double[] { x, y };
    }

    /**
     * {@inheritDoc}
     */
    public void update() {
        double[] temp = SimbrainMath.zeroVector(currentValue.length);
        for (OdorWorldEntity entity : parent.getParentWorld().getObjectList()) {

            // Don't smell yourself
            if (entity != parent) {
                SmellSource smell = entity.getSmellSource();
                if (smell != null) {
                    temp = SimbrainMath.addVector(temp, smell
                            .getStimulus(SimbrainMath.distance(getLocation(),
                                    entity.getCenterLocation())));
                }
            }
        }
        currentValue = temp;
    }

    /**
     * {@inheritDoc}
     */
    public List<Class> getApplicableTypes() {
        return null;
    }

    /**
     * @return the currentValue
     */
    public double[] getCurrentValue() {
        return currentValue;
    }

    /**
     * @return the parent
     */
    public OdorWorldEntity getParent() {
        return parent;
    }

    /**
     * @return the theta
     */
    public double getTheta() {
        return theta;
    }

    /**
     * @param theta the theta to set
     */
    public void setTheta(double theta) {
        this.theta = theta;
    }

    /**
     * @return the radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * @param radius the radius to set
     */
    public void setRadius(double radius) {
        this.radius = radius;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return smell sensor getter with specified index.
     *
     * @param i index
     * @return the getter
     */
    public SmellSensorGetter createGetter(int i) {
        return new SmellSensorGetter(i);
    }

    /**
     * Helper object for use with couplings. An object of this class is
     * associated with one dimension of a smell sensor.
     */
    public class SmellSensorGetter {

        /** Index. */
        private int index;

        /**
         * Construct a setter object.
         *
         * @param index index of the bar to set
         */
        public SmellSensorGetter(final int index) {
            this.index = index;
        }

        /**
         * Return the current value of the sensor for the given index.
         *
         * @return current value.
         */
        public double getValue() {
            return getCurrentValue()[index];
        }

        /**
         * Return the index number.
         *
         * @return the index
         */
        public int getIndex() {
            return index;
        }
    }
}
