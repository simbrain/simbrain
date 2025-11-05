package org.simbrain.network.connections

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuron
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.decayfunctions.LinearDecayFunction

class DistancedBasedTest {

    val network = Network()

    @Test
    fun `check that connectNeurons produces the appropriate number of synapses`() {
        runBlocking {
            with(network) {
                val distanceBased = DistanceBased(decayFunction = LinearDecayFunction(80.0), seed = 42L)
                val n1 = addNeuron(0, 0)
                val n2 = addNeuron(0, 40)
                val count = (0..1000).sumOf {
                    distanceBased.connectNeurons(listOf(n1), listOf(n2)).size
                }
                assert(count in 450..550)
            }
        }
    }

    @Test
    fun `strategy created with the same seed should produce the same same pattern`() {
        assertStrategiesPatterns(
            network,
            DistanceBased(seed = 42L),
            DistanceBased(seed = 42L),
            25
        )
    }


    @Test
    fun `strategy created with different seeds should produce different patterns`() {
        assertStrategiesPatterns(
            network,
            DistanceBased(seed = 42L),
            DistanceBased(seed = 43L),
            expectIdentical = false
        )
    }

    @Test
    fun `calling connectNeurons on the same strategy object should produce different patterns each time`() {
        val distanceBased = DistanceBased(seed = 42L)
        assertStrategiesPatterns(
            network,
            distanceBased,
            distanceBased,
            expectIdentical = false
        )
    }

    @Test
    fun `percentExcitatory should be respected with all BOTH polarity neurons`() {
        val sources = List(10) { Neuron() }
        val targets = List(10) { Neuron() }
        val distanceBased = DistanceBased(decayFunction = LinearDecayFunction(1000.0), seed = 42L)
        
        distanceBased.percentExcitatory = 50.0
        val syns = distanceBased.connectNeurons(sources, targets)
        val excitatoryCount = syns.count { it.strength > 0 }
        val expectedExcitatory = (syns.size * 0.5).toInt()
        assertEquals(expectedExcitatory, excitatoryCount)
        
        distanceBased.percentExcitatory = 0.0
        val syns2 = distanceBased.connectNeurons(sources, targets)
        val excitatoryCount2 = syns2.count { it.strength > 0 }
        assertEquals(0, excitatoryCount2)
        
        distanceBased.percentExcitatory = 100.0
        val syns3 = distanceBased.connectNeurons(sources, targets)
        assertTrue(syns3.all { it.strength > 0 })
    }

    @Test
    fun `EXCITATORY neurons should always produce positive weights regardless of percentExcitatory`() {
        val sources = List(10) { Neuron().apply { polarity = Polarity.EXCITATORY } }
        val targets = List(10) { Neuron() }
        val distanceBased = DistanceBased(decayFunction = LinearDecayFunction(1000.0), seed = 42L)
        
        distanceBased.percentExcitatory = 0.0
        val syns = distanceBased.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength > 0 }) {
            "All synapses from EXCITATORY neurons should be positive, but found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `INHIBITORY neurons should always produce negative weights regardless of percentExcitatory`() {
        val sources = List(10) { i -> Neuron().apply { 
            polarity = Polarity.INHIBITORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val distanceBased = DistanceBased(decayFunction = LinearDecayFunction(1000.0), seed = 42L)
        
        distanceBased.percentExcitatory = 100.0
        val syns = distanceBased.connectNeurons(sources, targets)
        assertTrue(syns.isNotEmpty() && syns.all { it.strength < 0 }) {
            "All synapses from INHIBITORY neurons should be negative, but found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `mixed polarity neurons should respect both pre-polarized and BOTH neurons`() {
        val excitatoryNeurons = List(3) { Neuron().apply { polarity = Polarity.EXCITATORY } }
        val inhibitoryNeurons = List(3) { Neuron().apply { polarity = Polarity.INHIBITORY } }
        val bothNeurons = List(4) { Neuron().apply { polarity = Polarity.BOTH } }
        val sources = excitatoryNeurons + inhibitoryNeurons + bothNeurons
        val targets = List(10) { Neuron() }
        val distanceBased = DistanceBased(decayFunction = LinearDecayFunction(1000.0), seed = 42L)
        
        distanceBased.percentExcitatory = 50.0
        val syns = distanceBased.connectNeurons(sources, targets)
        
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
            x = i * 10.0
            y = 0.0
        }}
        val inhibitoryNeurons = List(5) { i -> Neuron().apply { 
            polarity = Polarity.INHIBITORY
            x = i * 10.0 + 5.0
            y = 0.0
        }}
        val sources = excitatoryNeurons + inhibitoryNeurons
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val distanceBased = DistanceBased(decayFunction = LinearDecayFunction(1000.0), seed = 42L)
        
        distanceBased.percentExcitatory = 30.0
        val syns = distanceBased.connectNeurons(sources, targets)
        
        val excitatoryCount = syns.count { it.strength > 0 }
        val inhibitoryCount = syns.count { it.strength < 0 }
        assertTrue(excitatoryCount > 0 && inhibitoryCount > 0)
    }

}