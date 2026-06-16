package org.simbrain.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.BiConsumer
import java.util.function.Consumer
import javax.swing.SwingUtilities
import kotlin.time.Duration.Companion.milliseconds

/**
 * Flow-based event bus: the single event bus for all model and UI events.
 *
 * It separates the two patterns the previous coroutine event bus conflated into one `wait`-flag mechanism:
 *
 * - **Fire-and-forget pub/sub** ([NoArgEvent], [OneArgEvent], [ChangedEvent], [BatchOneArgEvent]). Handlers
 *   register SYNCHRONOUSLY: `on()` adds the handler to a [CopyOnWriteArrayList] the instant it returns (like the
 *   old bus), so a `fire()` immediately after `on()` can never miss it. `fire()` never blocks and is safe from
 *   any thread. Handlers run on the Swing EDT by default (so the common "model changed -> repaint" handler is
 *   EDT-safe without ceremony); pass [Dispatchers.Default] to opt out for background work. Un-throttled events
 *   dispatch directly from `fire()`; throttled/debounced events feed a [MutableSharedFlow] driven by a single
 *   eager collector that applies [sample]/[debounce] once and fans out to the handlers. `on()` returns a [Job];
 *   cancel it to unsubscribe.
 *
 * - **Await-for-completion barrier** ([AwaitableEvent], [NoArgAwaitableEvent]) for the rare event whose firer
 *   must wait until every handler has finished (serialization ordering, the update loop). `fire()` is a suspend
 *   function that returns only once all handlers complete; [AwaitableEvent.fireAndBlock] is the Java / non-suspend
 *   bridge and must not be called on the EDT, and [AwaitableEvent.fireAsync] returns an awaitable from a
 *   non-suspend caller.
 *
 * Handler coroutines live on this object's [SupervisorJob], so one failing handler never cancels the scope or
 * its siblings, and [close] cancels everything at end of life.
 */
private val edtDispatcher: CoroutineDispatcher get() = Dispatchers.Swing.immediate

/**
 * Warns (rather than silently freezing the UI) if a blocking fire happens on the Swing EDT. For
 * [FlowEvents.AwaitableEvent] this is advisory: barrier handlers default to [Dispatchers.Default], so an on-EDT
 * fire only deadlocks if a handler also explicitly requires the EDT.
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
     * object (e.g. [org.simbrain.workspace.WorkspaceComponent.close]) — NOT on model delete(): undo/redo reuses
     * the same model instances, and a cancelled scope can never fire again, so closing on delete would leave
     * resurrected models permanently inert.
     */
    override fun close() {
        job.cancel()
    }

    enum class TimingMode { Throttle, Debounce }

    /**
     * Base for the fire-and-forget pub/sub events. Handlers register SYNCHRONOUSLY in [handlers], so a `fire()`
     * that immediately follows an `on()` can never miss the handler — the gap that a per-handler async
     * `launchIn` collector left open. Un-throttled events ([interval] == 0) dispatch directly; throttled and
     * debounced events feed [raw] and a single eager collector applies the timing operator and fans out.
     */
    @OptIn(FlowPreview::class)
    abstract inner class FlowEvent<T>(val interval: Int, val timingMode: TimingMode) {

        private val handlers = CopyOnWriteArrayList<Pair<CoroutineDispatcher, suspend (T) -> Unit>>()

        private val raw by lazy {
            MutableSharedFlow<T>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.SUSPEND)
        }

        init {
            // Throttling/debouncing needs a timer, so it runs through [raw] and one eager collector that shapes
            // and fans out. [TimingMode.Throttle] uses [sample] (trailing-edge: drop intermediate fires, deliver
            // the latest at the end of each window) — deliberately unlike the old leading-edge throttle, since the
            // real throttled events coalesce repaint/outline requests and want the final state painted.
            if (interval != 0) {
                when (timingMode) {
                    TimingMode.Throttle -> raw.sample(interval.milliseconds)
                    TimingMode.Debounce -> raw.debounce(interval.milliseconds)
                }.onEach { dispatch(it) }.launchIn(this@FlowEvents)
            }
        }

        private fun dispatch(value: T) {
            handlers.forEach { (dispatcher, handler) -> launch(dispatcher) { handler(value) } }
        }

        /** Deliver [value] to all current handlers: directly when un-throttled, else through the shaping collector. */
        protected fun fireShaped(value: T) {
            if (interval == 0) dispatch(value) else raw.tryEmit(value)
        }

        protected fun onFlow(dispatcher: CoroutineDispatcher, handler: suspend (T) -> Unit): Job {
            val entry = dispatcher to handler
            handlers.add(entry)
            return Job().apply { invokeOnCompletion { handlers.remove(entry) } }
        }
    }

    inner class NoArgEvent(interval: Int = 0, timingMode: TimingMode = TimingMode.Debounce) :
        FlowEvent<Unit>(interval, timingMode) {

        fun fire() = fireShaped(Unit)

        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: suspend () -> Unit): Job =
            onFlow(dispatcher) { handler() }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: Runnable): Job =
            onFlow(dispatcher) { handler.run() }
    }

    inner class OneArgEvent<T>(interval: Int = 0, timingMode: TimingMode = TimingMode.Debounce) :
        FlowEvent<T>(interval, timingMode) {

        fun fire(value: T) = fireShaped(value)

        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: suspend (T) -> Unit): Job =
            onFlow(dispatcher, handler)

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: Consumer<T>): Job =
            onFlow(dispatcher) { handler.accept(it) }
    }

    inner class ChangedEvent<T>(interval: Int = 0, timingMode: TimingMode = TimingMode.Debounce) :
        FlowEvent<Pair<T, T>>(interval, timingMode) {

        fun fire(new: T, old: T) { if (new != old) fireShaped(new to old) }

        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: suspend (new: T, old: T) -> Unit): Job =
            onFlow(dispatcher) { (new, old) -> handler(new, old) }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: BiConsumer<T, T>): Job =
            onFlow(dispatcher) { (new, old) -> handler.accept(new, old) }
    }

    /**
     * Accumulates every value fired during a timing window and delivers them to handlers as one batch (a
     * [List]), once per window — the batch analogue of [OneArgEvent]. Use when every fired value must be handled
     * (e.g. removing each deleted node) but the handling can be coalesced, unlike the plain throttled/debounced
     * events which keep only the latest value. Batch order is not significant.
     *
     * Like the other pub/sub events, handlers register synchronously. The eager collector always drains the
     * buffer (so it can never grow unbounded, even with no subscriber — matching the previous event bus's batch,
     * whose flush ran regardless of handlers); a batch is delivered only while a handler is registered.
     */
    @OptIn(FlowPreview::class)
    inner class BatchOneArgEvent<T>(val interval: Int, val timingMode: TimingMode = TimingMode.Debounce) {

        private val raw = MutableSharedFlow<T>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.SUSPEND)
        private val buffer = ConcurrentLinkedQueue<T>()
        private val handlers = CopyOnWriteArrayList<Pair<CoroutineDispatcher, suspend (List<T>) -> Unit>>()

        private fun drain(): List<T> = buildList {
            while (true) add(buffer.poll() ?: break)
        }

        init {
            val ticks = if (interval == 0) raw else when (timingMode) {
                TimingMode.Throttle -> raw.sample(interval.milliseconds)
                TimingMode.Debounce -> raw.debounce(interval.milliseconds)
            }
            ticks.onEach {
                val batch = drain()
                if (batch.isNotEmpty()) {
                    handlers.forEach { (dispatcher, handler) -> launch(dispatcher) { handler(batch) } }
                }
            }.launchIn(this@FlowEvents)
        }

        fun fire(value: T) {
            buffer.add(value)
            raw.tryEmit(value)
        }

        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: suspend (List<T>) -> Unit): Job {
            val entry = dispatcher to handler
            handlers.add(entry)
            return Job().apply { invokeOnCompletion { handlers.remove(entry) } }
        }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = edtDispatcher, handler: Consumer<List<T>>): Job =
            on(dispatcher) { handler.accept(it) }
    }

    inner class AwaitableEvent<T> {

        private val handlers = CopyOnWriteArrayList<suspend (T) -> Unit>()

        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Default, handler: suspend (T) -> Unit): () -> Unit {
            val wrapped: suspend (T) -> Unit = { withContext(dispatcher) { handler(it) } }
            handlers.add(wrapped)
            return { handlers.remove(wrapped) }
        }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Default, handler: Consumer<T>): () -> Unit =
            on(dispatcher) { handler.accept(it) }

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

        /**
         * Fires from a non-suspend caller and returns a [Deferred] that completes once every handler has run.
         * Await it for the barrier (e.g. wait until a model's node has been created); ignore it for
         * fire-and-forget. Unlike [fireAndBlock] it never blocks the calling thread, so it is safe on the EDT.
         */
        fun fireAsync(value: T): Deferred<Unit> = async { fire(value) }
    }

    /**
     * No-argument [AwaitableEvent]: same barrier semantics, for events whose firer must wait for all handlers
     * but which carry no payload (e.g. the network update -> repaint barrier).
     */
    inner class NoArgAwaitableEvent {

        private val delegate = AwaitableEvent<Unit>()

        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Default, handler: suspend () -> Unit): () -> Unit =
            delegate.on(dispatcher) { handler() }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Default, handler: Runnable): () -> Unit =
            delegate.on(dispatcher) { handler.run() }

        suspend fun fire() = delegate.fire(Unit)

        fun fireAndBlock() = delegate.fireAndBlock(Unit)

        fun fireAsync(): Deferred<Unit> = delegate.fireAsync(Unit)
    }
}
