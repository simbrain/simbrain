package org.simbrain.util

import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds


class TestEvents : Events() {
    val throttlingEvent = NoArgEvent(interval = 100, timingMode = TimingMode.Throttle)
    val debouncingEvent = NoArgEvent(interval = 100, timingMode = TimingMode.Debounce)
    val debounceAddedEvent = OneArgEvent<String>(interval = 100, timingMode = TimingMode.Debounce)
    val longEvent = NoArgEvent()
    val blockingEvent = NoArgEvent()
    val longFireAndForgetEvent = NoArgEvent()
    val changedEvent = ChangedEvent<String>()
    val batchedAddedEvent = BatchOneArgEvent<String>(interval = 100)
}

class EventTesting {

    val testEvents = TestEvents()

    @Test
    fun `ensure throttle is limiting rates`() {
        var counter = 0
        testEvents.throttlingEvent.on {
            counter++
        }
        runBlocking {
            repeat(20) {
                testEvents.throttlingEvent.fire()
                delay(50L)
            }
            // 10 events for 1 second (20*50 milliseconds)
            assertEquals(10, counter)
        }
    }

    @Test
    fun `ensure debounce is limiting rates`() {
        var counter = 0
        testEvents.debouncingEvent.on {
            counter++
        }
        runBlocking {
            testEvents.debouncingEvent.fire()
            delay(50L)
            assertEquals(0, counter, "should not fire before timeout")
            delay(100L)
            assertEquals(1, counter, "event should fire after timeout")

            counter = 0

            repeat(10) {
                testEvents.debouncingEvent.fire()
                delay(50L)
            }
            delay(100L)
            assertEquals(1, counter, "all but one event should fire after timeout")
        }
    }

    @Test
    fun `ensure suspending events wait for handler to complete`() {
        testEvents.longEvent.on(wait = true) {
            delay(100L)
        }
        val time = measureTimeMillis {
            runBlocking {
                repeat(10) {
                    testEvents.longEvent.fire().join()
                }
            }
        }
        assert(time > 1000) { "expect test to take longer than 1 seconds, took actually ${time}ms" }
    }

    @Test
    fun `ensure suspending debounced events wait for handler to complete`() {
        var counter = 0
        var last = ""
        testEvents.debounceAddedEvent.on(wait = true) {
            delay(250L)
            last = it
            counter++
        }
        runBlocking {
            val time = measureTimeMillis {
                (0 until 10).map {
                    delay(5L)
                    launch {
                        testEvents.debounceAddedEvent.fire("$it").join()
                    }
                }.joinAll()
            }
            assert(time > 250L) { "expect test to take longer than 250 ms, took actually ${time}ms" }
            assertEquals("9", last)
            assertEquals(1, counter)
        }
    }

    @Test
    fun `ensure blocking events wait for handler to complete`() {
        testEvents.blockingEvent.on(wait = true) {
            runBlocking {
                delay(100L)
            }
        }
        val time = measureTimeMillis {
            repeat(10) {
                testEvents.blockingEvent.fireAndBlock()
            }
        }
        assert(time > 1000) { "expect test to take longer than 1 seconds, took actually ${time}ms" }
    }

    @Test
    fun `ensure fire() doesn't wait for handler to complete`() {
        testEvents.longFireAndForgetEvent.on(Dispatchers.Default) {
            delay(200L)
        }
        val time = measureTimeMillis {
            repeat(20) {
                testEvents.longFireAndForgetEvent.fire()
            }
        }
        assert(time < 1500) { "expect test to take only a bit more than 1 second, took actually ${time}ms" }
    }

    @Test
    fun `if old and new are the same, event should not fire, else it should fire`() {
        var fired = false
        testEvents.changedEvent.on(wait = true) { _, _ ->
            fired = true
        }
        testEvents.changedEvent.fireAndBlock("test", "test")
        assert(!fired) { "event should not have fired" }
        testEvents.changedEvent.fireAndBlock("test", "test2")
        assert(fired) { "event should have fired" }
    }

    @Test
    fun `make sure debounced event only fires only after the set interval`() {
        var counter = 0
        var result = ""
        testEvents.debounceAddedEvent.on {
            counter++
            result = it
        }
        runBlocking {
            repeat(10) {
                testEvents.debounceAddedEvent.fire("$it")
                delay(5L)
            }
            delay(200L)
            assertEquals(1, counter)
            assertEquals("9", result)
        }
    }

    @Test
    fun `make sure batch event catches all changes`() {
        runBlocking {
            val result = ArrayList<String>()
            var counter = 0
            val timing = ArrayList<Long>()
            testEvents.batchedAddedEvent.on {
                result.addAll(it)
                counter++
            }
            timing.add(System.nanoTime())
            testEvents.batchedAddedEvent.fire("test1")
            timing.add(System.nanoTime())
            testEvents.batchedAddedEvent.fire("test2")
            timing.add(System.nanoTime())
            testEvents.batchedAddedEvent.fire("test3")
            timing.add(System.nanoTime())
            testEvents.batchedAddedEvent.fire("test4")
            timing.add(System.nanoTime())
            delay(1000L)
            assertEquals(1, counter)

            val complements = setOf("test1", "test2", "test3", "test4") complement  result.toSet()
            assert(complements.isIdentical()) { "missing events: left[${complements.leftComp}] right[${complements.rightComp}]" }
        }
    }

    @Test
    fun `make sure batch events does not block individual invocation`() {
        runBlocking {
            val result = HashMap<String, Long>()
            testEvents.batchedAddedEvent.on(wait = true) { events ->
                events.forEach {
                    result[it] = System.nanoTime()
                }
            }
            testEvents.batchedAddedEvent.fire("test1")
            testEvents.batchedAddedEvent.fire("test2")
            testEvents.batchedAddedEvent.fire("test3")
            testEvents.batchedAddedEvent.fire("test4")
            delay(1000L)
            val timing = listOf("test1", "test2", "test3", "test4").windowed(2).map { (a, b) -> abs(result[a]!! - result[b]!!).nanoseconds }
            assert(timing.all { it < 10.milliseconds }) {
                "took too long to fire: $timing"
            }
        }
    }
}