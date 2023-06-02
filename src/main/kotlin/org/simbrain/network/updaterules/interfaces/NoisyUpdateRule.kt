package org.simbrain.network.updaterules.interfaces

import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution

/**
 * An interface which should be implemented by any neuron update rule that uses
 * a randomizer to provide noise.
 *
 * @author Zoë
 */
interface NoisyUpdateRule {
    /**
     * Set the noise generator.
     *
     * @param rand the generator to set
     */
    @UserParameter(label = "Randomizer", isObjectType = true, showDetails = false, order = 1000, tab = "Noise")
    var noiseGenerator: ProbabilityDistribution

    /**
     * Set whether noise should be used.
     *
     * @param noise true if noise should be used; false otherwise.
     */
    @UserParameter(
        label = "Add noise",
        description = "If this is set to true, random values are added to the activation via a noise generator.",
        order = 99,
        tab = "Noise"
    )
    var addNoise: Boolean
}