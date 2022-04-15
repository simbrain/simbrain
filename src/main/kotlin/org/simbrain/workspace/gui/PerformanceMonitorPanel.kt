package org.simbrain.workspace.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.simbrain.util.format
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.updater.*
import java.awt.BorderLayout
import java.awt.Font
import java.lang.Long.max
import java.lang.Long.min
import java.util.*
import javax.swing.*
import kotlin.math.roundToLong

/**
 * Display update action performance and thread monitor.
 *
 * @author jyoshimi, Tim Shea
 */
class PerformanceMonitorPanel(private val workspace: Workspace) : JPanel(BorderLayout()) {

    private val contentPanel = JPanel()

    data class TimerProperties(val identifier: String, val min: Long, val avg: Long, val max: Long, var markForDeletion: Boolean = true)
    data class ThreadAction(val threadName: String, val actionName: String, var markForDeletion: Boolean = true)

    private val timers: MutableMap<Any, TimerProperties> = LinkedHashMap()

    private val threadActions: MutableMap<String, ThreadAction> = TreeMap()

    /**
     * Constructor for viewer panel.
     *
     * @param workspace reference to parent workspace.
     */
    init {
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.X_AXIS)
        val timersList = JList<String>()
        val updateActionPane = JScrollPane(timersList)
        updateActionPane.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Update Actions"),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        )
        timersList.font = Font("Monospaced", Font.PLAIN, 11)
        contentPanel.add(updateActionPane)
        val threadsList = JList<String>()
        val threadPane = JScrollPane(threadsList)
        threadPane.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Threads"),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        )
        contentPanel.add(threadPane)

        this.add("Center", contentPanel)

        workspace.coroutineScope.launch(Dispatchers.Default) {
            PerformanceMonitor.flow.collect { (identifier, name, threadName, nanoTime) ->
                val properties = timers.getOrPut(identifier) { TimerProperties(name, nanoTime, nanoTime, nanoTime) }
                val (_, min, avg, max) = properties
                timers[identifier] = properties.copy(
                    min = min(min, nanoTime),
                    avg = (0.99 * avg + 0.01 * nanoTime).roundToLong(),
                    max = max(max, nanoTime),
                    markForDeletion = false
                )
                threadActions[threadName] = ThreadAction(threadName, name, markForDeletion = false)
            }
        }

        workspace.addNonRemovalAction("performance monitor") {
            timers.entries.filter { (_, record) ->
                record.markForDeletion
            }.forEach { (key) -> timers.remove(key) }

            threadActions.entries.filter { (_, record) ->
                record.markForDeletion
            }.forEach { (key) -> threadActions.remove(key) }

            timersList.model = DefaultListModel<String>().apply { addAll(timers.values.map { record ->
                record.markForDeletion = true
                val (recordName, min, avg, max) = record
                fun Long.toSecondString()  = (this / 1e9).format(5)
                val minSeconds = min.toSecondString()
                val avgSeconds = avg.toSecondString()
                val maxSeconds = max.toSecondString()
                val maxLabelLength = 50
                val formattedRecordName = if (recordName.length > maxLabelLength - 3) {
                    recordName.substring(0, maxLabelLength - 3) + "..."
                } else {
                    recordName
                }
                "%-${maxLabelLength}s min:%s avg:%s max:%s".format(formattedRecordName, minSeconds, avgSeconds, maxSeconds)
            }) }
            timersList.repaint()
            threadsList.model = DefaultListModel<String>().apply { addAll(threadActions.values.map { record ->
                record.markForDeletion = true
                val (threadName, name) = record
                "$threadName: $name"
            }) }
        }

    }
}