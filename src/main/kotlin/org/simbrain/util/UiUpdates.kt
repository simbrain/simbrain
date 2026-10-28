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
 * first post, so headless workspaces pay nothing. [onUi] and [onUiBatch] subscribe an event straight into one, and
 * [EdtUpdates] is the [UiInbox] a model keeps for its own dataset.
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
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

/** Default cap for view refreshes: about one per display frame. */
const val UI_FRAME_MS = 16L

/**
 * Default cadence for coalescing GUI refreshes caused by very fast model updates.
 *
 * This was introduced to prevent language-model token generation from being paced by the display.
 * It is kept generic because other high-rate simulations can use the same protection.
 *
 * Use [UI_FRAME_MS] for views that should keep up with the display, and this for views whose source fires far faster
 * than anyone can read (per-token text, per-iteration labels) and whose block is costly.
 */
const val HIGH_RATE_GUI_REFRESH_INTERVAL_MS = 33L

/**
 * Tracks the primitives in this file that have EDT work scheduled, so a display sync can run it right away and tests
 * can wait for posted updates to land.
 */
object UiWork {

    /** Primitives with work scheduled but not yet finished, so a display sync can run it right away. */
    private val active: MutableSet<UiPoster> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    internal fun begin(poster: UiPoster) {
        active += poster
    }

    internal fun end(poster: UiPoster) {
        active -= poster
    }

    /** The primitives with work scheduled, for diagnostics. */
    internal fun activePosters(): List<UiPoster> = active.toList()

    /**
     * Runs every pending update now instead of when its rate cap next allows, so a caller about to paint (such as a
     * per-iteration display sync) draws the latest state. Must be called on the EDT.
     */
    fun flushPending() {
        check(SwingUtilities.isEventDispatchThread()) { "UiWork.flushPending must run on the EDT" }
        active.toList().forEach { it.flush() }
    }

    /**
     * For tests: blocks until every posted update has run and every run loop has ended. Pending work is run now
     * rather than when its rate cap next allows, so the wait is at most the trailing check each loop makes one
     * interval after its last run. Throws, naming the primitives still scheduled, if work keeps arriving for
     * [timeoutMs]. Must not be called on the EDT, which is what runs the work.
     */
    fun awaitIdle(timeoutMs: Long = 10_000) {
        check(!SwingUtilities.isEventDispatchThread()) { "UiWork.awaitIdle would block the EDT it waits for" }
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            // Checked on the EDT, where a run loop ending and rescheduling for a late post happens in one step
            var scheduled = emptyList<UiPoster>()
            SwingUtilities.invokeAndWait {
                flushPending()
                scheduled = active.toList()
            }
            if (scheduled.isEmpty()) return
            check(System.currentTimeMillis() < deadline) {
                "UI updates still scheduled after $timeoutMs ms: ${scheduled.joinToString()}"
            }
            Thread.sleep(1)
        }
    }
}

/**
 * The shared run loop: at most one drain coroutine per primitive, started on demand and ending when nothing is
 * left to do, so an idle primitive holds no coroutine.
 */
sealed class UiPoster(private val scope: CoroutineScope, private val minIntervalMs: Long) {

    private val running = AtomicBoolean(false)

    @Volatile
    private var lastRun = 0L

    /** The block this primitive runs, named in diagnostics by its class. */
    protected abstract val work: Any

    /** Runs the pending work on the EDT; returns false when there was none. */
    protected abstract fun runPending(): Boolean

    internal abstract fun hasPending(): Boolean

    /** Drops pending work without running it. */
    protected abstract fun dropPending()

    /** Whether a run loop is scheduled or running. */
    internal val isRunning get() = running.get()

    /**
     * Applies anything pending right now, blocking until it has run on the EDT. For saves and other callers that
     * must see every post land; runs directly when called on the EDT.
     */
    fun flush() = runOnEventThread { runPendingLogged() }

    /**
     * [runPending], logging a failure instead of throwing it, so a broken view block neither ends a paced run (whose
     * display sync flushes every primitive) nor stops the run loop. Returns true after a failure, since work ran.
     */
    private fun runPendingLogged(): Boolean = try {
        runPending()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logHandlerFailure("${this::class.simpleName} block", e)
        true
    }

    override fun toString() = "${this::class.simpleName}(${work.javaClass.name})"

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
                val ran = runPendingLogged()
                if (!ran) break
                lastRun = System.currentTimeMillis()
            }
        }
        job.invokeOnCompletion { cause ->
            // Leave the active set before allowing a new loop: the other order lets a post start one, register it,
            // and then lose the registration here, so display syncs would skip this primitive for as long as posts
            // keep that loop alive. A post that arrives between the two is picked up by the schedule below
            UiWork.end(this)
            running.set(false)
            if (cause != null) dropPending()
            // A post that saw the loop still running just before it exited would otherwise be stranded
            else if (hasPending()) schedule()
        }
    }
}

class UiRefresh internal constructor(scope: CoroutineScope, minIntervalMs: Long, private val block: () -> Unit) :
    UiPoster(scope, minIntervalMs) {

    private val dirty = AtomicBoolean(false)

    override val work get() = block

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

    override val work get() = apply

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

    override val work get() = drain

    /** Queues [item] for the next drain. Never blocks. */
    fun post(item: T) {
        queue.add(item)
        schedule()
    }

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
 * A model's changes to an EDT-confined dataset, such as a chart's, applied on the EDT in the order they were made
 * without the caller waiting. Each model holds one in a `@Transient` field: [flush] lands what is still queued
 * before a save, [awaitUpdates] lets a script read the dataset after its own changes, and [close], called when the
 * model's component leaves the workspace, drops what is pending and stops later posts.
 */
class EdtUpdates<T>(drain: (List<T>) -> Unit) : AutoCloseable {

    private val scope = UiScope()

    private val inbox = scope.uiInbox(drain = drain)

    /** Queues [update] for the EDT. Never blocks. */
    fun post(update: T) = inbox.post(update)

    /** Applies everything queued, blocking until it has run on the EDT; runs directly when called on the EDT. */
    fun flush() = inbox.flush()

    /** Suspends until everything queued so far has been applied. */
    suspend fun awaitUpdates() = withContext(Dispatchers.Swing) { inbox.flush() }

    override fun close() = scope.cancel()
}

/**
 * A scope for view-owned [UiRefresh]/[UiLatest]/[UiInbox] instances, to be cancelled when the view goes away.
 * Its dispatcher is irrelevant: the primitives always run on the EDT.
 */
fun UiScope(): CoroutineScope = CoroutineScope(SupervisorJob())

/**
 * Runs [unsubscribe] when [scope] ends, or when the returned Job is cancelled first. The Job is a child of [scope],
 * so cancelling it early also detaches it and a long-lived scope does not accumulate ended subscriptions. Every
 * scope-bound subscription in this file goes through here.
 */
private fun subscribedUntil(scope: CoroutineScope, unsubscribe: () -> Unit): Job =
    Job(scope.coroutineContext.job).apply { invokeOnCompletion { unsubscribe() } }

/**
 * Ends this event subscription when [scope] ends, so a view's subscriptions go with the view. The tie detaches from
 * [scope] once the subscription ends on its own. Returns the subscription, which can still be cancelled earlier.
 */
fun Job.cancelWith(scope: CoroutineScope): Job = also { subscription ->
    val tie = subscribedUntil(scope) { subscription.cancel() }
    subscription.invokeOnCompletion { tie.cancel() }
}

/**
 * [cancelWith] for an awaitable-event subscription, which returns a remover instead of a Job. Returns a Job that
 * removes the subscription when cancelled; use it, not the remover, to unsubscribe early.
 */
fun (() -> Unit).cancelWith(scope: CoroutineScope): Job = subscribedUntil(scope, this)

/** Cancels this scope (such as a panel's view scope) when [scope] (such as its window's) ends. */
fun CoroutineScope.cancelWith(scope: CoroutineScope): CoroutineScope = also { coroutineContext.job.cancelWith(scope) }

/**
 * Redraws on the EDT after this event, coalescing fires into at most one run per [minIntervalMs]. The event's
 * fire never waits on the EDT. Use for view refreshes only: a caller that relies on the handler having finished
 * when fire returns needs the awaited [FlowEvents.NoArgAwaitableEvent.on].
 */
fun FlowEvents.NoArgAwaitableEvent.onUi(scope: CoroutineScope, minIntervalMs: Long = UI_FRAME_MS, block: () -> Unit): Job {
    val refresh = scope.uiRefresh(minIntervalMs, block)
    return on(Dispatchers.Unconfined) { refresh.request() }.cancelWith(scope)
}

/** [onUi] for a valued awaitable event: only the newest value since the last run is applied. */
fun <T> FlowEvents.AwaitableEvent<T>.onUi(scope: CoroutineScope, minIntervalMs: Long = UI_FRAME_MS, apply: (T) -> Unit): Job {
    val latest = scope.uiLatest(minIntervalMs, apply)
    return on(Dispatchers.Unconfined) { latest.post(it) }.cancelWith(scope)
}

/** Every value fired, in order, handed to [drain] in batches on the EDT. The event's fire never waits on the EDT. */
fun <T> FlowEvents.AwaitableEvent<T>.onUiBatch(scope: CoroutineScope, minIntervalMs: Long = 0, drain: (List<T>) -> Unit): Job {
    val inbox = scope.uiInbox(minIntervalMs, drain)
    return on(Dispatchers.Unconfined) { inbox.post(it) }.cancelWith(scope)
}

fun FlowEvents.NoArgEvent.onUi(scope: CoroutineScope, minIntervalMs: Long = UI_FRAME_MS, block: () -> Unit): Job {
    val refresh = scope.uiRefresh(minIntervalMs, block)
    return onImmediate { refresh.request() }.cancelWith(scope)
}

fun <T> FlowEvents.OneArgEvent<T>.onUi(scope: CoroutineScope, minIntervalMs: Long = UI_FRAME_MS, apply: (T) -> Unit): Job {
    val latest = scope.uiLatest(minIntervalMs, apply)
    return onImmediate { latest.post(it) }.cancelWith(scope)
}

fun <T> FlowEvents.OneArgEvent<T>.onUiBatch(scope: CoroutineScope, minIntervalMs: Long = 0, drain: (List<T>) -> Unit): Job {
    val inbox = scope.uiInbox(minIntervalMs, drain)
    return onImmediate { inbox.post(it) }.cancelWith(scope)
}
