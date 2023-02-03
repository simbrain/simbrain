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
package org.simbrain.world.odorworld.effectors;

import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.world.odorworld.entities.PeripheralAttribute;
import org.simbrain.world.odorworld.events.SensorEffectorEvents2;

import java.util.List;

/**
 * Abstract class for Odor World effectors.
 */
public abstract class Effector implements PeripheralAttribute {

    /**
     * Distributions for drop-down list used by
     * {@link org.simbrain.util.propertyeditor.ObjectTypeEditor}
     * to set a type of effector.
     */
    private static List<Class<? extends Effector>> EFFECTORS_LIST = List.of(
            Speech.class,
            StraightMovement.class,
            Turning.class
    );

    /**
     * Called via reflection using {@link UserParameter#typeListMethod()}.
     */
    public static List<Class<? extends Effector>> getTypes() {
        return EFFECTORS_LIST;
    }

    /**
     * The id of this smell effector.
     */
    @UserParameter(label = "Effector ID", description = "A unique id for this effector",
            order = 0, editable = false)
    private String id;

    /**
     * Public label of this effector.
     */
    @UserParameter(label = "Label", description = "Optional string description associated with this effector",
            initialValueMethod = "getLabel", order = 2)
    private String label = "";

    /**
     * Handle events.
     */
    private transient SensorEffectorEvents2 events = new SensorEffectorEvents2();

    /**
     * Construct an effector.
     *
     * @param label  a label for this effector
     */
    public Effector(String label) {
        super();
        this.label = label;
    }

    /**
     * Construct a copy of an effector.
     *
     * @param effector the effector to copy
     */
    public Effector(Effector effector) {
        super();
        this.label = effector.label;
    }

    /**
     * Default no-arg constructor for {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     */
    public Effector() {
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
    public abstract Effector copy();

    @Override
    public SensorEffectorEvents2 getEvents() {
        return events;
    }

    private Object readResolve() {
        events = new SensorEffectorEvents2();
        return this;
    }

    public static class EffectorCreator implements EditableObject {

        @UserParameter(label="Effector", isObjectType = true)
        private Effector effector = new StraightMovement();

        public EffectorCreator(String proposedLabel) {
            effector.label = proposedLabel;
        }

        public Effector getEffector() {
            return effector;
        }

        public void setEffector(Effector effector) {
            this.effector = effector;
        }
    }
}
