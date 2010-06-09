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
 * Encapsulates type information about a particular attribute or potential
 * attribute.  Used to determine which potential attributes are visible in 
 * coupling creation GUI.
 *
 * @author jyoshimi
 */
public class AttributeType {

    /** ID for this type. */
    private String typeID;

    /** The root name of a getter or setter; i.e. "X" in "getX" or "setX".  */
    private String methodBaseName;

    /** Class of this attribute. */
    private Class<?> dataType;

    /** Whether this type of attribute is currently visible. */
    private boolean visible;

    /**
     * Construct an attribute type object.
     *
     * @param typeID String identification of type id
     * @param methodName name of method 
     * @param dataType data type (return type for producers; argument type for consumers)
     * @param visible whether this attribute should be visible for a given component
     */
    public AttributeType(String typeID, String methodName, Class dataType, boolean visible) {
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
     * Returns a description of this potential attribute.
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
    }

    /**
     * @return the typeID
     */
    public String getTypeID() {
        return typeID;
    }

    /**
     * @param typeID the typeID to set
     */
    public void setTypeID(String typeID) {
        this.typeID = typeID;
    }

    /**
     * @return the subtype
     */
    public String getAttributeName() {
        return methodBaseName;
    }

    /**
     * @param subtype the subtype to set
     */
    public void setAttributeName(String subtype) {
        this.methodBaseName = subtype;
    }

    /**
     * @return the dataType
     */
    public Class getDataType() {
        return dataType;
    }

    /**
     * @param dataType the dataType to set
     */
    public void setDataType(Class dataType) {
        this.dataType = dataType;
    }

}
