/**
 * Tests for [ControlPanelKt]'s button-task execution contract: non-suspending tasks run one at a
 * time in click order (so rapid clicks cannot interleave writes to shared model state), while an
 * explicitly passed dispatcher opts a task out of that serialization.
 */
package org.simbrain.util

import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.event.ActionEvent
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JButton

class ControlPanelKtTest {

    private fun JButton.click() {
        actionListeners.forEach { it.actionPerformed(ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click")) }
    }

    @Test
    fun `button tasks run one at a time in click order`() {
        val panel = ControlPanelKt()
        val numClicks = 20
        val order = Collections.synchronizedList(mutableListOf<Int>())
        val inTask = AtomicBoolean(false)
        val overlapped = AtomicBoolean(false)
        val done = CountDownLatch(numClicks)
        val buttons = (0 until numClicks).map { i ->
            panel.addButton("b$i") {
                if (!inTask.compareAndSet(false, true)) overlapped.set(true)
                order.add(i)
                Thread.sleep(1)
                inTask.set(false)
                done.countDown()
            }
        }
        buttons.forEach { it.click() }
        assertTrue(done.await(10, TimeUnit.SECONDS), "button tasks did not complete in time")
        assertFalse(overlapped.get(), "two button tasks ran concurrently")
        assertEquals((0 until numClicks).toList(), order.toList())
    }

    @Test
    fun `explicit dispatcher opts a button out of serialization`() {
        val panel = ControlPanelKt()
        val blockerStarted = CountDownLatch(1)
        val release = CountDownLatch(1)
        val concurrentRan = CountDownLatch(1)
        val blocker = panel.addButton("blocker") {
            blockerStarted.countDown()
            release.await(10, TimeUnit.SECONDS)
        }
        val concurrent = panel.addButton("concurrent", context = Dispatchers.Default) {
            concurrentRan.countDown()
        }
        blocker.click()
        assertTrue(blockerStarted.await(10, TimeUnit.SECONDS), "blocker task did not start")
        concurrent.click()
        val ranWhileBlocked = concurrentRan.await(2, TimeUnit.SECONDS)
        release.countDown()
        assertTrue(ranWhileBlocked, "opted-out task should run while a serialized task is blocked")
    }
}
