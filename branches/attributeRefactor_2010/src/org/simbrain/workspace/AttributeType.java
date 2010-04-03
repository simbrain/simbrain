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
 * Encapsulates type information about a particular attribute or potential attribute.
 *
 * @author jyoshimi
 */
public class AttributeType {

    /** ID for this type. */
    private String typeID;

    /** ID for subtype. Null if there is none. */ 
    private String subtype;

    /** Class of this attribute. */
    private Class dataType;

    /**
     * Return a description of the attribute.
     *
     * @return description
     */
    public String getDescription() {
        return getSimpleDescription() + typeClass();
    }

    public String getSimpleDescription() {
        if (subtype != null) {
            return typeID + ":" + subtype;
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
     * @param typeID
     * @param subtype
     * @param visible
     */
    public AttributeType(String typeID, String subtype, boolean visible, Class dataType) {
        this.typeID = typeID;
        this.subtype = subtype;
        this.visible = visible;
        this.dataType = dataType;
    }

    /** Whether this type of attribute is currently visible. */
    private boolean visible;

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

    public String toString() {
        return getDescription();
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
    public String getSubtype() {
        return subtype;
    }

    /**
     * @param subtype the subtype to set
     */
    public void setSubtype(String subtype) {
        this.subtype = subtype;
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
