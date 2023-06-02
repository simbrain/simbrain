package org.simbrain.network.updaterules.interfaces

import org.simbrain.util.UserParameter

/**
 * Interface for update rules which can clip their values above and below upper
 * and lower bounds (clipping can be turned on or off). Thus
 * ClippableUpdateRules are always BoundedUpdateRules (but some
 * BoundedUpdateRules, like sigmoidal, are not Clippable, since they have
 * intrinsic bounds).
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
interface ClippableUpdateRule {
    /**
     * Clip the current activation.
     *
     * @param val the value to clip
     * @return the clipped value
     */
    fun clip(`val`: Double): Double

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
}