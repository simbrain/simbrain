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
package org.simbrain.util

import org.simbrain.util.SimpleIdManager.SimpleId
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function

/**
 * Maintains a map from Classes to [SimpleId]'s, to easily
 * manage ids for a set of classes. You give it a way to associat
 *
 * Initialize with a function that associates types to initial ids.
 *
 * See [org.simbrain.network.core.Network] for an example.
 */
class SimpleIdManager (
    private val initIdFunction: Function<Class<*>, Int>
) {

    private val idMap = HashMap<Class<*>, SimpleId>()

    /**
     * Initialize a class > id mapping.
     *
     * @param clazz the class to associate with ids
     * @param rootName the root name for the id, e.g. Neuron
     * @param initId the initial id number, e.g. 2 to start at Neuron_2
     */
    private fun initClassIdMapping(clazz: Class<*>, rootName: String, initId: Int) {
        idMap[clazz] = SimpleId(rootName, initId)
    }

    private fun initClassIdMapping(clazz: Class<*>, initId: Int) {
        initClassIdMapping(clazz, clazz.simpleName, initId)
    }

    /**
     * Get the id associated with a class. Increments the id number.
     */
    fun getAndIncrementId(clazz: Class<*>): String {
        if (!idMap.containsKey(clazz)) {
            initClassIdMapping(clazz, initIdFunction.apply(clazz))
        }
        return idMap[clazz]!!.andIncrement
    }

    /**
     * Get the [SimpleId.getProposedId] associated with a class.
     */
    fun getProposedId(clazz: Class<*>): String {
        if (!idMap.containsKey(clazz)) {
            initClassIdMapping(clazz, initIdFunction.apply(clazz))
        }
        return idMap[clazz]!!.proposedId
    }

    /**
     * An id based on a base name and an integer index.
     */
    class SimpleId(
        /**
         * The base name of the id.
         */
        private val rootName: String, initialIndex: Int
    ) {

        /**
         * The starting index.
         */
        private val index: AtomicInteger

        init {
            index = AtomicInteger(initialIndex)
        }

        /**
         * Returns a simple identifier and increments id index.
         */
        val andIncrement: String
            get() = rootName + "_" + index.getAndIncrement()

        /**
         * "Peek" ahead the next id that will be made if [.getAndIncrement] is called.
         */
        val proposedId: String
            get() = rootName + "_" + index
        val currentIndex: Int
            get() = index.get()
    }
}