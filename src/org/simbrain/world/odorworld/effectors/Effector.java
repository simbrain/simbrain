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
import org.simbrain.util.propertyeditor2.CopyableObject;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.world.odorworld.entities.PeripheralAttribute;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import java.util.List;

/**
 * Abstract class for Odor World effectors.
 */
public abstract class Effector implements CopyableObject, PeripheralAttribute {

    /**
     * Distributions for drop-down list used by
     * {@link org.simbrain.util.propertyeditor2.ObjectTypeEditor}
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
     * Reference to parent entity.
     */
    protected OdorWorldEntity parent;

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
            defaultValue = "", order = 2)
    private String label;

    /**
     * Construct the effector.
     *
     * @param parent the parent entity
     * @param label  a label for this effector
     */
    public Effector(OdorWorldEntity parent, String label) {
        super();
        this.parent = parent;
        this.label = label;
    }

    /**
     * Default constructor for {@link org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor}.
     *
     * NOTE:
     * {@link org.simbrain.world.odorworld.dialogs.AddEffectorDialog} handles the set up of {@link #parent}.
     * When calling this directly, remember to set up the required field {@link #parent} accordingly.
     */
    public Effector() {
        super();
    }

    /**
     * Move the agent in a manner appropriate to the effector type.
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

    @Override
    public OdorWorldEntity getParent() {
        return parent;
    }

    public abstract void setParent(OdorWorldEntity parent);

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

    public static class EffectorCreator implements EditableObject {

        @UserParameter(label="Effector", isObjectType = true)
        private Effector effector = new StraightMovement();

        public Effector getEffector() {
            return effector;
        }

        public void setEffector(Effector effector) {
            this.effector = effector;
        }
    }
}
