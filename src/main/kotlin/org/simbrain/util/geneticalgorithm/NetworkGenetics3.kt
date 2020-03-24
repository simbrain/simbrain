package org.simbrain.util.geneticalgorithm

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.network
import org.simbrain.network.util.neuron
import org.simbrain.network.util.workspace

class XorNetwork(
        inputs: List<(Network) -> Neuron> = listOf(neuron(), neuron()),
        hidden: List<(Network) -> Neuron>,
        outputs: List<(Network) -> Neuron> = listOf(neuron())
) {
    val allow = listOf(
            inputs to hidden,
            inputs to outputs,
            hidden to outputs
    )

    val network: Network = network()

    val inputs = inputs.map { it(network) }
    val hidden = hidden.map { it(network) }
    val outputs = outputs.map { it(network) }

    private fun Neuron.simpleToString() = "$id[$activation]"

    override fun toString(): String =
            """
                ${inputs.joinToString(", ") { it.simpleToString() }}
                ${hidden.joinToString(", ") { it.simpleToString() }}
                ${outputs.joinToString(", ") { it.simpleToString() }}
            """.trimIndent()
}


fun main() {
    workspace {
        val xor = XorNetwork(hidden = listOf(neuron()))
        val network = +xor.network
        workspace.simpleIterate()
        println(xor)
    }
}