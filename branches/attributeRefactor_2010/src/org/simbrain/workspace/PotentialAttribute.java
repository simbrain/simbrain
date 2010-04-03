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

import java.lang.reflect.Method;

/**
 * TODO...
 *
 * @author jyoshimi
 *
 */
public class PotentialAttribute<E> {

    /** The ID. */
    private AttributeType type;

    /** Parent workspace component. */
    private WorkspaceComponent parent;

    /** Parent object on which an attribute will be set or get. */
    private Object parentObject;

    /** Key for finding parent object. */ 
    private String objectKey; //TODO: Use this for description and serialization

    /** Name of the method that sets or gets the attribute. */
    private String methodName;

    /** The method that sets or gets the attribute. */
    protected Method theMethod = null;

    /**
     * Construct a potential attribute.
     *
     * @param type
     * @param parent
     * @param parentObject
     * @param methodName
     */
    public PotentialAttribute(AttributeType type, WorkspaceComponent parent,
            Object parentObject, String methodName) {
        this.type = type;
        this.parent = parent;
        this.parentObject = parentObject;
        this.methodName = methodName;
    }

    /**
     * Returns a description for use in GUIs...
     *
     * @return description
     */
    public String getDescription() {
        return type.getDescription();
    }

    /**
     * @return the parent
     */
    public WorkspaceComponent getParent() {
        return parent;
    }


    /**
     * @return the type
     */
    public AttributeType getType() {
        return type;
    }


    /**
     * @param type the type to set
     */
    public void setType(AttributeType type) {
        this.type = type;
    }

    /**
     * @return the parentObject
     */
    public Object getParentObject() {
        return parentObject;
    }

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }


}
