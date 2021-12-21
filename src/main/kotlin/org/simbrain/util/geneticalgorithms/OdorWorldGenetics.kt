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

inline fun Chromosome<SmellSensor, SmellSensorGene>.smellSensorGene(options: SmellSensor.() -> Unit = { }): SmellSensorGene {
    return SmellSensorGene(this, SmellSensor().apply(options))
}

inline fun Chromosome<ObjectSensor, ObjectSensorGene>.objectSensorGene(options: ObjectSensor.() -> Unit = { }): ObjectSensorGene {
    return ObjectSensorGene(this, ObjectSensor().apply(options))
}

inline fun Chromosome<StraightMovement, StraightMovementGene>.straightMovementGene(options: StraightMovement.() -> Unit = { }): StraightMovementGene {
    return StraightMovementGene(this, StraightMovement().apply(options))
}

inline fun Chromosome<Turning, TurningGene>.turningGene(options: Turning.() -> Unit = { }): TurningGene {
    return TurningGene(this, Turning().apply(options))
}

inline fun entity(type: EntityType, crossinline template: OdorWorldEntity.() -> Unit = { }): (OdorWorld) -> OdorWorldEntity {
    return { world -> OdorWorldEntity(world, type).apply(template) }
}

abstract class OdorWorldEntityGene<P, G: OdorWorldEntityGene<P, G>>: Gene<P, G>() {
    abstract fun build(odorWorldEntity: OdorWorldEntity): P
}

class SmellSensorGene(override val chromosome: Chromosome<SmellSensor, SmellSensorGene>, private val template: SmellSensor):
        OdorWorldEntityGene<SmellSensor, SmellSensorGene>() {

    override val product = CompletableFuture<SmellSensor>()

    override fun copy(chromosome: Chromosome<SmellSensor, SmellSensorGene>): SmellSensorGene {
        return SmellSensorGene(chromosome, template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): SmellSensor {
        return SmellSensor(template).apply { parent = odorWorldEntity }.also { product.complete(it) }
    }

}

class ObjectSensorGene(override val chromosome: Chromosome<ObjectSensor, ObjectSensorGene>, private val template: ObjectSensor):
        OdorWorldEntityGene<ObjectSensor, ObjectSensorGene>() {

    override val product = CompletableFuture<ObjectSensor>()

    override fun copy(chromosome: Chromosome<ObjectSensor, ObjectSensorGene>): ObjectSensorGene {
        return ObjectSensorGene(chromosome, template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): ObjectSensor {
        return ObjectSensor(template).apply { parent = odorWorldEntity }.also { product.complete(it) }
    }

}

class StraightMovementGene(override val chromosome: Chromosome<StraightMovement, StraightMovementGene>, private val template: StraightMovement):
        OdorWorldEntityGene<StraightMovement, StraightMovementGene>() {

    override val product = CompletableFuture<StraightMovement>()

    override fun copy(chromosome: Chromosome<StraightMovement, StraightMovementGene>): StraightMovementGene {
        return StraightMovementGene(chromosome, template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): StraightMovement {
        return StraightMovement(template).apply { parent = odorWorldEntity }.also { product.complete(it) }
    }

}

class TurningGene(override val chromosome: Chromosome<Turning, TurningGene>, private val template: Turning):
        OdorWorldEntityGene<Turning, TurningGene>() {

    override val product = CompletableFuture<Turning>()

    override fun copy(chromosome: Chromosome<Turning, TurningGene>): TurningGene {
        return TurningGene(chromosome, template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): Turning {
        return Turning(template).apply { parent = odorWorldEntity }.also { product.complete(it) }
    }

}

class OdorWorldEntityGeneticsContext(val entity: OdorWorldEntity) {

    fun <P, G: OdorWorldEntityGene<P, G>> express(chromosome: Chromosome<P, G>): List<P> =
        chromosome.genes.map {
            it.build(entity).also { peripheralAttribute ->
                when (peripheralAttribute) {
                    is Sensor -> entity.addSensor(peripheralAttribute)
                    is Effector -> entity.addEffector(peripheralAttribute)
                }
            } 
        }

    operator fun <P, G: OdorWorldEntityGene<P, G>> Chromosome<P, G>.unaryPlus(): List<P> = express(this)

}

operator fun OdorWorldEntity.invoke(block: OdorWorldEntityGeneticsContext.() -> Unit) {
    OdorWorldEntityGeneticsContext(this).apply(block)
}