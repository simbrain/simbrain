package org.simbrain.util.projection

import org.simbrain.util.UserParameter
import kotlin.math.pow

class SammonProjection2: ProjectionMethod2(), IterableProjectionMethod2 {

    val downstairsInitializationMethod = CoordinateProjection2()

    @UserParameter(label = "Epsilon", minimumValue = 0.0, increment = .1)
    var epsilon = 0.1

    override fun project(dataset: Dataset2) {
        upstairsDistances = dataset.computeUpstairsDistances()
        upstairsDistanceSum = upstairsDistances?.sumOf { it.sum() }
        dataset.perturbOverlappingPoints()
    }

    override fun addPoint(dataset: Dataset2, point: DataPoint2) {
        downstairsInitializationMethod.addPoint(dataset, point)
    }

    var upstairsDistances: List<List<Double>>? = null
    var downstairsDistances: List<List<Double>>? = null
    var upstairsDistanceSum: Double? = null

    override fun iterate(dataset: Dataset2) {
        if (dataset.kdTree.size < 2) return
        downstairsDistances = dataset.computeDownstairsDistances()
        dataset.kdTree.forEachIndexed { j, p1 ->
            var partialSum = 0.0
            (0 until p1.downstairsPoint.size).forEach { d ->
                dataset.kdTree.forEachIndexed { i, p2 ->
                    if (i != j) {
                        partialSum += (
                                (upstairsDistances!![i][j] - downstairsDistances!![i][j])
                                        * (p2.downstairsPoint[d] - p1.downstairsPoint[d])
                                ) / upstairsDistances!![i][j] / downstairsDistances!![i][j]
                    }
                }

                p1.downstairsPoint[d] = p1.downstairsPoint[d] - ((epsilon * 2 * partialSum) / upstairsDistanceSum!!)
            }
        }

        // Computes Closeness
        error = 0.0
        for (i in 0 until dataset.kdTree.size) {
            for (j in i + 1 until dataset.kdTree.size) {
                error += (upstairsDistances!![i][j] - downstairsDistances!![i][j]).pow(2) / upstairsDistances!![i][j]
            }
        }
        // println(e / upstairsDistanceSum!!)
    }

    override var error = 0.0

    override val name = "Sammon"

    override fun copy() = SammonProjection2()

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProjectionMethod2.getTypes()
        }
    }
}