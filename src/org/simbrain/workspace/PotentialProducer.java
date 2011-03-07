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
 * Class which can be used to create a producer.
 * 
 * @author jyoshimi
 */
public class PotentialProducer extends PotentialAttribute {

    /**
     * Construct a potential producer.
     * 
     * @param parent parent workspace component
     * @param methodBaseName method name
     * @param dataType class of data
     * @param description description of this potential attribute
     */
    public PotentialProducer(WorkspaceComponent parent, Object object,
            String methodBaseName, Class<?> dataType, String description) {
        super(parent, object, methodBaseName, dataType, description);
    }

    /**
     * Construct a potential attribute for the case where the method has
     * arguments.
     * 
     * @param parent parent workspace component
     * @param object base object containing method to call
     * @param methodName name of method to call
     * @param dataType return type of method
     * @param argDataTypes datatype of argument to method
     * @param argValues values of argument to method
     * @param description description of the attribute
     */
    protected PotentialProducer(WorkspaceComponent parent, Object object,
            String methodName, Class<?> dataType, Class<?>[] argDataTypes,
            Class<?>[] argValues, String description) {
        super(parent, object, methodName, dataType, argDataTypes, argValues,
                description);
    }

    /**
     * Construct a potential attribute for the case where the method has one
     * argument only.
     *
     * @param parent parent workspace component
     * @param object base object containing method to call
     * @param methodName name of method to call
     * @param dataType return type of method
     * @param argDataType datatype of argument to method
     * @param argValue value of argument to method
     * @param description description of the attribute
     */
    public PotentialProducer(WorkspaceComponent parent, Object baseObject,
            String methodName, Class<?> dataType, Class<?> argDataType,
            Object argValue, String description) {
        super(parent, baseObject, methodName, dataType,
                new Class[] { argDataType }, new Object[] { argValue },
                description);
    }

    /**
     * Actualize this potential attribute into a producer.
     * 
     * @return the producer corresponding to this potential attribute.
     */
    public Producer<?> createProducer() {
        return getParent().getAttributeManager().createProducer(this);
    }

}
