package org.simbrain.util.table

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TableEventsTest {

    private lateinit var events: TableEvents
    private var currentRowChangedCount = 0
    private var rowNameChangedCount = 0

    @BeforeEach
    fun setUp() {
        events = TableEvents()
        currentRowChangedCount = 0
        rowNameChangedCount = 0
    }

    @Test
    fun `test current row changed event`() = runBlocking{

        // Add event listener
        events.currentRowChanged.on(wait = true) {
            currentRowChangedCount++
        }

        // Fire event and verify it was received
        events.currentRowChanged.fire().await()
        assertEquals(1, currentRowChangedCount)

        // Fire multiple times
        events.currentRowChanged.fire().await()
        events.currentRowChanged.fire().await()
        assertEquals(3, currentRowChangedCount)
    }

    @Test
    fun `test row name changed event`() = runBlocking{
        // Add event listener
        events.rowNameChanged.on(wait = true) {
            rowNameChangedCount++
        }

        // Fire event and verify it was received
        events.rowNameChanged.fire().await()
        assertEquals(1, rowNameChangedCount)

        // Fire multiple times
        events.rowNameChanged.fire().await()
        events.rowNameChanged.fire().await()
        assertEquals(3, rowNameChangedCount)
    }

    @Test
    fun `test multiple listeners on same event`() = runBlocking {
        var listener1Count = 0
        var listener2Count = 0

        // Add multiple listeners to the same event
        events.currentRowChanged.on(wait = true) { listener1Count++ }
        events.currentRowChanged.on(wait = true) { listener2Count++ }

        // Fire event once
        events.currentRowChanged.fire().await()

        // Both listeners should have been called
        assertEquals(1, listener1Count)
        assertEquals(1, listener2Count)
    }

    @Test
    fun `test events are independent`() = runBlocking {
        // Add listeners to both events
        events.currentRowChanged.on(wait = true) { currentRowChangedCount++ }
        events.rowNameChanged.on(wait = true) { rowNameChangedCount++ }

        // Fire only one event
        events.currentRowChanged.fire().await()

        // Only the corresponding counter should change
        assertEquals(1, currentRowChangedCount)
        assertEquals(0, rowNameChangedCount)

        // Fire the other event
        events.rowNameChanged.fire().await()

        // Now both should have changed
        assertEquals(1, currentRowChangedCount)
        assertEquals(1, rowNameChangedCount)
    }

    @Test
    fun `test event firing with no listeners`() {
        // Should not throw an exception when firing with no listeners
        assertDoesNotThrow {
            events.currentRowChanged.fire()
            events.rowNameChanged.fire()
        }
    }

    @Test
    fun `test table events inheritance`() {
        // TableEvents should inherit from Events
        assertTrue(events.javaClass.superclass.name.contains("Events"))
    }

    @Test
    fun `test event types are no arg events`() {
        // Both events should be NoArgEvent types
        assertTrue(events.currentRowChanged.javaClass.simpleName.contains("NoArgEvent"))
        assertTrue(events.rowNameChanged.javaClass.simpleName.contains("NoArgEvent"))
    }

    @Test
    fun `test integration with dataframe`() = runBlocking{
        // Test that TableEvents can be used with a real dataframe
        val df = BasicDataFrame(listOf(
            listOf("A", "B"),
            listOf("C", "D")
        ))

        var eventFired = false
        df.events.rowNameChanged.on(wait = true) { eventFired = true }

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