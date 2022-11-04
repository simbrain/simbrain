package org.simbrain.world.textworld

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import smile.math.matrix.Matrix

class TokenVectorMapTest {

    var map = TokenVectorMap(listOf("A", "B"), Matrix.eye(2))

    @Test
    fun `test retrieval`() {
        assertArrayEquals(doubleArrayOf(1.0, 0.0), map.get("A"))
        assertArrayEquals(doubleArrayOf(0.0, 1.0), map.get("B"))
    }

    // TODO: Below is what is happening but not sure we want this
    // @Test
    // fun `test zero vector when token is not in map`() {
    //     assertArrayEquals(doubleArrayOf(0.0, 0.0), map.get("C"))
    // }


}