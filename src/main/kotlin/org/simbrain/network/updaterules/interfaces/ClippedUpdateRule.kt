package org.simbrain.network.updaterules.interfaces

import org.simbrain.util.UserParameter
import org.simbrain.util.clip
import smile.math.matrix.Matrix

interface ClippedUpdateRule: BoundedUpdateRule {
    /**
     * Turn clipping on and off.
     *
     * @param clipping true if clipping should be on; false otherwise
     */
    @UserParameter(
        label = "Clipping",
        description = " If a neuron uses clipping, then if its activation exceeds its upper or lower bound, the activation is set to the upper or lower bound that it exceeds. Similarly with weights and their strength",
        order = -1
    )
    var isClipped: Boolean

    fun clip(value: Double) = if (isClipped) value.clip(lowerBound, upperBound) else value

    fun clip(value: Matrix) {
        if (isClipped) value.clip(lowerBound, upperBound)
    }
}