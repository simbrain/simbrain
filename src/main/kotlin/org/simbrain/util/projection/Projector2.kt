package org.simbrain.util.projection

class Projector2(initialDimension: Int) {

    var dimension: Int = initialDimension
        set(value) {
            dataset = Dataset2(dimension)
            field = value
        }

    var dataset = Dataset2(dimension)

    var tolerance: Double = 0.1

    var projectionMethod: ProjectionMethod2 = CoordinateProjection2(dimension)

    fun addDataPoint(newPoint: DataPoint2) {
        val closestPoint = dataset.kdTree.findClosestNPoint(newPoint)
        if (closestPoint != null && closestPoint.euclideanDistance(newPoint) < tolerance) {
            dataset.currentPoint = closestPoint
        } else {
            dataset.kdTree.insert(newPoint)
            dataset.currentPoint = newPoint
            projectionMethod.initializeDownstairsPoint(dataset, newPoint)
        }
    }

    fun addDataPoint(array: DoubleArray) = addDataPoint(DataPoint2(array))

    fun project() = projectionMethod.project(dataset)

}

fun main() {
    val projector = Projector2(4)
    projector.project()
    println(projector.dataset)
    projector.addDataPoint(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0))
    projector.addDataPoint(doubleArrayOf(2.0, 3.0, 4.0, 5.0, 6.0))
    projector.addDataPoint(doubleArrayOf(3.0, 4.0, 5.0, 6.0, 7.0))
    projector.addDataPoint(doubleArrayOf(4.0, 5.0, 6.0, 7.0, 8.0))
    projector.addDataPoint(doubleArrayOf(5.0, 6.0, 7.0, 8.0, 9.0))
    println(projector.dataset)
    projector.project()
    println(projector.dataset)
}