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
import java.util.function.Consumer
import javax.swing.SwingUtilities

class FlowEventsTest {

    @Test
    fun `pubsub one arg event delivers value to handler`() = runBlocking {
        val events = FlowEvents()
        val event = events.OneArgEvent<String>()
        val received = CompletableDeferred<String>()

        event.on(Dispatchers.Default) { received.complete(it) }
        delay(100)

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
        // The old leading-edge throttle would have delivered 1 instead.
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

        job.cancel() // removes the handler from the list; the shaping collector keeps running
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

        job1.cancel() // removes handler1 from the list
        delay(100)

        val job2 = event.on(Dispatchers.Default) { second.add(it) } // registers a fresh handler synchronously
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

        jobA.cancel() // removes handler A; handler B stays in the list
        delay(100)
        event.fire(2)
        delay(150)

        assertEquals(1, a.get()) // cancelled subscriber stopped
        assertEquals(2, b.get()) // survivor keeps receiving
        events.close()
    }

    @Test
    fun `batch debounce delivers all values fired in a window as one list`() = runBlocking {
        val events = FlowEvents()
        val event = events.BatchOneArgEvent<String>(interval = 50, timingMode = FlowEvents.TimingMode.Debounce)
        val batches = Collections.synchronizedList(mutableListOf<List<String>>())

        event.on(Dispatchers.Default) { batches.add(it) }
        delay(50) // let the subscriber attach to the eagerly-shared flush flow

        event.fire("a")
        event.fire("b")
        event.fire("c")
        delay(150) // past the debounce window

        // Unlike a plain debounced OneArgEvent (which would deliver only "c"), the batch carries every value.
        assertEquals(1, batches.size)
        assertEquals(setOf("a", "b", "c"), batches.single().toSet())
        events.close()
    }

    @Test
    fun `batch clears between windows so values are not redelivered`() = runBlocking {
        val events = FlowEvents()
        val event = events.BatchOneArgEvent<String>(interval = 50, timingMode = FlowEvents.TimingMode.Debounce)
        val batches = Collections.synchronizedList(mutableListOf<List<String>>())

        event.on(Dispatchers.Default) { batches.add(it) }
        delay(50)

        event.fire("a")
        event.fire("b")
        delay(150) // first window flushes [a, b]

        event.fire("c")
        delay(150) // second window flushes [c] only

        assertEquals(2, batches.size)
        assertEquals(setOf("a", "b"), batches[0].toSet())
        assertEquals(listOf("c"), batches[1])
        events.close()
    }

    @Test
    fun `batch throttle flushes each fired value exactly once across windows`() = runBlocking {
        val events = FlowEvents()
        val event = events.BatchOneArgEvent<Int>(interval = 50, timingMode = FlowEvents.TimingMode.Throttle)
        val batches = Collections.synchronizedList(mutableListOf<List<Int>>())

        event.on(Dispatchers.Default) { batches.add(it) }
        delay(50)

        event.fire(1)
        event.fire(2)
        delay(150)

        assertEquals(listOf(1, 2), batches.flatten().sorted()) // every value delivered once, no loss/dupe
        events.close()
    }

    @Test
    fun `cancelling a batch subscriber stops further delivery`() = runBlocking {
        val events = FlowEvents()
        val event = events.BatchOneArgEvent<Int>(interval = 50, timingMode = FlowEvents.TimingMode.Debounce)
        val batches = Collections.synchronizedList(mutableListOf<List<Int>>())

        val job = event.on(Dispatchers.Default) { batches.add(it) }
        delay(50)
        event.fire(1)
        delay(150)
        assertEquals(1, batches.size)

        job.cancel() // the eager flush flow keeps draining (to nobody); delivery stops
        delay(50)
        event.fire(2)
        delay(150)

        assertEquals(1, batches.size)
        events.close()
    }

    @Test
    fun `noarg awaitable event awaits all handlers before fire returns`() = runBlocking {
        val events = FlowEvents()
        val event = events.NoArgAwaitableEvent()
        val order = Collections.synchronizedList(mutableListOf<String>())

        event.on(Dispatchers.Default) { delay(30); order.add("a") }
        event.on(Dispatchers.Default) { delay(10); order.add("b") }

        event.fire()
        order.add("after")

        assertEquals(listOf("a", "b", "after"), order.toList()) // sequential, both done before fire returns
        events.close()
    }

    @Test
    fun `awaitable fireAsync completes only after all handlers finish`() = runBlocking {
        val events = FlowEvents()
        val event = events.AwaitableEvent<String>()
        val received = AtomicReference<String>()

        event.on(Dispatchers.Default) { delay(20); received.set(it) }
        event.fireAsync("x").await() // non-suspend fire, awaitable barrier

        assertEquals("x", received.get())
        events.close()
    }

    @Test
    fun `noarg awaitable fireAndBlock bridges from a non-suspend caller`() {
        val events = FlowEvents()
        val event = events.NoArgAwaitableEvent()
        val ran = AtomicReference(false)

        event.on(Dispatchers.Default) { ran.set(true) }
        event.fireAndBlock()

        assertTrue(ran.get())
        events.close()
    }

    @Test
    fun `handler registered with on receives a fire that immediately follows it`() = runBlocking {
        val events = FlowEvents()
        val event = events.OneArgEvent<String>()
        val received = CompletableDeferred<String>()

        // No delay between on() and fire(): registration is synchronous, so the fire cannot race the
        // subscription. A per-handler async launchIn could drop this; this is the regression guard.
        event.on(Dispatchers.Default) { received.complete(it) }
        event.fire("now")

        assertEquals("now", withTimeout(2000) { received.await() })
        events.close()
    }

    @Test
    fun `noarg event delivers a fire that immediately follows on`() = runBlocking {
        val events = FlowEvents()
        val event = events.NoArgEvent()
        val fired = CompletableDeferred<Unit>()

        event.on(Dispatchers.Default) { fired.complete(Unit) }
        event.fire()

        withTimeout(2000) { fired.await() }
        events.close()
    }

    @Test
    fun `awaitable event Consumer overload is awaited like the suspend form`() {
        val events = FlowEvents()
        val event = events.AwaitableEvent<String>()
        val received = AtomicReference<String>()

        event.on(Dispatchers.Default, Consumer { received.set(it) })
        event.fireAndBlock("java")

        assertEquals("java", received.get())
        events.close()
    }

    @Test
    fun `noarg awaitable Runnable overload is awaited like the suspend form`() {
        val events = FlowEvents()
        val event = events.NoArgAwaitableEvent()
        val ran = AtomicReference(false)

        event.on(Dispatchers.Default, Runnable { ran.set(true) })
        event.fireAndBlock()

        assertTrue(ran.get())
        events.close()
    }
}
