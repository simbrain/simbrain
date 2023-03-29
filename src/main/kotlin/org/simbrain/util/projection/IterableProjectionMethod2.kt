package org.simbrain.util.projection

interface IterableProjectionMethod2 {

    var error: Double

    fun iterate(dataset: Dataset2)

}