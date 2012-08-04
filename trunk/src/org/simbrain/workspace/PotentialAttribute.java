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
 * Even though a <code>Coupling</code> consists of a <code>Producer</code> and a
 * <code>Consumer</code>, these attributes are usually not directly created, but
 * are created from a <code>PotentialProducer</code> or
 * <code>PotentialConsumer</code> object (using creation methods in
 * <code>AttributeManager</code>).
 * <p>
 * Part of the reason for this is that a WorkspaceComponent needs to return a
 * list of potential attributes that are not members of couplings, but that can
 * be "actualized" to real attributes when creating a coupling.
 * <p>
 * PotentialAttributes contain all the same information as an attribute. For
 * more on what these fields mean see the javadocs for <code>Attribute</code>
 *
 * @author jyoshimi
 *
 * @see Attribute
 * @see AttributeManager
 * @see Coupling
 */
public class PotentialAttribute {

    /** Parent workspace component. */
    private WorkspaceComponent parent;

    /** Potential producing or consuming object. */
    private Object baseObject;

    /** Name of the method that sets or gets the attribute. */
    private String methodName;

    /**
     * The "main" data type (double, string, etc) consumed by a consumer or
     * produced by a producer. See the javadocs for <code>Attribute</code>.
     */
    private Class<?> dataType;

    /**
     * Returns method signature for auxiliary arguments, or null if there are
     * none. See the javadocs for <code>Attribute</code>.
     */
    private Class<?>[] argumentDataTypes;

    /**
     * Returns argument values for auxiliary arguments, or null if there are
     * none. See the javadocs for <code>Attribute</code>.
     */
    private Object[] argumentValues;

    /** Returns a description of this type of potential attribute. */
    private String description;

    /**
     * Construct a potential attribute. Should generally not be called directly.
     * Instead the methods in <code>AttributeManager</code> should be called.
     * For more on what these fields mean see the javadocs for
     * <code>Attribute</code>
     *
     * @param parent parent workspace component
     * @param object base object containing method to call
     * @param methodName name of method to call
     * @param dataType main data type
     * @param argDataTypes method signature for auxiliary arguments
     * @param argValues values for auxiliary arguments
     * @param description description of the attribute
     */
    protected PotentialAttribute(WorkspaceComponent parent, Object object,
            String methodName, Class<?> dataType, Class<?>[] argDataTypes,
            Object[] argValues, String description) {
        this.parent = parent;
        this.baseObject = object;
        this.methodName = methodName;
        this.dataType = dataType;
        this.argumentDataTypes = argDataTypes;
        this.argumentValues = argValues;
        this.description = description;
    }

    /**
     * Returns a description of this potential attribute; used in GUI.
     *
     * @return a description of this attribute
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the parent
     */
    public WorkspaceComponent getParent() {
        return parent;
    }

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @return the attributeType
     */
    public Class<?> getDataType() {
        return dataType;
    }

    /**
     * @return the object
     */
    public Object getBaseObject() {
        return baseObject;
    }

    /**
     * @return the argumentDataTypes
     */
    public Class<?>[] getArgumentDataTypes() {
        return argumentDataTypes;
    }

    /**
     * @return the argumentValues
     */
    public Object[] getArgumentValues() {
        return argumentValues;
    }

    /**
     * @param argumentDataTypes the argumentDataTypes to set
     */
    protected void setArgumentDataTypes(Class<?>[] argumentDataTypes) {
        this.argumentDataTypes = argumentDataTypes;
    }

    /**
     * @param argumentValues the argumentValues to set
     */
    protected void setArgumentValues(Object[] argumentValues) {
        this.argumentValues = argumentValues;
    }

    /**
     * Set a custom description, overriding the default description.
     *
     * @param description the description to set
     */
    public void setCustomDescription(String description) {
        this.description = description;
    }

}
