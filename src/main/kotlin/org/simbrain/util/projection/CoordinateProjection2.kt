package org.simbrain.util.projection

import org.simbrain.util.UserParameter

class CoordinateProjection2: ProjectionMethod2() {

    @UserParameter(label = "dim1", minimumValue = 0.0)
    var dim1 = 0

    @UserParameter(label = "dim2", minimumValue = 0.0)
    var dim2 = 1

    override fun project(dataset: Dataset2) {
        dataset.kdTree.forEach {
            projectPoint(it)
        }
    }

    private fun projectPoint(point: DataPoint2) {
        point.downstairsPoint[0] = point.upstairsPoint[dim1]
        point.downstairsPoint[1] = point.upstairsPoint[dim2]
    }

    override fun addPoint(dataset: Dataset2, point: DataPoint2) {
        projectPoint(point)
    }

    override fun copy() = CoordinateProjection2()

    override val name = "Coordinate"

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProjectionMethod2.getTypes()
        }
    }
}