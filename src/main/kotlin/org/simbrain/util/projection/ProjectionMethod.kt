package org.simbrain.util.projection

import org.simbrain.util.propertyeditor.CopyableObject

/**
 * A method for projecting a set of high-dimensional or "upstairs" points in a [Dataset] object
 * with a downstairs point. That is, a dimensionality reduction technique.
 */
abstract class ProjectionMethod() : CopyableObject {

    /**
     * Initialize (or re-initialize) the projection method, e.g. when changing the combo box from one type of
     * projection method to another. For some projection methods this involves a re-projection of every point from
     * upstairs to downstairs.
     */
    abstract fun init(dataset: Dataset)

    /**
     * Called when adding a new datapoint to a dataset.
     */
    abstract fun addPoint(dataset: Dataset, point: DataPoint)

    abstract override fun copy(): ProjectionMethod

    /**
     * Note that this is used by the combo box, so be careful about changing it.
     */
    override fun toString(): String {
        return name
    }

    override fun getTypeList() = projectionTypes

    companion object {

        @JvmStatic
        fun getTypes() = listOf(
            CoordinateProjection::class.java,
            PCAProjection::class.java,
            SammonProjection::class.java,
            TriangulateProjection::class.java,
            TSNEProjection::class.java
        )
    }

}

val projectionTypes = listOf(
    CoordinateProjection::class.java,
    PCAProjection::class.java,
    SammonProjection::class.java,
    TriangulateProjection::class.java,
    TSNEProjection::class.java
)