/**
 * Frame-driven repainting for chart panels. Stock JFreeChart repaints on every dataset notification,
 * so a chart fed one value per workspace iteration is repainted per data point and, because coupled
 * values are delivered to plots through invokeAndWait, the event thread's paint throughput becomes
 * the ceiling on the whole workspace's iteration rate.
 *
 * [AdaptiveChartRepainter] replaces that with a self-clocking frame loop: chart change events only
 * mark the chart dirty, a frame paints whatever the data is at that moment, and when it finishes
 * drawing the next frame is scheduled if anything changed meanwhile. The frame rate therefore adapts
 * to what the machine sustains — a cheap chart repaints often, an expensive one less — with
 * [minFrameMillis] as a floor so slowly trickling data does not spin frames, and a chart that is not
 * showing paints nothing while still tracking dirtiness for when it reappears.
 */
package org.simbrain.plot

import org.jfree.chart.ChartPanel
import org.jfree.chart.event.ChartChangeEvent
import org.jfree.chart.event.ChartChangeListener
import org.jfree.chart.event.ChartProgressEvent
import org.jfree.chart.event.ChartProgressListener
import org.simbrain.util.MinIntervalGate
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.SwingUtilities

class AdaptiveChartRepainter @JvmOverloads constructor(
    private val chartPanel: ChartPanel,
    private val minFrameMillis: Int = DEFAULT_MIN_FRAME_MILLIS,
    private val staleLatchMillis: Int = STALE_LATCH_MILLIS
) : ChartChangeListener, ChartProgressListener {

    /** Whether the chart changed since the last requested frame. */
    private var dirty = false

    /** Whether a frame has been requested and not yet finished drawing. */
    private var framePending = false

    /** Whether the chart is currently inside a draw, during which its own side effects fire changes. */
    private var drawing = false

    /**
     * When [framePending] or [drawing] was last raised. JFreeChart fires DRAWING_FINISHED outside
     * any finally, so a draw that throws would leave the latches raised forever; a change arriving
     * after [staleLatchMillis] treats them as wedged and reschedules.
     */
    private var latchArmedAt = 0L

    private val frameGate = MinIntervalGate(minFrameMillis) { requestFrame() }

    private val hierarchyListener = HierarchyListener { e ->
        if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L &&
            chartPanel.isShowing && dirty && !framePending && !drawing
        ) {
            scheduleFrame()
        }
    }

    /** Take over the panel's repainting. Call once, after the panel's chart has been set. */
    fun install() {
        val chart = requireNotNull(chartPanel.chart) { "Install after the panel's chart is set" }
        chart.removeChangeListener(chartPanel)
        chart.addChangeListener(this)
        chart.addProgressListener(this)
        chartPanel.addHierarchyListener(hierarchyListener)
    }

    /** Restore the panel's stock repaint-per-notification behavior. */
    fun uninstall() {
        frameGate.stop()
        chartPanel.removeHierarchyListener(hierarchyListener)
        chartPanel.chart?.let {
            it.removeChangeListener(this)
            it.removeProgressListener(this)
            it.addChangeListener(chartPanel)
        }
    }

    override fun chartChanged(event: ChartChangeEvent) {
        // Scripted data can arrive off the event thread; all state lives on it
        if (SwingUtilities.isEventDispatchThread()) markDirty() else SwingUtilities.invokeLater { markDirty() }
    }

    private fun markDirty() {
        dirty = true
        if (!drawing && !framePending) {
            scheduleFrame()
        } else if (System.currentTimeMillis() - latchArmedAt > staleLatchMillis) {
            // A frame this old never finished (a draw threw, or its repaint was swallowed); recover
            drawing = false
            framePending = false
            scheduleFrame()
        }
    }

    private fun scheduleFrame() {
        if (!chartPanel.isShowing) return
        framePending = true
        latchArmedAt = System.currentTimeMillis()
        frameGate.request()
    }

    private fun requestFrame() {
        if (!chartPanel.isShowing) {
            // Went away while the frame was pending; dirtiness survives for the hierarchy listener
            framePending = false
            return
        }
        dirty = false
        chartPanel.setRefreshBuffer(true)
        chartPanel.repaint()
    }

    override fun chartProgress(event: ChartProgressEvent) {
        when (event.type) {
            ChartProgressEvent.DRAWING_STARTED -> {
                drawing = true
                latchArmedAt = System.currentTimeMillis()
            }
            ChartProgressEvent.DRAWING_FINISHED -> {
                drawing = false
                framePending = false
                frameGate.stamp()
                if (dirty) scheduleFrame()
            }
        }
    }

    companion object {
        /** Floor between frames, so slow data streams do not repaint per point; ~33 fps ceiling. */
        const val DEFAULT_MIN_FRAME_MILLIS = 30

        /** How long a raised latch may go without progress before it is presumed wedged. */
        const val STALE_LATCH_MILLIS = 2000
    }
}
