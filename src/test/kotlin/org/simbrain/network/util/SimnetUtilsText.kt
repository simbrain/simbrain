package org.simbrain.network.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.connect
import org.simbrain.network.util.SimnetUtils.getWeights

class SimnetUtilsText {

    val net = Network()

    @Test
    fun `test single source and target with weight`() {
        val src = Neuron()
        val tar = Neuron()
        val syn = Synapse(src, tar).apply {
            strength = 2.0
        }
        val weights = getWeights(listOf(src), listOf(tar))
        // Should be a 1x1 matrix with one entry
        assertEquals(1, weights.size)
        assertEquals(2.0, weights[0][0])
    }

    @Test
    fun `test multiple sources and targets`() {
        val src = Neuron()
        val tar = Neuron()

        Synapse(src, tar).apply { strength = 0.1 }
        Synapse(tar, src).apply { strength = 0.2 }

        val weights = getWeights(listOf(src, tar), listOf(src, tar))

        assertEquals(0.0, weights[0][0])
        assertEquals(0.1, weights[0][1])
        assertEquals(0.2, weights[1][0])
        assertEquals(0.0, weights[1][1])
    }

    @Test
    fun `test missing connections correspond to 0 entries`() {
        val s1 = Neuron()
        val s2 = Neuron()
        val t1 = Neuron()
        val t2 = Neuron()

        Synapse(s1, t1).apply { strength = 0.1 }
        Synapse(s1, t2).apply { strength = 0.2 }
        Synapse(s2, t1).apply { strength = 0.3 }
        Synapse(s2, t2).apply { strength = 0.4 }

        val weights = getWeights(listOf(s1, s2), listOf(t1, t2))

        assertEquals(0.1, weights[0][0])
        assertEquals(0.2, weights[0][1])
        assertEquals(0.3, weights[1][0])
        assertEquals(0.4, weights[1][1])
    }


}