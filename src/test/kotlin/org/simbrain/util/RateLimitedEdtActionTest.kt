package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities

class RateLimitedEdtActionTest {

    @Test
    fun `a burst runs once immediately and once more at the trailing edge`() {
        val count = AtomicInteger()
        val action = RateLimitedEdtAction(50) { count.incrementAndGet() }

        SwingUtilities.invokeAndWait { repeat(10) { action() } }
        assertEquals(1, count.get(), "the burst's first call runs on the leading edge")

        val deadline = System.currentTimeMillis() + 2000
        while (count.get() < 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
        assertEquals(2, count.get(), "the rest of the burst coalesces into one trailing run")
    }

    @Test
    fun `spaced calls all run immediately`() {
        val count = AtomicInteger()
        val action = RateLimitedEdtAction(20) { count.incrementAndGet() }
        repeat(3) {
            SwingUtilities.invokeAndWait { action() }
            Thread.sleep(40)
        }
        assertEquals(3, count.get())
    }
}
