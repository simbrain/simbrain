package org.simbrain.util.projection

import org.simbrain.util.UserParameter

class CoordinateProjection2 @JvmOverloads constructor (dimension: Int = 3): ProjectionMethod2(dimension) {

    @UserParameter(label = "dim1")
    var dim1 = 0
        set(value) {
            if (value in 0 until dimension) {
                field = value
            }
        }

    @UserParameter(label = "dim2")
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

    override fun copy() = CoordinateProjection2(dimension)

    override val name = "Coordinate"

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProjectionMethod2.getTypes()
        }
    }
}