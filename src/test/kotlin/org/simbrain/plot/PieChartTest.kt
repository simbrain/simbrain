package org.simbrain.plot

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
    fun `equal values should lead to equal pie slices`() {
        ng.activations = doubleArrayOf(.5, .5)
        workspace.simpleIterate()
        //  Size should be 2
        println(pieChart.dataset.getItemCount())
        // Keys should be "Neuron 1" and "Neuron 2"
        println(pieChart.dataset.keys)
        // Values should be .5 and .5
        println(pieChart.dataset.getValue(0))
        println(pieChart.dataset.getValue(1))
    }



}