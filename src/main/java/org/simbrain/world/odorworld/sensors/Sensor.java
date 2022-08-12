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
import org.simbrain.world.odorworld.entities.PeripheralAttribute;
import org.simbrain.world.odorworld.events.SensorEffectorEvents;

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
    private transient SensorEffectorEvents events = new SensorEffectorEvents(this);

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

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
        getEvents().firePropertyChanged();
    }

    @Override
    public abstract Sensor copy();

    public SensorEffectorEvents getEvents() {
        return events;
    }

    public Object readResolve() {
        events = new SensorEffectorEvents(this);
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
