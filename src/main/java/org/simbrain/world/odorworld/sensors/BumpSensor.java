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
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.OdorWorldUtilsKt;
import org.simbrain.world.odorworld.entities.Bound;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

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
            order = 4)
    private double baseValue = 1;

    /**
     * The length of the sides of the square sensor shape
     */
    private int sensorSize = 5;

    /**
     * Construct bump sensor.
     *
     * @param baseValue value
     */
    public BumpSensor(double baseValue) {
        super("Bump Sensor " + baseValue);
        this.baseValue = baseValue;
    }

    public BumpSensor() {
        super("Bump sensor");
    }

    /**
     * Construct a copy of a bump sensor.
     *
     * @param bumpSensor the bump sensor to copy
     */
    public BumpSensor(BumpSensor bumpSensor) {
        super(bumpSensor);
        this.baseValue = bumpSensor.baseValue;
    }

    @Override
    public void update(OdorWorldEntity parent) {
        value = 0;
        var bound = new Bound(
                parent.getX() - sensorSize / 2.0,
                parent.getY() - sensorSize / 2.0,
                parent.getWidth() + sensorSize,
                parent.getHeight() + sensorSize
        );
        var collided = parent.getWorld().getCollidableObjects()
                .stream()
                .filter(it -> it != parent)
                .anyMatch(it -> OdorWorldUtilsKt.intersect(bound, it).getIntersect());
        if (collided) {
            value = baseValue;
        }
    }

    @Override
    public BumpSensor copy() {
        return new BumpSensor(this);
    }

    @Override
    public String getName() {
        return "Bump Sensor";
    }

    @Producible(customDescriptionMethod = "getAttributeDescription")
    public double getCurrentValue() {
        return value;
    }

    public int getSensorSize() {
        return sensorSize;
    }

    @Override
    public String getLabel() {
        if (super.getLabel().isEmpty()) {
            return getDirectionString() + "Bump Sensor";
        } else {
            return super.getLabel();
        }
    }
}