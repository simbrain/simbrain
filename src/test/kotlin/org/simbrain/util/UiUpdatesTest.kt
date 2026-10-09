/**
 * Behavior of the EDT hand-off primitives: coalescing, ordering against already-queued EDT work, never running
 * inline or blocking the poster, rate caps, lifetime, and the awaitable event subscriptions built on them.
 */
package org.simbrain.util

import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities

class UiUpdatesTest {

    /** Holds the EDT until the returned latch is counted down. */
    private fun holdEdt(): CountDownLatch {
        val release = CountDownLatch(1)
        val held = CountDownLatch(1)
        SwingUtilities.invokeLater {
            held.countDown()
            release.await()
        }
        held.await()
        return release
    }

    @Test
    fun `requests made while a refresh is pending collapse into one run on the edt`() {
        val scope = UiScope()
        val runs = AtomicInteger()
        var onEdt = true
        val refresh = scope.uiRefresh(minIntervalMs = 0) {
            onEdt = onEdt && SwingUtilities.isEventDispatchThread()
            runs.incrementAndGet()
        }
        val release = holdEdt()
        repeat(1000) { refresh.request() }
        release.countDown()
        UiWork.awaitIdle()
        assertEquals(1, runs.get())
        assertTrue(onEdt)
        scope.cancel()
    }

    @Test
    fun `a refresh runs after edt work queued before it was requested`() {
        val scope = UiScope()
        val seen = Collections.synchronizedList(mutableListOf<String>())
        var state = "old"
        val refresh = scope.uiRefresh(minIntervalMs = 0) { seen += state }
        val release = holdEdt()
        refresh.request()
        SwingUtilities.invokeLater { state = "new" }
        refresh.request()
        release.countDown()
        UiWork.awaitIdle()
        assertEquals(listOf("new"), seen.toList())
        scope.cancel()
    }

    @Test
    fun `a request made on the edt runs on a later turn, not inline`() {
        val scope = UiScope()
        val order = Collections.synchronizedList(mutableListOf<String>())
        val refresh = scope.uiRefresh(minIntervalMs = 0) { order += "refresh" }
        SwingUtilities.invokeAndWait {
            refresh.request()
            order += "caller"
        }
        UiWork.awaitIdle()
        assertEquals(listOf("caller", "refresh"), order.toList())
        scope.cancel()
    }

    @Test
    fun `the interval caps runs and a trailing run follows the last request`() = runBlocking {
        val scope = UiScope()
        val runs = AtomicInteger()
        var lastSeen = -1
        var counter = 0
        val refresh = scope.uiRefresh(minIntervalMs = 50) {
            runs.incrementAndGet()
            lastSeen = counter
        }
        val start = System.currentTimeMillis()
        repeat(40) {
            counter = it
            refresh.request()
            delay(5)
        }
        val elapsed = System.currentTimeMillis() - start
        withContext(Dispatchers.IO) { UiWork.awaitIdle() }
        assertTrue(runs.get() <= elapsed / 50 + 2, "ran ${runs.get()} times in $elapsed ms")
        assertEquals(39, lastSeen)
        scope.cancel()
    }

    @Test
    fun `posting never blocks while the edt is busy and the inbox keeps every item in order`() {
        val scope = UiScope()
        val drained = Collections.synchronizedList(mutableListOf<Int>())
        val batches = AtomicInteger()
        val inbox = scope.uiInbox<Int> {
            batches.incrementAndGet()
            drained += it
        }
        val release = holdEdt()
        val start = System.nanoTime()
        repeat(10_000) { inbox.post(it) }
        val postMs = (System.nanoTime() - start) / 1_000_000
        release.countDown()
        UiWork.awaitIdle()
        assertTrue(postMs < 1000, "posting took $postMs ms")
        assertEquals((0 until 10_000).toList(), drained.toList())
        assertEquals(1, batches.get())
        scope.cancel()
    }

    @Test
    fun `latest applies only the newest value posted before it runs`() {
        val scope = UiScope()
        val applied = Collections.synchronizedList(mutableListOf<Int>())
        val latest = scope.uiLatest<Int>(minIntervalMs = 0) { applied += it }
        val release = holdEdt()
        repeat(100) { latest.post(it) }
        release.countDown()
        UiWork.awaitIdle()
        assertEquals(listOf(99), applied.toList())
        scope.cancel()
    }

    @Test
    fun `cancelling the scope drops pending work and later posts do nothing`() {
        val scope = UiScope()
        val drained = AtomicInteger()
        val inbox = scope.uiInbox<Int> { drained.addAndGet(it.size) }
        val release = holdEdt()
        repeat(10) { inbox.post(it) }
        scope.cancel()
        inbox.post(99)
        release.countDown()
        UiWork.awaitIdle()
        assertEquals(0, drained.get())
    }

    @Test
    fun `a throwing block does not stop later requests from running`() {
        val scope = UiScope()
        val runs = AtomicInteger()
        val refresh = scope.uiRefresh(minIntervalMs = 0) {
            if (runs.incrementAndGet() == 1) throw IllegalStateException("expected by the test")
        }
        refresh.request()
        UiWork.awaitIdle()
        refresh.request()
        UiWork.awaitIdle()
        assertEquals(2, runs.get())
        scope.cancel()
    }

    @Test
    fun `flush applies queued items before returning`() {
        val scope = UiScope()
        val drained = Collections.synchronizedList(mutableListOf<Int>())
        val inbox = scope.uiInbox<Int> { drained += it }
        val release = holdEdt()
        repeat(5) { inbox.post(it) }
        Thread {
            Thread.sleep(50)
            release.countDown()
        }.start()
        inbox.flush()
        assertEquals((0 until 5).toList(), drained.toList())
        UiWork.awaitIdle()
        assertEquals(5, drained.size)
        scope.cancel()
    }

    private class TestEvents : FlowEvents() {
        val barrier = NoArgAwaitableEvent()
        val valued = AwaitableEvent<Int>()
        val ping = NoArgEvent()
    }

    @Test
    fun `firing an awaitable event with a ui subscriber does not wait on the edt`() = runBlocking {
        val scope = UiScope()
        val events = TestEvents()
        val runs = AtomicInteger()
        events.barrier.onUi(scope, minIntervalMs = 0) { runs.incrementAndGet() }
        val release = holdEdt()
        val fired = withTimeoutOrNull(2000) { repeat(100) { events.barrier.fire() } }
        release.countDown()
        withContext(Dispatchers.IO) { UiWork.awaitIdle() }
        assertNotNull(fired, "fire waited on the held EDT")
        assertEquals(1, runs.get())
        scope.cancel()
    }

    @Test
    fun `a batch subscription delivers every value in order`() = runBlocking {
        val scope = UiScope()
        val events = TestEvents()
        val got = Collections.synchronizedList(mutableListOf<Int>())
        events.valued.onUiBatch(scope) { got += it }
        repeat(50) { events.valued.fire(it) }
        withContext(Dispatchers.IO) { UiWork.awaitIdle() }
        assertEquals((0 until 50).toList(), got.toList())
        scope.cancel()
    }

    @Test
    fun `cancelling the scope unsubscribes the event`() = runBlocking {
        val scope = UiScope()
        val events = TestEvents()
        val runs = AtomicInteger()
        events.barrier.onUi(scope, minIntervalMs = 0) { runs.incrementAndGet() }
        scope.cancel()
        events.barrier.fire()
        withContext(Dispatchers.IO) { UiWork.awaitIdle() }
        assertEquals(0, runs.get())
    }

    @Test
    fun `a subscription bound to a scope ends when the scope ends`() {
        val scope = UiScope()
        val events = TestEvents()
        var runs = 0
        events.ping.onImmediate { runs++ }.cancelWith(scope)

        events.ping.fire()
        scope.cancel()
        events.ping.fire()

        assertEquals(1, runs)
    }

    @Test
    fun `an awaitable subscription bound to a scope ends when the scope ends`() = runBlocking {
        val scope = UiScope()
        val events = TestEvents()
        var runs = 0
        events.barrier.on(Dispatchers.Unconfined) { runs++ }.cancelWith(scope)

        events.barrier.fire()
        scope.cancel()
        events.barrier.fire()

        assertEquals(1, runs)
    }

    @Test
    fun `subscriptions ended early do not pile up on a long-lived scope`() {
        val scope = UiScope()
        val events = TestEvents()

        repeat(1000) { events.ping.onImmediate { }.cancelWith(scope).cancel() }

        assertEquals(0, scope.coroutineContext.job.children.count())
    }

    @Test
    fun `a panel scope bound to its window's scope ends with it`() {
        val window = UiScope()
        val panel = UiScope().cancelWith(window)

        window.cancel()

        assertFalse(panel.isActive)
    }
}
