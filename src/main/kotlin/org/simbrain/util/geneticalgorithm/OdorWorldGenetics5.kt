package org.simbrain.util.geneticalgorithm

import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.SmellSensor

inline fun smellSensorGene(options: SmellSensor.() -> Unit = { }): SmellSensorGene5 {
    return SmellSensorGene5(SmellSensor().apply(options))
}

inline fun objectSensorGene(options: ObjectSensor.() -> Unit = { }): ObjectSensorGene5 {
    return ObjectSensorGene5(ObjectSensor().apply(options))
}

inline fun straightMovementGene(options: StraightMovement.() -> Unit = { }): StraightMovementGene5 {
    return StraightMovementGene5(StraightMovement().apply(options))
}

inline fun turningGene(options: Turning.() -> Unit = { }): TurningGene5 {
    return TurningGene5(Turning().apply(options))
}

inline fun entity(type: EntityType, crossinline template: OdorWorldEntity.() -> Unit = { }): (OdorWorld) -> OdorWorldEntity {
    return { world -> OdorWorldEntity(world, type).apply(template) }
}

class SmellSensorGene5(template: SmellSensor): Gene5<SmellSensor>(template) {

    override fun copy(): SmellSensorGene5 {
        return SmellSensorGene5(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): SmellSensor {
        return SmellSensor(template).apply { parent = odorWorldEntity }
    }

}

class ObjectSensorGene5(template: ObjectSensor): Gene5<ObjectSensor>(template) {

    override fun copy(): ObjectSensorGene5 {
        return ObjectSensorGene5(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): ObjectSensor {
        return ObjectSensor(template).apply { parent = odorWorldEntity }
    }

}

class StraightMovementGene5(template: StraightMovement): Gene5<StraightMovement>(template) {

    override fun copy(): StraightMovementGene5 {
        return StraightMovementGene5(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): StraightMovement {
        return StraightMovement(template).apply { parent = odorWorldEntity }
    }

}

class TurningGene5(template: Turning): Gene5<Turning>(template) {

    override fun copy(): TurningGene5 {
        return TurningGene5(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): Turning {
        return Turning(template).apply { parent = odorWorldEntity }
    }

}