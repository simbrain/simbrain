package org.simbrain.network.llm

import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.nio.file.Path

/**
 * The LFM2 gate shared by every weights-dependent test: skips (a JUnit assumption) when the
 * condition fails, or fails loudly when -Dsimbrain.requireLfm2Weights=true declares that this
 * machine must be able to run the LFM2 suite.
 */
fun assumeOrRequireLfm2(condition: Boolean, message: String) {
    if (!condition && System.getProperty("simbrain.requireLfm2Weights") == "true") {
        fail<Nothing>("$message (required by simbrain.requireLfm2Weights)")
    }
    assumeTrue(condition, message)
}

/** Gates on the local LFM2 weights and returns their directory. */
fun assumeOrRequireWeights(): Path {
    val dir = Lfm2Weights.findWeightsDirectory()
    assumeOrRequireLfm2(dir != null, "LFM2 weights not found in the Simbrain or HF cache")
    return dir!!
}
