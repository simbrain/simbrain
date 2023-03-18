package org.simbrain.util.projection

class SammonProjection2(dimension: Int): ProjectionMethod2(dimension) {

    val downstairsInitializationMethod = CoordinateProjection2(dimension)

    override fun project(dataset: Dataset2) {
    }

    override fun initializeDownstairsPoint(dataset: Dataset2, point: DataPoint2) {
        downstairsInitializationMethod.initializeDownstairsPoint(dataset, point)
    }

    fun iterate(dataset: Dataset2) {
        if (dataset.kdTree.size < 2) return


    }

}