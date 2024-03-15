package org.simbrain.plot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.plot.piechart.PieChartComponent
import org.simbrain.plot.piechart.PieChartModel
import org.simbrain.workspace.Workspace

class PieChartTest {

    // TODO: Empty pie test, xml test
    val workspace = Workspace()
    val net = Network()
    val pieChart = PieChartModel()
    val ng = NeuronGroup(2).apply {
        setClamped(true)
    }

    init {
        net.addNetworkModelsAsync(ng)
        workspace.addWorkspaceComponent(NetworkComponent("Net", net))
        workspace.addWorkspaceComponent(PieChartComponent("Pie", pieChart))
        workspace.couplingManager.createCoupling(ng, pieChart)
    }

    @Test
    fun `pie is empty with no input or low input`() {

        // No input yet so pie is empty
        assertEquals(1,pieChart.dataset.itemCount)

        // With input below threshold it remains empty
        pieChart.emptyPieThreshold = .2
        ng.activations = doubleArrayOf(.1, .0)
        workspace.simpleIterate()
        assertEquals(1,pieChart.dataset.itemCount)

        // With input above threshold it is no longer empty
        ng.activations = doubleArrayOf(.1, .3)
        workspace.simpleIterate()
        assertEquals(2,pieChart.dataset.itemCount)

    }

    @Test
    fun `pie chart is initialized correctly`() {
        ng.activations = doubleArrayOf(.5, .5)
        workspace.simpleIterate()
        // There should be two "slices" of the pie
        assertEquals(2,pieChart.dataset.itemCount)
        // Keys should be "Neuron 1" and "Neuron 2"
        assertEquals(ng.getNeuron(0).displayName, pieChart.sliceNames[0])
        assertEquals(ng.getNeuron(1).displayName, pieChart.sliceNames[1])
    }


    @Test
    fun `equal values should lead to equal pie slices`() {
        ng.activations = doubleArrayOf(1.5, 1.5)
        workspace.simpleIterate()
        assertEquals(pieChart.dataset.getValue(0).toDouble(), pieChart.dataset.getValue(1).toDouble())
    }

    @Test
    fun `pie slices have correct proportions`() {
        ng.activations = doubleArrayOf(.5, 1.0)
        workspace.simpleIterate()
        assertEquals(.33, pieChart.dataset.getValue(0).toDouble(), .01)
        assertEquals(.66, pieChart.dataset.getValue(1).toDouble(), .01)
    }

    @Test
    fun `test xml rep`() {
        val pieChartComponent = PieChartComponent("test", pieChart)
        ng.activations = doubleArrayOf(.5, 1.0)
        workspace.simpleIterate()
        val xml = pieChartComponent.xml

        val deserializedPieChart = PieChartModel.getXStream().fromXML(xml) as PieChartModel
        assertEquals(2,deserializedPieChart.dataset.itemCount)
        assertEquals(ng.getNeuron(0).displayName, deserializedPieChart.sliceNames[0])
        assertEquals(ng.getNeuron(1).displayName, deserializedPieChart.sliceNames[1])
        assertEquals(.33, deserializedPieChart.dataset.getValue(0).toDouble(), .01)
        assertEquals(.66, deserializedPieChart.dataset.getValue(1).toDouble(), .01)

    }



}