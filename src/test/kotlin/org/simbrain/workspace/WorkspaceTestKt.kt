package org.simbrain.workspace

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Also see [WorkspaceTest] for java based test
 */
class WorkspaceTestKt {

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
    fun `iterate(n)'s finishing task should be called after n iterations'`() {
        var counter = 0
        workspace.addUpdateAction("increment counter"){
            println("Update action: $counter")
            counter++
        }
        workspace.iterate(10) {
            counter++
        }
        Thread.sleep(500)
        assertEquals(11, counter)
    }

}