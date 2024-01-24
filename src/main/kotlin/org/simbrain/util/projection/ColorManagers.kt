package org.simbrain.util.projection

import org.simbrain.util.HSBInterpolate
import org.simbrain.util.UserParameter
import org.simbrain.util.math.SimbrainMath.max
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.toHSB
import java.awt.Color
import kotlin.math.min

/**
 * Manages the colors of datapoints in a [DataPoint]]. Most subclasses maintain a mapping from datapoints to values
 * which are then mapped to colors.
 */
abstract class ColoringManager: CopyableObject {

    /**
     * Gets the color associated with a datapoint. This is called once per datapoint every iteration so avoid
     * computationally intensive code.
     */
    context(Projector)
    abstract fun getColor(dataPoint: DataPoint): Color?

    context(Projector)
    abstract fun getActivation(dataPoint: DataPoint): Double

    /**
     * Sets this point as the "active" point, i.e the [Dataset.currentPoint].
     */
    abstract fun activate(dataPoint: DataPoint)

    /**
     * Update colors associated with all points in the [Dataset]
     */
    abstract fun updateAllColors()

    abstract fun reset()

    override fun getTypeList() = coloringManagerTypes

}

val coloringManagerTypes = listOf(
    NoOpColoringManager::class.java,
    DecayColoringManager::class.java,
    FrequencyColoringManager::class.java,
    MarkovColoringManager::class.java,
    HaloColoringManager::class.java
)

/**
 * "Null" coloring manager for when we don't use colors.
 */
class NoOpColoringManager: ColoringManager() {


    context(Projector)
    override fun getColor(dataPoint: DataPoint): Color? {
        return null
    }

    override fun getActivation(dataPoint: DataPoint): Double = 0.0

    override fun activate(dataPoint: DataPoint) {
    }

    override fun updateAllColors() {
    }

    override fun reset() {
    }

    override fun copy(): NoOpColoringManager {
        return NoOpColoringManager()
    }

    override val name = "None"

}


/**
 * When activated a color goes to [Projector.hotColor] then decays to [Projector.baseColor] in a set number of steps.
 */
class DecayColoringManager: ColoringManager() {

    @UserParameter(label = "Steps", description = "Steps to base color", minimumValue = 0.0)
    var stepsToBase = 100
        set(value) {
            if (field != value) {
                field = value
                isValuesToColorsDirty = true
                updateAllColors()
            }
        }

    var baseColor = Color.DARK_GRAY
        set(value) {
            if (field != value) {
                field = value
                isValuesToColorsDirty = true
                updateAllColors()
            }
        }

    var hotColor = Color.red
        set(value) {
            if (field != value) {
                field = value
                isValuesToColorsDirty = true
                updateAllColors()
            }
        }

    /**
     * A list of colors indexed by values.
     */
    lateinit var valuesToColors: List<Color>

    private var isValuesToColorsDirty = true

    context(Projector)
    fun initColors(): List<Color> {
        this@DecayColoringManager.baseColor = baseColor
        this@DecayColoringManager.hotColor = hotColor
        return HSBInterpolate(baseColor.toHSB(), hotColor.toHSB(), stepsToBase)
    }

    private val pointsToValues: MutableMap<DataPoint, Int> = HashMap()

    override fun activate(dataPoint: DataPoint) {
        pointsToValues[dataPoint] = stepsToBase - 1
    }

    context(Projector)
    override fun getColor(dataPoint: DataPoint): Color {
        if (isValuesToColorsDirty) {
            valuesToColors = initColors()
            isValuesToColorsDirty = false
        }
        val colorIndex = pointsToValues.getOrDefault(dataPoint, 0)
        return valuesToColors[colorIndex]
    }

    override fun getActivation(dataPoint: DataPoint) = TODO("not implemented")

    override fun updateAllColors() {
       pointsToValues.keys.forEach { dataPoint ->
           pointsToValues[dataPoint]?.let {
               if (it > 0) {
                   pointsToValues[dataPoint] = min(it - 1, stepsToBase - 1)
               }
           }
       }
    }

    override fun reset() {
        pointsToValues.clear()
    }

    override fun copy(): DecayColoringManager {
        return DecayColoringManager().also {
            it.stepsToBase = stepsToBase
        }
    }

    override val name = "DecayColoringManager"

}

/**
 * Colors points so that more frequently visited points are colored hotter.
 */
class FrequencyColoringManager: ColoringManager() {

    @UserParameter(label = "High frequency color", order = 10)
    var highFrequencyColor = Color.green

    private val visitCounts: MutableMap<DataPoint, Int> = HashMap()

    private var maxCount = 1

    override fun activate(dataPoint: DataPoint) {
        val count = visitCounts.getOrDefault(dataPoint, 0)
        visitCounts[dataPoint] = count + 1
        maxCount = max(maxCount, count)
    }

    // TODO: Cache hotcolor and bascolor
    context(Projector)
    override fun getColor(dataPoint: DataPoint): Color {
        val t = getActivation(dataPoint)
        return HSBInterpolate(baseColor.toHSB(), highFrequencyColor.toHSB(), t)
    }

    context(Projector)
    override fun getActivation(dataPoint: DataPoint) = (visitCounts[dataPoint] ?: 0).toDouble() / maxCount

    override fun updateAllColors() {
    }

    override fun reset() {
        maxCount = 1
        visitCounts.clear()
    }

    override fun copy() = FrequencyColoringManager()

    override val name = "FrequencyColoringManager"

}


class MarkovColoringManager: ColoringManager() {

    @UserParameter(label = "High frequency color", order = 10)
    var highFrequencyColor = Color.green

    private val transitionCounts: MutableMap<DataPoint, MutableMap<DataPoint, Int>> = HashMap()

    private var lastPoint: DataPoint? = null

    private var maxCounts: MutableMap<DataPoint, Int> = HashMap()

    override fun activate(dataPoint: DataPoint) {
        lastPoint?.let { prev ->
            transitionCounts.getOrPut(prev) {
                mutableMapOf(dataPoint to 0)
            }
            val count = transitionCounts[prev]!![dataPoint] ?: 0
            transitionCounts[prev]!![dataPoint] = count + 1
            maxCounts[prev] = max(maxCounts[prev]?:0, count)
        }
        lastPoint = dataPoint
    }

    context(Projector)
    override fun getColor(dataPoint: DataPoint): Color {
        val t = getActivation(dataPoint)
        return HSBInterpolate(baseColor.toHSB(), highFrequencyColor.toHSB(), t)
    }

    context(Projector)
    override fun getActivation(dataPoint: DataPoint): Double {
        val currentPoint = dataset.currentPoint
        return (transitionCounts[currentPoint]?.get(dataPoint) ?: 0).toDouble() / (maxCounts[currentPoint] ?: 1).coerceAtLeast(1)
    }

    override fun updateAllColors() {
    }

    override fun reset() {
        maxCounts.clear()
        transitionCounts.clear()
        lastPoint = null
    }

    override fun copy() = MarkovColoringManager()

    override val name = "MarkovColoringManager"

}

class HaloColoringManager: ColoringManager() {

    @UserParameter(label = "Radius", description = "Radius of the halo", minimumValue = 0.0)
    var radius = 0.2
        set(value) {
            field = value
            updateAllColors()
        }

    var useCustomCenter = false

    private var center: DataPoint? = null

    var customCenter
        get() = center
        set(value) {
            useCustomCenter = true
            center = value
        }

    override fun activate(dataPoint: DataPoint) {
        if (!useCustomCenter) {
            center = dataPoint
        }
    }

    context(Projector)
    override fun getColor(dataPoint: DataPoint): Color {
        val t = getActivation(dataPoint)
        return HSBInterpolate(hotColor.toHSB(), baseColor.toHSB(), t)
    }

    override fun getActivation(dataPoint: DataPoint) = center?.let { target ->
        val distance = dataPoint.euclideanDistance(target)
        (distance / radius).coerceIn(0.0, 1.0)
    } ?: 0.0

    override fun updateAllColors() {
    }

    override fun reset() {

    }

    override fun copy(): HaloColoringManager {
        return HaloColoringManager().also {
            it.radius = radius
        }
    }

    override val name = "HaloColoringManager"

}