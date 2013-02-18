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

import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Interface for 2d world sensors.
 */
public abstract class Sensor {

    /** Reference to parent entity. */
    protected OdorWorldEntity parent;

    /** The id of this smell sensor.. */
    private String id;

    /** Public label of this sensor. */
    private String label;

    /**
     * Update the sensor.
     */
    public abstract void update();

    /**
     * Return a list of entity types which can use this type of sensor.
     *
     * @return list of applicable types.
     */
    public List<Class<?>> getApplicableTypes() {
        return null;
    }

    /**
     * @return the parent
     */
    public OdorWorldEntity getParent() {
        return parent;
    }

    /**
     * @return the name
     */
    public String getId() {
        return id;
    }

    /**
     * @param name the name to set
     */
    public void setId(String name) {
        this.id = name;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

}
