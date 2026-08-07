package org.simbrain.plot

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTimeSeriesComponent
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeurons
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.plot.timeseries.createTimeSeriesModel
import org.simbrain.workspace.Workspace

class TimeSeriesTest {

    @Test
    fun `ensure neurons send appropriate data to time series model with serialization`() {
        val sim = newSim {
            workspace.clearWorkspace()
            val networkComponent = addNetworkComponent("Network")
            val network = networkComponent.network
            val (neuron1, neuron2) = network.addNeurons(2)

            neuron1.bias = 0.5

            val timeSeriesComponent = addTimeSeriesComponent("TimeSeries", listOf("one", "two"))
            with(couplingManager) {
                neuron1 couple timeSeriesComponent.model.timeSeriesList[0]
                neuron2 couple timeSeriesComponent.model.timeSeriesList[1]
            }

            workspace.iterateSuspend(2)
            assertEquals(0.5, timeSeriesComponent.model.timeSeriesList[0].series.getY(1) as Double, 0.0)
            assertEquals(0.0, timeSeriesComponent.model.timeSeriesList[1].series.getY(1) as Double, 0.0)

            val data = workspace.zipDataHeadless
            workspace.clearWorkspace()
            workspace.openFromZipData(data)

            assertEquals(2, workspace.time)

            workspace.iterateSuspend(2)

            val newTimeSeriesComponent = workspace.getComponent("TimeSeries") as TimeSeriesPlotComponent

            assertEquals(4, workspace.time)
            assertEquals(0.5, newTimeSeriesComponent.model.timeSeriesList[0].series.getY(3) as Double, 0.0)
            assertEquals(0.0, newTimeSeriesComponent.model.timeSeriesList[1].series.getY(3) as Double, 0.0)
        }
        runBlocking { sim.run() }
    }

    @Test
    fun `array coupling names series after neuron labels and follows renames`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Network")
        workspace.addWorkspaceComponent(networkComponent)
        val network = networkComponent.network
        val neurons = List(3) { Neuron() }
        neurons.forEach { network.addNetworkModelAsync(it) }
        neurons[0].label = "Alpha"
        neurons[1].label = "Beta"
        neurons[2].label = "Gamma"
        val collection = NeuronCollection(neurons)
        network.addNetworkModelAsync(collection)

        val timeSeriesComponent = TimeSeriesPlotComponent("TimeSeries")
        workspace.addWorkspaceComponent(timeSeriesComponent)
        workspace.couplingManager.createCoupling(collection, timeSeriesComponent.model)

        awaitUntil(message = "Series were not named after neuron labels") {
            timeSeriesComponent.model.timeSeriesList.map { it.description } == listOf("Alpha", "Beta", "Gamma")
        }

        workspace.simpleIterate()
        assertEquals(1, timeSeriesComponent.model.timeSeriesList[1].series.itemCount)

        neurons[1].label = "Delta"
        awaitUntil(message = "Series name did not follow the neuron rename") {
            timeSeriesComponent.model.timeSeriesList.map { it.description } == listOf("Alpha", "Delta", "Gamma")
        }
        // The legend renders series keys, so those must follow the rename too, and data must be preserved
        assertEquals(listOf("Alpha", "Delta", "Gamma"), timeSeriesComponent.model.timeSeriesList.map { it.series.key })
        assertEquals(1, timeSeriesComponent.model.timeSeriesList[1].series.itemCount)
    }

    @Test
    fun `duplicate labels are disambiguated in positional order`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Network")
        workspace.addWorkspaceComponent(networkComponent)
        val network = networkComponent.network
        val neurons = List(3) { Neuron() }
        neurons.forEach { network.addNetworkModelAsync(it) }
        neurons[0].label = "Alpha"
        neurons[1].label = "Beta"
        neurons[2].label = "Alpha"
        val collection = NeuronCollection(neurons)
        network.addNetworkModelAsync(collection)

        val timeSeriesComponent = TimeSeriesPlotComponent("TimeSeries")
        workspace.addWorkspaceComponent(timeSeriesComponent)
        workspace.couplingManager.createCoupling(collection, timeSeriesComponent.model)

        awaitUntil(message = "Duplicate labels were not disambiguated in positional order") {
            timeSeriesComponent.model.timeSeriesList.map { it.description } == listOf("Alpha[0]", "Beta", "Alpha[1]")
        }
    }

    @Test
    fun `test basic time series model creation and operations`() = runBlocking {
        val workspace = Workspace()
        val timeSeriesModel = workspace.createTimeSeriesModel()
        timeSeriesModel.timeSupplier = { workspace.time }

        // Test initial state
        assertEquals(0, timeSeriesModel.timeSeriesList.size)
        assertEquals(0, timeSeriesModel.dataset.seriesCount)
        assertNotNull(timeSeriesModel.timeSupplier)

        // Test adding series
        val series1 = timeSeriesModel.addTimeSeries("Series 1")
        assertEquals(1, timeSeriesModel.timeSeriesList.size)
        assertEquals("Series 1", series1.description)
        assertEquals(1, timeSeriesModel.dataset.seriesCount)

        val series2 = timeSeriesModel.addTimeSeries("Series 2") 
        assertEquals(2, timeSeriesModel.timeSeriesList.size)
        assertEquals(2, timeSeriesModel.dataset.seriesCount)

        // Test removing series
        timeSeriesModel.removeLastTimeSeries()
        assertEquals(1, timeSeriesModel.timeSeriesList.size)
        assertEquals(1, timeSeriesModel.dataset.seriesCount)
        assertEquals("Series 1", timeSeriesModel.timeSeriesList[0].description)
    }

    @Test
    fun `test time series data management`() = runBlocking {
        val workspace = Workspace()
        val timeSeriesModel = workspace.createTimeSeriesModel()
        timeSeriesModel.timeSupplier = { workspace.time }

        val series = timeSeriesModel.addTimeSeries("Test Series")
        
        // Test adding data points
        workspace.resetTime()
        series.setValue(10.0)
        
        val xySeries = series.series
        assertEquals(1, xySeries.itemCount)
        assertEquals(0, xySeries.getX(0))
        assertEquals(10.0, xySeries.getY(0))

        // Test progression over time using iteration
        workspace.iterateSuspend(1)
        series.setValue(20.0)
        
        assertEquals(2, xySeries.itemCount)
        assertEquals(1, xySeries.getX(1))
        assertEquals(20.0, xySeries.getY(1))
    }

    @Test  
    fun `test time series configuration`() = runBlocking {
        val workspace = Workspace()
        val timeSeriesModel = workspace.createTimeSeriesModel()
        
        // Use custom time supplier for precise control
        var customTime = 0
        timeSeriesModel.timeSupplier = { customTime }

        // Test window size limit
        timeSeriesModel.fixedWidth = true
        timeSeriesModel.windowSize = 5
        val series = timeSeriesModel.addTimeSeries("Windowed Series")
        
        // Add more data points than window size
        repeat(10) { i ->
            customTime = i
            series.setValue(i.toDouble())
        }
        
        // Should only keep last 'windowSize' points when fixed width is enabled
        assertTrue(series.series.itemCount <= timeSeriesModel.windowSize)
        
        // Test auto range
        timeSeriesModel.isAutoRange = true
        series.setValue(100.0)
        assertTrue(timeSeriesModel.isAutoRange)
        
        // Test fixed range
        timeSeriesModel.isAutoRange = false
        timeSeriesModel.rangeUpperBound = 50.0
        timeSeriesModel.rangeLowerBound = 0.0
        assertEquals(50.0, timeSeriesModel.rangeUpperBound)
        assertEquals(0.0, timeSeriesModel.rangeLowerBound)
    }

    @Test
    fun `test multiple time series interactions`() = runBlocking {
        val workspace = Workspace()
        val timeSeriesModel = workspace.createTimeSeriesModel()
        timeSeriesModel.timeSupplier = { workspace.time }

        val series1 = timeSeriesModel.addTimeSeries("Signal A")
        val series2 = timeSeriesModel.addTimeSeries("Signal B")
        val series3 = timeSeriesModel.addTimeSeries("Signal C")

        // Test simultaneous data addition
        workspace.resetTime()
        series1.setValue(1.0)
        series2.setValue(2.0)  
        series3.setValue(3.0)

        assertEquals(3, timeSeriesModel.dataset.seriesCount)
        
        // All series should have data at the same time point
        assertEquals(1, series1.series.itemCount)
        assertEquals(1, series2.series.itemCount)
        assertEquals(1, series3.series.itemCount)
        
        // Verify different values
        assertEquals(1.0, series1.series.getY(0))
        assertEquals(2.0, series2.series.getY(0))
        assertEquals(3.0, series3.series.getY(0))
        
        // Test removing last series
        timeSeriesModel.removeLastTimeSeries()
        assertEquals(2, timeSeriesModel.dataset.seriesCount)
        
        // Should have removed series3
        assertTrue(timeSeriesModel.timeSeriesList.contains(series1))
        assertTrue(timeSeriesModel.timeSeriesList.contains(series2))
        assertEquals(2, timeSeriesModel.timeSeriesList.size)
    }

    @Test
    fun `test time supplier functionality`() = runBlocking {
        val workspace = Workspace()
        val timeSeriesModel = workspace.createTimeSeriesModel()
        
        // Test custom time supplier
        var customTime = 0
        timeSeriesModel.timeSupplier = { customTime }
        
        val series = timeSeriesModel.addTimeSeries("Custom Time")
        
        customTime = 5
        series.setValue(100.0)
        
        assertEquals(5, series.series.getX(0))
        assertEquals(100.0, series.series.getY(0))
        
        customTime = 10
        series.setValue(200.0)
        
        assertEquals(10, series.series.getX(1))
        assertEquals(200.0, series.series.getY(1))
    }

    @Test
    fun `test clear all data`() = runBlocking {
        val workspace = Workspace()
        val timeSeriesModel = workspace.createTimeSeriesModel()
        
        // Use custom time supplier for precise control
        var customTime = 0
        timeSeriesModel.timeSupplier = { customTime }

        val series1 = timeSeriesModel.addTimeSeries("Series 1")
        val series2 = timeSeriesModel.addTimeSeries("Series 2")
        
        // Add data to both series
        repeat(5) { i ->
            customTime = i
            series1.setValue(i.toDouble())
            series2.setValue((i * 2).toDouble())
        }
        
        assertEquals(5, series1.series.itemCount)
        assertEquals(5, series2.series.itemCount)
        
        // Clear all data
        timeSeriesModel.clearData()
        
        assertEquals(0, series1.series.itemCount)
        assertEquals(0, series2.series.itemCount)
        
        // Series should still exist
        assertEquals(2, timeSeriesModel.timeSeriesList.size)
    }

}