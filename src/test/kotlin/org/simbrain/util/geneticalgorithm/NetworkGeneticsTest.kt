package org.simbrain.util.geneticalgorithm

import org.junit.jupiter.api.Test
import kotlin.random.Random

class NetworkGeneticsTest {

    @Test
    fun `test run one`() {
        var count = 100
        val expected = count
        repeat(100) {
            Random.runOne(1 to { count++ }, 1 to { count-- })
        }
        assert(count in expected-20..expected+20)
    }
}