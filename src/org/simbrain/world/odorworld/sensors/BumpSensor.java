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
     * The length of the sides of the square sensor shape
     */
    private int sensorSize = 5;

    /**
     * Reference to the world this sensor is in
     */
    private OdorWorld world;

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
     * Default constructor for {@link org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor}.
     *
     * NOTE:
     * {@link org.simbrain.world.odorworld.dialogs.AddSensorDialog} handles the set up of {@link #parent}.
     * When calling this directly, remember to set up the required field {@link #parent} accordingly.
     */
    public BumpSensor() {
        super();
    }

    @Override
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
     * Update the {@link #collisionBound} base on the updated location of this sensor.
     */
    public void updateCollisionBound() {
        collisionBound.setVelocity(parent.getVelocityX(), parent.getVelocityY());
        collisionBound.setLocation(
                getRelativeLocation().getX() + parent.getX(),
                getRelativeLocation().getY() + parent.getY()
        );
    }

    @Override
    public String getTypeDescription() {
        return "Bump";
    }

    @Override
    public void setParent(OdorWorldEntity parent) {
        this.parent = parent;
    }

    @Override
    public EditableObject copy() {
        return new BumpSensor(parent, baseValue);
    }

    @Override
    public String getName() {
        return "Bump";
    }

    @Producible(idMethod = "getId", customDescriptionMethod = "getAttributeDescription")
    public double getCurrentValue() {
        return value;
    }

    public int getSensorSize() {
        return sensorSize;
    }

}
