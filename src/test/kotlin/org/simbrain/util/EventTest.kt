package org.simbrain.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class EventTest {

    private val events: Events

    init {
        events = Events(timeout = 1.seconds)
    }

    @Test
    fun `test no arg event`() {
        val counter = AtomicInteger(0)
        val noArgEvent = events.NoArgEvent()
        
        noArgEvent.on { counter.incrementAndGet() }
        
        noArgEvent.fireAndBlock()
        assertEquals(1, counter.get())
        
        noArgEvent.fireAndBlock()
        assertEquals(2, counter.get())
    }

    @Test
    fun `test one arg event`() {
        val receivedValues = mutableListOf<String>()
        val oneArgEvent = events.OneArgEvent<String>()
        
        oneArgEvent.on { value -> receivedValues.add(value) }
        
        oneArgEvent.fireAndBlock("test1")
        oneArgEvent.fireAndBlock("test2")
        
        assertEquals(listOf("test1", "test2"), receivedValues)
    }

    @Test
    fun `test changed event fires only on change`() {
        val changes = mutableListOf<Pair<String, String>>()
        val changedEvent = events.ChangedEvent<String>()
        
        changedEvent.on { new, old -> changes.add(new to old) }
        
        // Should fire
        changedEvent.fireAndBlock("new", "old")
        
        // Should not fire (same values)
        changedEvent.fireAndBlock("same", "same")
        
        // Should fire again
        changedEvent.fireAndBlock("newer", "old")
        
        assertEquals(2, changes.size)
        assertEquals("new" to "old", changes[0])
        assertEquals("newer" to "old", changes[1])
    }

    @Test
    fun `test multiple handlers on same event`() {
        val counter1 = AtomicInteger(0)
        val counter2 = AtomicInteger(0)
        val noArgEvent = events.NoArgEvent()
        
        noArgEvent.on { counter1.incrementAndGet() }
        noArgEvent.on { counter2.incrementAndGet() }
        
        noArgEvent.fireAndBlock()
        
        assertEquals(1, counter1.get())
        assertEquals(1, counter2.get())
    }

    @Test
    fun `test event handler removal`() {
        val counter = AtomicInteger(0)
        val noArgEvent = events.NoArgEvent()
        
        val removeHandler = noArgEvent.on { counter.incrementAndGet() }
        
        noArgEvent.fireAndBlock()
        assertEquals(1, counter.get())
        
        // Remove handler and fire again
        removeHandler()
        noArgEvent.fireAndBlock()
        
        // Counter should not have incremented
        assertEquals(1, counter.get())
    }

    @Test
    fun `test async event with suspend handler`() = runBlocking {
        val receivedValues = mutableListOf<Int>()
        val oneArgEvent = events.OneArgEvent<Int>()
        
        oneArgEvent.on { value ->
            delay(10) // Simulate async work
            receivedValues.add(value)
        }
        
        oneArgEvent.fire(42).await()
        assertEquals(listOf(42), receivedValues)
    }

    @Test
    fun `test batch one arg event`() {
        val batchEvent = events.BatchOneArgEvent<String>(interval = 0)
        val receivedBatches = mutableListOf<List<String>>()
        
        batchEvent.on { batch -> receivedBatches.add(batch.toList()) }
        
        // Fire multiple values rapidly
        batchEvent.fireAndBlock("a")
        batchEvent.fireAndBlock("b")
        batchEvent.fireAndBlock("c")
        
        // Should receive them in batches
        assertTrue(receivedBatches.isNotEmpty())
        val allValues = receivedBatches.flatten()
        assertTrue(allValues.contains("a"))
        assertTrue(allValues.contains("b"))
        assertTrue(allValues.contains("c"))
    }

    @Test
    fun `test throttling mode`() = runBlocking {
        val counter = AtomicInteger(0)
        val throttledEvent = events.NoArgEvent(interval = 50, timingMode = Events.TimingMode.Throttle)
        
        throttledEvent.on { counter.incrementAndGet() }
        
        // Fire multiple times rapidly
        val fired1 = throttledEvent.fire()
        val fired2 = throttledEvent.fire()
        val fired3 = throttledEvent.fire()
        
        fired1.await()
        fired2.await()
        fired3.await()
        
        // Should have throttled some events
        assertTrue(counter.get() < 3)
    }

    @Test
    fun `test debouncing mode`() = runBlocking {
        val counter = AtomicInteger(0)
        val debouncedEvent = events.NoArgEvent(interval = 50, timingMode = Events.TimingMode.Debounce)
        
        debouncedEvent.on { counter.incrementAndGet() }
        
        // Fire multiple times rapidly
        debouncedEvent.fire()
        debouncedEvent.fire()
        debouncedEvent.fire()
        
        // Wait for debounce interval
        delay(100)
        
        // Should have debounced to only one execution
        assertEquals(1, counter.get())
    }

    @Test
    fun `test wait flag behavior`() {
        val executionOrder = mutableListOf<String>()
        val noArgEvent = events.NoArgEvent()
        
        noArgEvent.on(wait = true) { 
            Thread.sleep(50)
            executionOrder.add("waited")
        }
        noArgEvent.on(wait = false) { 
            executionOrder.add("no-wait")
        }
        
        noArgEvent.fireAndBlock()
        executionOrder.add("after-fire")
        
        // "waited" should complete before "after-fire" due to wait=true
        assertTrue(executionOrder.contains("waited"))
        assertTrue(executionOrder.contains("no-wait"))
        assertTrue(executionOrder.contains("after-fire"))
        
        val waitedIndex = executionOrder.indexOf("waited")
        val afterFireIndex = executionOrder.indexOf("after-fire")
        assertTrue(waitedIndex < afterFireIndex)
    }

    @Test
    fun `test batch changed event`() {
        val batchChangedEvent = events.BatchChangedEvent<String>(interval = 0)
        val receivedChanges = mutableListOf<Pair<List<String>, List<String>>>()
        
        batchChangedEvent.on { newBatch, oldBatch ->
            receivedChanges.add(newBatch.toList() to oldBatch.toList())
        }
        
        batchChangedEvent.fireAndBlock("new1", "old1")
        batchChangedEvent.fireAndBlock("new2", "old2")
        
        // With interval = 0, each fireAndBlock should process immediately as a separate batch
        assertEquals(2, receivedChanges.size)
        
        // First call processes first event
        val (firstNew, firstOld) = receivedChanges[0]
        assertEquals(listOf("new1"), firstNew)
        assertEquals(listOf("old1"), firstOld)
        
        // Second call processes second event (queue was cleared after first call)
        val (secondNew, secondOld) = receivedChanges[1]
        assertEquals(listOf("new2"), secondNew)
        assertEquals(listOf("old2"), secondOld)
    }

    @Test
    fun `test event with no handlers`() {
        val noArgEvent = events.NoArgEvent()
        
        // Should not crash when firing with no handlers
        assertDoesNotThrow {
            noArgEvent.fireAndBlock()
        }
    }

    @Test
    fun `test concurrent event firing`() = runBlocking {
        val counter = AtomicInteger(0)
        val oneArgEvent = events.OneArgEvent<Int>()
        
        oneArgEvent.on { value -> 
            delay(10)
            counter.addAndGet(value) 
        }
        
        // Fire multiple events concurrently
        val jobs = (1..5).map { value ->
            oneArgEvent.fire(value)
        }
        
        jobs.forEach { it.await() }
        
        // All values should have been processed
        assertEquals(15, counter.get()) // 1+2+3+4+5 = 15
    }

    @Test
    fun `test exception handling in event handlers`() {
        val successCounter = AtomicInteger(0)
        val noArgEvent = events.NoArgEvent()
        
        // Handler that throws
        noArgEvent.on { throw RuntimeException("Test exception") }
        
        // Handler that should still execute
        noArgEvent.on { successCounter.incrementAndGet() }
        
        // Event firing should not crash despite exception
        assertDoesNotThrow {
            noArgEvent.fireAndBlock()
        }
        
        // Non-throwing handler should still execute
        assertEquals(1, successCounter.get())
    }

    @Test
    fun `test generic type safety`() {
        val stringEvent = events.OneArgEvent<String>()
        val intEvent = events.OneArgEvent<Int>()
        
        val receivedStrings = mutableListOf<String>()
        val receivedInts = mutableListOf<Int>()
        
        stringEvent.on { value -> receivedStrings.add(value) }
        intEvent.on { value -> receivedInts.add(value) }
        
        stringEvent.fireAndBlock("test")
        intEvent.fireAndBlock(42)
        
        assertEquals(listOf("test"), receivedStrings)
        assertEquals(listOf(42), receivedInts)
    }

    @Test
    fun `test event system cleanup`() {
        val counter = AtomicInteger(0)
        val noArgEvent = events.NoArgEvent()
        
        val removeHandler = noArgEvent.on { counter.incrementAndGet() }
        
        noArgEvent.fireAndBlock()
        assertEquals(1, counter.get())
        
        // Cleanup
        removeHandler()
        
        // Verify cleanup worked
        noArgEvent.fireAndBlock()
        assertEquals(1, counter.get()) // Should not increment
    }
} 