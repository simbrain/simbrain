package org.simbrain.util.projection

abstract class ProjectionMethod2(val dimension: Int) {

    abstract fun project(dataset: Dataset2)

    abstract fun initializeDownstairsPoint(dataset: Dataset2, point: DataPoint2)

}