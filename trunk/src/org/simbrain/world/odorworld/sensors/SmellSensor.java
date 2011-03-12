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

import java.util.ArrayList;
import java.util.List;

import org.simbrain.plot.barchart.BarChartComponent.BarChartSetter;
import org.simbrain.util.SimbrainMath;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * A sensor which is updated based on the presence of SmellSources near it.
 *
 * @see org.simbrain.util.environment.SmellSource
 */
public class SmellSensor extends Sensor {

    /** Angle of whisker in radians. */
    public static double DEFAULT_THETA = Math.PI / 4;

    /** Initial length of mouse whisker. */
    private final double DEFAULT_RADIUS = 23;

    /** Relative location of the sensor in polar coordinates. */
    private double theta = DEFAULT_THETA;

    /** Relative location of the sensor in polar coordinates. */
    private double radius = DEFAULT_RADIUS;

    /** Current value of this sensor, as an array of doubles. */
    private double[] currentValue = new double[5];

    /**
     * Construct a smell sensor.
     *
     * @param parent parent
     * @param id id
     * @param theta offset from straight in degrees radians
     * @param radius length of "whisker"
     */
    public SmellSensor(final OdorWorldEntity parent, final String id,
            double theta, double radius) {
        this.parent = parent;
        this.id = id;
        this.theta = theta;
        this.radius = radius;
        setLabel(parent.getName() + ":" + getId());
    }

    /**
     * @return the location
     */
    public double[] getLocation() {
        // TODO: Formalize rule that this sensor applies to rotating entity
        // only,
        // or relax the code so that it will work for non-rotating entities
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
     * @return the currentValue
     */
    public double[] getCurrentValue() {
        return currentValue;
    }

    /**
     * The current value at an index.
     *
     * @return the currentValue
     */
    public double getCurrentValue(int index) {
        return currentValue[index];
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

}
