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
 * which potential attributes are visible in coupling creation GUI. Displayed as
 * typename:method<datatype>
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
     * type. Useful when multiple attribute types correspond to the same method
     * name. E.g. in odor world right and left are both based on turn-amount,
     * but using different values.
     */
    private final String typeName;

    /**
     * The name of the method to call on the base object for this type of
     * attribute. Can be empty in cases where one type maps to multiple method
     * calls.
     */
    private final String methodName;

    /** Class of this attribute. */
    private final Class<?> dataType;

    /** Whether this type of attribute is currently visible. */
    private boolean visible;

    /**
     * Construct an attribute type object.
     *
     * @param parent reference to parent component
     * @param typeName String identification of type id
     * @param methodName name of method
     * @param dataType data type (return type for producers; argument type for
     *            consumers)
     * @param visible whether this attribute should be visible for a given
     *            component.
     */
    public AttributeType(WorkspaceComponent parent, String typeName,
            String methodName, Class<?> dataType, boolean visible) {
        this.parentComponent = parent;
        this.typeName = typeName;
        this.methodName = methodName;
        this.dataType = dataType;
        this.visible = visible;
    }

    /**
     * Construct an attribute type object with no method name.
     *
     * @param parent reference to parent component
     * @param typeName String identification of type id
     * @param dataType data type (return type for producers; argument type for
     *            consumers)
     * @param visible whether this attribute should be visible for a given
     *            component.
     */
    public AttributeType(WorkspaceComponent parent, String typeName,
             Class<?> dataType, boolean visible) {
        this(parent, typeName, "", dataType, visible);
    }

    /**
     * Returns a description using a custom base name (e.g. a named neuron, like
     * "Neuron 5") as a base. Adds method base name and data type.
     *
     * @param baseName the custom base name
     * @return the formatted string.
     */
    public String getDescription(String baseName) {
        return baseName + ":" + methodName + typeClass();
    }

    /**
     * Like getDescription(String) but does not add the method base name.
     *
     * @param baseName the custom base name
     * @return the formatted String
     */
    public String getSimpleDescription(String baseName) {
        return baseName + typeClass();
    }

    /**
     * @return a formatted description of the class.
     */
    private String typeClass() {
        return " <" + dataType.getSimpleName() + ">";
    }

    /**
     * Returns a description of this type, including method base name and data
     * type.
     *
     * @return the formatted String
     */
    public String getDescription() {
        return getBaseDescription() + typeClass();
    }

    /**
     * Like getDescription() but does not return method base name.
     *
     * @return the formatted String
     */
    public String getSimpleDescription() {
        return typeName + typeClass();
    }

    /**
     * Returns the description string of this datatype, with a method name if
     * there is one.
     *
     * @return the description
     */
    public String getBaseDescription() {
        if (methodName != null) {
            return typeName + ":" + methodName;
        } else {
            return typeName;
        }
    }

    @Override
    public String toString() {
        return getBaseDescription();
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
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return the subtype
     */
    public String getAttributeName() {
        return methodName;
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
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }

}
