package org.simbrain.util

val counters = HashMap<String, Int>()

/**
 * Utilities for performance tuning.
 */

/**
 * Count the number of times an event (labelled by a string) occurs.
 * Call e.g. with count("neuron.setLocation").  Get the count with
 * getCounters().get("neuron.setLocation").
 */
fun count(name: String) {
    if (counters.containsKey(name)) {
        counters[name] = counters[name]!! + 1
    } else {
        counters[name] = 1
    }
}
