package org.simbrain.util.geneticalgorithms

import org.simbrain.workspace.Consumer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.couplings.CouplingManager
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.entities.PeripheralAttribute
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

interface OdorWorldEntityBuildable<T> {
    fun build(odorWorldEntity: OdorWorldEntity): T
}

class SmellSensorGene(template: SmellSensor):
        Gene<SmellSensor>(template),
        OdorWorldEntityBuildable<SmellSensor>,
        ProducerGene<SmellSensor> {

    override fun copy(): SmellSensorGene {
        return SmellSensorGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): SmellSensor {
        return SmellSensor(template).apply { parent = odorWorldEntity }
    }

    override fun CouplingManager.defaultProducer(container: SmellSensor): Producer? {
        TODO("Not yet implemented")
    }

}

class ObjectSensorGene(template: ObjectSensor):
        Gene<ObjectSensor>(template),
        OdorWorldEntityBuildable<ObjectSensor>,
        ProducerGene<ObjectSensor> {

    override fun copy(): ObjectSensorGene {
        return ObjectSensorGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): ObjectSensor {
        return ObjectSensor(template).apply { parent = odorWorldEntity }
    }

    override fun CouplingManager.defaultProducer(container: ObjectSensor): Producer? {
        return container.getProducerByMethodName("getCurrentValue")
    }

}

class StraightMovementGene(template: StraightMovement):
        Gene<StraightMovement>(template),
        OdorWorldEntityBuildable<StraightMovement>,
        ConsumerGene<StraightMovement> {

    override fun copy(): StraightMovementGene {
        return StraightMovementGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): StraightMovement {
        return StraightMovement(template).apply { parent = odorWorldEntity }
    }

    override fun CouplingManager.defaultConsumer(container: StraightMovement): Consumer? {
        return container.getConsumerByMethodName("setAmount")
    }

}

class TurningGene(template: Turning):
        Gene<Turning>(template),
        OdorWorldEntityBuildable<Turning>,
        ConsumerGene<Turning> {

    override fun copy(): TurningGene {
        return TurningGene(template.copy())
    }

    override fun build(odorWorldEntity: OdorWorldEntity): Turning {
        return Turning(template).apply { parent = odorWorldEntity }
    }

    override fun CouplingManager.defaultConsumer(container: Turning): Consumer? {
        return container.getConsumerByMethodName("setAmount")
    }

}

class OdorWorldBuilderProvider :
        BuilderProvider<OdorWorld, OdorWorldGeneticBuilder, OdorWorldBuilderContext>,
        WorkspaceBuilderContextInvokable<OdorWorldBuilderContext, OdorWorld> {

    lateinit var product: OdorWorld

    override fun createWorkspaceComponent(name: String) = OdorWorldComponent(name, product)

    fun createBuilder(productMap: ProductMap): OdorWorldGeneticBuilder {
        return OdorWorldGeneticBuilder(productMap)
    }

    fun createContext(builder: OdorWorldGeneticBuilder): OdorWorldBuilderContext {
        return OdorWorldBuilderContext(builder)
    }

    override fun createProduct(productMap: ProductMap, template: OdorWorldBuilderContext.() -> Unit): OdorWorld {
        return createBuilder(productMap).also { createContext(it).apply(template) }.build().also { product = it }
    }

}


class OdorWorldGeneticBuilder(override val productMap: ProductMap) : GeneticBuilder<OdorWorld> {

    private val tasks = ArrayList<(OdorWorld) -> Unit>()

    fun addTask(task: (OdorWorld) -> Unit) {
        tasks.add(task)
    }

    fun build() = OdorWorld().also { world -> tasks.forEach { it(world) } }

}

class OdorWorldBuilderContext(val builder: OdorWorldGeneticBuilder): BuilderContext {

    operator fun OdorWorldEntityBuilderProvider.invoke(template: OdorWorldEntityBuilderContext.() -> Unit) {
        with(builder) {
            addTask { world ->
                world.addEntity(createProduct(world, productMap, template).apply(entityTemplate).also {
                    builder.productMap[this@invoke] = it
                })
            }
        }
    }

    operator fun OdorWorldEntityBuilderProvider.unaryPlus() {
        this { }
    }

}

fun useOdorWorld() = OdorWorldBuilderProvider()

class OdorWorldEntityBuilderProvider(val type: EntityType, val entityTemplate: OdorWorldEntity.() -> Unit)
    : BuilderProvider<OdorWorldEntity, OdorWorldEntityGeneticBuilder, OdorWorldEntityBuilderContext> {

    fun createBuilder(productMap: ProductMap): OdorWorldEntityGeneticBuilder {
        return OdorWorldEntityGeneticBuilder(productMap, type)
    }

    fun createContext(builder: OdorWorldEntityGeneticBuilder): OdorWorldEntityBuilderContext {
        return OdorWorldEntityBuilderContext(builder)
    }

    fun createProduct(world: OdorWorld, productMap: ProductMap, template: OdorWorldEntityBuilderContext.() -> Unit):
            OdorWorldEntity {
        return createBuilder(productMap).also { createContext(it).apply(template) }.build(world)
    }

}

class OdorWorldEntityGeneticBuilder(override val productMap: ProductMap, val type: EntityType) :
        GeneticBuilder<OdorWorldEntity> {

    private val tasks = ArrayList<(OdorWorldEntity) -> Unit>()

    fun addTask(task: (OdorWorldEntity) -> Unit) {
        tasks.add(task)
    }

    fun build(world: OdorWorld): OdorWorldEntity {
        return OdorWorldEntity(world, type).also { tasks.forEach { task -> task(it) } }
    }

}

class OdorWorldEntityBuilderContext(val builder: OdorWorldEntityGeneticBuilder): BuilderContext {

    operator fun <T, G> Chromosome<T, G>.unaryPlus() where G: Gene<T>, G: OdorWorldEntityBuildable<T>, T: PeripheralAttribute {
        with(builder) {
            addTask { entity ->
                genes.forEach { gene ->
                    when (val product = gene.build(entity).also { productMap[gene] = it }) {
                        is Sensor -> entity.addSensor(product)
                        is Effector -> entity.addEffector(product)
                    }
                }
            }
        }
    }

}

fun useEntity(entityType: EntityType, template: OdorWorldEntity.() -> Unit): OdorWorldEntityBuilderProvider {
    return OdorWorldEntityBuilderProvider(entityType, template)
}