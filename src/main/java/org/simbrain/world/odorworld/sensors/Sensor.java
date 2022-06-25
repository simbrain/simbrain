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

import org.jetbrains.annotations.Nullable;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.PeripheralAttribute;
import org.simbrain.world.odorworld.events.AttributeEvents;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;

/**
 * Interface for 2d world sensors.  Sensors have a position given in polar
 * coordinates.
 */
public abstract class Sensor implements PeripheralAttribute {

    public static List<Class> SENSOR_LIST =
            Arrays.asList(
                    SmellSensor.class,
                    Hearing.class,
                    GridSensor.class,
                    ObjectSensor.class,
                    BumpSensor.class,
                    TileSensor.class
            );

    public static List<Class> getTypes() {
        return SENSOR_LIST;
    }

    /**
     * Angle of sensor in radians.
     */
    public static double DEFAULT_THETA = 0;

    /**
     * Initial length of mouse whisker.
     */
    public static final double DEFAULT_RADIUS = 0;

    /**
     * Relative location of the sensor in polar coordinates.
     */
    @UserParameter(label = "Sensor angle", description = "The angle theta (in polar coordinates, with radius) at " +
            "which the sensor will be added.", order = 3)
    protected double theta = DEFAULT_THETA;

    /**
     * Relative location of the sensor in polar coordinates.
     */
    @UserParameter(label = "Sensor length",
        description = "The distance in pixels from the center of the entity to which the sensor is to be added."
        , order = 4)
    protected double radius = DEFAULT_RADIUS;

    /**
     * Returns the sensor location in the local coordinate frame of the entity.
     */
    public Point2D.Double computeLocationFrom(OdorWorldEntity entity) {
        Point2D.Double sensorLocation = new Point2D.Double(0,0);
        sensorLocation.x = radius * Math.cos(Math.toRadians(entity.getHeading() + theta));
        sensorLocation.x += entity.getWidth()/2;
        sensorLocation.y = -radius * Math.sin(Math.toRadians(entity.getHeading() + theta));
        sensorLocation.y += entity.getHeight()/2;
        return sensorLocation;
    }

    /**
     * The id of this smell sensor.
     */
    @UserParameter(label = "Sensor ID", description = "A unique id for this sensor",
            order = 0, editable = false)
    private String id;

    /**
     * Public label of this sensor.
     */
    @UserParameter(label = "Label", description = "Optional string description associated with this sensor",
            initialValueMethod = "getLabel", order = 1)
    private String label = "";

    /**
     * Handle events.
     */
    private transient AttributeEvents events = new AttributeEvents(this);

    /**
     * Construct a sensor.
     *
     * @param label  a label for this sensor
     */
    public Sensor(String label) {
        super();
        this.label = label;
    }


    /**
     * Construct a copy of a sensor.
     *
     * @param sensor the sensor to copy
     */
    public Sensor(Sensor sensor) {
        this.radius = sensor.radius;
        this.theta = sensor.theta;
        this.label = sensor.label;
    }

    /**
     * Default no-arg constructor for {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     */
    public Sensor() {
        super();
    }

    public void setId(String name) {
        this.id = name;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Return String direction (left / right) based on angle of the sensor
     */
    public String getDirectionString() {
        if (getTheta() < 0 && getTheta() > -45  ) {
            return "Right ";
        } else if (getTheta() > 0 && getTheta() < 45 ) {
            return "Left ";
        } else {
            return "";
        }
        // TODO: Maybe add front, back, left-back and right-back
        // With length = 0 can also have center
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
        getEvents().fireUpdate();
    }

    /**
     * Returns angle in radians
     */
    public double getTheta() {
        return theta;
    }

    /**
     * Sets angle in radians
     */
    public void setTheta(double theta) {
        this.theta = theta;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public abstract Sensor copy();

    public AttributeEvents getEvents() {
        return events;
    }

    public Object readResolve() {
        events = new AttributeEvents(this);
        return this;
    }

    public static class SensorCreator implements EditableObject {

        @UserParameter(label="Sensor", isObjectType = true)
        private Sensor sensor = new SmellSensor();

        public SensorCreator(String proposedLabel) {
            sensor.label = proposedLabel;
        }

        public Sensor getSensor() {
            return sensor;
        }

        public void setSensor(Sensor sensor) {
            this.sensor = sensor;
        }

        @Nullable
        @Override
        public String getName() {
            return EditableObject.super.getName();
        }

        @Override
        public void onCommit() {
            EditableObject.super.onCommit();
        }
    }
}
