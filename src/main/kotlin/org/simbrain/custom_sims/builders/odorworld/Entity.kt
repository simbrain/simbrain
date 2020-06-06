package org.simbrain.custom_sims.builders.odorworld

import org.simbrain.custom_sims.builders.AssignOnce
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.entities.OdorWorldEntity

class OdorWorldEntityBuilder(template: OdorWorldEntity.() -> Unit) {

    var world by AssignOnce<OdorWorld>()

    val entity by lazy { OdorWorldEntity(world).apply(template) }

}