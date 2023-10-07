package org.simbrain.util.projection

interface IterableProjectionMethod {

    var error: Double

    fun iterate(dataset: Dataset)

}