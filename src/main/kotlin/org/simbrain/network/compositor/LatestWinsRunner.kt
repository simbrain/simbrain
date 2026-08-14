package org.simbrain.network.compositor

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Runs submitted blocks on [executor], keeping only the newest while one is in flight — the
 * [LogitLens] drain pattern generalized. Superseded blocks never run, so a rapid burst of
 * submissions coalesces into the latest one.
 */
class LatestWinsRunner(
    private val executor: Executor,
    private val onRan: (() -> Unit)? = null,
) {

    private val pending = AtomicReference<(() -> Unit)?>(null)

    private val draining = AtomicBoolean(false)

    fun submit(block: () -> Unit) {
        pending.set(block)
        if (draining.compareAndSet(false, true)) executor.execute(::drain)
    }

    private fun drain() {
        while (true) {
            val block = pending.getAndSet(null) ?: break
            block()
            onRan?.invoke()
        }
        draining.set(false)
        if (pending.get() != null && draining.compareAndSet(false, true)) executor.execute(::drain)
    }

    companion object {
        /** One daemon worker shared by every compositor, like the logit lens's. */
        val sharedWorker: Executor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "compositor-replay").apply { isDaemon = true }
        }
    }
}
