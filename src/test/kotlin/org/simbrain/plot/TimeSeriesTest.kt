package org.simbrain.plot

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTimeSeries
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.addNeurons
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent

class TimeSeriesTest {

    @Test
    fun `ensure neurons send appropriate data to time series model with serialization`() {
        val sim = newSim {
            workspace.clearWorkspace()
            val networkComponent = addNetworkComponent("Network")
            val network = networkComponent.network
            val (neuron1, neuron2) = network.addNeurons(2)

            (neuron1.dataHolder as BiasedScalarData).bias = 0.5

            val timeSeriesComponent = addTimeSeries("TimeSeries")
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

}