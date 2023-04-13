package org.simbrain.util.projection

import org.simbrain.util.propertyeditor.CopyableObject

/**
 * A method for projecting a set of high-dimensional or "upstairs" points in a [Dataset2] object
 * with a downstairs point. That is, a dimensionality reduction technique.
 */
abstract class ProjectionMethod2() : CopyableObject {

    /**
     * Initialize (or re-initialize) the projection method, e.g. when changing the combo box from one type of
     * projection method to another. For some projection methods this involves a re-projection of every point from
     * upstairs to downstairs.
     */
    abstract fun init(dataset: Dataset2)

    /**
     * Called when adding a new datapoint to a dataset.
     */
    abstract fun addPoint(dataset: Dataset2, point: DataPoint2)

    abstract override fun copy(): ProjectionMethod2

    /**
     * Note that this is used by the combo box, so be careful about changing it.
     */
    override fun toString(): String {
        return name
    }

    companion object {

        @JvmStatic
        fun getTypes() = listOf(
            CoordinateProjection2::class.java,
            PCAProjection2::class.java,
            SammonProjection2::class.java,
            TriangulateProjection2::class.java,
            TSNEProjection::class.java
        )
    }

}