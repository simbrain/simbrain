package org.simbrain.plot

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.plot.heatmap.HeatMapComponent
import org.simbrain.plot.heatmap.HeatMapModel
import org.simbrain.plot.heatmap.HeatMapPanel
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class HeatMapTest {

    val workspace = Workspace()
    val nc = NetworkComponent("Test Network")
    val na = NeuronArray(4).apply { isClamped = true }
    val hmc = HeatMapComponent("Test Heat Map")

    init {
        nc.network.addNetworkModelAsync(na)
        workspace.addWorkspaceComponent(nc)
        workspace.addWorkspaceComponent(hmc)
    }

    @Test
    fun `defaults use an auto-ranged cool to hot scale centered at zero`() {
        assertEquals(ChartColorMap.COOL_TO_HOT, hmc.model.colorMap)
        assertTrue(hmc.model.isAutoRange)
        assertEquals(-1.0, hmc.model.colorRange().start, 1e-12)
        assertEquals(1.0, hmc.model.colorRange().endInclusive, 1e-12)
    }

    @Test
    fun `each iteration appends one column of activations`() {
        with(workspace.couplingManager) { na couple hmc.model }
        na.activationArray = doubleArrayOf(-1.0, 0.0, 1.0, 2.0)

        workspace.simpleIterate()
        workspace.simpleIterate()

        assertEquals(2, hmc.model.columnCount)
        assertEquals(4, hmc.model.rowCount)
        assertEquals(listOf(-1.0, 0.0, 1.0, 2.0), hmc.model.columns.last().toList())
    }

    @Test
    fun `fixed width discards the oldest columns`() {
        hmc.model.fixedWidth = true
        hmc.model.windowSize = 3
        with(workspace.couplingManager) { na couple hmc.model }

        repeat(6) { step ->
            na.activationArray = doubleArrayOf(step.toDouble(), 0.0, 0.0, 0.0)
            workspace.simpleIterate()
        }

        assertEquals(3, hmc.model.columnCount)
        assertEquals(listOf(3.0, 4.0, 5.0), hmc.model.columns.map { it[0] })
    }

    @Test
    fun `auto range tracks the values present in the window`() {
        hmc.model.isAutoRange = true
        with(workspace.couplingManager) { na couple hmc.model }
        na.activationArray = doubleArrayOf(-2.0, 0.0, 0.0, 7.0)

        workspace.simpleIterate()

        val range = hmc.model.colorRange()
        assertEquals(-7.0, range.start, 1e-12)
        assertEquals(7.0, range.endInclusive, 1e-12)
    }

    @Test
    fun `a fixed color range ignores the data`() {
        hmc.model.isAutoRange = false
        hmc.model.rangeLowerBound = 0.0
        hmc.model.rangeUpperBound = 1.0
        with(workspace.couplingManager) { na couple hmc.model }
        na.activationArray = doubleArrayOf(-50.0, 0.0, 0.0, 50.0)

        workspace.simpleIterate()

        assertEquals(-1.0, hmc.model.colorRange().start, 1e-12)
        assertEquals(1.0, hmc.model.colorRange().endInclusive, 1e-12)
    }

    @Test
    fun `a constant matrix still yields a usable color range`() {
        with(workspace.couplingManager) { na couple hmc.model }
        na.activationArray = doubleArrayOf(3.0, 3.0, 3.0, 3.0)

        workspace.simpleIterate()

        assertTrue(hmc.model.colorRange().endInclusive > hmc.model.colorRange().start)
    }

    @Test
    fun `the dataset reports one item per cell with matching coordinates`() {
        with(workspace.couplingManager) { na couple hmc.model }
        na.activationArray = doubleArrayOf(10.0, 20.0, 30.0, 40.0)
        workspace.simpleIterate()
        na.activationArray = doubleArrayOf(50.0, 60.0, 70.0, 80.0)
        workspace.simpleIterate()

        val dataset = hmc.model.dataset()
        assertEquals(8, dataset.getItemCount(0))
        assertEquals(0, dataset.getY(0, 0))
        assertEquals(10.0, dataset.getZ(0, 0))
        assertEquals(3, dataset.getY(0, 3))
        assertEquals(40.0, dataset.getZ(0, 3))
        assertEquals(50.0, dataset.getZ(0, 4))
        assertEquals(dataset.getX(0, 0), dataset.getX(0, 3))
    }

    @Test
    fun `color maps span their full ramp and clamp outside it`() {
        ChartColorMap.entries.forEach { map ->
            assertEquals(map.color(0.0), map.color(-5.0), "$map should clamp below zero")
            assertEquals(map.color(1.0), map.color(5.0), "$map should clamp above one")
            assertTrue(map.color(0.0) != map.color(1.0), "$map endpoints should differ")
        }
    }

    @Test
    fun `the paint scale maps its bounds to the ends of the color map`() {
        val scale = ChartColorMapPaintScale(-1.0, 1.0) { ChartColorMap.JET }

        assertEquals(ChartColorMap.JET.color(0.0), scale.getPaint(-1.0))
        assertEquals(ChartColorMap.JET.color(0.5), scale.getPaint(0.0))
        assertEquals(ChartColorMap.JET.color(1.0), scale.getPaint(1.0))
    }

    @Test
    fun `a constant matrix of large values still yields a positive-length color range`() {
        val model = HeatMapModel()
        listOf(1.0, 16384.0, 1e5, -5e4, 1e9).forEach { constant ->
            model.clearData()
            model.setValues(DoubleArray(4) { constant })

            val range = model.colorRange()
            assertTrue(
                range.endInclusive - range.start > 0.0,
                "Color range collapsed to zero length at constant $constant"
            )
        }
    }

    @Test
    fun `an inverted fixed color range is corrected rather than left degenerate`() {
        hmc.model.isAutoRange = false
        hmc.model.rangeLowerBound = 1e6
        hmc.model.rangeUpperBound = 0.0

        val range = hmc.model.colorRange()

        assertTrue(range.endInclusive > range.start, "Expected a positive-length range, got $range")
    }

    @Test
    fun `columns retain their rows when the coupled array shrinks`() {
        val model = HeatMapModel()
        model.setValues(doubleArrayOf(1.0, 2.0, 3.0, 4.0))
        model.setValues(doubleArrayOf(5.0, 6.0))

        assertEquals(4, model.rowCount)
        val dataset = model.dataset()
        assertEquals(8, dataset.getItemCount(0))
        assertEquals(4.0, dataset.getZ(0, 3))
        assertEquals(6.0, dataset.getZ(0, 5))
        assertEquals(null, dataset.getZ(0, 6), "Cells past a short column's end should have no value")
    }

    @Test
    fun `cells with no value are transparent rather than the bottom of the color map`() {
        val scale = ChartColorMapPaintScale(0.0, 1.0) { ChartColorMap.HOT }

        assertEquals(NO_DATA, scale.getPaint(Double.NaN))
        assertEquals(0, NO_DATA.alpha)
        assertTrue(NO_DATA != ChartColorMap.HOT.color(0.0))
    }

    @Test
    fun `the consumer the coupled plot menu looks up is reachable by method reference`() {
        with(workspace.couplingManager) {
            na.getProducer(na::activationArray) couple hmc.model.getConsumer(HeatMapModel::setValues)
        }
        na.activationArray = doubleArrayOf(1.0, 2.0, 3.0, 4.0)

        workspace.simpleIterate()

        assertEquals(1, workspace.couplingManager.couplings.size)
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), hmc.model.columns.last().toList())
    }

    @Test
    fun `neuron collection row labels initialize and follow label changes`() {
        val neurons = listOf(Neuron().apply { label = "Input" }, Neuron().apply { label = "Output" })
        val collection = NeuronCollection(neurons)
        nc.network.addNetworkModelsAsync(neurons)
        nc.network.addNetworkModelAsync(collection)

        with(workspace.couplingManager) { collection couple hmc.model }

        awaitUntil(message = "Row labels were not initialized from neuron labels") {
            hmc.model.componentNames == listOf("Input", "Output")
        }
        neurons[1].label = "Target"
        awaitUntil(message = "Row labels did not follow the neuron rename without an iteration") {
            hmc.model.componentNames == listOf("Input", "Target")
        }
    }

    @Test
    fun `a workspace time reset does not invert the domain range`() {
        val model = HeatMapModel()
        var now = 500
        model.timeSupplier = { now }
        model.setValues(doubleArrayOf(1.0, 2.0))
        now = 0
        model.setValues(doubleArrayOf(3.0, 4.0))

        val range = HeatMapPanel(model).chartPanel.chart.xyPlot.domainAxis.range

        assertTrue(range.length > 0.0, "Domain range was inverted after a time reset: $range")
    }

    @Test
    fun `block width follows the spacing between recorded times`() {
        val model = HeatMapModel()
        var now = 0
        model.timeSupplier = { now }
        listOf(0, 5, 10, 15).forEach { time ->
            now = time
            model.setValues(doubleArrayOf(1.0, 2.0))
        }

        val range = HeatMapPanel(model).chartPanel.chart.xyPlot.domainAxis.range

        // Four columns five time units apart, each 5 wide, so the axis spans -2.5 to 17.5.
        assertEquals(-2.5, range.lowerBound, 1e-9)
        assertEquals(17.5, range.upperBound, 1e-9)
    }

    @Test
    fun `the component factory creates a heat map by its menu name`() {
        val blank = Workspace()

        blank.componentFactory.createWorkspaceComponent("Heat map")

        assertEquals(1, blank.componentList.filterIsInstance<HeatMapComponent>().size)
    }

    @Test
    fun `test serialization`() {
        with(workspace.couplingManager) { na couple hmc.model }
        na.activationArray = doubleArrayOf(-1.0, 0.0, 1.0, 2.0)
        hmc.model.colorMap = ChartColorMap.HOT
        workspace.simpleIterate()

        val serializer = WorkspaceSerializer(workspace)
        val bas = ByteArrayOutputStream()
        serializer.serialize(bas, true)
        bas.close()
        workspace.clearWorkspace()

        val bis = ByteArrayInputStream(bas.toByteArray())
        runBlocking { serializer.deserialize(bis) }
        bis.close()

        val reopened = workspace.getComponent("Test Heat Map") as HeatMapComponent
        assertEquals(1, workspace.couplingManager.couplings.size)
        assertEquals(ChartColorMap.HOT, reopened.model.colorMap)
        assertEquals(1, reopened.model.columnCount)
        assertEquals(listOf(-1.0, 0.0, 1.0, 2.0), reopened.model.columns.last().toList())

        workspace.simpleIterate()
        assertEquals(2, reopened.model.columnCount)
    }
}
