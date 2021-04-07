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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Maintains a map from Classes to {@link SimpleId}'s, to easily
 * manage ids for a set of classes.
 */
public class SimpleIdManager {

    /**
     * The map backing this class.
     */
    private final HashMap<Class<?>, SimpleId> idMap = new HashMap<>();

    /**
     * Function to initialize id count assocaitd with a class.
     */
    private final Function<Class<?>, Integer> initIdFunction;

    /**
     * Initialize with a initialization function
     */
    public SimpleIdManager(Function<Class<?>, Integer> initIdFunction) {
        this.initIdFunction = initIdFunction;
    }

    /**
     * Initialize a class > id mapping.
     *
     * @param clazz the class to associate with ids
     * @param rootName the root name for the id, e.g. Neuron
     * @param initId the initial id number, e.g. 2 to start at Neuron_2
     */
    private void initId(Class clazz, String rootName, int initId) {
        idMap.put(clazz, new SimpleId(rootName, initId));
    }

    /**
     * @see #initId(Class, int)
     */
    private void initId(Class<?> clazz, int initId) {
        initId(clazz, clazz.getSimpleName(), initId);
    }

    /**
     * Get the id associated with a class. Increments the id number.
     */
    public String getAndIncrementId(Class<?> clazz) {
        if (!idMap.containsKey(clazz)) {
            initId(clazz, initIdFunction.apply(clazz));
        }
        return idMap.get(clazz).getAndIncrement();
    }

    /**
     * Get the {@link SimpleId#getProposedId()} associated with a class.
     */
    public String getProposedId(Class<?> clazz) {
        if (!idMap.containsKey(clazz)) {
            initId(clazz, initIdFunction.apply(clazz));
        }
        return idMap.get(clazz).getProposedId();
    }

    /**
     * <b>SimpleId</b> provides an id based on a base name and an integer index.
     */
    public static class SimpleId {

        /**
         * The base name of the id.
         */
        private String rootName;

        /**
         * The starting index.
         */
        private AtomicInteger index;

        /**
         * Construct simpleId.
         *
         * @param rootName root name.
         * @param initialIndex beginning index.
         */
        public SimpleId(final String rootName, final int initialIndex) {
            this.rootName = rootName;
            this.index = new AtomicInteger(initialIndex);
        }

        /**
         * Returns a simple identifier and increments id index.
         *
         * @return a unique identification
         */
        public String getAndIncrement() {
            String id = rootName + "_" + index.getAndIncrement();
            return id;
        }

        /**
         * "Peek" ahead the next id that will be made if {@link #getAndIncrement()} is called.
         */
        public String getProposedId() {
            return rootName + "_" + index;
        }

        public int getCurrentIndex() {
            return index.get();
        }

    }
}
