/**
 * A running workspace must never wait on the EDT: plots, the network canvas and text views catch up on their own.
 * Waiting on the EDT once per iteration paced whole simulations by whatever the EDT was painting (the Cortical
 * Layers sim ran 7x slower with its windows up). These tests hold the EDT and require iterations to finish anyway.
 */
package org.simbrain.workspace

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.plot.barchart.BarChartComponent
import org.simbrain.plot.heatmap.HeatMapComponent
import org.simbrain.plot.heatmap.HeatMapModel
import org.simbrain.plot.histogram.HistogramComponent
import org.simbrain.plot.piechart.PieChartComponent
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.UiWork
import org.simbrain.util.finishesWhileEdtIsBlocked
import org.simbrain.world.textworld.TextWorld
import org.simbrain.world.textworld.gui.TextWorldPanel
import javax.swing.SwingUtilities

class CoupledViewsEdtTest {

    @Test
    fun `a network coupled to every plot type iterates while the edt is blocked`() = runBlocking {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Network")
        workspace.addWorkspaceComponent(networkComponent)
        val neurons = List(4) { Neuron() }
        networkComponent.network.addNetworkModels(neurons)
        val collection = NeuronCollection(neurons).apply { isClamped = true }
        networkComponent.network.addNetworkModel(collection)
        NetworkPanel(networkComponent)

        val timeSeries = TimeSeriesPlotComponent("Time series").also { workspace.addWorkspaceComponent(it) }
        val bars = BarChartComponent("Bars").also { workspace.addWorkspaceComponent(it) }
        val pie = PieChartComponent("Pie").also { workspace.addWorkspaceComponent(it) }
        val heatMap = HeatMapComponent("Heat map").also { workspace.addWorkspaceComponent(it) }
        val raster = RasterPlotComponent("Raster").also { workspace.addWorkspaceComponent(it) }
        val histogram = HistogramComponent("Histogram").also { workspace.addWorkspaceComponent(it) }
        with(workspace.couplingManager) {
            createCoupling(collection, timeSeries.model)
            createCoupling(collection, bars.model)
            createCoupling(collection, pie.model)
            collection.getProducer(collection::activationArray) couple heatMap.model.getConsumer(HeatMapModel::setValues)
            collection.getProducer(collection::activationArray) couple raster.model.rasterConsumerList[0].getConsumer("setValues")
            createCoupling(collection, histogram.model)
        }
        // Series and names are set up on first contact; let that settle so only iteration is measured
        collection.activationArray = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        workspace.iterateSuspend(1)
        UiWork.awaitIdle()

        val finished = finishesWhileEdtIsBlocked { workspace.iterateSuspend(20) }

        assertTrue(finished, "iterating waited on the blocked EDT")
        UiWork.awaitIdle()
        assertEquals(21, timeSeries.model.timeSeriesList[0].series.itemCount)
        assertEquals(21, heatMap.model.columnCount)
    }

    @Test
    fun `appending to a text world with a view open does not wait on the edt`() = runBlocking {
        val world = TextWorld()
        lateinit var panel: TextWorldPanel
        SwingUtilities.invokeAndWait { panel = TextWorldPanel(world) }

        val finished = finishesWhileEdtIsBlocked {
            repeat(50) { world.addTextAtEnd("word$it") }
        }

        assertTrue(finished, "appending waited on the blocked EDT")
        // The view catches up to the world's latest text once the EDT is free
        UiWork.awaitIdle()
        var shown = ""
        SwingUtilities.invokeAndWait { shown = panel.textArea.text }
        assertEquals(world.text, shown)
        assertTrue(shown.endsWith("word49"))
    }
}
