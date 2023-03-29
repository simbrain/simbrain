package org.simbrain.util.projection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.simbrain.util.UserParameter
import org.simbrain.util.createDialog
import org.simbrain.util.display
import org.simbrain.util.propertyeditor.EditableObject

class Projector2(initialDimension: Int) : EditableObject, CoroutineScope {

    @Transient
    private var job = SupervisorJob()

    @Transient
    override var coroutineContext = Dispatchers.Default + job

    @Transient
    val events = ProjectorEvents3()

    var dimension: Int = initialDimension
        set(value) {
            dataset = Dataset2(dimension)
            field = value
        }

    var dataset = Dataset2(dimension)

    @UserParameter(label = "tolerance", minimumValue = 0.0)
    var tolerance: Double = 0.1

    @UserParameter(label = "Projection Method", useSetter = true, isObjectType = true)
    var projectionMethod: ProjectionMethod2 = CoordinateProjection2()
        set(value) {
            val oldMethod = field
            field = value
            project()
            events.methodChanged.fireAndForget(oldMethod, value)
        }

    fun addDataPoint(newPoint: DataPoint2) {
        val closestPoint = dataset.kdTree.findClosestNPoint(newPoint)
        if (closestPoint != null && closestPoint.euclideanDistance(newPoint) < tolerance) {
            dataset.currentPoint = closestPoint
        } else {
            dataset.kdTree.insert(newPoint)
            dataset.currentPoint = newPoint
            projectionMethod.addPoint(dataset, newPoint)
        }
    }

    fun addDataPoint(array: DoubleArray) = addDataPoint(DataPoint2(array))

    fun project() = projectionMethod.project(dataset)

    private fun readResolve(): Any {
        job = SupervisorJob()
        coroutineContext = Dispatchers.Default + job
        return this
    }

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
    projector.createDialog {

    }.display()
}