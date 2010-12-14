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
            String methodBaseName, Class dataType, String description) {
        super(parent, object, methodBaseName, dataType, description);
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
