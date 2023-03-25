package org.simbrain.util.projection

import org.simbrain.util.euclideanDistance
import kotlin.random.Random

class Dataset2(val dimension: Int) {

    val kdTree = KDTree(dimension)

    fun computeUpstairsArray() = kdTree.map { it.upstairsPoint }.toTypedArray()

    fun computeDownstairsArray() = kdTree.map { it.downstairsPoint }.toTypedArray()

    fun computeUpstairsDistances() = kdTree.map {  a ->
        kdTree.map { b ->
            a.upstairsPoint.euclideanDistance(b.upstairsPoint)
        }
    }

    fun computeDownstairsDistances() = kdTree.map {  a ->
        kdTree.map { b ->
            a.downstairsPoint.euclideanDistance(b.downstairsPoint)
        }
    }

    var currentPoint: DataPoint2? = null

    fun setDownstairsData(data: Array<DoubleArray>) {
        (kdTree zip data).forEach { (datapoint, downstairsPoint) ->
            datapoint.setDownstairs(downstairsPoint)
        }
    }

    fun randomizeDownstairs() {
        kdTree.forEach {
            it.setDownstairs(DoubleArray(it.downstairsPoint.size) {
                Random.nextDouble()
            })
        }
    }

    override fun toString() = """
        |upstairs:
        |${kdTree.joinToString("\n|") { it.upstairsPoint.contentToString() }}
        |downstairs:
        |${kdTree.joinToString("\n|") { it.downstairsPoint.contentToString() }}
    """.trimMargin()
}