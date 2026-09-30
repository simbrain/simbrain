package org.simbrain.plot

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.plot.histogram.HistogramComponent
import org.simbrain.plot.histogram.HistogramModel
import org.simbrain.util.UiWork
import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.Workspace


class HistogramTest {

    val workspace = Workspace()
    val net = Network()
    val nwc = NetworkComponent("Net", net)
    val ng: NeuronCollection
    val histogram = HistogramModel()
    val hgc = HistogramComponent("Histogram", histogram)

    init {
        val ngNeurons = List(2) { Neuron() }
        ngNeurons.forEach { net.addNetworkModelAsync(it) }
        ng = NeuronCollection(ngNeurons).apply { isClamped = true }
        net.addNetworkModelAsync(ng)
        workspace.addWorkspaceComponent(hgc)
        workspace.addWorkspaceComponent(nwc)
        workspace.couplingManager.createCoupling(ng, histogram)
    }

    /** Iterates, then waits for the histogram's queued data, which lands on the EDT after the iteration returns. */
    private fun iterate() {
        workspace.simpleIterate()
        UiWork.awaitIdle()
    }

    @Test
    fun `test data is transferred properly`() {
        ng.activationArray = doubleArrayOf(1.0, 2.0)
        iterate()
        assertEquals(2, histogram.data[0].size)
        assertEquals(1.0, histogram.data[0][0])
        assertEquals(2.0, histogram.data[0][1])
    }

    @Test
    fun `test equal inputs produce one bin of height 2`() {
        ng.activationArray = doubleArrayOf(2.0, 2.0)
        iterate()
        assertEquals(1, histogram.seriesData.first().data.count{
            it.count == 2
        })
    }

    @Test
    fun `test unequal inputs produce two bins of height 1`() {
        ng.activationArray = doubleArrayOf(1.0, 2.0)
        iterate()
        assertEquals(2, histogram.seriesData.first().data.count{
            it.count == 1
        })
    }

    @Test
    fun `each iteration replaces the previous data`() {
        ng.activationArray = doubleArrayOf(1.0, 2.0)
        iterate()
        ng.activationArray = doubleArrayOf(5.0, 5.0)
        iterate()
        assertArrayEquals(doubleArrayOf(5.0, 5.0), histogram.data[0])
    }

    @Test
    fun `saving right after adding data includes it and the reopened model keeps accepting data`() {
        val model = HistogramModel()
        runBlocking { model.addData(doubleArrayOf(0.5, 1.5, 2.5)) }
        val xstream = getSimbrainXStream()
        val restored = xstream.fromXML(xstream.toXML(model)) as HistogramModel
        assertArrayEquals(doubleArrayOf(0.5, 1.5, 2.5), restored.data[0])
        runBlocking { restored.addData(doubleArrayOf(9.0)) }
        UiWork.awaitIdle()
        assertArrayEquals(doubleArrayOf(9.0), restored.data[0])
    }
}
