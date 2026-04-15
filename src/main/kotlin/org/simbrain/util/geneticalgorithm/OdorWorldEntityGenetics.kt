package org.simbrain.util.geneticalgorithm

import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.Sensor
import kotlin.reflect.full.primaryConstructor

class SensorGene<S: Sensor>(override val template: S): Gene<OdorWorldEntity, S>() {
    override suspend fun express(context: OdorWorldEntity): S = template.copy().also { context.addSensor(it) } as S

    override fun copy(): Gene<OdorWorldEntity, S> = SensorGene(template::class.primaryConstructor!!.call(template.copy()))
}

class EffectorGene<E: Effector>(override val template: E): Gene<OdorWorldEntity, E>() {
    override suspend fun express(context: OdorWorldEntity): E = template.copy().also { context.addEffector(it) } as E

    override fun copy(): Gene<OdorWorldEntity, E> = EffectorGene(template::class.primaryConstructor!!.call(template.copy()))
}
