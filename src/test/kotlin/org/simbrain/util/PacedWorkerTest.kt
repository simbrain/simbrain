/**
 * Tests for [PacedWorker]: in-order processing, rendezvous backpressure, reset dropping pending work
 * while the worker stays usable, and silent drops after close.
 */
package org.simbrain.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.Collections

@Timeout(20)
class PacedWorkerTest {

    @Test
    fun `items are processed in submission order`() {
        val processed = Collections.synchronizedList(mutableListOf<Int>())
        val done = CompletableDeferred<Unit>()
        val worker = PacedWorker<Int> {
            processed.add(it)
            if (it == 5) done.complete(Unit)
        }
        runBlocking {
            (1..5).forEach { worker.submit(it) }
            done.await()
        }
        assertEquals(listOf(1, 2, 3, 4, 5), processed)
        worker.close()
    }

    @Test
    fun `submit waits while the worker is busy`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val worker = PacedWorker<Int> {
            if (it == 1) {
                started.complete(Unit)
                gate.await()
            }
        }
        worker.submit(1)
        started.await()
        val second = launch { worker.submit(2) }
        repeat(20) { yield() }
        assertFalse(second.isCompleted, "Second submit completed while the worker was still busy")
        gate.complete(Unit)
        second.join()
        worker.close()
    }

    @Test
    fun `reset drops pending work and the worker accepts new items`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val done = CompletableDeferred<Unit>()
        val processed = Collections.synchronizedList(mutableListOf<Int>())
        val worker = PacedWorker<Int> { item ->
            if (item == 1) {
                started.complete(Unit)
                gate.await()
            }
            processed.add(item)
            if (item == 3) done.complete(Unit)
        }
        worker.submit(1)
        started.await()
        val waiting = launch { worker.submit(2) }
        repeat(20) { yield() }

        worker.reset()
        waiting.join()

        worker.submit(3)
        done.await()
        assertEquals(listOf(3), processed)
        worker.close()
    }

    @Test
    fun `submits after close are dropped silently`() {
        val processed = Collections.synchronizedList(mutableListOf<Int>())
        val worker = PacedWorker<Int> { processed.add(it) }
        worker.close()
        runBlocking { worker.submit(1) }
        assertTrue(processed.isEmpty())
    }
}
