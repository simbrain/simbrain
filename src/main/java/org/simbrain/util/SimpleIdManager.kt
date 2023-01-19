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

/**
 * Maintains a map from Classes to [SimpleId]'s, to easily manage ids for a set of classes.
 * Can also be used as a way to generate names for objects.
 *
 * See [org.simbrain.network.core.Network] for an example.
 */
class SimpleIdManager @JvmOverloads constructor (
    /**
     * Associate classes with initial numbers. Like network.class -> networkList.size()
     */
    var initIdFunction: (Class<*>) -> Int = {1},

    /**
     * Associate classes with id "root" name. Like "NetworkComponent" -> Network
     */
    var baseNameGenerator: (Class<*>) -> String = { c -> c.simpleName},

    /**
     * Network_1, Network_2, etc.
     */
    var delimeter: String = "_"
) {


    /**
     * E.g. String.class -> "Neuron_1".  The integer in neuron 1 keeps getting incremented as more neurons are added.
     */
    private val idMap = HashMap<Class<*>, SimpleId>()

    private fun putClassIdMapping(clazz: Class<*>) {
        val rootName = baseNameGenerator(clazz)
        val initId = initIdFunction(clazz)
        idMap[clazz] = SimpleId(rootName, initId, delimeter)
    }

    /**
     * Get the id associated with a class. Increments the id number.
     */
    fun getAndIncrementId(clazz: Class<*>): String {
        if (!idMap.containsKey(clazz)) {
            putClassIdMapping(clazz)
        }
        return idMap[clazz]!!.andIncrement
    }

    /**
     * Get the [SimpleId.getProposedId] associated with a class.
     */
    fun getProposedId(clazz: Class<*>): String {
        if (!idMap.containsKey(clazz)) {
            putClassIdMapping(clazz)
        }
        return idMap[clazz]!!.proposedId
    }

    /**
     * An id based on a base name and an integer index.
     */
    class SimpleId @JvmOverloads constructor(
        val rootName: String,
        val initialIndex: Int,
        val delimeter: String = "_"
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
            get() = rootName + delimeter + index.getAndIncrement()

        /**
         * "Peek" ahead the next id that will be made if [.getAndIncrement] is called.
         */
        val proposedId: String
            get() = rootName + delimeter + index
        val currentIndex: Int
            get() = index.get()
    }
}