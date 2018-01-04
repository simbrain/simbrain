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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * A sensor which is updated based on the presence of SmellSources near it.
 *
 * @see org.simbrain.util.environment.SmellSource
 */
public class SmellSensor extends Sensor {

    /** Default label. */
    public static final String DEFAULT_LABEL = "SmellSensor";

    /** Angle of whisker in radians. */
    public static double DEFAULT_THETA = Math.PI / 4;

    /** Initial length of mouse whisker. */
    public static final double DEFAULT_RADIUS = 23;

    /** Relative location of the sensor in polar coordinates. */
    private double theta = DEFAULT_THETA;

    /** Relative location of the sensor in polar coordinates. */
    private double radius = DEFAULT_RADIUS;

    /** Current value of this sensor, as an array of doubles. */
    // TODO: Settable numDims!
    private double[] currentValue = new double[10];

    /**
     * Construct a smell sensor.
     *
     * @param parent parent
     * @param label label for this sensor (entity name will be added)
     * @param theta offset from straight in degrees radians
     * @param radius length of "whisker"
     */
    public SmellSensor(final OdorWorldEntity parent, final String label,
            double theta, double radius) {
        super(parent, label);
        this.parent = parent;
        this.theta = theta;
        this.radius = radius;
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

    @Override
    public void update() {
        double[] temp = new double[currentValue.length];
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
    @Producible(customDescriptionMethod = "getId")
    public double[] getCurrentValues() {
        return currentValue;
    }

    //TODO. Rename...
    public List<Integer> getDimensionList() {
        return IntStream.range(1, this.getCurrentValues().length).boxed()
                .collect(Collectors.toList());
    }

    /**
     * The current value at an index.
     * @param index
     * @return the currentValue
     */
    @Producible(indexListMethod = "getDimensionList")
    public double getCurrentValue(Integer index) {
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

    @Override
    public String getTypeDescription() {
        return "Smell";
    }

}
