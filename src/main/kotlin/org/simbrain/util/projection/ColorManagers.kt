package org.simbrain.util.projection

import org.simbrain.util.HSBInterpolate
import org.simbrain.util.UserParameter
import org.simbrain.util.math.SimbrainMath.max
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.toHSB
import java.awt.Color
import kotlin.math.min

/**
 * Manages the colors of datapoints in a [DataPoint2]]. Most subclasses maintain a mapping from datapoints to values
 * which are then mapped to colors.
 */
abstract class ColoringManager(open var projector: Projector2? = null): CopyableObject {

    /**
     * Gets the color associated with a datapoint.
     */
    abstract fun getColor(dataPoint: DataPoint2): Color?

    /**
     * Sets this point as the "active" point, i.e the [Dataset2.currentPoint].
     */
    abstract fun activate(dataPoint: DataPoint2)

    /**
     * Update colors associated with all points in the [Dataset2]
     */
    abstract fun updateAllColors()

    abstract fun reset()

    companion object {

        @JvmStatic
        fun getTypes() = listOf(
            NoOpColoringManager::class.java,
            DecayColoringManager::class.java,
            FrequencyColoringManager::class.java,
            MarkovColoringManager::class.java
        )
    }

}

/**
 * "Null" coloring manager for when we don't use colors.
 */
class NoOpColoringManager @JvmOverloads constructor(projector: Projector2? = null): ColoringManager(projector) {


    override fun getColor(dataPoint: DataPoint2): Color? {
        return null
    }

    override fun activate(dataPoint: DataPoint2) {
    }

    override fun updateAllColors() {
    }

    override fun reset() {

    }

    override fun copy(): NoOpColoringManager {
        return NoOpColoringManager()
    }

    override val name = "None"

    companion object {
        @JvmStatic
        fun getTypes() = ColoringManager.getTypes()
    }
}


/**
 * When activated a color goes to [Projector2.hotColor] then decays to [Projector2.baseColor] in a set number of steps.
 */
class DecayColoringManager @JvmOverloads constructor(projector: Projector2? = null): ColoringManager(projector) {

    override var projector: Projector2? = projector
        set(value) {
            field = value
            valuesToColors = initColors()
        }

    @UserParameter(label = "Steps", description = "Steps to base color", useSetter = true, minimumValue = 0.0)
    var stepsToBase = 100
        set(value) {
            field = value
            valuesToColors = initColors()
            updateAllColors()
        }

    /**
     * A list of colors indexed by values.
     */
    var valuesToColors = initColors()

    fun initColors() = projector?.let {
        HSBInterpolate(it.baseColor.toHSB(), it.hotColor.toHSB(), stepsToBase)
    } ?: listOf()

    private val pointsToValues: MutableMap<DataPoint2, Int> = HashMap()

    override fun activate(dataPoint: DataPoint2) {
        pointsToValues[dataPoint] = stepsToBase - 1
    }

    override fun getColor(dataPoint: DataPoint2): Color {
        val colorIndex = pointsToValues.getOrDefault(dataPoint, 0)
        return valuesToColors[colorIndex]
    }

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
            it.projector = projector
            it.stepsToBase = stepsToBase
        }
    }

    override val name = "DecayColoringManager"

    companion object {

        @JvmStatic
        fun getTypes() = ColoringManager.getTypes()
    }
}

/**
 * Colors points so that more frequently visited points are colored hotter.
 */
class FrequencyColoringManager @JvmOverloads constructor(projector: Projector2? = null): ColoringManager(projector) {

    @UserParameter(label = "High frequency color", order = 10)
    var highFrequencyColor = Color.green

    private val visitCounts: MutableMap<DataPoint2, Int> = HashMap()

    private var maxCount = 1

    override fun activate(dataPoint: DataPoint2) {
        val count = visitCounts.getOrDefault(dataPoint, 0)
        visitCounts[dataPoint] = count + 1
        maxCount = max(maxCount, count)
    }

    // TODO: Cache hotcolor and bascolor
    override fun getColor(dataPoint: DataPoint2): Color {
        val t = (visitCounts[dataPoint] ?: 0).toDouble() / maxCount
        return HSBInterpolate(projector!!.baseColor.toHSB(), highFrequencyColor.toHSB(), t)
    }

    override fun updateAllColors() {
    }

    override fun reset() {
        maxCount = 1
        visitCounts.clear()
    }

    override fun copy() = FrequencyColoringManager()

    override val name = "FrequencyColoringManager"

    companion object {

        @JvmStatic
        fun getTypes() = ColoringManager.getTypes()
    }
}


class MarkovColoringManager @JvmOverloads constructor(projector: Projector2? = null): ColoringManager(projector) {

    @UserParameter(label = "High frequency color", order = 10)
    var highFrequencyColor = Color.green

    private val transitionCounts: MutableMap<DataPoint2, MutableMap<DataPoint2, Int>> = HashMap()

    private var lastPoint: DataPoint2? = null

    private var maxCounts: MutableMap<DataPoint2, Int> = HashMap()

    override fun activate(dataPoint: DataPoint2) {
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

    override fun getColor(dataPoint: DataPoint2): Color {
        val currentPoint = projector!!.dataset.currentPoint
        val t = (transitionCounts[currentPoint]?.get(dataPoint) ?: 0).toDouble() / (maxCounts[currentPoint] ?: 1)
        return HSBInterpolate(projector!!.baseColor.toHSB(), highFrequencyColor.toHSB(), t)
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

    companion object {

        @JvmStatic
        fun getTypes() = ColoringManager.getTypes()
    }
}