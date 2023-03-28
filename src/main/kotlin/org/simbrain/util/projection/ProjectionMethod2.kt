package org.simbrain.util.projection

import org.simbrain.util.propertyeditor.CopyableObject

abstract class ProjectionMethod2(): CopyableObject {

    abstract fun project(dataset: Dataset2)

    abstract fun initializeDownstairsPoint(dataset: Dataset2, point: DataPoint2)

    abstract override fun copy(): ProjectionMethod2

    override fun toString(): String {
        return name
    }

    companion object {

        /**
         * Decay functions for drop-down list used by [org.simbrain.util.propertyeditor.ObjectTypeEditor] to set a
         * type of probability distribution.
         */
        @JvmStatic
        fun getTypes() = listOf(
                CoordinateProjection2::class.java,
                PCAProjection2::class.java,
                SammonProjection2::class.java,
                TriangulateProjection2::class.java
            )
    }

}