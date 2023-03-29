package org.simbrain.util.projection

import org.junit.jupiter.api.Test

class KDTreeTest {

    @Test
    fun `iteration order is deterministic`() {
        val points = listOf(
            DataPoint2(doubleArrayOf(3.0, 6.0)),
            DataPoint2(doubleArrayOf(3.0, 6.0)),
            DataPoint2(doubleArrayOf(3.0, 6.0)),
            DataPoint2(doubleArrayOf(3.0, 6.0)),
            DataPoint2(doubleArrayOf(17.0, 15.0)),
            DataPoint2(doubleArrayOf(13.0, 15.0)),
            DataPoint2(doubleArrayOf(6.0, 12.0)),
            DataPoint2(doubleArrayOf(9.0, 1.0)),
            DataPoint2(doubleArrayOf(9.0, 1.0)),
            DataPoint2(doubleArrayOf(9.0, 1.0)),
            DataPoint2(doubleArrayOf(9.0, 1.0)),
            DataPoint2(doubleArrayOf(2.0, 7.0)),
            DataPoint2(doubleArrayOf(2.0, 7.0)),
            DataPoint2(doubleArrayOf(2.0, 7.0)),
            DataPoint2(doubleArrayOf(2.0, 7.0)),
            DataPoint2(doubleArrayOf(2.0, 7.0))
        )

        val kdTree = KDTree(2)
        points.forEach {
            kdTree.insert(it)
        }
        val a = kdTree.toList()
        val b = kdTree.toList()
        (a zip b).forEach { (aa, bb) ->
            assert(aa === bb)
        }
    }
}