package org.simbrain.custom_sims.builders

import org.simbrain.custom_sims.builders.network.NetworkBuilder
import org.simbrain.custom_sims.builders.network.NeuronBuilder
import org.simbrain.custom_sims.builders.network.SynapseBuilderStub
import org.simbrain.custom_sims.builders.odorworld.ObjectSensorBuilder
import org.simbrain.custom_sims.builders.odorworld.OdorWorldBuilder
import org.simbrain.custom_sims.builders.odorworld.OdorWorldEntityBuilder
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.workspace.CouplingManager
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import kotlin.random.Random

open class KSimulation {

    val workspace = Workspace()

    fun neuron(template: Neuron.() -> Unit = { }) = NeuronBuilder { apply(template) }

    fun network(init: NetworkBuilder.() -> Unit) = NetworkBuilder().apply(init)

    fun entity(template: OdorWorldEntity.() -> Unit = { }) = OdorWorldEntityBuilder {
        apply(template)
    }

    fun objectSensor(template: ObjectSensor.() -> Unit = { }) = ObjectSensorBuilder {

    }

//    fun agent(template: AgentBuilder.() -> Unit = { }) = AgentBuilder().apply(template)


    fun odorWorld(template: OdorWorldBuilder.() -> Unit) = OdorWorldBuilder().apply(template)

    fun sim(init: SimContext.() -> Unit) {
        SimContext(init).also { sims.add(it) }
    }

    fun coupling(init: CouplingContext.() -> Unit) {

    }

    fun couplingManager(init: CouplingManager.() -> Unit) {
        workspace.couplingManager.apply(init)
    }

    val sims = ArrayList<SimContext>()

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

    fun iterate() {
        sims.forEach { it.apply { runtime() } }
    }

    fun afterInitialize(context: (IterationContext) -> Unit) {

    }

    fun beforeIteration(context: (IterationContext) -> Unit) {

    }

    fun afterIteration(context: (IterationContext) -> Unit) {

    }

    class IterationContext

}

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


class SimContext(val runtime: SimContext.() -> Unit) {

    val neurons = HashMap<NeuronBuilder, Neuron>()
    val networks = HashMap<NetworkBuilder, NetworkComponent>()
    val synapses = HashMap<SynapseBuilderStub, Synapse>()

    val networkInitialized = HashSet<NetworkBuilder>()

    fun NetworkBuilder.iterate(repeat: Int = 1, callback: NetworkComponent.() -> Unit) {
        repeat(repeat) {
            getNetwork(this).callback()
            getNetwork(this).update()
        }
    }

    val NeuronBuilder.activation
        get() = getNeuron(this).activation

    val NeuronBuilder.self get() = getNeuron(this)

    fun getNeuron(neuronBuilder: NeuronBuilder) = neurons.getOrPut(neuronBuilder) {
        neuronBuilder.buildProduct { Neuron(getNetwork(it).network) }
    }

    fun getSynapse(synapseBuilder: SynapseBuilderStub) = synapses.getOrPut(synapseBuilder) {
        synapseBuilder.buildProduct { Synapse(getNeuron(synapseBuilder.source), getNeuron(synapseBuilder.target)) }
    }

    fun getNetwork(networkBuilder: NetworkBuilder): NetworkComponent = networks.getOrPut(networkBuilder) {
        NetworkComponent("Network ${Random.nextInt()}", Network())
    }

    fun setupNetwork(networkBuilder: NetworkBuilder) {
        if (networkBuilder !in networkInitialized) {
            networkBuilder.neurons.forEach { getNeuron(it) }
            networkBuilder.synapses.forEach { getSynapse(it) }
            networkInitialized.add(networkBuilder)
        }
    }

}

open class Tester