package org.simbrain.network.connections

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.util.SimbrainConstants.Polarity
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

    @Test
    fun `percentExcitatory should be respected with all BOTH polarity neurons`() {
        val sources = List(10) { Neuron() }
        val targets = List(10) { Neuron() }
        val oneToOne = OneToOne()
        
        oneToOne.percentExcitatory = 50.0
        val syns = oneToOne.connectNeurons(sources, targets)
        val excitatoryCount = syns.count { it.strength > 0 }
        assertEquals(5, excitatoryCount)
        
        oneToOne.percentExcitatory = 0.0
        val syns2 = oneToOne.connectNeurons(sources, targets)
        val excitatoryCount2 = syns2.count { it.strength > 0 }
        assertEquals(0, excitatoryCount2)
        
        oneToOne.percentExcitatory = 100.0
        val syns3 = oneToOne.connectNeurons(sources, targets)
        val excitatoryCount3 = syns3.count { it.strength > 0 }
        assertEquals(10, excitatoryCount3)
    }

    @Test
    fun `EXCITATORY neurons should always produce positive weights regardless of percentExcitatory`() {
        val sources = List(10) { Neuron().apply { polarity = Polarity.EXCITATORY } }
        val targets = List(10) { Neuron() }
        val oneToOne = OneToOne()
        
        oneToOne.percentExcitatory = 0.0
        val syns = oneToOne.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength > 0 }) {
            "All synapses from EXCITATORY neurons should be positive, but found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `INHIBITORY neurons should always produce negative weights regardless of percentExcitatory`() {
        val sources = List(10) { i -> Neuron().apply { 
            polarity = Polarity.INHIBITORY
            x = 0.0
            y = i * 10.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = 50.0
            y = i * 10.0
        }}
        val oneToOne = OneToOne()
        
        oneToOne.percentExcitatory = 100.0
        val syns = oneToOne.connectNeurons(sources, targets)
        assertTrue(syns.isNotEmpty() && syns.all { it.strength < 0 }) {
            "All synapses from INHIBITORY neurons should be negative, but found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `mixed polarity neurons should respect both polarized and BOTH neurons`() {
        val excitatoryNeurons = List(3) { Neuron().apply { polarity = Polarity.EXCITATORY } }
        val inhibitoryNeurons = List(3) { Neuron().apply { polarity = Polarity.INHIBITORY } }
        val bothNeurons = List(4) { Neuron().apply { polarity = Polarity.BOTH } }
        val sources = excitatoryNeurons + inhibitoryNeurons + bothNeurons
        val targets = List(10) { Neuron() }
        val oneToOne = OneToOne()
        
        oneToOne.percentExcitatory = 50.0
        val syns = oneToOne.connectNeurons(sources, targets)
        
        val excitatorySourceSyns = syns.filter { it.source in excitatoryNeurons }
        assertTrue(excitatorySourceSyns.all { it.strength > 0 })
        
        val inhibitorySourceSyns = syns.filter { it.source in inhibitoryNeurons }
        assertTrue(inhibitorySourceSyns.all { it.strength < 0 })
        
        val totalSyns = syns.size
        val expectedExcitatory = (totalSyns * 0.5).toInt()
        val actualExcitatory = syns.count { it.strength > 0 }
        assertEquals(expectedExcitatory, actualExcitatory)
    }

    @Test
    fun `with only polarized neurons percentExcitatory should be ignored`() {
        val excitatoryNeurons = List(5) { i -> Neuron().apply { 
            polarity = Polarity.EXCITATORY
            x = 0.0
            y = i * 10.0
        }}
        val inhibitoryNeurons = List(5) { i -> Neuron().apply { 
            polarity = Polarity.INHIBITORY
            x = 0.0
            y = (i + 5) * 10.0
        }}
        val sources = excitatoryNeurons + inhibitoryNeurons
        val targets = List(10) { i -> Neuron().apply {
            x = 50.0
            y = i * 10.0
        }}
        val oneToOne = OneToOne()
        
        oneToOne.percentExcitatory = 30.0
        val syns = oneToOne.connectNeurons(sources, targets)
        
        val excitatoryCount = syns.count { it.strength > 0 }
        assertEquals(5, excitatoryCount)
    }

    @Test
    fun `bidirectional connections should respect polarity in both directions`() {
        val excitatoryNeurons = List(3) { Neuron().apply { polarity = Polarity.EXCITATORY } }
        val inhibitoryNeurons = List(3) { Neuron().apply { polarity = Polarity.INHIBITORY } }
        val sources = excitatoryNeurons + inhibitoryNeurons
        val targets = List(6) { Neuron() }
        val oneToOne = OneToOne(useBidirectionalConnections = true)
        
        oneToOne.percentExcitatory = 50.0
        val syns = oneToOne.connectNeurons(sources, targets)
        
        val forwardExcitatory = syns.filter { it.source in excitatoryNeurons && it.target in targets }
        assertTrue(forwardExcitatory.all { it.strength > 0 })
        
        val forwardInhibitory = syns.filter { it.source in inhibitoryNeurons && it.target in targets }
        assertTrue(forwardInhibitory.all { it.strength < 0 })
    }

}