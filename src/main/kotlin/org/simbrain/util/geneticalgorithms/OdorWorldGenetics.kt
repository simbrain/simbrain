package org.simbrain.util.geneticalgorithms

import kotlinx.coroutines.CompletableDeferred
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.Sensor
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

abstract class OdorWorldEntityGene<P>: Gene<P>() {
    abstract fun build(odorWorldEntity: OdorWorldEntity): P
}

class SmellSensorGene(private val template: SmellSensor):
        OdorWorldEntityGene<SmellSensor>() {

    override val product = CompletableDeferred<SmellSensor>()

    override fun copy(): SmellSensorGene {
        return SmellSensorGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): SmellSensor {
        return template.copy().also { product.complete(it) }
    }

}

class ObjectSensorGene(private val template: ObjectSensor):
        OdorWorldEntityGene<ObjectSensor>() {

    override val product = CompletableDeferred<ObjectSensor>()

    override fun copy(): ObjectSensorGene {
        return ObjectSensorGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): ObjectSensor {
        return template.copy().also { product.complete(it) }
    }

}

class StraightMovementGene(private val template: StraightMovement):
        OdorWorldEntityGene<StraightMovement>() {

    override val product = CompletableDeferred<StraightMovement>()

    override fun copy(): StraightMovementGene {
        return StraightMovementGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): StraightMovement {
        return StraightMovement(template).also { product.complete(it) }
    }

}

class TurningGene(private val template: Turning):
        OdorWorldEntityGene<Turning>() {

    override val product = CompletableDeferred<Turning>()

    override fun copy(): TurningGene {
        return TurningGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): Turning {
        return Turning(template).also { product.complete(it) }
    }

}

class OdorWorldEntityGeneticsContext(val entity: OdorWorldEntity) {

    fun <P, G: OdorWorldEntityGene<P>> express(chromosome: Chromosome<P, G>): List<P> =
        chromosome.map {
            it.build(entity).also { peripheralAttribute ->
                when (peripheralAttribute) {
                    is Sensor -> entity.addSensor(peripheralAttribute)
                    is Effector -> entity.addEffector(peripheralAttribute)
                }
            } 
        }

    operator fun <P, G: OdorWorldEntityGene<P>> Chromosome<P, G>.unaryPlus(): List<P> = express(this)

}

operator fun OdorWorldEntity.invoke(block: OdorWorldEntityGeneticsContext.() -> Unit) {
    OdorWorldEntityGeneticsContext(this).apply(block)
}