package org.simbrain.plot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.plot.histogram.HistogramComponent
import org.simbrain.plot.histogram.HistogramModel
import org.simbrain.workspace.Workspace


class HistogramTest {

    val workspace = Workspace()
    val net = Network()
    val nwc = NetworkComponent("Net", net)
    val ng = NeuronGroup(2).apply {
        setClamped(true)
    }
    val histogram = HistogramModel()
    val hgc = HistogramComponent("Histogram", histogram)

    init {
        net.addNetworkModels(ng)
        workspace.addWorkspaceComponent(hgc)
        workspace.addWorkspaceComponent(nwc)
        workspace.couplingManager.createCoupling(ng, histogram)
    }

    @Test
    fun `test data is transferred properly`() {
        ng.activationArray = doubleArrayOf(1.0, 2.0)
        workspace.simpleIterate()
        assertEquals(2, histogram.data[0].size)
        assertEquals(1.0, histogram.data[0][0])
        assertEquals(2.0, histogram.data[0][1])
    }

    @Test
    fun `test equal inputs produce one bin of height 2`() {
        ng.activationArray = doubleArrayOf(2.0, 2.0)
        workspace.simpleIterate()
        assertEquals(1, histogram.seriesData.first().data.count{
            it.count == 2
        })
    }

    @Test
    fun `test unequal inputs produce two bins of height 1`() {
        ng.activationArray = doubleArrayOf(1.0, 2.0)
        workspace.simpleIterate()
        assertEquals(2, histogram.seriesData.first().data.count{
            it.count == 1
        })
    }

}


