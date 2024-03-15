package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.neurongroups.SOMGroup

class SOMGroupTest {

    var net = Network()
    val SOM = SOMGroup(10)

    init {
        net.addNetworkModelsAsync(SOM)
    }

    @Test
    fun `Test copy function`() {
        val SOM2 = SOM.copy()
        net.addNetworkModelsAsync(SOM2)
        assertEquals(10, SOM2.neuronList.size)
        assertEquals(100.0, SOM2.neighborhoodSize)
        assertEquals(0.06, SOM2.learningRate)
        assertEquals(0.0, SOM2.distance)
        assertEquals(0.0, SOM2.value)
        assertEquals(null, SOM2.winner)
    }

    @Test
    fun `Test reset function`() {
        repeat(5) {
            net.update()
        }
        SOM.reset()
        assertEquals(SOM.params.initNeighborhoodSize, SOM.neighborhoodSize)
        assertEquals(SOM.params.initialLearningRate, SOM.learningRate)
    }

}