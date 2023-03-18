package org.simbrain.util.projection

class Dataset2(val dimension: Int) {

    val kdTree = KDTree(dimension)

    fun computeUpstairsArray() = kdTree.map { it.upstairsPoint }.toTypedArray()

    fun computeDownstairsArray() = kdTree.map { it.downstairsPoint }.toTypedArray()

    var currentPoint: DataPoint2? = null

    fun setDownstairsData(data: Array<DoubleArray>) {
        (kdTree zip data).forEach { (datapoint, downstairsPoint) ->
            datapoint.setDownstairs(downstairsPoint)
        }
    }

    override fun toString() = """
        |upstairs:
        |${kdTree.joinToString("\n|") { it.upstairsPoint.contentToString() }}
        |downstairs:
        |${kdTree.joinToString("\n|") { it.downstairsPoint.contentToString() }}
    """.trimMargin()
}