/**
 * How a workspace run is paced against its views: by default each iteration waits once for the display to catch up
 * (through the hook the desktop installs), a workspace set to run as fast as possible never does, and the update delay
 * slows every iteration down.
 */
package org.simbrain.workspace

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorkspacePacingTest {

    @Test
    fun `each iteration waits once for the display by default`() = runBlocking {
        val workspace = Workspace()
        var syncs = 0
        workspace.displaySync = { syncs++ }
        workspace.iterateSuspend(5)
        assertEquals(5, syncs)
    }

    @Test
    fun `a workspace running as fast as possible never waits for the display`() = runBlocking {
        val workspace = Workspace()
        var syncs = 0
        workspace.displaySync = { syncs++ }
        workspace.runAsFastAsPossible = true
        workspace.iterateSuspend(5)
        assertEquals(0, syncs)
    }

    @Test
    fun `the update delay slows each iteration`() = runBlocking {
        val workspace = Workspace()
        workspace.updateDelay = 20
        val start = System.nanoTime()
        workspace.iterateSuspend(5)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue(elapsedMs >= 100, "5 iterations with a 20 ms delay took only $elapsedMs ms")
    }

    @Test
    fun `clearing the workspace restores default pacing`() {
        val workspace = Workspace()
        workspace.runAsFastAsPossible = true
        workspace.updateDelay = 50
        workspace.clearWorkspace()
        assertFalse(workspace.runAsFastAsPossible)
        assertEquals(0, workspace.updateDelay)
    }

    @Test
    fun `pacing settings are saved with the workspace`() = runBlocking {
        val workspace = Workspace()
        workspace.runAsFastAsPossible = true
        workspace.updateDelay = 30
        val saved = workspace.zipDataHeadless
        val reopened = Workspace()
        reopened.openFromZipData(saved)
        assertTrue(reopened.runAsFastAsPossible)
        assertEquals(30, reopened.updateDelay)
    }
}
