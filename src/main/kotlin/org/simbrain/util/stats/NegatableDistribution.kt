package org.simbrain.util.stats

import org.simbrain.util.UserParameter

interface NegatableDistribution {

    @get:UserParameter(
        label = "Negate",
        description = "Return -1 * the sampled value.",
        order = 10
    )
    var negate: Boolean

    fun Double.conditionalNegate() =
        if (negate) -1 * this
        else this

    fun Int.conditionalNegate() =
        if (negate) -1 * this
        else this

    fun DoubleArray.conditionalNegate() =
        if (negate) map { it.conditionalNegate() }.toDoubleArray()
        else this

    fun IntArray.conditionalNegate() =
        if (negate) map { it.conditionalNegate() }.toIntArray()
        else this

}
