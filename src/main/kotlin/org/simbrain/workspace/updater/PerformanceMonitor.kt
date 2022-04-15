package org.simbrain.workspace.updater

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.system.measureNanoTime

object PerformanceMonitor {

    var enabled = false

    private val mutableSharedFlow = MutableSharedFlow<PerformanceMetrics>()

    val flow = mutableSharedFlow.asSharedFlow()

    suspend fun record(identifier: Any, name: String = identifier.toString(), block: suspend () -> Unit) {
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

    suspend operator fun UpdateAction.invoke() {
        record(description?: "(Unnamed Action)") {
            run()
        }
    }
}

data class PerformanceMetrics(val identifier: Any, val name: String, val threadName: String, val nanoTime: Long);