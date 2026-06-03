package org.simbrain.workspace

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.newSim

class WorkspaceIterateTest {

    val workspace = Workspace()

    @Test
    fun `simple iterate should call a custom action each iteration`() {
        var counter = 0
        workspace.addUpdateAction("increment counter"){
            counter++
        }
        repeat(10) {
            workspace.simpleIterate()
        }
        assertEquals(10, counter)
    }

    @Test
    fun `iterateSuspend(n) should call a custom action each iteration`() {
        var counter = 0
        workspace.addUpdateAction("increment counter"){
            println("Update action: $counter")
            counter++
        }
        runBlocking {
            workspace.iterateSuspend(10)
            assertEquals(10, counter)
        }
    }

    @Test
    fun `iterateSuspend inside a simulation build does not deadlock`() {
        var counter = 0
        lateinit var builtWorkspace: Workspace
        val sim = newSim {
            builtWorkspace = workspace
            workspace.addUpdateAction("increment counter") { counter++ }
            workspace.iterateSuspend(5)
        }
        runBlocking {
            withTimeout(30_000) { sim.run() }
        }
        assertEquals(5, counter)
        assertEquals(5, builtWorkspace.time)
    }

    @Test
    fun `iterateWhile inside a simulation build does not deadlock`() {
        var counter = 0
        val sim = newSim {
            workspace.addUpdateAction("increment counter") { counter++ }
            workspace.iterateWhile { counter < 5 }
        }
        runBlocking {
            withTimeout(30_000) { sim.run() }
        }
        assertEquals(5, counter)
    }

}