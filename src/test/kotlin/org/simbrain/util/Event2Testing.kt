package org.simbrain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis


class TestEvents : Events2() {
    val throttlingEvent = NoArgEvent(interval = 100, timingMode = TimingMode.Throttle)
    val debouncingEvent = NoArgEvent(interval = 100, timingMode = TimingMode.Debounce)
    val longEvent = NoArgEvent()
    val blockingEvent = NoArgEvent()
    val longFireAndForgetEvent = NoArgEvent()
    val changedEvent = ChangedEvent<String>()
}

class Event2Testing {

    val testEvents2 = TestEvents()

    @Test
    fun `ensure throttle is limiting rates`() {
        var counter = 0
        testEvents2.throttlingEvent.on {
            counter++
        }
        runBlocking {
            repeat(20) {
                testEvents2.throttlingEvent.fireAndForget()
                delay(50L)
            }
            // 10 events for 1 second (20*50 milliseconds)
            assertEquals(10, counter)
        }
    }

    @Test
    fun `ensure debounce is limiting rates`() {
        var counter = 0
        testEvents2.debouncingEvent.on {
            counter++
        }
        runBlocking {
            testEvents2.debouncingEvent.fireAndForget()
            delay(50L)
            assertEquals(0, counter, "should not fire before timeout")
            delay(100L)
            assertEquals(1, counter, "event should fire after timeout")

            counter = 0

            repeat(10) {
                testEvents2.debouncingEvent.fireAndForget()
                delay(50L)
            }
            delay(100L)
            assertEquals(1, counter, "all but one event should fire after timeout")
        }
    }

    @Test
    fun `ensure suspending events wait for handler to complete`() {
        testEvents2.longEvent.on {
            delay(100L)
        }
        val time = measureTimeMillis {
            runBlocking {
                repeat(10) {
                    testEvents2.longEvent.fireAndSuspend()
                }
            }
        }
        assert(time > 1000) { "expect test to take longer than 1 seconds, took actually ${time}ms" }
    }

    @Test
    fun `ensure blocking events wait for handler to complete`() {
        testEvents2.blockingEvent.on {
            runBlocking {
                delay(100L)
            }
        }
        val time = measureTimeMillis {
            repeat(10) {
                testEvents2.blockingEvent.fireAndBlock()
            }
        }
        assert(time > 1000) { "expect test to take longer than 1 seconds, took actually ${time}ms" }
    }

    @Test
    fun `ensure fireAndForget() doesn't wait for handler to complete`() {
        testEvents2.longFireAndForgetEvent.on(Dispatchers.Default) {
            delay(200L)
        }
        val time = measureTimeMillis {
            repeat(20) {
                testEvents2.longFireAndForgetEvent.fireAndForget()
            }
        }
        assert(time < 1500) { "expect test to take only a bit more than 1 second, took actually ${time}ms" }
    }

    @Test
    fun `if old and new are the same, event should not fire, else it should fire`() {
        var fired = false
        testEvents2.changedEvent.on { _, _ ->
            fired = true
        }
        testEvents2.changedEvent.fireAndBlock("test", "test")
        assert(!fired)
        testEvents2.changedEvent.fireAndBlock("test", "test2")
        assert(fired)
    }
}