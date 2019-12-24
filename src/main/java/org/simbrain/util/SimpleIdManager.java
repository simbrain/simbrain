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
package org.simbrain.util;

import java.util.HashMap;

/**
 * Maintains a map from Classes to {@link SimpleId}'s, to easily
 * manage ids for a set of classes.
 */
public class SimpleIdManager {

    /**
     * The map backing this class.
     */
    private HashMap<Class, SimpleId> idMap = new HashMap<>();

    /**
     * Initialize a class > id mapping.
     *
     * @param clazz the class to associate with ids
     * @param rootName the root name for the id, e.g. Neuron
     * @param initId the initial id number, e.g. 2 to start at Neuron_2
     */
    public void initId(Class clazz, String rootName, int initId) {
        idMap.put(clazz, new SimpleId(rootName, initId));
    }

    /**
     * @see #initId(Class, int)
     */
    public void initId(Class clazz, int initId) {
        initId(clazz, clazz.getSimpleName(), initId);
    }

    /**
     * Get the id associated with a class. Increments the id number.
     */
    public String getId(Class clazz) {
        return idMap.get(clazz).getId();
    }

    /**
     * Get the {@link SimpleId#getProposedId()} associated with a class.
     */
    public String getProposedId(Class clazz) {
        return idMap.get(clazz).getProposedId();
    }
}
