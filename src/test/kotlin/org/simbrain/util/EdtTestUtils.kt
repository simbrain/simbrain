/**
 * Test support for EDT behavior: hold the EDT while code runs and check that code finishes anyway, count how many tasks
 * code posts to the EDT, read the scene on the EDT, wait for asynchronous work to land, and iterate a workspace then
 * wait for the UI work the iteration queued.
 */
package org.simbrain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.simbrain.workspace.Workspace
import java.awt.AWTEvent
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.event.InvocationEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities

/**
 * Holds the EDT and returns once it is held; counting down the returned latch releases it. Must not be called on the
 * EDT. Prefer [withEdtHeld], which releases the EDT even when the code under test throws.
 */
fun holdEdt(): CountDownLatch {
    val release = CountDownLatch(1)
    val held = CountDownLatch(1)
    SwingUtilities.invokeLater {
        held.countDown()
        release.await()
    }
    held.await()
    return release
}

/**
 * Runs [block] while the EDT is held, so everything it posts to the EDT queues up behind the hold, then releases the
 * EDT and returns [block]'s result.
 */
inline fun <T> withEdtHeld(block: () -> T): T {
    val release = holdEdt()
    try {
        return block()
    } finally {
        release.countDown()
    }
}

/**
 * Runs [block] off the EDT while the EDT is held, returning whether it finished before the hold was released. A
 * watchdog releases the EDT after [timeoutMs] so code stuck on the EDT fails the check instead of hanging the test:
 * work waiting on a held EDT can't even be cancelled until the EDT is free again.
 */
suspend fun finishesWhileEdtIsBlocked(timeoutMs: Long = 10_000, block: suspend () -> Unit): Boolean {
    val release = holdEdt()
    val watchdog = Thread {
        try {
            Thread.sleep(timeoutMs)
        } catch (_: InterruptedException) {
        }
        release.countDown()
    }.apply {
        isDaemon = true
        start()
    }
    try {
        withContext(Dispatchers.Default) { block() }
        return release.count == 1L
    } finally {
        release.countDown()
        watchdog.interrupt()
    }
}

/**
 * Runs [block] on the EDT and returns its result, for tests that read the Piccolo scene: the EDT mutates it, so a read
 * from the test thread can race those changes (and throw from inside Piccolo's child lists).
 */
fun <T> onEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    var result: Result<T>? = null
    SwingUtilities.invokeAndWait { result = runCatching(block) }
    return result!!.getOrThrow()
}

/** An event queue that counts the invocation events (posted tasks) it dispatches until it is removed. */
private class CountingEventQueue : EventQueue() {
    val dispatched = AtomicInteger()

    override fun dispatchEvent(event: AWTEvent) {
        if (event is InvocationEvent) dispatched.incrementAndGet()
        super.dispatchEvent(event)
    }

    fun remove() = pop()
}

/**
 * Runs [block] and returns how many tasks reached the EDT while it ran, including any it left queued. With [settleMs],
 * waits that long after [block] before counting, so work it scheduled for a later frame is counted too; the wait is a
 * sleep rather than a poll, because polling the EDT would add tasks of its own. Must not be called on the EDT.
 */
fun countEdtTasks(settleMs: Long = 0, block: () -> Unit): Int {
    val queue = CountingEventQueue()
    Toolkit.getDefaultToolkit().systemEventQueue.push(queue)
    try {
        block()
        if (settleMs > 0) Thread.sleep(settleMs)
        SwingUtilities.invokeAndWait {}
        return queue.dispatched.get() - 1
    } finally {
        queue.remove()
    }
}

/**
 * Polls until [condition] holds, failing the test if it does not within [timeoutMillis]. A condition that reads the
 * scene or other Swing state should read it through [onEdt].
 */
fun awaitUntil(timeoutMillis: Long = 5000, message: String = "Condition not met in time", condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (System.currentTimeMillis() < deadline) {
        if (condition()) return
        Thread.sleep(10)
    }
    assertTrue(condition(), message)
}

/**
 * Iterates [iterations] times, then waits for the UI work the iterations queued, such as plot updates, which land on
 * the EDT after each iteration returns.
 */
fun Workspace.iterateAndSettle(iterations: Int = 1) {
    repeat(iterations) { simpleIterate() }
    UiWork.awaitIdle()
}
