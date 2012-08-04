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
 * A <code>PotentialAttribute</code> corresponding to a <code>Producer</code>.
 *
 * @author jyoshimi
 *
 * @see PotentialAttribute
 * @see Attribute
 */
public class PotentialProducer extends PotentialAttribute {


    /**
     * Construct a potential consumer. For more on what these fields mean
     * see the javadocs for <code>Attribute</code>.
     *
     * @param parent parent workspace component
     * @param baseObject base object containing method to call
     * @param methodName name of method to call
     * @param dataType return type of method
     * @param argDataTypes for auxiliary arguments
     * @param argValues for auxiliary arguments
     * @param description description of the attribute
     */
    protected PotentialProducer(WorkspaceComponent parent, Object baseObject,
            String methodName, Class<?> dataType, Class<?>[] argDataTypes,
            Object[] argValues, String description) {
        super(parent, baseObject, methodName, dataType,
                argDataTypes, argValues, description);
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
