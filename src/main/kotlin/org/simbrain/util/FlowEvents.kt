package org.simbrain.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.BiConsumer
import java.util.function.Consumer
import javax.swing.SwingUtilities
import kotlin.time.Duration.Companion.milliseconds

/**
 * Flow-based event bus (Option B spike). Runs in parallel to [Events] and is not yet a replacement; only
 * [org.simbrain.docviewer.DocViewerEvents] has been migrated so far.
 *
 * It separates the two patterns the old [Events] bus conflated into one `wait`-flag mechanism:
 *
 * - **Fire-and-forget pub/sub** ([NoArgEvent], [OneArgEvent], [ChangedEvent]) backed by [MutableSharedFlow].
 *   `fire()` never blocks and is safe from any thread (it `tryEmit`s). Handlers registered with `on()` run on
 *   the Swing EDT by default (so the common "model changed -> repaint" handler is EDT-safe without ceremony);
 *   pass [Dispatchers.Default] to opt out for background work. Throttling/debouncing is delegated to the Flow
 *   [sample]/[debounce] operators. `on()` returns the [Job] of the collector; cancel it to unsubscribe.
 *
 * - **Await-for-completion barrier** ([AwaitableEvent]) for the rare event whose firer must wait until every
 *   handler has finished (serialization ordering, the update loop). `fire()` is a suspend function that returns
 *   only once all handlers complete; [AwaitableEvent.fireAndBlock] is the Java / non-suspend bridge and must not
 *   be called on the EDT.
 *
 * Handler coroutines live on this object's [SupervisorJob], so one failing handler never cancels the scope or
 * its siblings, and [close] cancels everything at end of life.
 */
private val edtDispatcher: CoroutineDispatcher get() = Dispatchers.Swing.immediate

/**
 * Warns (rather than silently freezing the UI) if a blocking fire happens on the Swing EDT. Mirrors the guard
 * on [Events.fireAndBlock]. For [FlowEvents.AwaitableEvent] this is advisory: barrier handlers default to
 * [Dispatchers.Default], so an on-EDT fire only deadlocks if a handler also explicitly requires the EDT.
 */
internal fun warnIfFireAndBlockOnEdt() {
    if (SwingUtilities.isEventDispatchThread()) {
        System.err.println(
            "Warning: fireAndBlock() was called on the Swing event dispatch thread. This blocks the UI and " +
                "can deadlock against EDT handlers. Fire from a coroutine instead."
        )
        Thread.dumpStack()
    }
}

open class FlowEvents : CoroutineScope, AutoCloseable {

    private val job = SupervisorJob()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        System.err.println("Uncaught exception in ${this::class.simpleName} event handler:")
        throwable.printStackTrace()
    }

    override val coroutineContext = Dispatchers.Default + job + exceptionHandler

    /**
     * Cancels this event scope, unsubscribing every handler. Call only at the true end of life of the owning
     * object. See [Events.close] for the undo/redo caveat.
     */
    override fun close() {
        job.cancel()
    }

    enum class TimingMode { Throttle, Debounce }

    @OptIn(FlowPreview::class)
    abstract inner class FlowEvent<T>(val interval: Int, val timingMode: TimingMode) {

        protected val raw = MutableSharedFlow<T>(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.SUSPEND
        )

        /**
         * Timing is applied once and shared by all handlers (matching the old per-event throttle/debounce), so
         * a single shared collector drives the [sample]/[debounce] operator rather than one per subscriber.
         *
         * Note the deliberate edge change for [TimingMode.Throttle]: the old [Events] throttle was leading-edge
         * (deliver the first fire in a window, drop the rest, never deliver a trailing one), whereas [sample] is
         * trailing-edge (drop intermediate fires, deliver the latest value at the end of each window). For the
         * real throttled events — repaint/outline coalescing ([NetworkModelEvents.updateGraphics],
         * [NeuronCollectionEvents.shouldUpdateOutline]) — trailing-edge is what's actually wanted: the last paint
         * reflects the current model state, where leading-edge could leave the final state unpainted until the
         * next fire. A single isolated fire is still delivered (at the next sample tick).
         *
         * Because the shaped flow is shared via [SharingStarted.WhileSubscribed], the upstream [sample]/[debounce]
         * collector starts on the first subscriber and stops when the last one cancels, restarting if a new
         * subscriber arrives. [raw] has no replay, so a fire that lands while there are zero subscribers is
         * dropped — fine for throttled UI refreshes, but a reason to keep barrier/once-only events off this path.
         */
        private val shaped: Flow<T> by lazy {
            if (interval == 0) raw
            else when (timingMode) {
                TimingMode.Throttle -> raw.sample(interval.milliseconds)
                TimingMode.Debounce -> raw.debounce(interval.milliseconds)
            }.shareIn(this@FlowEvents, SharingStarted.WhileSubscribed())
        }

        protected fun onFlow(dispatcher: CoroutineDispatcher, handler: suspend (T) -> Unit): Job =
            shaped.onEach(handler).flowOn(dispatcher).launchIn(this@FlowEvents)
    }

    inner class NoArgEvent(interval: Int = 0, timingMode: TimingMode = TimingMode.Debounce) :
        FlowEvent<Unit>(interval, timingMode) {

        fun fire() { raw.tryEmit(Unit) }

        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: suspend () -> Unit): Job =
            onFlow(dispatcher) { handler() }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: Runnable): Job =
            onFlow(dispatcher) { handler.run() }
    }

    inner class OneArgEvent<T>(interval: Int = 0, timingMode: TimingMode = TimingMode.Debounce) :
        FlowEvent<T>(interval, timingMode) {

        fun fire(value: T) { raw.tryEmit(value) }

        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: suspend (T) -> Unit): Job =
            onFlow(dispatcher, handler)

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: Consumer<T>): Job =
            onFlow(dispatcher) { handler.accept(it) }
    }

    inner class ChangedEvent<T>(interval: Int = 0, timingMode: TimingMode = TimingMode.Debounce) :
        FlowEvent<Pair<T, T>>(interval, timingMode) {

        fun fire(new: T, old: T) { if (new != old) raw.tryEmit(new to old) }

        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: suspend (new: T, old: T) -> Unit): Job =
            onFlow(dispatcher) { (new, old) -> handler(new, old) }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: BiConsumer<T, T>): Job =
            onFlow(dispatcher) { (new, old) -> handler.accept(new, old) }
    }

    inner class AwaitableEvent<T> {

        private val handlers = CopyOnWriteArrayList<suspend (T) -> Unit>()

        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Default, handler: suspend (T) -> Unit): () -> Unit {
            val wrapped: suspend (T) -> Unit = { withContext(dispatcher) { handler(it) } }
            handlers.add(wrapped)
            return { handlers.remove(wrapped) }
        }

        /**
         * Runs every handler to completion before returning, in registration order. Sequential (not concurrent)
         * to match the old `on(wait = true)` semantics, where ordering between handlers can matter (e.g. the
         * XStream post-deserialization wiring in [ConvertedObjectEvent]).
         */
        suspend fun fire(value: T) {
            for (handler in handlers) handler(value)
        }

        fun fireAndBlock(value: T) = runBlocking {
            warnIfFireAndBlockOnEdt()
            fire(value)
        }
    }
}
