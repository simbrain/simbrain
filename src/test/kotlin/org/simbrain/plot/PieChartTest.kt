package org.simbrain.plot

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.plot.piechart.PieChartComponent
import org.simbrain.plot.piechart.PieChartModel
import org.simbrain.workspace.Workspace

class PieChartTest {

    val workspace = Workspace()
    val net = Network()
    val nwc = NetworkComponent("Net", net)
    val ng: NeuronCollection
    val pieChart = PieChartModel()
    val pcc = PieChartComponent("Pie", pieChart)

    init {
        val ngNeurons = List(2) { Neuron() }
        ngNeurons.forEach { net.addNetworkModelAsync(it) }
        ng = NeuronCollection(ngNeurons).apply { isClamped = true }
        net.addNetworkModelAsync(ng)
        workspace.addWorkspaceComponent(pcc)
        workspace.addWorkspaceComponent(nwc)
        workspace.couplingManager.createCoupling(ng, pieChart)
    }

    @Test
    fun `pie is empty with no input or low input`() {

        // No input yet so pie is empty
        assertEquals(1,pieChart.dataset.itemCount)

        // With input below threshold it remains empty
        pieChart.emptyPieThreshold = .2
        ng.activationArray = doubleArrayOf(.1, .0)
        workspace.simpleIterate()
        assertEquals(1,pieChart.dataset.itemCount)

        // With input above threshold it is no longer empty
        ng.activationArray = doubleArrayOf(.1, .3)
        workspace.simpleIterate()
        assertEquals(2,pieChart.dataset.itemCount)

    }

    @Test
    fun `pie chart is initialized correctly`() {
        ng.activationArray = doubleArrayOf(.5, .5)
        workspace.simpleIterate()
        // There should be two "slices" of the pie
        assertEquals(2,pieChart.dataset.itemCount)
        // Keys should be "Neuron 1" and "Neuron 2"
        assertEquals(ng.getNeuron(0).displayName, pieChart.sliceNames[0])
        assertEquals(ng.getNeuron(1).displayName, pieChart.sliceNames[1])
    }


    @Test
    fun `slice names follow neuron renames without an iteration`() {
        ng.activationArray = doubleArrayOf(.5, .5)
        workspace.simpleIterate()
        awaitUntil(message = "Slices were not named after the neurons") {
            pieChart.dataset.keys == listOf(ng.getNeuron(0).displayName, ng.getNeuron(1).displayName)
        }

        ng.getNeuron(0).label = "Renamed"
        awaitUntil(message = "Slice name did not follow the neuron rename without an iteration") {
            pieChart.dataset.keys.firstOrNull() == "Renamed"
        }
        assertEquals(.5, pieChart.dataset.getValue(0).toDouble(), .01)
    }

    @Test
    fun `equal values should lead to equal pie slices`() {
        ng.activationArray = doubleArrayOf(1.5, 1.5)
        workspace.simpleIterate()
        assertEquals(pieChart.dataset.getValue(0).toDouble(), pieChart.dataset.getValue(1).toDouble())
    }

    @Test
    fun `pie slices have correct proportions`() {
        ng.activationArray = doubleArrayOf(.5, 1.0)
        workspace.simpleIterate()
        assertEquals(.33, pieChart.dataset.getValue(0).toDouble(), .01)
        assertEquals(.66, pieChart.dataset.getValue(1).toDouble(), .01)
    }

    @Test
    fun `test xml rep`() {
        ng.activationArray = doubleArrayOf(.5, 1.0)
        workspace.simpleIterate()
        val xml = pcc.xml
        val deserializedPieChart = PieChartModel.getXStream().fromXML(xml) as PieChartModel
        assertEquals(2,deserializedPieChart.dataset.itemCount)
        assertEquals(ng.getNeuron(0).displayName, deserializedPieChart.sliceNames[0])
        assertEquals(ng.getNeuron(1).displayName, deserializedPieChart.sliceNames[1])
        assertEquals(.33, deserializedPieChart.dataset.getValue(0).toDouble(), .01)
        assertEquals(.66, deserializedPieChart.dataset.getValue(1).toDouble(), .01)
    }

    @Test
    fun `test workspace couplings`() {
        // Put pie chat in 50/50 state
        ng.activationArray = doubleArrayOf(1.5, 1.5)
        workspace.simpleIterate()

        // Zip and create a new workspace
        val zip = workspace.zipDataHeadless
        val workspace2 = Workspace()

        // Iterate the new workspace with new inputs and proportions should change accordingly
        runBlocking { workspace2.openFromZipData(zip) }
        val savedNg = (workspace2.getComponent("Net") as NetworkComponent).network.getModels<NeuronCollection>().first()
        savedNg.activationArray = doubleArrayOf(1.5, .5)
        val savedPie = (workspace2.getComponent("Pie") as PieChartComponent).model as PieChartModel
        workspace2.simpleIterate()
        assertEquals(.75, savedPie.dataset.getValue(0).toDouble())
        assertEquals(.25, savedPie.dataset.getValue(1).toDouble())
    }



}