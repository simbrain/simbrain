package org.simbrain.util.geneticalgorithm2

import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.entities.PeripheralAttribute
import org.simbrain.world.odorworld.sensors.Sensor
import kotlin.reflect.full.primaryConstructor

abstract class OdorWorldEntityGene2<P : PeripheralAttribute> : Gene2<P>() {
    abstract suspend fun express(entity: OdorWorldEntity): P
}

class SensorGene2<S: Sensor>(override val template: S): OdorWorldEntityGene2<S>() {
    override suspend fun express(entity: OdorWorldEntity): S = template.copy().also { entity.addSensor(it) } as S

    override fun copy(): OdorWorldEntityGene2<S> = SensorGene2(template::class.primaryConstructor!!.call(template.copy()))
}

class EffectorGene2<E: Effector>(override val template: E): OdorWorldEntityGene2<E>() {
    override suspend fun express(entity: OdorWorldEntity): E = template.copy().also { entity.addEffector(it) } as E

    override fun copy(): OdorWorldEntityGene2<E> = EffectorGene2(template::class.primaryConstructor!!.call(template.copy()))
}
