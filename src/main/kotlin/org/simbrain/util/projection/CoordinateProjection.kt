package org.simbrain.util.projection

import org.simbrain.util.UserParameter

// TODO: Auto-find feature from 3. But if used, do NOT use it on 2d data
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

}