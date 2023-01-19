package org.simbrain.workspace.updater

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.simbrain.workspace.gui.PerformanceMonitorPanel
import kotlin.system.measureNanoTime

/**
 * A singleton object that can be used to track how long blocks of code take to execute. Used by the
 * [PerformanceMonitorPanel] to display performance statistics.
 */
object PerformanceMonitor {

    /**
     * The monitor consumes resources so by default it is disabled. It is only enabled when the
     * [PerformanceMonitorPanel] is visible.
     */
    var enabled = false

    /**
     * Asynchronously buffers measured events for tracking their performance stats.
     */
    private val mutableSharedFlow = MutableSharedFlow<PerformanceMetrics>()
    val flow = mutableSharedFlow.asSharedFlow()

    /**
     * Record the time a provided block takes to execute.
     */
    suspend fun record(
        /**
         * An object which identifies the "type" of the event, so that statistics can be gathered like average, max,
         * and min time it takes that type of event to execute.
         */
        identifier: Any,
        /**
         * String description for [PerformanceMonitorPanel].
         */
        name: String = identifier.toString(),
        /**
         * The block to be executed and measured.
         */
        block: suspend () -> Unit
    ) {
        if (enabled) {
            val nanoTime = measureNanoTime {
                block()
            }
            val thread = Thread.currentThread()

            mutableSharedFlow.emit(PerformanceMetrics(identifier, name, thread.name, nanoTime))
        } else {
            block()
        }
    }

    /**
     * Convenient way to invoke actions and record their performance.
     */
    suspend operator fun UpdateAction.invoke() {
        record(this, description?: "(Unnamed Action)") {
            run()
        }
    }
}

data class PerformanceMetrics(val identifier: Any, val name: String, val threadName: String, val nanoTime: Long);