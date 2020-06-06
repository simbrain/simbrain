package org.simbrain.custom_sims.builders.odorworld

import org.simbrain.world.odorworld.OdorWorld

class OdorWorldBuilder {

    val world = OdorWorld()

    operator fun OdorWorldEntityBuilder.unaryPlus() {
        world = this@OdorWorldBuilder.world

    }

}