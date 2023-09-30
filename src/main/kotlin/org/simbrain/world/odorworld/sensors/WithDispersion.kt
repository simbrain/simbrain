package org.simbrain.world.odorworld.sensors

import org.simbrain.util.UserParameter
import org.simbrain.util.decayfunctions.DecayFunction

interface WithDispersion {
    @get:UserParameter(
        label = "Show dispersion",
        description = "Show dispersion of the sensor",
        order = 4
    )
    var showDispersion: Boolean

    val decayFunction: DecayFunction
}