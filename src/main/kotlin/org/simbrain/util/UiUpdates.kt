/**
 * Non-blocking hand-off of work to the EDT. Model code and background threads post to these and move on; the EDT
 * picks the work up in coalesced batches when it gets to it. They exist because awaiting the EDT once per item or
 * per iteration (a Swing handler on an awaited event, `invokeAndWait`, `withContext(Swing)` in a coupling
 * consumer) makes that code wait behind whatever the EDT is painting, pacing simulations and bulk edits by the
 * display.
 *
 * - [UiRefresh]: "something changed, redraw" — requests collapse into one run of a block that reads current state.
 * - [UiLatest]: like [UiRefresh] but carries a value; only the newest one posted is applied.
 * - [UiInbox]: every posted item is kept, in order, and handed to the drain in batches.
 *
 * Each is created from a [CoroutineScope] (like `launch`), and its lifetime is that scope's: once the scope is
 * cancelled, pending work is dropped and posting does nothing. Nothing runs, and no coroutine exists, until the
 * first post, so headless workspaces pay nothing. [onUi] and [onUiBatch] subscribe an event straight into one.
 *
 * Every run happens on a later EDT turn, never inline (even when posted from the EDT), and after the EDT tasks
 * already queued when it was posted, so a refresh sees state updates those tasks apply. [minIntervalMs] caps how
 * often a block runs; the first run after a quiet period is never delayed.
 */
package org.simbrain.util

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

/** Default cap for view refreshes: about one per display frame. */
const val UI_FRAME_MS = 16L

/**
 * Counts EDT work that has been posted but not yet run, across every primitive in this file, so tests and saves
 * can wait for posted updates to land.
 */
object UiWork {

    private val inFlight = AtomicInteger()

    /** Primitives with work scheduled but not yet finished, so a display sync can run it right away. */
    private val active: MutableSet<UiPoster> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    internal fun begin(poster: UiPoster) {
        inFlight.incrementAndGet()
        active += poster
    }

    internal fun end(poster: UiPoster) {
        active -= poster
        inFlight.decrementAndGet()
    }

    /**
     * Runs every pending update now instead of when its rate cap next allows, so a caller about to paint (such as a
     * per-iteration display sync) draws the latest state. Must be called on the EDT.
     */
    fun flushPending() {
        check(SwingUtilities.isEventDispatchThread()) { "UiWork.flushPending must run on the EDT" }
        active.toList().forEach { it.flush() }
    }

    val isIdle get() = inFlight.get() == 0

    /**
     * Blocks until every posted update has run, or [timeoutMs] passes. Must not be called on the EDT, which is
     * what runs the work.
     */
    fun awaitIdle(timeoutMs: Long = 10_000) {
        check(!SwingUtilities.isEventDispatchThread()) { "UiWork.awaitIdle would block the EDT it waits for" }
        val deadline = System.currentTimeMillis() + timeoutMs
        do {
            SwingUtilities.invokeAndWait {}
            if (isIdle) return
            Thread.sleep(1)
        } while (System.currentTimeMillis() < deadline)
    }
}

/**
 * Runs [block] on the EDT, blocking the caller until it finishes. Only for the rare caller that genuinely needs the
 * result applied before it continues (saving, one-off setup); per-item or per-iteration code should post to a
 * [UiRefresh], [UiLatest] or [UiInbox] instead.
 */
fun blockOnEdt(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeAndWait(block)
}

/**
 * The shared run loop: at most one drain coroutine per primitive, started on demand and ending when nothing is
 * left to do, so an idle primitive holds no coroutine.
 */
abstract class UiPoster internal constructor(private val scope: CoroutineScope, private val minIntervalMs: Long) {

    private val running = AtomicBoolean(false)

    @Volatile
    private var lastRun = 0L

    /** Runs the pending work on the EDT; returns false when there was none. */
    protected abstract fun runPending(): Boolean

    protected abstract fun hasPending(): Boolean

    /** Drops pending work without running it. */
    protected abstract fun dropPending()

    /**
     * Applies anything pending right now, blocking until it has run on the EDT. For saves and other callers that
     * must see every post land; runs directly when called on the EDT.
     */
    fun flush() = blockOnEdt { runPending() }

    protected fun schedule() {
        if (!scope.isActive) {
            dropPending()
            return
        }
        if (!running.compareAndSet(false, true)) return
        UiWork.begin(this)
        val job = scope.launch(Dispatchers.Swing) {
            while (true) {
                val wait = lastRun + minIntervalMs - System.currentTimeMillis()
                if (wait > 0) delay(wait)
                // Go to the back of the EDT queue so this run follows the tasks queued before it was requested
                yield()
                val ran = try {
                    runPending()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    System.err.println("Uncaught exception in ${this@UiPoster::class.simpleName} block:")
                    e.printStackTrace()
                    true
                }
                if (!ran) break
                lastRun = System.currentTimeMillis()
            }
        }
        job.invokeOnCompletion { cause ->
            running.set(false)
            UiWork.end(this)
            if (cause != null) dropPending()
            // A post that saw the loop still running just before it exited would otherwise be stranded
            else if (hasPending()) schedule()
        }
    }
}

class UiRefresh internal constructor(scope: CoroutineScope, minIntervalMs: Long, private val block: () -> Unit) :
    UiPoster(scope, minIntervalMs) {

    private val dirty = AtomicBoolean(false)

    /** Asks for one run of the block; requests made before it runs collapse into that run. Never blocks. */
    fun request() {
        dirty.set(true)
        schedule()
    }

    override fun runPending(): Boolean {
        if (!dirty.getAndSet(false)) return false
        block()
        return true
    }

    override fun hasPending() = dirty.get()

    override fun dropPending() = dirty.set(false)
}

class UiLatest<T> internal constructor(scope: CoroutineScope, minIntervalMs: Long, private val apply: (T) -> Unit) :
    UiPoster(scope, minIntervalMs) {

    private object None

    private val latest = AtomicReference<Any?>(None)

    /** Replaces whatever value is waiting to be applied. Never blocks. */
    fun post(value: T) {
        latest.set(value)
        schedule()
    }

    @Suppress("UNCHECKED_CAST")
    override fun runPending(): Boolean {
        val value = latest.getAndSet(None)
        if (value === None) return false
        apply(value as T)
        return true
    }

    override fun hasPending() = latest.get() !== None

    override fun dropPending() = latest.set(None)
}

class UiInbox<T> internal constructor(scope: CoroutineScope, minIntervalMs: Long, private val drain: (List<T>) -> Unit) :
    UiPoster(scope, minIntervalMs) {

    private val queue = ConcurrentLinkedQueue<T>()

    /** Queues [item] for the next drain. Never blocks. */
    fun post(item: T) {
        queue.add(item)
        schedule()
    }

    /** Discards queued items without draining them, e.g. when the data they would be added to is cleared. */
    fun clear() = queue.clear()

    override fun runPending(): Boolean {
        val batch = buildList { while (true) add(queue.poll() ?: break) }
        if (batch.isEmpty()) return false
        drain(batch)
        return true
    }

    override fun hasPending() = queue.isNotEmpty()

    override fun dropPending() = queue.clear()
}

fun CoroutineScope.uiRefresh(minIntervalMs: Long = UI_FRAME_MS, block: () -> Unit) =
    UiRefresh(this, minIntervalMs, block)

fun <T> CoroutineScope.uiLatest(minIntervalMs: Long = UI_FRAME_MS, apply: (T) -> Unit) =
    UiLatest(this, minIntervalMs, apply)

fun <T> CoroutineScope.uiInbox(minIntervalMs: Long = 0, drain: (List<T>) -> Unit) =
    UiInbox(this, minIntervalMs, drain)

/**
 * A scope for view-owned [UiRefresh]/[UiLatest]/[UiInbox] instances, to be cancelled when the view goes away.
 * Its dispatcher is irrelevant: the primitives always run on the EDT.
 */
fun UiScope(): CoroutineScope = CoroutineScope(SupervisorJob())

/** Ties an event subscription's removal to [scope]'s lifetime and returns a Job that also removes it. */
private fun subscribedUntil(scope: CoroutineScope, unsubscribe: () -> Unit): Job {
    val subscription = Job(scope.coroutineContext[Job])
    subscription.invokeOnCompletion { unsubscribe() }
    return subscription
}

/**
 * Redraws on the EDT after this event, coalescing fires into at most one run per [minIntervalMs]. The event's
 * fire never waits on the EDT. Use for view refreshes only: a caller that relies on the handler having finished
 * when fire returns needs the awaited [FlowEvents.NoArgAwaitableEvent.on].
 */
fun FlowEvents.NoArgAwaitableEvent.onUi(scope: CoroutineScope, minIntervalMs: Long = UI_FRAME_MS, block: () -> Unit): Job {
    val refresh = scope.uiRefresh(minIntervalMs, block)
    return subscribedUntil(scope, on(Dispatchers.Unconfined) { refresh.request() })
}

/** [onUi] for a valued awaitable event: only the newest value since the last run is applied. */
fun <T> FlowEvents.AwaitableEvent<T>.onUi(scope: CoroutineScope, minIntervalMs: Long = UI_FRAME_MS, apply: (T) -> Unit): Job {
    val latest = scope.uiLatest(minIntervalMs, apply)
    return subscribedUntil(scope, on(Dispatchers.Unconfined) { latest.post(it) })
}

/** Every value fired, in order, handed to [drain] in batches on the EDT. The event's fire never waits on the EDT. */
fun <T> FlowEvents.AwaitableEvent<T>.onUiBatch(scope: CoroutineScope, minIntervalMs: Long = 0, drain: (List<T>) -> Unit): Job {
    val inbox = scope.uiInbox(minIntervalMs, drain)
    return subscribedUntil(scope, on(Dispatchers.Unconfined) { inbox.post(it) })
}

fun FlowEvents.NoArgEvent.onUi(scope: CoroutineScope, minIntervalMs: Long = UI_FRAME_MS, block: () -> Unit): Job {
    val refresh = scope.uiRefresh(minIntervalMs, block)
    val handler = on(Dispatchers.Unconfined) { refresh.request() }
    return subscribedUntil(scope) { handler.cancel() }
}

fun <T> FlowEvents.OneArgEvent<T>.onUi(scope: CoroutineScope, minIntervalMs: Long = UI_FRAME_MS, apply: (T) -> Unit): Job {
    val latest = scope.uiLatest(minIntervalMs, apply)
    val handler = on(Dispatchers.Unconfined) { latest.post(it) }
    return subscribedUntil(scope) { handler.cancel() }
}

fun <T> FlowEvents.OneArgEvent<T>.onUiBatch(scope: CoroutineScope, minIntervalMs: Long = 0, drain: (List<T>) -> Unit): Job {
    val inbox = scope.uiInbox(minIntervalMs, drain)
    val handler = on(Dispatchers.Unconfined) { inbox.post(it) }
    return subscribedUntil(scope) { handler.cancel() }
}
