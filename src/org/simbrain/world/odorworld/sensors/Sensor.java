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

import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor2.CopyableObject;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.world.odorworld.entities.PeripheralAttribute;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;

/**
 * Interface for 2d world sensors.  Sensors have a position given in polar
 * coordinates.
 */
public abstract class Sensor implements CopyableObject, PeripheralAttribute {

    public static List<Class> SENSOR_LIST =
            Arrays.asList(
                    SmellSensor.class,
                    Hearing.class,
                    LocationSensor.class,
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
    public static double DEFAULT_THETA = Math.PI / 4;

    /**
     * Initial length of mouse whisker.
     */
    public static final double DEFAULT_RADIUS = 23;

    /**
     * Relative location of the sensor in polar coordinates.
     */
    @UserParameter(label = "Sensor angle", description = "The angle at which the smell sensor will be added. "
        + "A sensor angle of 0 a smell sensor that is directly in front of the agent. "
        + "A positive sensor angle locates the sensor at a position to the left of the agent's heading. "
        + "A negative sensor angle locates the sensor at a position to the right of the agent's heading.",
        defaultValue = "" + (Math.PI / 4), order = 3)
    protected double theta = DEFAULT_THETA;

    /**
     * Relative location of the sensor in polar coordinates.
     */
    @UserParameter(label = "Sensor length",
        description = "The distance from the center of the entity to which the smell sensor is to be added."
            + "A sensor length of 0 makes sensor angle irrelevant since located at the center of the agent.",
        defaultValue = "" + DEFAULT_RADIUS, order = 4)
    protected double radius = DEFAULT_RADIUS;

    /**
     * The relative location of this sensor to the top left of the entity
     */
    private Point2D.Double relativeLocation = new Point2D.Double();

    /**
     * Reference to parent entity.
     */
    protected OdorWorldEntity parent;

    /**
     * The id of this smell sensor..
     */
    @UserParameter(label = "Sensor ID", description = "A unique id for this sensor",
            order = 0, editable = false)
    private String id;

    /**
     * Public label of this sensor.
     */
    @UserParameter(label = "Label", description = "Optional string description associated with this sensor",
            defaultValue = "", order = 1)
    private String label;

    /**
     * Construct the sensor.
     *
     * @param parent the parent entity
     * @param label  a label for this sensor
     */
    public Sensor(OdorWorldEntity parent, String label) {
        super();
        this.parent = parent;
        this.label = label;
    }

    /**
     * Default constructor for {@link org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor}.
     *
     * NOTE:
     * {@link org.simbrain.world.odorworld.dialogs.AddSensorDialog} handles the set up of {@link #parent}.
     * When calling this directly, remember to set up the required field {@link #parent} accordingly.
     */
    public Sensor() {
        super();
    }

    /**
     * Update the sensor.
     */
    public abstract void update();

    @Override
    public OdorWorldEntity getParent() {
        return parent;
    }

    public void setId(String name) {
        this.id = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public abstract String getTypeDescription();

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    /**
     * Location of sensor in "non-relative" world coordinates.
     *
     * @return the sensor location
     */
    public double[] getLocation() {
        updateRelativeLocation();
        double[] ret = {relativeLocation.x, relativeLocation.y};
        ret[0] += parent.getX();
        ret[1] += parent.getY();
        return ret;
    }

    /**
     * Update the sensor {@link #relativeLocation} base on the heading of the entity.
     */
    public void updateRelativeLocation() {
        double x =  (radius * Math.cos(parent.getHeadingRadians() + theta))
            + parent.getEntityType().getImageWidth() / 2;
        double y = -(radius * Math.sin(parent.getHeadingRadians() + theta))
            + parent.getEntityType().getImageWidth() / 2;
        relativeLocation.setLocation(x, y);
    }

    /**
     * Update and get the {@link #relativeLocation} of this sensor.
     * @return the updated {@link #relativeLocation}
     */
    public Point2D.Double getRelativeLocation() {
        updateRelativeLocation();
        return relativeLocation;
    }

    /**
     * Perform initialization of objects after de-serializing.
     */
    public void postSerializationInit() {
        relativeLocation = new Point2D.Double();
    }

    public static class SensorCreator implements EditableObject {

        @UserParameter(label="Sensor", isObjectType = true)
        private Sensor sensor = new SmellSensor();

        public Sensor getSensor() {
            return sensor;
        }

        public void setSensor(Sensor sensor) {
            this.sensor = sensor;
        }
    }
}
