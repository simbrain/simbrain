package org.simbrain.plot

import kotlinx.coroutines.runBlocking
import org.jfree.chart.renderer.AbstractRenderer
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
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel
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
    fun `deleting a neuron drops only its series and keeps the others' data`() {
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
        repeat(3) { workspace.simpleIterate() }
        assertEquals(listOf(3, 3, 3), timeSeriesComponent.model.timeSeriesList.map { it.series.itemCount })

        runBlocking { neurons[1].delete() }

        awaitUntil(message = "The deleted neuron's series was not dropped") {
            timeSeriesComponent.model.timeSeriesList.map { it.description } == listOf("Alpha", "Gamma")
        }
        // The surviving neurons are untouched by their neighbour's deletion, so they keep their history
        assertEquals(listOf(3, 3), timeSeriesComponent.model.timeSeriesList.map { it.series.itemCount })
        assertEquals(listOf("Alpha", "Gamma"), timeSeriesComponent.model.timeSeriesList.map { it.series.key })
    }

    @Test
    fun `adding a neuron gives it a new series without disturbing the others`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Network")
        workspace.addWorkspaceComponent(networkComponent)
        val network = networkComponent.network
        val neurons = List(2) { Neuron() }
        neurons.forEach { network.addNetworkModelAsync(it) }
        neurons[0].label = "Alpha"
        neurons[1].label = "Beta"
        val collection = NeuronCollection(neurons)
        network.addNetworkModelAsync(collection)

        val timeSeriesComponent = TimeSeriesPlotComponent("TimeSeries")
        workspace.addWorkspaceComponent(timeSeriesComponent)
        workspace.couplingManager.createCoupling(collection, timeSeriesComponent.model)

        awaitUntil { timeSeriesComponent.model.timeSeriesList.size == 2 }
        repeat(3) { workspace.simpleIterate() }

        val added = Neuron().apply { label = "Gamma" }
        network.addNetworkModelAsync(added)
        collection.addNeuron(added)

        awaitUntil(message = "The added neuron did not get a series") {
            timeSeriesComponent.model.timeSeriesList.map { it.description } == listOf("Alpha", "Beta", "Gamma")
        }
        assertEquals(listOf(3, 3, 0), timeSeriesComponent.model.timeSeriesList.map { it.series.itemCount })
    }

    @Test
    fun `scalar couplings name series after the producing neuron and follow renames`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Network")
        workspace.addWorkspaceComponent(networkComponent)
        val network = networkComponent.network
        val neurons = List(3) { Neuron() }
        neurons.forEach { network.addNetworkModelAsync(it) }
        neurons[0].label = "Alpha"
        neurons[1].label = "Beta"

        val timeSeriesComponent = TimeSeriesPlotComponent("TimeSeries")
        workspace.addWorkspaceComponent(timeSeriesComponent)
        with(workspace.couplingManager) {
            neurons.forEach { neuron ->
                val series = timeSeriesComponent.addTimeSeries("placeholder ${neuron.id}")
                neuron.getProducer("getActivation") couple series.getConsumer(TimeSeriesModel.TimeSeries::setValue)
            }
        }

        // Every series comes from an activation, so the method name adds nothing and is left off. The third
        // neuron has no label, so it falls back to its id.
        awaitUntil(message = "Series were not named after the producing neurons") {
            timeSeriesComponent.model.timeSeriesList.map { it.description } ==
                    listOf("Alpha", "Beta", neurons[2].id)
        }

        workspace.simpleIterate()
        neurons[1].label = "Delta"
        awaitUntil(message = "Series name did not follow the neuron rename") {
            timeSeriesComponent.model.timeSeriesList.map { it.description } ==
                    listOf("Alpha", "Delta", neurons[2].id)
        }
        assertEquals(1, timeSeriesComponent.model.timeSeriesList[1].series.itemCount)
    }

    @Test
    fun `scalar couplings keep the method name when it is what distinguishes the series`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Network")
        workspace.addWorkspaceComponent(networkComponent)
        val network = networkComponent.network
        val neuron = Neuron()
        network.addNetworkModelAsync(neuron)
        neuron.label = "Alpha"

        val timeSeriesComponent = TimeSeriesPlotComponent("TimeSeries")
        workspace.addWorkspaceComponent(timeSeriesComponent)
        with(workspace.couplingManager) {
            listOf("getActivation", "getBias").forEach { method ->
                val series = timeSeriesComponent.addTimeSeries("placeholder $method")
                neuron.getProducer(method) couple series.getConsumer(TimeSeriesModel.TimeSeries::setValue)
            }
        }

        awaitUntil(message = "Series from one neuron's two attributes were not distinguished by method name") {
            timeSeriesComponent.model.timeSeriesList.map { it.description } ==
                    listOf("Alpha:Activation", "Alpha:Bias")
        }
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
    fun `a series with no data yet does not blank the chart`() {
        val model = TimeSeriesModel()
        var time = 0
        model.timeSupplier = { time }
        val populated = model.addTimeSeries("Alpha")
        repeat(3) {
            time = it
            runBlocking { populated.setValue(it.toDouble() + 1) }
        }
        // Stands in for the series added when a deleted neuron is restored: it has no values until the
        // next update, and an empty JFreeChart series reports its bounds as NaN
        model.addTimeSeries("Beta")

        val range = TimeSeriesPlotPanel(model).chartPanel.chart.xyPlot.rangeAxis.range

        assertFalse(range.lowerBound.isNaN() || range.upperBound.isNaN(), "Range went NaN: $range")
        assertTrue(range.lowerBound <= 1.0 && range.upperBound >= 3.0, "Range excludes the data: $range")
    }

    @Test
    fun `hiding a series drops it from the renderer and auto-range until shown again`() {
        val model = TimeSeriesModel()
        var time = 0
        model.timeSupplier = { time }
        val small = model.addTimeSeries("Small")
        val large = model.addTimeSeries("Large")
        repeat(3) {
            time = it
            runBlocking {
                small.setValue(it.toDouble())
                large.setValue(it * 100.0)
            }
        }
        val panel = TimeSeriesPlotPanel(model)
        val plot = panel.chartPanel.chart.xyPlot
        assertTrue(plot.rangeAxis.range.upperBound >= 200.0)

        large.visible = false
        awaitUntil(message = "Hiding did not reach the renderer") {
            !(plot.renderer as AbstractRenderer).isSeriesVisible(1)
        }
        awaitUntil(message = "Auto-range still includes the hidden series") {
            plot.rangeAxis.range.upperBound < 100.0
        }

        large.visible = true
        awaitUntil(message = "Showing did not reach the renderer") {
            (plot.renderer as AbstractRenderer).isSeriesVisible(1)
        }
        // Hiding is view-only, so the series kept its history
        assertEquals(3, large.series.itemCount)
    }

    @Test
    fun `series visibility survives serialization`() {
        val model = TimeSeriesModel()
        model.timeSupplier = { 0 }
        model.addTimeSeries("Alpha")
        model.addTimeSeries("Beta")
        model.timeSeriesList[1].visible = false

        val xml = TimeSeriesPlotComponent.timeSeriesXStream.toXML(model)
        val restored = TimeSeriesPlotComponent.timeSeriesXStream.fromXML(xml) as TimeSeriesModel

        assertEquals(listOf("Alpha", "Beta"), restored.timeSeriesList.map { it.description })
        assertEquals(listOf(true, false), restored.timeSeriesList.map { it.visible })
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
    fun `default names skip numbers still in use after a mid-list removal`() {
        val model = TimeSeriesModel()
        model.timeSupplier = { 0 }
        model.addTimeSeries(3)
        assertEquals(listOf("Series 1", "Series 2", "Series 3"), model.timeSeriesList.map { it.description })

        model.removeTimeSeries(model.timeSeriesList[0])
        model.addTimeSeries()

        assertEquals(listOf("Series 2", "Series 3", "Series 1"), model.timeSeriesList.map { it.description })
        assertEquals(3, model.dataset.seriesCount)
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