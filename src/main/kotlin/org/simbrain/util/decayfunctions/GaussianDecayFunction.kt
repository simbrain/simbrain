package org.simbrain.util.decayfunctions

import kotlin.math.exp
import kotlin.math.pow

/**
 * Gaussian decay.
 *
 * Peak distance corresponds to the mean.
 *
 * Standard deviation is dispersion / 2.  That is, dispersion corresponds to 2 standard deviations away from the
 * mean, which produces intuitive results in terms of amount of decay.
 *
 * The standard normalizing factor that makes a Gaussian integrate to 1 has been removed so that [getScalingFactor]
 * returns a 1 at [peakDistance], as expected.
 */
class GaussianDecayFunction @JvmOverloads constructor(dispersion: Double = 70.0): DecayFunction(dispersion) {

    override fun getScalingFactor(distance: Double): Double {
        val mean = peakDistance
        val std = dispersion / 2
        return exp(-.5 * ((distance - mean) / std).pow(2.0))
    }

    override fun copy(): GaussianDecayFunction {
        return GaussianDecayFunction(dispersion)
            .also {
                it.peakDistance = peakDistance
            }
    }

    override val name = "Exponential"

}