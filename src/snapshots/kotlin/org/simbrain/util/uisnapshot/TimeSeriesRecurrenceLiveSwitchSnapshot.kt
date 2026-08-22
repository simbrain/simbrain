package org.simbrain.util.uisnapshot

import org.simbrain.plot.timeseries.RecurrencePanel
import org.simbrain.plot.timeseries.RecurrenceView
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.workspace.Workspace
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDesktopPane
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.math.sin

/**
 * The stacked view reached the way a user reaches it live: the GUI is built in the default
 * time-series-only view, data flows, the view switches to stacked through the same
 * model-mutate-plus-fire path the toolbar uses, and more data arrives afterwards. This once caught
 * the recurrence time axis freezing at its switch-time range while the line chart scrolled on — the
 * alignment must keep following the line chart, not capture its range once.
 */
class TimeSeriesRecurrenceLiveSwitchSnapshot : UiSnapshotDef {
    override val name = "time_series_recurrence_live_switch"

    override fun build(): Component {
        val workspace = Workspace()
        workspace.componentFactory.createWorkspaceComponent("Time series")
        val component = workspace.componentList.filterIsInstance<TimeSeriesPlotComponent>().single()

        lateinit var host: JFrame
        SwingUtilities.invokeAndWait {
            val frame = GenericJInternalFrame("Time series", true, true, true, true)
            frame.contentPane.add(workspace.componentFactory.createGuiComponent(frame, component))
            // The frame starts at single-plot size; switching to the stacked view grows it toward
            // the comfortable two-plot height, clamped to this desktop pane
            frame.setBounds(16, 16, 640, 520)
            frame.isVisible = true
            val desktop = JDesktopPane().apply { preferredSize = Dimension(672, 780) }
            desktop.add(frame)
            host = JFrame().apply { contentPane = desktop }
            host.pack()
            host.isVisible = false
        }

        SwingUtilities.invokeAndWait {
            component.model.timeSeriesList.forEachIndexed { index, _ ->
                repeat(80) { t -> component.model.addData(index, t.toDouble(), sin((t + 10.0 * index) / (5.0 + index))) }
            }
        }

        SwingUtilities.invokeAndWait {
            component.model.recurrenceView = RecurrenceView.BOTH
            component.model.events.propertyChanged.fire()
        }
        // The propertyChanged handler is dispatched as a coroutine on the event thread; flush it
        repeat(10) {
            Thread.sleep(50)
            SwingUtilities.invokeAndWait { }
        }

        SwingUtilities.invokeAndWait {
            component.model.timeSeriesList.forEachIndexed { index, _ ->
                repeat(40) { t -> component.model.addData(index, 80.0 + t, sin((80 + t + 10.0 * index) / (5.0 + index))) }
            }
        }
        repeat(10) {
            Thread.sleep(50)
            SwingUtilities.invokeAndWait { }
        }
        // Offscreen the panels are never "showing", so the refresh their dataset listener would run
        // in a real window is invoked directly, mimicking the live per-iteration refresh
        SwingUtilities.invokeAndWait {
            findRecurrencePanels(host).forEach { it.refresh() }
        }

        return host
    }

    private fun findRecurrencePanels(root: Component): List<RecurrencePanel> {
        if (root is RecurrencePanel) return listOf(root)
        if (root !is java.awt.Container) return emptyList()
        return root.components.flatMap { findRecurrencePanels(it) }
    }
}
