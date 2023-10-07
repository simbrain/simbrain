package org.simbrain.util.projection

import org.simbrain.util.UserParameter

class CoordinateProjection: ProjectionMethod() {

    @UserParameter(label = "dim1", minimumValue = 0.0)
    var dim1 = 0

    @UserParameter(label = "dim2", minimumValue = 0.0)
    var dim2 = 1

    override fun init(dataset: Dataset) {
        dataset.kdTree.forEach {
            projectPoint(it)
        }
    }

    private fun projectPoint(point: DataPoint) {
        point.downstairsPoint[0] = point.upstairsPoint[dim1]
        point.downstairsPoint[1] = point.upstairsPoint[dim2]
    }

    override fun addPoint(dataset: Dataset, point: DataPoint) {
        projectPoint(point)
    }

    override fun copy() = CoordinateProjection()

    override val name = "Coordinate"

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProjectionMethod.getTypes()
        }
    }
}