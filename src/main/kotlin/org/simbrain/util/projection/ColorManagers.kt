package org.simbrain.util.projection

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import java.awt.Color

abstract class ColoringManager: CopyableObject {

    abstract var projector: Projector2?

    abstract fun bumpColor(dataPoint: DataPoint2)

    abstract fun getColor(dataPoint: DataPoint2): Color

    abstract fun updateAllColors()

    companion object {

        /**
         * Decay functions for drop-down list used by [org.simbrain.util.propertyeditor.ObjectTypeEditor] to set a
         * type of probability distribution.
         */
        @JvmStatic
        fun getTypes() = listOf(
            DecayColoringManager::class.java
        )
    }

}

class DecayColoringManager: ColoringManager() {

    override var projector: Projector2? = null
        set(value) {
            field = value
            colors = initColors()
        }

    @UserParameter(label = "Steps", description = "Steps to base color", minimumValue = 0.0)
    var stepsToBase = 100
        set(value) {
            field = value
            colors = initColors()
        }

    var colors = initColors()

    fun initColors() = projector?.let {
        HSBInterpolate(it.baseColor.toHSB(), it.hotColor.toHSB(), stepsToBase)
    } ?: listOf()

    private var dataPointSteps: MutableMap<DataPoint2, Int> = HashMap()

    override fun bumpColor(dataPoint: DataPoint2) {
        dataPointSteps[dataPoint] = stepsToBase - 1
    }

    override fun getColor(dataPoint: DataPoint2): Color {
        val colorIndex = dataPointSteps.getOrDefault(dataPoint, 0)
        return colors[colorIndex]
    }

    override fun updateAllColors() {
       dataPointSteps.keys.forEach { dataPoint ->
           dataPointSteps[dataPoint]?.let {
               if (it > 0) {
                   dataPointSteps[dataPoint] = it - 1
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

        /**
         * Decay functions for drop-down list used by [org.simbrain.util.propertyeditor.ObjectTypeEditor] to set a
         * type of probability distribution.
         */
        @JvmStatic
        fun getTypes() = ColoringManager.getTypes()
    }
}

fun Color.toHSB() = FloatArray(3) { 0.0f }.let { Color.RGBtoHSB(red, green, blue, it) }

fun HSBInterpolate(fromColor: FloatArray, toColor: FloatArray, steps: Int): List<Color> {
    val difference = toColor - fromColor
    // hue is a flattened circle of length 1
    if (difference[0] > 0.5) {
        difference[0] = difference[0] - 1
    } else if (difference[0] < -0.5) {
        difference[0] = difference[0] + 1
    }
    val (h, s, b) = fromColor
    val (dh, ds, db) = difference / steps.toFloat()
    return (0 until steps).map {
        Color.getHSBColor((1 + h + it * dh) % 1.0f, s + it * ds, b + it * db)
    }
}

operator fun FloatArray.minus(other: FloatArray) = (this zip other).map { (a, b) -> a - b }.toFloatArray()

operator fun FloatArray.div(scalar: Float) = map { it / scalar }.toFloatArray()