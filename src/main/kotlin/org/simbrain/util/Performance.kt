package org.simbrain.util

val counters = HashMap<String, Int>()
val timers = HashMap<String, List<Long>>()

class Duration(val start: Long, val end: Long?)

fun count(name: String) {
    if (counters.containsKey(name)) {
        counters[name] = counters[name]!! + 1
    } else {
        counters[name] = 1
    }
}

fun timeStart(name: String) {

}