package org.simbrain.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

class FlowEventsTest {

    @Test
    fun `pubsub one arg event delivers value to handler`() = runBlocking {
        val events = FlowEvents()
        val event = events.OneArgEvent<String>()
        val received = CompletableDeferred<String>()

        event.on(Dispatchers.Default) { received.complete(it) }
        delay(100) // SharedFlow has no replay; let the collector subscribe before firing

        event.fire("hello")

        assertEquals("hello", withTimeout(2000) { received.await() })
        events.close()
    }

    @Test
    fun `changed event suppresses equal values`() = runBlocking {
        val events = FlowEvents()
        val event = events.ChangedEvent<String>()
        val count = AtomicInteger(0)

        event.on(Dispatchers.Default) { _, _ -> count.incrementAndGet() }
        delay(100)

        event.fire("a", "a") // suppressed (new == old)
        event.fire("b", "a") // delivered
        delay(100)

        assertEquals(1, count.get())
        events.close()
    }

    @Test
    fun `debounce coalesces rapid fires into one`() = runBlocking {
        val events = FlowEvents()
        val event = events.NoArgEvent(interval = 50, timingMode = FlowEvents.TimingMode.Debounce)
        val count = AtomicInteger(0)

        event.on(Dispatchers.Default) { count.incrementAndGet() }
        delay(100)

        event.fire()
        event.fire()
        event.fire()
        delay(200) // past the debounce window

        assertEquals(1, count.get())
        events.close()
    }

    @Test
    fun `cancelling the job returned by on unsubscribes the handler`() = runBlocking {
        val events = FlowEvents()
        val event = events.OneArgEvent<String>()
        val count = AtomicInteger(0)

        val job = event.on(Dispatchers.Default) { count.incrementAndGet() }
        delay(100)
        event.fire("a")
        delay(100)

        job.cancel()
        delay(50)
        event.fire("b")
        delay(100)

        assertEquals(1, count.get())
        events.close()
    }

    @Test
    fun `awaitable event runs handlers sequentially and returns only after all finish`() = runBlocking {
        val events = FlowEvents()
        val event = events.AwaitableEvent<Int>()
        val order = Collections.synchronizedList(mutableListOf<String>())

        // "slow" is registered first; sequential execution means it completes before "fast" starts,
        // even though it sleeps longer.
        event.on(Dispatchers.Default) { delay(50); order.add("slow") }
        event.on(Dispatchers.Default) { delay(10); order.add("fast") }

        event.fire(1)
        order.add("after-fire")

        assertEquals(listOf("slow", "fast", "after-fire"), order.toList())
        events.close()
    }

    @Test
    fun `awaitable fireAndBlock bridges from a non-suspend caller`() {
        val events = FlowEvents()
        val event = events.AwaitableEvent<String>()
        val received = AtomicReference<String>()

        event.on(Dispatchers.Default) { received.set(it) }
        event.fireAndBlock("x")

        assertEquals("x", received.get())
        events.close()
    }

    @Test
    fun `awaitable fireAndBlock on the EDT completes when handlers run off-EDT`() {
        val events = FlowEvents()
        val event = events.AwaitableEvent<String>()
        val received = AtomicReference<String>()
        event.on(Dispatchers.Default) { received.set(it) }

        // Blocks the EDT, but the handler runs on Dispatchers.Default, so there is no deadlock.
        SwingUtilities.invokeAndWait {
            event.fireAndBlock("edt")
        }

        assertEquals("edt", received.get())
        events.close()
    }

    @Test
    fun `throttle delivers only the latest value fired within a window`() = runBlocking {
        val events = FlowEvents()
        val event = events.OneArgEvent<Int>(interval = 50, timingMode = FlowEvents.TimingMode.Throttle)
        val received = Collections.synchronizedList(mutableListOf<Int>())

        event.on(Dispatchers.Default) { received.add(it) }
        delay(100) // let the shared sample collector subscribe

        event.fire(1)
        event.fire(2)
        event.fire(3) // all within one 50ms window
        delay(200)    // past the sample window

        // sample() is trailing-edge: intermediate fires are dropped and only the latest survives.
        // The old leading-edge Events throttle would have delivered 1 instead.
        assertEquals(listOf(3), received.toList())
        events.close()
    }

    @Test
    fun `throttle still delivers a single isolated fire`() = runBlocking {
        val events = FlowEvents()
        val event = events.OneArgEvent<Int>(interval = 50, timingMode = FlowEvents.TimingMode.Throttle)
        val received = CompletableDeferred<Int>()

        event.on(Dispatchers.Default) { received.complete(it) }
        delay(100)

        event.fire(7) // a lone fire is flushed at the next sample tick, not lost

        assertEquals(7, withTimeout(2000) { received.await() })
        events.close()
    }

    @Test
    fun `cancelling a throttled subscriber stops further delivery`() = runBlocking {
        val events = FlowEvents()
        val event = events.OneArgEvent<Int>(interval = 50, timingMode = FlowEvents.TimingMode.Throttle)
        val count = AtomicInteger(0)

        val job = event.on(Dispatchers.Default) { count.incrementAndGet() }
        delay(100)
        event.fire(1)
        delay(150)
        assertEquals(1, count.get())

        job.cancel() // last subscriber gone -> the shared WhileSubscribed collector tears down
        delay(100)
        event.fire(2)
        delay(150)

        assertEquals(1, count.get()) // nothing delivered after cancel
        events.close()
    }

    @Test
    fun `re-subscribing to a throttled event after cancel still delivers`() = runBlocking {
        val events = FlowEvents()
        val event = events.OneArgEvent<Int>(interval = 50, timingMode = FlowEvents.TimingMode.Throttle)
        val first = Collections.synchronizedList(mutableListOf<Int>())
        val second = Collections.synchronizedList(mutableListOf<Int>())

        val job1 = event.on(Dispatchers.Default) { first.add(it) }
        delay(100)
        event.fire(1)
        delay(150)
        assertEquals(listOf(1), first.toList())

        job1.cancel() // shared collector stops
        delay(100)

        val job2 = event.on(Dispatchers.Default) { second.add(it) } // restarts the shared collector
        delay(100)
        event.fire(2)
        delay(150)

        assertEquals(listOf(1), first.toList())  // the cancelled handler saw nothing more
        assertEquals(listOf(2), second.toList()) // nothing lost across the cancel/resubscribe churn
        events.close()
    }

    @Test
    fun `cancelling one throttled subscriber leaves the other receiving`() = runBlocking {
        val events = FlowEvents()
        val event = events.OneArgEvent<Int>(interval = 50, timingMode = FlowEvents.TimingMode.Throttle)
        val a = AtomicInteger(0)
        val b = AtomicInteger(0)

        val jobA = event.on(Dispatchers.Default) { a.incrementAndGet() }
        val jobB = event.on(Dispatchers.Default) { b.incrementAndGet() }
        delay(100)
        event.fire(1)
        delay(150)
        assertEquals(1, a.get())
        assertEquals(1, b.get())

        jobA.cancel() // one of two subscribers leaves; the collector stays up for jobB
        delay(100)
        event.fire(2)
        delay(150)

        assertEquals(1, a.get()) // cancelled subscriber stopped
        assertEquals(2, b.get()) // survivor keeps receiving from the still-shared collector
        events.close()
    }
}
