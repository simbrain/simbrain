package org.simbrain.plot

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.updaterules.SpikingThresholdRule
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.util.setValuesInPlace
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class RasterPlotTest {

    val workspace = Workspace()
    val nc = NetworkComponent("Test Network")
    val na = NeuronArray(5).apply {
        isClamped = true
    }
    val rpc = RasterPlotComponent("Test Raster Plot")

    init {
        nc.network.addNetworkModel(na)
        workspace.addWorkspaceComponent(nc)
        workspace.addWorkspaceComponent(rpc)
    }

    @Test
    fun `test raster plot communication`() {
        with(workspace.couplingManager) {
            na couple rpc.model.rasterConsumerList[0]
        }
        // Should produce (1,2) and (1,4) since after 1 iteration, since second and fourth components are above threshld
        na.activationArray = doubleArrayOf(-1.0, 0.0, 1.0, 0.0, 2.0)
        workspace.simpleIterate()
        assertEquals(1.0, rpc.model.dataset.getSeries(0).getDataItem(0).xValue)
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(0).yValue)
        assertEquals(1.0, rpc.model.dataset.getSeries(0).getDataItem(1).xValue)
        assertEquals(4.0, rpc.model.dataset.getSeries(0).getDataItem(1).yValue)
        na.activationArray = doubleArrayOf(0.0, 0.0, 1.0, 2.0, -1.0)
        workspace.simpleIterate()
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(2).xValue)
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(2).yValue)
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(3).xValue)
        assertEquals(3.0, rpc.model.dataset.getSeries(0).getDataItem(3).yValue)
    }

    @Test
    fun `test raster plot with spiking neurons`() {
        with(workspace.couplingManager) {
            na.getProducer(na::spikes) couple rpc.model.rasterConsumerList[0].getConsumer("setValues")
        }
        na.isClamped = false
        na.updateRule = SpikingThresholdRule()
        na.inputs.setValuesInPlace { i, j -> if ((i + j) % 2 == 1 ) 100.0 else -100.0 }
        workspace.simpleIterate()
        workspace.simpleIterate()
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(1).xValue)
        assertEquals(1.0, rpc.model.dataset.getSeries(0).getDataItem(1).yValue)
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(2).xValue)
        assertEquals(3.0, rpc.model.dataset.getSeries(0).getDataItem(2).yValue)
    }

    @Test
    fun `test serialization`() {
        rpc.model.addDataSources(2)
        with(workspace.couplingManager) {
            na couple rpc.model.rasterConsumerList[0]
        }
        // Should produce (1,2) and (1,4) since after 1 iteration, since second and fourth components are above threshld
        na.activationArray = doubleArrayOf(-1.0, 0.0, 1.0, 0.0, 2.0)

        val serializer = WorkspaceSerializer(workspace)
        workspace.simpleIterate()
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

        val rpc = workspace.getComponent("Test Raster Plot") as RasterPlotComponent
        val networkComponent = workspace.getComponent("Test Network") as NetworkComponent
        val na = networkComponent.network.getModels<NeuronArray>().first()

        assertEquals(1, workspace.couplingManager.couplings.size)

        assertEquals(3, rpc.model.numDataSources)
        assertEquals(1.0, rpc.model.dataset.getSeries(0).getDataItem(0).xValue)
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(0).yValue)
        assertEquals(1.0, rpc.model.dataset.getSeries(0).getDataItem(1).xValue)
        assertEquals(4.0, rpc.model.dataset.getSeries(0).getDataItem(1).yValue)
        na.activationArray = doubleArrayOf(0.0, 0.0, 1.0, 2.0, -1.0)
        workspace.simpleIterate()
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(2).xValue)
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(2).yValue)
        assertEquals(2.0, rpc.model.dataset.getSeries(0).getDataItem(3).xValue)
        assertEquals(3.0, rpc.model.dataset.getSeries(0).getDataItem(3).yValue)
    }

}