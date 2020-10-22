package org.simbrain.custom_sims.builders

import org.simbrain.custom_sims.builders.network.NetworkBuilder
import org.simbrain.custom_sims.builders.network.NeuronBuilder
import org.simbrain.custom_sims.builders.network.SynapseBuilderStub
import org.simbrain.custom_sims.builders.odorworld.ObjectSensorBuilder
import org.simbrain.custom_sims.builders.odorworld.OdorWorldBuilder
import org.simbrain.custom_sims.builders.odorworld.OdorWorldEntityBuilder
import org.simbrain.custom_sims.helper_classes.Simulation
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import kotlin.random.Random

/**
 * Initial work towards a Kotlin version of [Simulation].
 */
open class KSimulation {

    fun neuron(template: Neuron.() -> Unit = { }) = NeuronBuilder { apply(template) }

    fun network(init: NetworkBuilder.() -> Unit) = NetworkBuilder().apply(init)

    fun entity(template: OdorWorldEntity.() -> Unit = { }) = OdorWorldEntityBuilder {
        apply(template)
    }

    fun objectSensor(template: ObjectSensor.() -> Unit = { }) = ObjectSensorBuilder {

    }

//    fun agent(template: AgentBuilder.() -> Unit = { }) = AgentBuilder().apply(template)


    fun odorWorld(template: OdorWorldBuilder.() -> Unit) = OdorWorldBuilder().apply(template)

    fun task(init: Task.() -> Unit) = Task(init).also { tasks.add(it) }

    fun coupling(init: CouplingContext.() -> Unit) {

    }

    val tasks = ArrayList<Task>()

    inner class CouplingContext {

//        infix fun NeuronBuilder.to(consumer: NeuronBuilder) {
//            val producer = this
//            afterInitialize {
//                workspace.couplingManager.apply {
//                    producer.neuron.visibleProducers.first() couple consumer.neuron.visibleConsumers.first()
//                }
//            }
//        }


    }

    fun run() {
        tasks.forEach { it.apply { runtime() } }
    }

    fun afterInitialize(context: (IterationContext) -> Unit) {

    }

    fun beforeIteration(context: (IterationContext) -> Unit) {

    }

    fun afterIteration(context: (IterationContext) -> Unit) {

    }

    class IterationContext

}

fun sim(setup: KSimulation.() -> Unit) = KSimulation().apply(setup)

interface SimComponentBuilder<T> {

}

//class AgentBuilder {
//    val network by lazy { NetworkBuilder() }
//    operator fun NeuronBuilder.unaryPlus(): NeuronBuilder {
//        this@AgentBuilder.network.let { net ->
//
//        }
//    }
//}


class Task(val runtime: Task.() -> Unit) {

    val workspace = Workspace()

    val neurons = HashMap<NeuronBuilder, Neuron>()
    val networks = HashMap<NetworkBuilder, NetworkComponent>()
    val synapses = HashMap<SynapseBuilderStub, Synapse>()

    val networkInitialized = HashSet<NetworkBuilder>()

    fun NetworkBuilder.iterate(repeat: Int = 1, callback: NetworkComponent.() -> Unit) {
        repeat(repeat) {
            getNetwork(this).callback()
            workspace.simpleIterate()
        }
    }

    val NeuronBuilder.activation
        get() = getNeuron(this).activation

    // Provides accesss to Neuron object from NeuronBuilder object
    val NeuronBuilder.self get() = getNeuron(this)
    val NetworkBuilder.self get() = getNetwork(this)

    fun getNeuron(neuronBuilder: NeuronBuilder) = neurons.getOrPut(neuronBuilder) {
        neuronBuilder.buildProduct { Neuron(getNetwork(it).network) }
    }

    fun getSynapse(synapseBuilder: SynapseBuilderStub) = synapses.getOrPut(synapseBuilder) {
        synapseBuilder.buildProduct { Synapse(getNeuron(synapseBuilder.source), getNeuron(synapseBuilder.target)) }
    }

    fun getNetwork(networkBuilder: NetworkBuilder): NetworkComponent = networks.getOrPut(networkBuilder) {
        NetworkComponent("Network ${Random.nextInt()}", Network()).also { workspace.addWorkspaceComponent(it) }
    }

    fun setupNetwork(networkBuilder: NetworkBuilder) {
        if (networkBuilder !in networkInitialized) {
            val network = getNetwork(networkBuilder).network
            networkBuilder.neurons.forEach { network.addLooseNeuron(getNeuron(it)) }
            networkBuilder.synapses.forEach { network.addLooseSynapse(getSynapse(it)) }
            networkInitialized.add(networkBuilder)
        }
    }

}

open class Tester