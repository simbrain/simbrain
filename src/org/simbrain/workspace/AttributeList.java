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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating and maintaining a list of "getterSetters", which
 * are objects which are simple objects that call get or set for some property.
 * Used to create access to objects in lists or arrays for Attributes to use.
 * <p>
 * TODO: No longer being used in favor of auxiliary arguments.  Possibly remove.
 *
 * @param <E> the data type used in the underlying getterSetter objects.
 */
public class AttributeList<E> {

    /** The main list. */
    private final List<GetterSetter<E>> attributeList;

    /**
     * Construct the list.
     *
     * @param size initial size of list
     */
    public AttributeList(final int size) {
        attributeList = new ArrayList<GetterSetter<E>>(size);
        for (int i = 0; i < size; i++) {
            attributeList.add(i, new GetterSetter<E>());
        }
    }

    /**
     * Returns the getterSetter associated with this index.
     *
     * @param index index to get
     * @return the appropriate getter setter object
     */
    public GetterSetter<E> getGetterSetter(int index) {
        return attributeList.get(index);
    }

    /**
     * Return the value of the getter at the specified index.
     *
     * @param index the index of the getter to query
     * @return the value at that index
     */
    public E getVal(int index) {
        return attributeList.get(index).getValue();
    }

    /**
     * Set the value of setter at the specified index.
     *
     * @param index the index of the setter to set
     * @param value the value to set on that object
     */
    public void setVal(int index, E value) {
        attributeList.get(index).setValue(value);
    }

    /**
     * Helper object for use with couplings. An object of this class is
     * associated with one dimension of a smell sensor.
     */
    public class GetterSetter<E> {

        /** The value to get and set. */
        private E value;

        /**
         * @return the value
         */
        public E getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(E value) {
            this.value = value;
        }
    }

}
