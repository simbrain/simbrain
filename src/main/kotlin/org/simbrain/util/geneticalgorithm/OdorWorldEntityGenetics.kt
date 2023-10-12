package org.simbrain.util.geneticalgorithm

import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.entities.PeripheralAttribute
import org.simbrain.world.odorworld.sensors.Sensor
import kotlin.reflect.full.primaryConstructor

abstract class OdorWorldEntityGene<P : PeripheralAttribute> : Gene<P>() {
    abstract suspend fun express(entity: OdorWorldEntity): P
}

class SensorGene<S: Sensor>(override val template: S): OdorWorldEntityGene<S>() {
    override suspend fun express(entity: OdorWorldEntity): S = template.copy().also { entity.addSensor(it) } as S

    override fun copy(): OdorWorldEntityGene<S> = SensorGene(template::class.primaryConstructor!!.call(template.copy()))
}

class EffectorGene<E: Effector>(override val template: E): OdorWorldEntityGene<E>() {
    override suspend fun express(entity: OdorWorldEntity): E = template.copy().also { entity.addEffector(it) } as E

    override fun copy(): OdorWorldEntityGene<E> = EffectorGene(template::class.primaryConstructor!!.call(template.copy()))
}
