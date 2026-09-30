/**
 * Test support for code that must not wait on the EDT: run it while the EDT is held and check it finishes anyway.
 */
package org.simbrain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
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
