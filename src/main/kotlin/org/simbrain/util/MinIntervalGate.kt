/**
 * Event-thread rate gate shared by the chart refresh paths: runs an action immediately when at least
 * [minMillis] has passed since the last [stamp], otherwise arms a single-shot timer for the tail of
 * the interval so the final request still lands. Stamping is explicit rather than implied by the
 * action running, because callers measure from different moments (a rebuild's start, a draw's end).
 */
package org.simbrain.util

import javax.swing.Timer

class MinIntervalGate(private val minMillis: Int, private val action: () -> Unit) {

    private var lastStampAt = 0L

    private val tailTimer = Timer(minMillis) { request() }.apply { isRepeats = false }

    /** Run [action] now if the interval has elapsed, else schedule one run for when it has. */
    fun request() {
        val elapsed = System.currentTimeMillis() - lastStampAt
        if (elapsed >= minMillis) {
            action()
        } else if (!tailTimer.isRunning) {
            tailTimer.initialDelay = (minMillis - elapsed).toInt().coerceAtLeast(1)
            tailTimer.restart()
        }
    }

    /** Mark now as the start of a fresh interval. */
    fun stamp() {
        lastStampAt = System.currentTimeMillis()
    }

    fun stop() {
        tailTimer.stop()
    }
}
