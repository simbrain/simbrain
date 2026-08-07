/**
 * Shared helpers for plot tests, mainly for awaiting asynchronous event propagation, e.g. label changes
 * relayed from network models to coupled plots.
 */
package org.simbrain.plot

import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Poll until [condition] holds, failing the test if it does not within [timeoutMillis].
 */
fun awaitUntil(timeoutMillis: Long = 5000, message: String = "Condition not met in time", condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (System.currentTimeMillis() < deadline) {
        if (condition()) return
        Thread.sleep(10)
    }
    assertTrue(condition(), message)
}
