package org.simbrain.network.connections

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.addNeuronCollection
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt

class OneToOneTest {

    val network = Network()

    @Test
    fun `one to one connection should produce the same number of synapses as the size smallest neuron set`() {
        runBlocking {
            with(network) {
                val oneToOne = OneToOne()
                val source = addNeuronCollection(Random.nextInt(50..100)).neuronList
                val target = addNeuronCollection(Random.nextInt(50.. 100)).neuronList
                val syns = oneToOne.connectNeurons(source, target)
                val expectedSize = min(source.size, target.size)
                assert(syns.size == expectedSize) {
                    "Expected $expectedSize synapses, but got ${syns.size}"
                }
            }
        }
    }

    @Test
    fun `with bidirectional connections one to one should produce the same number of synapses as twice the size of the smallest neuron set`() {
        runBlocking {
            with(network) {
                val oneToOne = OneToOne(useBidirectionalConnections = true)
                val source = addNeuronCollection(Random.nextInt(50..100)).neuronList
                val target = addNeuronCollection(Random.nextInt(50.. 100)).neuronList
                val syns = oneToOne.connectNeurons(source, target)
                val expectedSize = min(source.size, target.size) * 2
                assert(syns.size == expectedSize) {
                    "Expected $expectedSize synapses, but got ${syns.size}"
                }
            }
        }
    }


}