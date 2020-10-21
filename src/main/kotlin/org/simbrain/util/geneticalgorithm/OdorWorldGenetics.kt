package org.simbrain.util.geneticalgorithm

import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.SmellSensor

inline fun smellSensorGene(options: SmellSensor.() -> Unit = { }): SmellSensorGene {
    return SmellSensorGene(SmellSensor().apply(options))
}

inline fun objectSensorGene(options: ObjectSensor.() -> Unit = { }): ObjectSensorGene {
    return ObjectSensorGene(ObjectSensor().apply(options))
}

inline fun straightMovementGene(options: StraightMovement.() -> Unit = { }): StraightMovementGene {
    return StraightMovementGene(StraightMovement().apply(options))
}

inline fun turningGene(options: Turning.() -> Unit = { }): TurningGene {
    return TurningGene(Turning().apply(options))
}

inline fun entity(type: EntityType, crossinline template: OdorWorldEntity.() -> Unit = { }): (OdorWorld) -> OdorWorldEntity {
    return { world -> OdorWorldEntity(world, type).apply(template) }
}

class SmellSensorGene(template: SmellSensor): Gene<SmellSensor>(template) {

    override fun copy(): SmellSensorGene {
        return SmellSensorGene(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): SmellSensor {
        return SmellSensor(template).apply { parent = odorWorldEntity }
    }

}

class ObjectSensorGene(template: ObjectSensor): Gene<ObjectSensor>(template) {

    override fun copy(): ObjectSensorGene {
        return ObjectSensorGene(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): ObjectSensor {
        return ObjectSensor(template).apply { parent = odorWorldEntity }
    }

}

class StraightMovementGene(template: StraightMovement): Gene<StraightMovement>(template) {

    override fun copy(): StraightMovementGene {
        return StraightMovementGene(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): StraightMovement {
        return StraightMovement(template).apply { parent = odorWorldEntity }
    }

}

class TurningGene(template: Turning): Gene<Turning>(template) {

    override fun copy(): TurningGene {
        return TurningGene(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): Turning {
        return Turning(template).apply { parent = odorWorldEntity }
    }

}