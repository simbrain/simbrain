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
 * A <code>PotentialAttribute</code> corresponding to a <code>Consumer</code>.
 *
 * @author jyoshimi
 *
 * @see PotentialAttribute
 * @see Attribute
 */
public class PotentialConsumer extends PotentialAttribute {


    /**
     * Construct a potential consumer for the case where the method has multiple
     * arguments. For more on what these fields mean see the javadocs for
     * <code>Attribute</code>.
     *
     * @param parent parent workspace component
     * @param object base object containing method to call
     * @param methodName name of method to call
     * @param argDataTypes first member is main data type. Others for auxiliary
     *            methods.
     * @param argValues for auxiliary methods
     * @param description description of the attribute
     */
    protected PotentialConsumer(WorkspaceComponent parent, Object object,
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
