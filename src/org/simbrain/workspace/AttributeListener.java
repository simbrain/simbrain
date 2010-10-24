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
 * Listener for attribute related events, broadly (this includes potential
 * attributes and attribute types).
 *
 * @author jyoshimi
 */
public interface AttributeListener {

    /**
     * The visibility of an attribute type changed.
     *
     * @param type the type whose visibility changed
     */
    public void attributeTypeVisibilityChanged(AttributeType type);

    /**
     * Potential attributes have changed in some way, such that the list of
     * potential attributes for a given workspace component is changed.
     */
    public void potentialAttributesChanged();

    /**
     * The base object of an attribute has been removed. The corresponding
     * coupling should therefore be deleted.
     *
     * @param object the removed object.
     */
    public void attributeObjectRemoved(Object object);

}
