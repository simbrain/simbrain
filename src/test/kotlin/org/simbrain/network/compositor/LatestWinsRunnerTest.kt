package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LatestWinsRunnerTest {

    @Test
    fun `the latest wins runner drops intermediate blocks and runs the last`() {
        val queue = ArrayDeque<Runnable>()
        val ran = mutableListOf<Int>()
        val runner = LatestWinsRunner({ queue.add(it) })
        runner.submit { ran.add(1) }
        runner.submit { ran.add(2) }
        runner.submit { ran.add(3) }
        while (queue.isNotEmpty()) queue.removeFirst().run()
        assertEquals(listOf(3), ran)
    }

    @Test
    fun `a block submitted mid-drain still runs`() {
        val queue = ArrayDeque<Runnable>()
        val ran = mutableListOf<String>()
        lateinit var runner: LatestWinsRunner
        runner = LatestWinsRunner({ queue.add(it) }, onRan = { ran.add("landed") })
        runner.submit {
            ran.add("a")
            runner.submit { ran.add("b") }
        }
        while (queue.isNotEmpty()) queue.removeFirst().run()
        assertEquals(listOf("a", "landed", "b", "landed"), ran)
    }
}
