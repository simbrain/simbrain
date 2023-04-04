package org.simbrain.util.projection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.simbrain.util.UserParameter
import org.simbrain.util.createDialog
import org.simbrain.util.display
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.Color

class Projector2(initialDimension: Int = 25) : EditableObject, CoroutineScope {

    @Transient
    private var job = SupervisorJob()

    @Transient
    override var coroutineContext = Dispatchers.Default + job

    @Transient
    var events = ProjectorEvents3()

    var dimension: Int = initialDimension
        set(value) {
            dataset = Dataset2(dimension)
            field = value
        }

    var dataset = Dataset2(dimension)

    @UserParameter(label = "Tolerance", minimumValue = 0.0, order =  1)
    var tolerance: Double = 0.1

    @UserParameter(label = "Connect points", order = 10)
    var connectPoints = false

    @UserParameter(label = "Hot color", order = 20)
    var hotColor = Color.red

    @UserParameter(label = "Base color", order = 30)
    var baseColor = Color.DARK_GRAY

    @UserParameter(label = "Show labels", description = "Show text labels sometimes associated with points", order = 40)
    var showLabels = true


    @UserParameter(label = "Projection Method", useSetter = true, isObjectType = true, order = 100)
    var projectionMethod: ProjectionMethod2 = CoordinateProjection2()
        set(value) {
            val oldMethod = field
            field = value
            project()
            events.methodChanged.fireAndForget(oldMethod, value)
        }

    fun addDataPoint(newPoint: DataPoint2) {
        synchronized(dataset) {
            val closestPoint = dataset.kdTree.findClosestPoint(newPoint)
            if (closestPoint != null && closestPoint.euclideanDistance(newPoint) < tolerance) {
                dataset.currentPoint = closestPoint
            } else {
                dataset.kdTree.insert(newPoint)
                dataset.currentPoint = newPoint
                projectionMethod.addPoint(dataset, newPoint)
            }
            events.datasetChanged.fireAndBlock()
        }
    }

    fun addDataPoint(array: DoubleArray) = addDataPoint(DataPoint2(array))

    fun project() = projectionMethod.project(dataset)

    private fun readResolve(): Any {
        job = SupervisorJob()
        coroutineContext = Dispatchers.Default + job
        events = ProjectorEvents3()
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