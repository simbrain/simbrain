package org.simbrain.util

import org.simbrain.util.SimpleIdManager.SimpleId
import java.util.concurrent.atomic.AtomicInteger

/**
 * Maintains a map from Classes to [SimpleId]'s, to easily manage ids for a set of classes.
 */
class SimpleIdManager @JvmOverloads constructor (
    /**
     * Associate classes with initial numbers. Like network.class -> networkList.size()
     */
    var initIdFunction: (Class<*>) -> Int = { 1 },

    /**
     * Associate classes with id "root" name. Like "NetworkComponent" -> Network
     */
    var baseNameGenerator: (Class<*>) -> String = { c -> c.simpleName },

    /**
     * Network_1, Network_2, etc.
     */
    var delimeter: String = "_"
) {


    /**
     * E.g. Neuron.class -> "Neuron_1".  The integer in neuron 1 keeps getting incremented as more neurons are added.
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
        return idMap[clazz]!!.getAndIncrement()
    }

    /**
     * An id based on a base name and an integer index.
     */
    class SimpleId @JvmOverloads constructor(
        val rootName: String,
        initialIndex: Int,
        val delimeter: String = "_"
    ) {

        private val index: AtomicInteger = AtomicInteger(initialIndex)

        /**
         * Returns a simple identifier and increments id index.
         */
        fun getAndIncrement(): String = rootName + delimeter + index.getAndIncrement()

    }
}