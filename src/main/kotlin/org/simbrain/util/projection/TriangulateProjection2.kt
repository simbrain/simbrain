package org.simbrain.util.projection

import kotlin.math.abs
import kotlin.math.pow

/**
 * De Ridder, D., & Duin, R. P. (1997). Sammon's mapping using neural networks: A comparison. Pattern Recognition Letters, 18(11-13), 1307-1316.
 *
 * https://www.sciencedirect.com/science/article/abs/pii/S0167865597000937
 */
class TriangulateProjection2: ProjectionMethod2() {

    override fun init(dataset: Dataset2) {

    }

    private fun triangulatePoint(dataset: Dataset2, point: DataPoint2) {
        when (dataset.kdTree.size) {
            0 -> point.setDownstairs(doubleArrayOf(0.0, 0.0))
            1 -> {
                val nearestPoint = dataset.kdTree.findClosestPoint(point)!!
                val distance = nearestPoint.euclideanDistance(point)
                point.setDownstairs(doubleArrayOf(distance, 0.0))
            }
            2 -> {
                val (p1, p2) = dataset.kdTree.findClosestNPoints(point, 2)
                val d1 = p1.euclideanDistance(point)
                val d2 = p2.euclideanDistance(point)
                val x = (d1 * p1.downstairsPoint[0] + d2 * p2.downstairsPoint[0]) / (d1 + d2)
                val y = (d1 * p1.downstairsPoint[1] + d2 * p2.downstairsPoint[1]) / (d1 + d2)
                point.setDownstairs(doubleArrayOf(x, y))
            }
            else -> {
                val (p1, p2, p3) = dataset.kdTree.findClosestNPoints(point, 3)
                val (p1x, p1y) = p1.downstairsPoint
                val (p2x, p2y) = p2.downstairsPoint
                val (p3x, p3y) = p3.downstairsPoint
                val dist = ((p1x - p2x).pow(2) + (p1y - p2y).pow(2))
                val d1 = p1.euclideanDistance(point)
                val d2 = p2.euclideanDistance(point)
                val disc = (dist - (d2 - d1) * (d2 - d1)) * ((d2 + d1) * (d2 + d1) - dist)
                if (disc < 0) {
                    val x = (d1 * p1x + d2 * p2x) / (d1 + d2)
                    val y = (d1 * p1y + d2 * p2y) / (d1 + d2)
                    point.setDownstairs(doubleArrayOf(x, y))
                } else {
                    // Find candidates for intersection points of circles
                    val discx: Double =
                        (p1y - p2y) * (p1y - p2y) * disc
                    val discy: Double =
                        (p1x - p2x) * (p1x - p2x) * disc
                    val xfront: Double =
                        (p1x + p2x) * dist + (d1 * d1 - d2 * d2) * (p2x - p1x)
                    val yfront: Double =
                        (p1y + p2y) * dist + (d1 * d1 - d2 * d2) * (p2y - p1y)
                    val xplus = (xfront + Math.sqrt(discx)) / dist / 2
                    val xminus = (xfront - Math.sqrt(discx)) / dist / 2
                    val yplus = (yfront + Math.sqrt(discy)) / dist / 2
                    val yminus = (yfront - Math.sqrt(discy)) / dist / 2

                    // Find out which of the candidates are the intersection points
                    val d3 = p3.euclideanDistance(point)
                    if ((p1x - p2x) * (p1y - p2y) > 0) {
                        // mindful of the sign of the square root
                        val d1 = (xplus - p3x) * (xplus - p3x) + (yminus - p3y) * yminus - p3y
                        val d2 = (xminus - p3x) * (xminus - p3x) + (yplus - p3y) * yplus - p3y
                        // check which intersection maintain proper distance from third point
                        if (abs(d3 - d1) < abs(d3 - d2)) {
                            point.setDownstairs(doubleArrayOf(xplus, yminus))
                        } else {
                            point.setDownstairs(doubleArrayOf(xminus, yplus))
                        }
                    } else {
                        val d1 = (xplus - p3x) * (xplus - p3x) + (yplus - p3y) * yplus - p3y
                        val d2 = (xminus - p3x) * (xminus - p3x) + (yminus - p3y) * yminus - p3y
                        // check which intersection maintain proper distance from third point
                        if (abs(d3 - d1) < abs(d3 - d2)) {
                            point.setDownstairs(doubleArrayOf(xplus, yplus))
                        } else {
                            point.setDownstairs(doubleArrayOf(xminus, yminus))
                        }
                    }
                }
            }
        }
    }

    override fun addPoint(dataset: Dataset2, point: DataPoint2) {
        triangulatePoint(dataset, point)
    }

    override fun copy() = TriangulateProjection2()

    override val name = "Triangulate"

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProjectionMethod2.getTypes()
        }
    }
}