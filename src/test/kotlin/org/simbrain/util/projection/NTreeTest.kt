package org.simbrain.util.projection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NTreeTest {

    var ntree: NTree = NTree(10)
    var p1 = DataPoint(doubleArrayOf(1.0, 0.0))
    var p2 = DataPoint(doubleArrayOf(0.5, 0.0))
    var p3 = DataPoint(doubleArrayOf(0.0, 0.0))

    @Test
    fun `test closest point`() {
        ntree.add(p1)
        ntree.add(p2)
        ntree.add(p3)
        val p1_near = DataPoint(doubleArrayOf(1.0, 0.1))
        ntree.add(p1_near)
        assertEquals(p1_near, ntree.getClosestPoint(p3))
    }

}

