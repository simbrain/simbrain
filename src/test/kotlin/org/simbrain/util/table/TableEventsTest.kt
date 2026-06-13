package org.simbrain.util.table

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TableEventsTest {

    private val events: TableEvents
    private var currentRowChangedCount = 0
    private var rowNameChangedCount = 0

    init {
        events = TableEvents()
        currentRowChangedCount = 0
        rowNameChangedCount = 0
    }

    @Test
    fun `test current row changed event`() = runBlocking {

        // Add event listener
        events.currentRowChanged.on {
            currentRowChangedCount++
        }

        // Fire barrier event and verify it ran before fire returns
        events.currentRowChanged.fire()
        assertEquals(1, currentRowChangedCount)

        // Fire multiple times
        events.currentRowChanged.fire()
        events.currentRowChanged.fire()
        assertEquals(3, currentRowChangedCount)
    }

    @Test
    fun `test row name changed event`() = runBlocking {
        // Add event listener
        events.rowNameChanged.on {
            rowNameChangedCount++
        }

        // Fire event and verify it was received
        events.rowNameChanged.fire()
        delay(10L)
        assertEquals(1, rowNameChangedCount)

        // Fire multiple times
        events.rowNameChanged.fire()
        events.rowNameChanged.fire()
        delay(10L)
        assertEquals(3, rowNameChangedCount)
    }

    @Test
    fun `test multiple listeners on same event`() = runBlocking {
        var listener1Count = 0
        var listener2Count = 0

        // Add multiple listeners to the same event
        events.currentRowChanged.on { listener1Count++ }
        events.currentRowChanged.on { listener2Count++ }

        // Fire barrier event once; both handlers must complete before fire returns
        events.currentRowChanged.fire()

        // Both listeners should have been called
        assertEquals(1, listener1Count)
        assertEquals(1, listener2Count)
    }

    @Test
    fun `test events are independent`() = runBlocking {
        // Add listeners to both events
        events.currentRowChanged.on { currentRowChangedCount++ }
        events.rowNameChanged.on { rowNameChangedCount++ }

        // Fire only the barrier event
        events.currentRowChanged.fire()

        // Only the corresponding counter should change
        assertEquals(1, currentRowChangedCount)
        assertEquals(0, rowNameChangedCount)

        // Fire the other event
        events.rowNameChanged.fire()
        delay(10L)

        // Now both should have changed
        assertEquals(1, currentRowChangedCount)
        assertEquals(1, rowNameChangedCount)
    }

    @Test
    fun `test barrier fire awaits handler completion`() = runBlocking {
        // The barrier fire must return only after the (suspending) handler finishes.
        var handlerFinished = false
        events.currentRowChanged.on {
            delay(20L)
            handlerFinished = true
        }

        events.currentRowChanged.fire()

        // No additional wait: if fire awaited the handler, this is already true.
        assertTrue(handlerFinished)
    }

    @Test
    fun `test event firing with no listeners`() {
        // Should not throw an exception when firing with no listeners
        assertDoesNotThrow {
            events.currentRowChanged.fireAsync()
            events.rowNameChanged.fire()
        }
    }

    @Test
    fun `test table events inheritance`() {
        // TableEvents should inherit from FlowEvents
        assertTrue(events.javaClass.superclass.name.contains("FlowEvents"))
    }

    @Test
    fun `test event types`() {
        // currentRowChanged is a barrier event; rowNameChanged is plain pub/sub
        assertTrue(events.currentRowChanged.javaClass.simpleName.contains("NoArgAwaitableEvent"))
        assertTrue(events.rowNameChanged.javaClass.simpleName.contains("NoArgEvent"))
    }

    @Test
    fun `test integration with dataframe`() = runBlocking {
        // Test that TableEvents can be used with a real dataframe
        val df = BasicDataFrame(listOf(
            listOf("A", "B"),
            listOf("C", "D")
        ))

        var eventFired = false
        df.events.rowNameChanged.on { eventFired = true }

        // Change row names should fire the event
        df.rowNames = listOf("Row1", "Row2")
        delay(10L)
        assertTrue(eventFired)
    }

    @Test
    fun `test event object properties`() {
        // Verify the events exist and are properly initialized
        assertNotNull(events.currentRowChanged)
        assertNotNull(events.rowNameChanged)
    }
}
