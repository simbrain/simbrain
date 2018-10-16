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
import org.simbrain.world.odorworld.entities.PeripheralAttribute;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import java.util.Arrays;
import java.util.List;

/**
 * Interface for 2d world sensors.
 */
public abstract class Sensor implements CopyableObject, PeripheralAttribute {

    public static List<Class> SENSOR_LIST =
            Arrays.asList(SmellSensor.class, Hearing.class, TileSensor.class, ObjectSensor.class);

    public static List<Class> getTypes() {
        return SENSOR_LIST;
    }

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

}
