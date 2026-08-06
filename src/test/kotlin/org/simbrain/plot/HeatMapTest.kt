package org.simbrain.plot

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NeuronArray
import org.simbrain.plot.heatmap.HeatMapComponent
import org.simbrain.plot.heatmap.HeatMapModel
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
        assertEquals(-2.0, range.start, 1e-12)
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

        assertEquals(0.0, hmc.model.colorRange().start, 1e-12)
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
