package org.simbrain.network.llm

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DocumentSyncGateTest {

    @Test
    fun `deleting the last token while halted is an edit`() {
        val gate = DocumentSyncGate()
        gate.publish("a b", advancing = true)
        gate.publish("a b c", advancing = true)
        assertFalse(gate.isEdit("a b c", "a b c", advancing = false),
            "the echo of the final window confirms the tail")

        assertTrue(gate.isEdit("a b", "a b c", advancing = false),
            "once confirmed, a document matching the previous window is a real deletion")
    }

    @Test
    fun `the one-phase stale echo during advance is not an edit`() {
        val gate = DocumentSyncGate()
        gate.publish("a", advancing = true)
        gate.publish("a b", advancing = true)
        assertFalse(gate.isEdit("a", "a b", advancing = true),
            "couplings lag their producer by one iteration while advancing")
    }

    @Test
    fun `the stale echo after halting but before tail confirmation is not an edit`() {
        val gate = DocumentSyncGate()
        gate.publish("a", advancing = true)
        gate.invalidate()
        gate.publish("a b", advancing = false)
        assertFalse(gate.isEdit("a", "a b", advancing = false),
            "the lagging echo of the previous window is still absorbed before confirmation")
        assertFalse(gate.isEdit("a b", "a b", advancing = false), "confirmation")
        assertTrue(gate.isEdit("a", "a b", advancing = false),
            "after confirmation the same value is a real deletion")
    }
}
