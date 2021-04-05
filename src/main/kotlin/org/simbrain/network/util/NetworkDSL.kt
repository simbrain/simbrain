package org.simbrain.network.util

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.workspace.Workspace

fun neuron(config: Neuron.() -> Unit = { }) = fun(network: Network) =
        Neuron(network).apply(config).also { network.addNetworkModel(it) }

fun workspace(config: WorkspaceBuilder.() -> Unit = { }) = WorkspaceBuilder().apply(config).workspace

fun network(config: NetworkBuilder.() -> Unit = { }) =
        NetworkBuilder().apply(config).network

class WorkspaceBuilder {
    val workspace = Workspace()

    fun network(config: NetworkBuilder.() -> Unit = { }) =
            NetworkBuilder().apply(config).network
                    .also { workspace.addWorkspaceComponent(NetworkComponent("net", it)) }

    operator fun Network.unaryPlus() = apply {
        workspace.addWorkspaceComponent(NetworkComponent("net", this))
    }

    fun Network.iterate(block: Network.() -> Unit) {
        block()
        workspace.simpleIterate()
    }
}

class NetworkBuilder {
    val network = Network()

    fun neuron(config: Neuron.() -> Unit = { }) = Neuron(network).apply(config).also { network.addNetworkModel(it) }

    fun synapse(source: Neuron, target: Neuron, config: Synapse.() -> Unit = { }) = Synapse(source, target)
        .apply(config)
        .also { network.addNetworkModel(it) }

    operator fun ((Network) -> Neuron).unaryPlus() = this(network).also { network.addNetworkModel(it) }

    operator fun Collection<(Network) -> Neuron>.unaryPlus() = map { +it }

}