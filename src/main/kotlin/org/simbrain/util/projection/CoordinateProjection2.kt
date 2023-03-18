package org.simbrain.util.projection

class CoordinateProjection2(dimension: Int): ProjectionMethod2(dimension) {

    var dim1 = 0
        set(value) {
            if (value in 0 until dimension) {
                field = value
            }
        }

    var dim2 = 1
        set(value) {
            if (value in 0 until dimension) {
                field = value
            }
        }

    override fun project(dataset: Dataset2) {
        dataset.kdTree.forEach {
            it.downstairsPoint[0] = it.upstairsPoint[dim1]
            it.downstairsPoint[1] = it.upstairsPoint[dim2]
        }
    }

    override fun initializeDownstairsPoint(dataset: Dataset2, point: DataPoint2) {
        point.downstairsPoint[0] = point.upstairsPoint[dim1]
        point.downstairsPoint[1] = point.upstairsPoint[dim2]
    }
}