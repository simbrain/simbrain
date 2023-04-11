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
abstract class ColoringManager: CopyableObject {

    abstract var projector: Projector2?

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

    companion object {

        @JvmStatic
        fun getTypes() = listOf(
            NoOpColoringManager::class.java,
            DecayColoringManager::class.java,
            FrequencyColoringManager::class.java
        )
    }

}

/**
 * "Null" coloring manager for when we don't use colors.
 */
class NoOpColoringManager: ColoringManager() {

    override var projector: Projector2? = null

    override fun getColor(dataPoint: DataPoint2): Color? {
        return null
    }

    override fun activate(dataPoint: DataPoint2) {
    }

    override fun updateAllColors() {
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
class DecayColoringManager: ColoringManager() {

    override var projector: Projector2? = null
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

    private var pointsToValues: MutableMap<DataPoint2, Int> = HashMap()

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
class FrequencyColoringManager: ColoringManager() {

    override var projector: Projector2? = null

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
        return HSBInterpolate(projector!!.baseColor.toHSB(), projector!!.hotColor.toHSB(), t)
    }

    override fun updateAllColors() {
    }

    override fun copy() = FrequencyColoringManager()

    override val name = "FrequencyColoringManager"

    companion object {

        @JvmStatic
        fun getTypes() = ColoringManager.getTypes()
    }
}