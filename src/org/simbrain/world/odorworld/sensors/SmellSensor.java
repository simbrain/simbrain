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

import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * A sensor which is updated based on the presence of SmellSources near it.
 *
 * @see org.simbrain.util.environment.SmellSource
 */
public class SmellSensor extends Sensor implements VisualizableEntityAttribute {

    /**
     * Default label.
     */
    public static final String DEFAULT_LABEL = "SmellSensor";


    /**
     * Current value of this sensor, as an array of doubles.
     */
    private double[] currentValue = new double[0];

    /**
     * Construct a smell sensor.
     *
     * @param parent parent
     * @param label  label for this sensor (entity name will be added)
     * @param theta  offset from straight in degrees radians
     * @param radius length of "whisker"
     */
    public SmellSensor(final OdorWorldEntity parent, final String label, double theta, double radius) {
        super(parent, label);
        this.parent = parent;
        this.theta = theta;
        this.radius = radius;
    }

    /**
     * Construct a smell sensor.
     *
     * @param parent parent
     */
    public SmellSensor(final OdorWorldEntity parent) {
        super(parent, DEFAULT_LABEL);
        this.parent = parent;
    }

    /**
     * Default constructor for {@link org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor}.
     *
     * NOTE:
     * {@link org.simbrain.world.odorworld.dialogs.AddSensorDialog} handles the set up of {@link #parent}.
     * When calling this directly, remember to set up the required field {@link #parent} accordingly.
     */
    public SmellSensor() {
        super();
    }

    /**
     * Update the smell array ({@link #currentValue}) by iterating over entities
     * and adding up their distance-scaled smell vectors.
     */
    @Override
    public void update() {

        // Start with an empty array. It will grow in size as smell vectors are
        // added.
        currentValue = new double[0];
        for (OdorWorldEntity entity : parent.getParentWorld().getEntityList()) {
            // Don't smell yourself
            if (entity != parent) {
                double[] smell = entity.getSmellVector(getLocation());
                if (smell != null) {
                    currentValue = SimbrainMath.addVector(currentValue, smell);
                }
            }
        }
    }

    @Producible(idMethod = "getId", customDescriptionMethod = "getSmellSensorDescription")
    public double[] getCurrentValues() {
        return currentValue;
    }

    @Override
    public String getTypeDescription() {
        return "Smell";
    }

    @Override
    public void setParent(OdorWorldEntity parent) {
        this.parent = parent;
    }

    /**
     * Called by reflection to return a custom description for couplings.
     */
    public String getSmellSensorDescription() {
        return getParent().getName() + ":" + "Smell sensor (" +
            SimbrainMath.roundDouble(theta, 2) + "," +
            SimbrainMath.roundDouble(radius, 2) + ")";
    }

    @Override
    public EditableObject copy() {
        return new SmellSensor(parent, getLabel(), theta, radius);
    }

    @Override
    public String getName() {
        return "Smell";
    }

}
