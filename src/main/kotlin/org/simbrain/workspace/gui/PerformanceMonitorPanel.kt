/**
 * The desktop's Performance tab: per-update-action timings and what each thread last ran, collected from
 * [PerformanceMonitor]. It observes the update loop without taking part in it: measurements accumulate as they arrive
 * and the lists refresh a few times a second, only while the tab is showing, so the panel adds no work to an iteration
 * and a paced run does not wait for it to draw.
 */
package org.simbrain.workspace.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.simbrain.util.Theme
import org.simbrain.util.format
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.updater.PerformanceMonitor
import java.awt.BorderLayout
import java.awt.Font
import java.awt.event.HierarchyEvent
import java.util.TreeMap
import javax.swing.BoxLayout
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.Timer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Display update action performance and thread monitor.
 *
 * @author Tim Shea, Yulin Li
 */
class PerformanceMonitorPanel(private val workspace: Workspace) : JPanel(BorderLayout()) {

    private data class TimerStats(val name: String, val min: Long, val avg: Long, val max: Long, val lastSeen: Long)

    private data class ThreadActivity(val actionName: String, val lastSeen: Long)

    /** Guards the maps below, which the collector writes as measurements arrive and the refresh reads on the EDT. */
    private val lock = Any()

    private val timers = LinkedHashMap<Any, TimerStats>()

    private val threadActions = TreeMap<String, ThreadActivity>()

    private var changed = false

    private val timersList = JList<String>().apply { font = Font("Monospaced", Font.PLAIN, 11) }

    private val threadsList = JList<String>()

    private val refreshTimer = Timer(REFRESH_INTERVAL_MS) { refresh() }

    init {
        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.X_AXIS)
        contentPanel.add(JScrollPane(timersList).apply { border = Theme.sectionBorder("Update Actions") })
        contentPanel.add(JScrollPane(threadsList).apply { border = Theme.sectionBorder("Threads") })
        add(contentPanel, BorderLayout.CENTER)

        workspace.launch(Dispatchers.Default) {
            PerformanceMonitor.flow.collect { (identifier, name, threadName, nanoTime) ->
                record(identifier, name, threadName, nanoTime)
            }
        }

        addHierarchyListener { e ->
            if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                if (isShowing) refreshTimer.start() else refreshTimer.stop()
            }
        }
    }

    private fun record(identifier: Any, name: String, threadName: String, nanoTime: Long) {
        val now = System.nanoTime()
        synchronized(lock) {
            val previous = timers[identifier]
            timers[identifier] = previous?.copy(
                min = min(previous.min, nanoTime),
                avg = (0.99 * previous.avg + 0.01 * nanoTime).roundToLong(),
                max = max(previous.max, nanoTime),
                lastSeen = now
            ) ?: TimerStats(name, nanoTime, nanoTime, nanoTime, now)
            threadActions[threadName] = ThreadActivity(name, now)
            changed = true
        }
    }

    /**
     * Shows the latest statistics. Actions and threads that have stopped reporting while others still do are dropped;
     * when nothing reports, as when the simulation is stopped, the lists keep their last contents.
     */
    private fun refresh() {
        val (timerLines, threadLines) = synchronized(lock) {
            if (!changed) return
            changed = false
            val newest = timers.values.maxOf { it.lastSeen }
            timers.values.removeIf { newest - it.lastSeen > STALE_NANOS }
            threadActions.values.removeIf { newest - it.lastSeen > STALE_NANOS }
            timers.values.map { it.describe() } to threadActions.map { (thread, activity) -> "$thread: ${activity.actionName}" }
        }
        timersList.setListData(timerLines.toTypedArray())
        threadsList.setListData(threadLines.toTypedArray())
    }

    private fun TimerStats.describe(): String {
        fun Long.toSecondString() = (this / 1e9).format(5)
        val label = if (name.length > MAX_LABEL_LENGTH - 3) name.substring(0, MAX_LABEL_LENGTH - 3) + "..." else name
        return "%-${MAX_LABEL_LENGTH}s min:%s avg:%s max:%s".format(
            label, min.toSecondString(), avg.toSecondString(), max.toSecondString()
        )
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 250
        private const val STALE_NANOS = 2_000_000_000L
        private const val MAX_LABEL_LENGTH = 50
    }
}
