/**
 * Test support for EDT behavior: run code while the EDT is held and check it finishes anyway, count how many tasks
 * code posts to the EDT, and read the scene on the EDT.
 */
package org.simbrain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.AWTEvent
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.event.InvocationEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities

/**
 * Runs [block] off the EDT while the EDT is held, returning whether it finished before the hold was released. A
 * watchdog releases the EDT after [timeoutMs] so code stuck on the EDT fails the check instead of hanging the test:
 * work waiting on a held EDT can't even be cancelled until the EDT is free again.
 */
suspend fun finishesWhileEdtIsBlocked(timeoutMs: Long = 10_000, block: suspend () -> Unit): Boolean {
    val release = CountDownLatch(1)
    val blocked = CountDownLatch(1)
    SwingUtilities.invokeLater {
        blocked.countDown()
        release.await()
    }
    blocked.await()
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
 * Runs [block] and returns how many tasks reached the EDT while it ran, including any it left queued. Must not be
 * called on the EDT.
 */
fun countEdtTasks(block: () -> Unit): Int {
    val queue = CountingEventQueue()
    Toolkit.getDefaultToolkit().systemEventQueue.push(queue)
    try {
        block()
        SwingUtilities.invokeAndWait {}
        return queue.dispatched.get() - 1
    } finally {
        queue.remove()
    }
}
