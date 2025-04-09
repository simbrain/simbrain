package org.simbrain.plot

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTimeSeriesComponent
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.addNeurons
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class RasterPlotTest {

    val workspace = Workspace()
    val nc = NetworkComponent("Test")
    val na = NeuronArray(5).apply {
        isClamped = true
    }
    val rpc = RasterPlotComponent("Test")

    init {
        nc.network.addNetworkModel(na)
        workspace.addWorkspaceComponent(nc)
        workspace.addWorkspaceComponent(rpc)
        with(workspace.couplingManager) {
            na couple rpc.model.rasterConsumerList[0]
        }
        // Should produce (1,2) and (1,4) since after 1 iteration, since second and fourth components are above threshld
        na.activationArray = doubleArrayOf(-1.0, 0.0, 1.0, 0.0, 2.0)
    }

    @Test
    fun `test raster plot communication`() {
        workspace.simpleIterate()
        assertEquals(1.0, rpc.model.dataset.getSeries(0).getDataItem(0).xValue)
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(0).yValue)
        assertEquals(1.0, rpc.model.dataset.getSeries(0).getDataItem(1).xValue)
        assertEquals(4.0, rpc.model.dataset.getSeries(0).getDataItem(1).yValue)
    }

    @Test
    fun `test serialization`() {

        val serializer = WorkspaceSerializer(workspace)
        val bas = ByteArrayOutputStream()
        serializer.serialize(bas, true)
        bas.close()
        workspace.clearWorkspace()

        // Reopen
        val bis = ByteArrayInputStream(bas.toByteArray())
        runBlocking {
            serializer.deserialize(bis)
        }
        bis.close()
        assertEquals(1, workspace.couplingManager.couplings.size)
        assertEquals(1.0, rpc.model.dataset.getSeries(0).getDataItem(0).xValue)
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(0).yValue)
        assertEquals(1.0, rpc.model.dataset.getSeries(0).getDataItem(1).xValue)
        assertEquals(4.0, rpc.model.dataset.getSeries(0).getDataItem(1).yValue)
        na.activationArray = doubleArrayOf(0.0, 0.0, 1.0, 2.0, -1.0)
        workspace.simpleIterate()
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(0).xValue)
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(0).yValue)
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(1).xValue)
        assertEquals(3.0, rpc.model.dataset.getSeries(0).getDataItem(1).yValue)
    }

}