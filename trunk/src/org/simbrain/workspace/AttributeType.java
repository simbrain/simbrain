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
package org.simbrain.workspace;

/**
 * Encapsulates type information about potential attribute. Used to determine
 * which potential attributes are visible in coupling creation GUI.
 *
 * Displayed as typename:method<datatype>
 *
 * @author jyoshimi
 */
public class AttributeType {

    /**
     * Reference to parent component; needed so that visibility change events
     * can be fired.
     */
    private WorkspaceComponent parentComponent;

    /**
     * Description of this attribute type; generally a description of the object
     * type.
     */
    private final String typeID;

    /**
     * The root name of a getter or setter; i.e. "X" in "getX" or "setX".
     * Effectively serves as a "subtype" of a class of attributes associated
     * with the base object.
     */
    private final String methodBaseName;

    /** Class of this attribute. */
    private final Class<?> dataType;

    /** Whether this type of attribute is currently visible. */
    private boolean visible;

    /**
     * Construct an attribute type object.
     *
     * @param parent reference to parent component
     * @param typeID String identification of type id
     * @param methodName name of method
     * @param dataType data type (return type for producers;
     *        argument type for consumers)
     * @param visible whether this attribute should be visible for a given
     *        component.
     */
    public AttributeType(WorkspaceComponent parent, String typeID, String methodName, Class<?> dataType, boolean visible) {
        this.parentComponent = parent;
        this.typeID = typeID;
        this.methodBaseName = methodName;
        this.dataType = dataType;
        this.visible = visible;
    }

    /**
     * Return a description of the attribute.
     *
     * @return description
     */
    public String getDescription() {
        return getSimpleDescription() + typeClass();
    }

    @Override
    public String toString() {
        return getDescription();
    }

    /**
     * Returns a description of this attribute type.
     *
     * @return the description
     */
    public String getSimpleDescription() {
        if (methodBaseName != null) {
            return typeID + ":" + methodBaseName;
        } else {
            return typeID;
        }
    }

    /**
     * @return a formatted description of the class.
     */
    private String typeClass() {
        return " <" + dataType.getSimpleName() + ">";
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        parentComponent.fireAttributeTypeVisibilityChanged(this);
    }

    /**
     * @return the typeID
     */
    public String getTypeID() {
        return typeID;
    }

    /**
     * @return the subtype
     */
    public String getAttributeName() {
        return methodBaseName;
    }

    /**
     * @return the dataType
     */
    public Class getDataType() {
        return dataType;
    }

    /**
     * @return the parentComponent
     */
    public WorkspaceComponent getParentComponent() {
        return parentComponent;
    }

    /**
     * @return the methodBaseName
     */
    public String getMethodBaseName() {
        return methodBaseName;
    }

}
