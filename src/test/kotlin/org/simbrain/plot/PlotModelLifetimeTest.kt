/**
 * A plot model stops applying updates once its component leaves the workspace, including when the workspace is
 * cleared, which removes components without closing them.
 */
package org.simbrain.plot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.plot.barchart.BarChartComponent
import org.simbrain.plot.barchart.BarChartModel
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.UiWork
import org.simbrain.util.onEdt
import org.simbrain.workspace.Workspace

class PlotModelLifetimeTest {

    @Test
    fun `clearing the workspace stops its plot models applying updates`() {
        val workspace = Workspace()
        val bar = BarChartModel()
        workspace.addWorkspaceComponent(BarChartComponent("Bar", bar))
        val timeSeries = TimeSeriesModel()
        workspace.addWorkspaceComponent(TimeSeriesPlotComponent("Time series", timeSeries))
        val series = timeSeries.addTimeSeries("Series").series

        workspace.clearWorkspace()
        bar.setBarValues(doubleArrayOf(1.0, 2.0))
        timeSeries.addData(0, 1.0, 1.0)
        UiWork.awaitIdle()

        assertEquals(0, onEdt { bar.getDataset().columnCount })
        assertEquals(0, onEdt { series.itemCount })
    }
}
