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
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.RectangleCollisionBound;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.gui.BumpSensorNode;
import org.simbrain.world.odorworld.gui.EntityAttributeNode;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Very simple bump sensor. Holding off on more sophisticated "touch" sensors in
 * case an existing library can provide it.
 * <p>
 * TODO: Implement once collisions are implemented. At that point can rename to
 * collision sensor? Can also give the sensor a location and make it visible.
 */
public class BumpSensor extends Sensor implements VisualizableEntityAttribute {

    /**
     * Current value of the sensor.
     */
    private double value = 0;

    /**
     * The value to output when the sensor is bumped.
     */
    @UserParameter(
            label = "Base Value",
            description = "The value to output when the sensor is bumped",
            defaultValue = "1", order = 4)
    private double baseValue = 1;

    /**
     * Relative location of the sensor in polar coordinates.
     */
    @UserParameter(label = "Sensor angle", description = "The angle at which the bump sensor will be added. "
            + "A sensor angle of 0 a bump sensor that is directly in front of the agent. "
            + "A positive sensor angle locates the sensor at a position to the left of the agent's heading. "
            + "A negative sensor angle locates the sensor at a position to the right of the agent's heading.",
            defaultValue = "0", order = 3)
    private double theta = 0;

    /**
     * Relative location of the sensor in polar coordinates.
     */
    @UserParameter(label = "Sensor length",
            description = "The distance from the center of the entity to which the bump sensor is to be added."
                    + "A sensor length of 0 makes sensor angle irrelevant since located at the center of the agent.",
            defaultValue = "48", order = 4)
    private double radius = 48;

    /**
     * The length of the sides of the square sensor shape
     */
    private int sensorSize = 5;

    /**
     * Reference to the world this sensor is in
     */
    private OdorWorld world;

    /**
     * The absolute location of this sensor in the world
     */
    private Point2D.Double location = new Point2D.Double();

    /**
     * The relative location of this sensor to the entity
     */
    private Point2D.Double relativeLocation = new Point2D.Double();

    /**
     * The collision bound of this sensor
     */
    private RectangleCollisionBound collisionBound = new RectangleCollisionBound(
            new Rectangle2D.Double(0, 0, sensorSize, sensorSize)
    );

    /**
     * Construct bump sensor.
     *
     * @param parent  parent entity
     * @param baseValue value
     */
    public BumpSensor(OdorWorldEntity parent, double baseValue) {
        super(parent, "Bump Sensor" + baseValue);
        this.baseValue = baseValue;
        this.world = parent.getParentWorld();
    }

    /**
     * Construct bump sensor with default values.
     *
     * @param parent  parent entity
     */
    public BumpSensor(OdorWorldEntity parent) {
        super(parent, "Bump Sensor");
        this.world = parent.getParentWorld();
    }

    /**
     * {@inheritDoc}
     */
    public void update() {
        value = 0;
        updateCollisionBound();
        if (collided()) {
            value = baseValue;
        }
    }

    /**
     * Check if this sensor is collided with any entity.
     * @return true if collided with an entity, false otherwise
     */
    public boolean collided() {
        for (OdorWorldEntity e : world.getEntityList()) {
            if (e != parent && e.getCollisionBound().collide(this.collisionBound)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update the sensor {@link #location} and {@link #relativeLocation} base on the location information
     * from the entity.
     */
    public void updateLocation() {
        updateRelativeLocation();
        location.setLocation(
                relativeLocation.getX() + parent.getCenterX(),
                relativeLocation.getY() + parent.getCenterY()
        );
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
     * Update the {@link #collisionBound} base on the updated {@link #location} of this sensor.
     * This method will update both {@link #location} and {@link #relativeLocation} when called.
     */
    public void updateCollisionBound() {
        updateLocation();
        collisionBound.setVelocity(parent.getVelocityX(), parent.getVelocityY());
        collisionBound.setLocation(location.getX(), location.getY());
    }

    @Override
    public String getTypeDescription() {
        return "Bump";
    }

    @Override
    public EditableObject copy() {
        return new BumpSensor(parent, baseValue);
    }

    @Override
    public String getName() {
        return "Bump";
    }

    /**
     * Called by reflection to return a custom description for the {@link
     * org.simbrain.workspace.gui.couplingmanager.AttributePanel.ProducerOrConsumer}
     * corresponding to object sensors.
     */
    public String getSensorDescription() {
        return getParent().getName() + ":" + getTypeDescription() + " sensor";
    }

    @Producible(idMethod = "getId", customDescriptionMethod = "getSensorDescription")
    public double getCurrentValue() {
        return value;
    }

    public int getSensorSize() {
        return sensorSize;
    }

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

    public Point2D.Double getLocation() {
        return location;
    }

    public void setLocation(Point2D.Double location) {
        this.location = location;
    }

    /**
     * Update and get the {@link #relativeLocation} of this sensor.
     * @return the updated {@link #relativeLocation}
     */
    public Point2D.Double getRelativeLocation() {
        updateRelativeLocation();
        return relativeLocation;
    }

    public void setRelativeLocation(Point2D.Double relativeLocation) {
        this.relativeLocation = relativeLocation;
    }

    @Override
    public EntityAttributeNode getNode() {
        return new BumpSensorNode(this);
    }
}
