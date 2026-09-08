package org.simbrain.workspace

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorkspaceUpdateDelayTest {

    @Test
    fun `the update delay pauses after every iteration`() = runBlocking {
        val workspace = Workspace()
        workspace.updateDelay = 40
        val start = System.nanoTime()
        workspace.updater.iterate(3)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue(elapsedMs >= 120, "three iterations should take at least 120 ms, took $elapsedMs")
    }

    @Test
    fun `clearing the workspace resets the update delay`() {
        val workspace = Workspace()
        workspace.updateDelay = 200
        workspace.clearWorkspace()
        assertEquals(0, workspace.updateDelay)
    }
}
