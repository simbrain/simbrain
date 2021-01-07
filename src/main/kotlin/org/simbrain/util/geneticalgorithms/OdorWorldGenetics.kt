package org.simbrain.util.geneticalgorithms

import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.Sensor
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.util.concurrent.CompletableFuture

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

interface OdorWorldEntityGene<T> {
    fun build(odorWorldEntity: OdorWorldEntity): T
}

class SmellSensorGene(private val template: SmellSensor):
        Gene<SmellSensor>(),
        OdorWorldEntityGene<SmellSensor> {

    override val promise = CompletableFuture<SmellSensor>()

    override fun copy(): SmellSensorGene {
        return SmellSensorGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): SmellSensor {
        return SmellSensor(template).apply { parent = odorWorldEntity }.also { promise.complete(it) }
    }

}

class ObjectSensorGene(private val template: ObjectSensor):
        Gene<ObjectSensor>(),
        OdorWorldEntityGene<ObjectSensor> {

    override val promise = CompletableFuture<ObjectSensor>()

    override fun copy(): ObjectSensorGene {
        return ObjectSensorGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): ObjectSensor {
        return ObjectSensor(template).apply { parent = odorWorldEntity }.also { promise.complete(it) }
    }

}

class StraightMovementGene(private val template: StraightMovement):
        Gene<StraightMovement>(),
        OdorWorldEntityGene<StraightMovement> {

    override val promise = CompletableFuture<StraightMovement>()

    override fun copy(): StraightMovementGene {
        return StraightMovementGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): StraightMovement {
        return StraightMovement(template).apply { parent = odorWorldEntity }.also { promise.complete(it) }
    }

}

class TurningGene(private val template: Turning):
        Gene<Turning>(),
        OdorWorldEntityGene<Turning>{

    override val promise = CompletableFuture<Turning>()

    override fun copy(): TurningGene {
        return TurningGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): Turning {
        return Turning(template).apply { parent = odorWorldEntity }.also { promise.complete(it) }
    }

}

class OdorWorldEntityGeneticsContext(val entity: OdorWorldEntity) {

    fun <T, G> express(chromosome: Chromosome<T, G>): List<T> where G: OdorWorldEntityGene<T>, G: Gene<T> =
        chromosome.genes.map {
            it.build(entity).also { peripheralAttribute ->
                when (peripheralAttribute) {
                    is Sensor -> entity.addSensor(peripheralAttribute)
                    is Effector -> entity.addEffector(peripheralAttribute)
                }
            } 
        }

    operator fun <T, G> Chromosome<T, G>.unaryPlus(): List<T> where G: OdorWorldEntityGene<T>, G: Gene<T> =
        express(this)

}

operator fun OdorWorldEntity.invoke(block: OdorWorldEntityGeneticsContext.() -> Unit) {
    OdorWorldEntityGeneticsContext(this).apply(block)
}