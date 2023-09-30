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
            dataset = Dataset2(value)
            field = value
        }

    /**
     * The main data structure for a projection. A set of [DataPoint2]s, each of which has two double arrays, one of
     * which ("upstairs") represents the high dimensional data, and the other of which ("downstairs") represents the
     * low dimensional data.
     */
    var dataset = Dataset2(dimension)

    /**
     * The method used to project from high dimensional data upstairs to low dimensional data downstairs.
     */
    @UserParameter(label = "Projection Method", order = 100)
    var projectionMethod: ProjectionMethod2 = PCAProjection2()
        set(value) {
            val oldMethod = field
            field = value
            init()
            events.methodChanged.fireAndForget(oldMethod, value)
        }

    @UserParameter(label = "Tolerance", minimumValue = 0.0, increment = .1, order =  1)
    var tolerance: Double = 0.1

    @UserParameter(label = "Connect points", order = 10)
    var connectPoints = false

    @UserParameter(label = "Hot color", order = 20)
    var hotColor = Color.red

    @UserParameter(label = "Base color", order = 30)
    var baseColor = Color.DARK_GRAY

    @UserParameter(label = "Show labels", description = "Show text labels sometimes associated with points", order = 40)
    var showLabels = true

    @UserParameter(label = "Use hot point", description = "If true, current point is rendered using the hotpoint color", order = 50)
    var useHotColor = true

    @UserParameter(label = "Coloring Manager", order = 110)
    var coloringManager: ColoringManager = NoOpColoringManager().also { it.projector = this }

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

    fun init() {
        projectionMethod.init(dataset)
    }

    fun addDataPoint(array: DoubleArray) = addDataPoint(DataPoint2(array))

    private fun readResolve(): Any {
        job = SupervisorJob()
        coroutineContext = Dispatchers.Default + job
        events = ProjectorEvents3()
        return this
    }

}

fun main() {
    val projector = Projector2(4)
    projector.init()
    println(projector.dataset)
    projector.addDataPoint(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0))
    projector.addDataPoint(doubleArrayOf(2.0, 3.0, 4.0, 5.0, 6.0))
    projector.addDataPoint(doubleArrayOf(3.0, 4.0, 5.0, 6.0, 7.0))
    projector.addDataPoint(doubleArrayOf(4.0, 5.0, 6.0, 7.0, 8.0))
    projector.addDataPoint(doubleArrayOf(5.0, 6.0, 7.0, 8.0, 9.0))
    println(projector.dataset)
    projector.init()
    println(projector.dataset)
    projector.createDialog {

    }.display()
}