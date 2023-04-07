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


    @Test
    fun `findClosestPoints finds closest points`() {
        val projector = Projector2(4)
        projector.init()
        println(projector.dataset)
        projector.addDataPoint(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0))
        projector.addDataPoint(doubleArrayOf(2.0, 3.0, 4.0, 5.0, 6.0))
        projector.addDataPoint(doubleArrayOf(3.0, 4.0, 5.0, 6.0, 7.0))
        projector.addDataPoint(doubleArrayOf(4.0, 5.0, 6.0, 7.0, 8.0))
        projector.addDataPoint(doubleArrayOf(5.0, 6.0, 7.0, 8.0, 9.0))

        val searchPoint = DataPoint2(doubleArrayOf(1.1, 2.0, 3.0, 4.0, 5.0))
        val closestPoints = projector.dataset.kdTree.findClosestNPoints(searchPoint, 3)
        closestPoints.forEach { point ->
            println("Closest point: $point")
        }

        projector.addDataPoint(doubleArrayOf(1.5, 2.0, 3.0, 4.0, 5.0))
        projector.addDataPoint(doubleArrayOf(2.5, 3.0, 4.0, 5.0, 6.0))
        projector.addDataPoint(doubleArrayOf(3.5, 4.0, 5.0, 6.0, 7.0))
        projector.addDataPoint(doubleArrayOf(4.5, 5.0, 6.0, 7.0, 8.0))
        projector.addDataPoint(doubleArrayOf(5.5, 6.0, 7.0, 8.0, 9.0))

        val closestPoints2 = projector.dataset.kdTree.findClosestNPoints(searchPoint, 3)
        closestPoints2.forEach { point ->
            println("Closest point: $point")
        }
    }
}