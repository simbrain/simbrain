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
        net.addNetworkModelsAsync(ng)
        workspace.addWorkspaceComponent(hgc)
        workspace.addWorkspaceComponent(nwc)
        workspace.couplingManager.createCoupling(ng, histogram)
    }

    @Test
    fun `Histogram is empty with no input`() {

        // No input yet so histogram is empty
        assertEquals(1, histogram.seriesData.size)

        // With input, it is no longer empty
        ng.activations = doubleArrayOf(1.0, 2.0)
        workspace.simpleIterate()

        println(histogram.seriesData.size)
        assertEquals(2, histogram.seriesData.size)
    }
}

