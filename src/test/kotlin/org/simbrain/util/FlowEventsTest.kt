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
}
