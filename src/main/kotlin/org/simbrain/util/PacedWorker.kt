/**
 * A single background worker fed through a rendezvous channel, for consumers whose reaction runs at
 * wall-clock speed (audio playback, long-running synthesis) rather than simulation speed. [submit]
 * suspends until the worker is ready for the next item, so a running simulation is paced to the
 * reaction rate instead of queueing work without bound; do not give the channel capacity or drop-on-full
 * behavior without revisiting that contract at each call site. [reset] discards whatever is queued or
 * suspended and starts fresh, which is what a stop or flush wants; owners tied to a workspace component
 * must [close] the worker when the component closes.
 */
package org.simbrain.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class PacedWorker<T>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val process: suspend (T) -> Unit
) {

    @Volatile
    private var scope = newScope()

    @Volatile
    private var channel = newChannel()

    init {
        startWorker()
    }

    /**
     * Hand one item to the worker, suspending until the worker is ready for it. An item submitted while
     * the worker is [reset] or [close]d is dropped silently — both mean pending work is no longer
     * wanted — including a submit already suspended when the reset happens. Cancellation of the calling
     * coroutine itself still propagates.
     */
    suspend fun submit(item: T) {
        try {
            channel.send(item)
        } catch (e: ClosedSendChannelException) {
            // Reset or close discards pending work by design
        } catch (e: CancellationException) {
            // Distinguish the channel's cancellation (reset/close: drop the item) from the calling
            // coroutine's own cancellation, which must propagate
            currentCoroutineContext().ensureActive()
        }
    }

    /**
     * Discard queued and suspended items, cancel the in-flight [process] call cooperatively, and start a
     * fresh worker. A process body doing non-cancellable blocking work must watch its own abort flag.
     */
    fun reset() {
        scope.cancel()
        // cancel(), not close(): only cancellation resumes senders already suspended in submit
        channel.cancel()
        scope = newScope()
        channel = newChannel()
        startWorker()
    }

    /**
     * Permanently stop the worker; subsequent [submit]s are dropped, as is a submit suspended right now.
     */
    fun close() {
        scope.cancel()
        channel.cancel()
    }

    private fun newScope() = CoroutineScope(dispatcher + SupervisorJob())

    private fun newChannel() = Channel<T>(Channel.RENDEZVOUS)

    private fun startWorker() {
        val current = channel
        scope.launch {
            for (item in current) {
                process(item)
            }
        }
    }
}
