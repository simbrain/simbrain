package org.simbrain.network.updaterules.interfaces

import org.simbrain.util.UserParameter
import org.simbrain.util.clip
import smile.math.matrix.Matrix

/**
 * An interface for updates rules that make use of an upper and lower bound,
 * either for clipping or for setting intrinsic bounds.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
interface BoundedUpdateRule {
    /**
     * Sets the upper bound of this neuron update rule's activation.
     *
     * @param upperBound the upper bound
     */
    @UserParameter(
        label = "Upper Bound",
        description = "Upper bound that determines the maximum level of activity of a node.",
        order = -2
    )
    var upperBound: Double

    /**
     * Sets the lower bound of this neuron update rule's activation.
     *
     * @param lowerBound the lower bound
     */
    @UserParameter(
        label = "Lower Bound",
        description = "Lower bound that determines the minimum level of activity of a node.",
        order = -1
    )
    var lowerBound: Double

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