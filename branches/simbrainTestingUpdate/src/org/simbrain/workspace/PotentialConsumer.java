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
 * Class which can be used to create a consumer.
 *
 * @author jyoshimi
 */
public class PotentialConsumer extends PotentialAttribute {

    /**
     * Construct a potential attribute for the (default) case where the method
     * has one argument only.
     *
     * @param parent parent workspace component
     * @param object base object containing method to call
     * @param methodName name of method to call
     * @param dataType return type of method
     * @param argDataType datatype of argument to method
     * @param argValue value of argument to method
     * @param description description of the attribute
     */
    public PotentialConsumer(WorkspaceComponent parent, Object baseObject,
            String methodName, Class<?> dataType, String description) {
        super(parent, baseObject, methodName, dataType, description);
    }

    /**
     * Construct a potential consumer for the case where the method has multiple
     * arguments. The first is the "main datatype" of the consumer.
     *
     * TODO: No support for more than two arguments currently.
     *
     * @param parent parent workspace component
     * @param object base object containing method to call
     * @param methodName name of method to call
     * @param dataType return type of method
     * @param argDataTypes datatypes of argument to method
     * @param argValues values of argument to method
     * @param description description of the attribute
     */
    public PotentialConsumer(WorkspaceComponent parent, Object object,
            String methodName, Class<?>[] argDataTypes, Object[] argValues,
            String description) {
        super(parent, object, methodName, argDataTypes[0], argDataTypes,
                argValues, description);
    }

    /**
     * Actualize this potential attribute into a consumer.
     *
     * @return the consumer corresponding to this potential attribute.
     */
    public Consumer<?> createConsumer() {
        return getParent().getAttributeManager().createConsumer(this);
    }

}
