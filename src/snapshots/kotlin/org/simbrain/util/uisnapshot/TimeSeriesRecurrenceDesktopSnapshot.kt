package org.simbrain.util.uisnapshot

import org.simbrain.plot.timeseries.RecurrenceMode
import org.simbrain.plot.timeseries.RecurrenceView
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.workspace.Workspace
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import javax.swing.JDesktopPane
import javax.swing.JFrame
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
import kotlin.math.sin

/**
 * The time series desktop frame in the stacked spectrum view — line chart above, per-series
 * recurrence tabs below — assembled through the component factory. Exercises the split-pane layout,
 * the quick-options toolbar, the per-series tab titles, and the stacked-view alignment: shared time
 * ticks between the two plots and the colorbar relocated to the bottom edge.
 */
class TimeSeriesRecurrenceDesktopSnapshot : UiSnapshotDef {
    override val name = "time_series_recurrence_desktop"

    override fun build(): Component {
        val workspace = Workspace()
        workspace.componentFactory.createWorkspaceComponent("Time series")
        val component = workspace.componentList.filterIsInstance<TimeSeriesPlotComponent>().single()
        component.model.recurrenceView = RecurrenceView.BOTH
        component.model.recurrenceMode = RecurrenceMode.SPECTRUM
        component.model.timeSeriesList.forEachIndexed { index, ts ->
            repeat(120) { t ->
                component.model.addData(component.model.timeSeriesList.indexOf(ts), t.toDouble(), sin((t + 10.0 * index) / (5.0 + index)))
            }
        }
        lateinit var host: JFrame
        SwingUtilities.invokeAndWait {
            val frame = GenericJInternalFrame("Time series", true, true, true, true)
            val guiComponent = workspace.componentFactory.createGuiComponent(frame, component)
            frame.contentPane.add(guiComponent)
            frame.setBounds(16, 16, 640, 860)
            frame.isVisible = true
            findTabbedPane(guiComponent)?.selectedIndex = 1
            val desktop = JDesktopPane().apply { preferredSize = Dimension(672, 892) }
            desktop.add(frame)
            host = JFrame().apply { contentPane = desktop }
            host.pack()
        }
        return host
    }

    private fun findTabbedPane(root: Component): JTabbedPane? {
        if (root is JTabbedPane) return root
        if (root !is Container) return null
        return root.components.firstNotNullOfOrNull { findTabbedPane(it) }
    }
}
