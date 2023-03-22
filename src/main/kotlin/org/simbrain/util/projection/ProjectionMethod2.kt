package org.simbrain.util.projection

import org.simbrain.util.propertyeditor.CopyableObject

abstract class ProjectionMethod2(val dimension: Int): CopyableObject {

    abstract fun project(dataset: Dataset2)

    abstract fun initializeDownstairsPoint(dataset: Dataset2, point: DataPoint2)

    companion object {

        /**
         * Decay functions for drop-down list used by [org.simbrain.util.propertyeditor.ObjectTypeEditor] to set a
         * type of probability distribution.
         */
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return listOf(
                CoordinateProjection2::class.java,
                PCAProjection2::class.java,
                SammonProjection2::class.java,
            )
        }
    }

}